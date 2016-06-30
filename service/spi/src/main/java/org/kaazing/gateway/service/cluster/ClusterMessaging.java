/**
 * Copyright 2007-2016, Kaazing Corporation. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.kaazing.gateway.service.cluster;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IQueue;
import com.hazelcast.core.ITopic;
import com.hazelcast.core.MessageListener;
import org.kaazing.gateway.util.scheduler.SchedulerProvider;

public class ClusterMessaging {

    private final String localTopicName;
    private final AtomicInteger nonce;
    private final Map<Class<?>, ReceiveListener<?>> receiveListeners;
    private final Map<Integer, SendListener> sendListeners;

    private final ScheduledExecutorService scheduler;
    private final ClusterContext clusterContext;
    private final HazelcastInstance cluster;

    private int syncTimeout = 1000 * 10;

    public ClusterMessaging(ClusterContext clusterContext, HazelcastInstance clusterInstance, SchedulerProvider schedulerProvider) {
        this.clusterContext = clusterContext;
        this.cluster = clusterInstance;

        localTopicName = getLocalTopicName(clusterContext);
        nonce = new AtomicInteger(1);
        receiveListeners = new HashMap<>();
        sendListeners = new HashMap<>();

        scheduler = schedulerProvider.getScheduler("clusterMessaging", true);

        init();
    }

    private int nextId() {
        return nonce.incrementAndGet();
    }

    private static String getTopicName(MemberId member) {
        return member.getId() + ":com";
    }

    // TODO: add a map here so we don't pay the cost of topic name creation and lookup
    private <T> ITopic<T> getTopic(MemberId member) {
        String topicName = getTopicName(member);
        ITopic<T> topic = cluster.getTopic(topicName);
        return topic;
    }

    private static String getLocalTopicName(ClusterContext clusterContext) {
        MemberId localMember = clusterContext.getLocalMember();
        String topicName = getTopicName(localMember);
        return topicName;
    }

    private void init() {
        // register default topic endpoint for direct messages
        addReceiveTopic(getLocalTopicName(clusterContext));
    }
    
    public void addReceiveQueue(final String name) {
        // here we want not only new messages that come into the queue, but also
        // messages that are already in the queue. So we create a thread with a take 
        // loop
        new Thread() {
            @Override
            public void run() {
                IQueue<Message> queue = clusterContext.getCollectionsFactory().getQueue(name);
                while (true) {
                    try {
                        Message msg = queue.take();
                        receiveMessage(msg);
                    }
                    catch (Exception e) {
                    }
                }
            }
        }.start();
    }

    public void addReceiveTopic(String name) {
        // here we only care about new messages that come over the topic so we can
        // just add a message listener
        ITopic<Message> topic = clusterContext.getCollectionsFactory().getTopic(name);
        topic.addMessageListener(new MessageListener<Message>() {
            @Override
            public void onMessage(Message msg) {
               receiveMessage(msg);
            }
        });
    }
    
    private void receiveMessage(Message msg) {
        Object payload = msg.getPayload();
        if (msg instanceof Request) {
            Request req = (Request)msg;
            String replyToName = req.getReplyTo();
            ITopic<Response> replyToTopic = cluster.getTopic(replyToName);
            ReceiveListener<?> receiveListener = receiveListeners.get(payload.getClass());
            if (receiveListener != null) {
                Object resPayload;
                try {
                    resPayload = receiveHelper(receiveListener, payload);
                }
                catch (Exception e) {
                    ErrorResponse res = new ErrorResponse(nextId(), req.getId());
                    res.setPayload(e);
                    replyToTopic.publish(res);
                    return;
                }
                Response res = new Response(nextId(), req.getId());
                res.setPayload(resPayload);
                replyToTopic.publish(res);
            }
            else {
                // no receiver found, ignore message
            }
        }
        else if (msg instanceof Response) {
            Response res = (Response)msg;
            SendListener sendListener = sendListeners.remove(res.getResponseTo());
            if (res instanceof ErrorResponse) {
                Exception exception = null;
                if (payload instanceof Exception) {
                    exception = (Exception)payload;
                }
                sendListener.onException(exception);
            }
            else {
                sendListener.onResponse(payload);
            }
        }
        else {
            // message type not supported, ignore
        }
    }
    
    // we need this helper to capture type T to cast the message to before calling onRecieve, thanks java.
    @SuppressWarnings("unchecked")
    private <T> Object receiveHelper(ReceiveListener<T> receiveListener, Object msg) throws Exception {
        return receiveListener.onReceive((T)msg);
    }

    public void destroy() {
        // TODO: this would cleanup resources like removing the topic listener
    }

    public static class Message implements Serializable {
        private static final long serialVersionUID = 1L;
        private final int id;
        private Object payload;

        public Message(int id) {
            this.id = id;
        }

        public int getId() {
            return id;
        }

        public void setPayload(Object payload) {
            this.payload = payload;
        }

        public Object getPayload() {
            return payload;
        }
    }

    public static class Request extends Message {
        private static final long serialVersionUID = 1L;
        private String replyTo;

        public Request(int id) {
            super(id);
        }

        public void setReplyTo(String replyTo) {
            this.replyTo = replyTo;
        }

        public String getReplyTo() {
            return replyTo;
        }
    }

    public static class Response extends Message {
        private static final long serialVersionUID = 1L;
        private int responseTo;

        public Response(int id, int responseTo) {
            super(id);
            this.responseTo = responseTo;
        }

        public int getResponseTo() {
            return responseTo;
        }
    }

    public static class ErrorResponse extends Response {
        private static final long serialVersionUID = 1L;

        public ErrorResponse(int id, int responseTo) {
            super(id, responseTo);
        }
    }

    public Object send(Object msg, MemberId member) throws Exception {
        final CountDownLatch latch = new CountDownLatch(1);
        final Object[] ptr = new Object[1];

        send(msg, new SendListener() {
            @Override
            public void onException(Exception e) {
                ptr[0] = e;
                latch.countDown();
            }

            @Override
            public void onResponse(Object msg) {
                ptr[0] = msg;
                latch.countDown();
            }
        }, member);

        // no need to timeout, as there is a timeout already on the send
        latch.await();

        if (ptr[0] instanceof Exception) {
            throw new Exception((Exception)ptr[0]);
        }
        else {
            return ptr[0];
        }
    }
    
    // TODO: should be able to factor out the common elements of this and the other send method
    public Object send(Object msg, String name) throws Exception {
        final CountDownLatch latch = new CountDownLatch(1);
        final Object[] ptr = new Object[1];

        send(msg, new SendListener() {
            @Override
            public void onException(Exception e) {
                ptr[0] = e;
                latch.countDown();
            }

            @Override
            public void onResponse(Object msg) {
                ptr[0] = msg;
                latch.countDown();
            }
        }, name);

        // no need to timeout, as there is a timeout already on the send
        latch.await();

        if (ptr[0] instanceof Exception) {
            throw new Exception((Exception)ptr[0]);
        }
        else {
            return ptr[0];
        }
    }

    public void send(Object msg, final SendListener listener, MemberId member) {
        ITopic<Request> receipient = getTopic(member);
        Request req = createRequest(msg, listener);
        receipient.publish(req);
    }
    
    public void send(Object msg, final SendListener listener, String name) {
        IQueue<Object> receipient = clusterContext.getCollectionsFactory().getQueue(name);
        Request req = createRequest(msg, listener);
        receipient.add(req);
    }

    private Request createRequest(Object msg, final SendListener listener) {
        final Request req = new Request(nextId());
        req.setPayload(msg);
        req.setReplyTo(localTopicName);
        
        // schedule timeout
        final ScheduledFuture<?> timeoutFuture = scheduler.schedule(new Runnable() {
            @Override
            public void run() {
                SendListener sendListener = sendListeners.remove(req.getId());
                if (sendListener != null) {
                    sendListener.onException(new Exception("Request timed out"));
                }
            }
        }, syncTimeout, TimeUnit.MILLISECONDS);

        // send listener
        SendListener sendListener = new SendListener() {
            @Override
            public void onException(Exception e) {
                if (timeoutFuture.cancel(false)) {
                    listener.onException(e);
                }
                else {
                    // race condition with the listener, do nothing
                }
            }

            @Override
            public void onResponse(Object msg) {
                if (timeoutFuture.cancel(false)) {
                    listener.onResponse(msg);
                }
                else {
                    // race condition with the listener, do nothing
                }
            }
        };

        sendListeners.put(req.getId(), sendListener);
        return req;
    }

    public <T> void setReceiver(Class<T> type, ReceiveListener<T> receiveListener) {
        receiveListeners.put(type, receiveListener);
    }
    
    public <T> void removeReceiver(Class<T> type) {
        receiveListeners.remove(type);
    }
}

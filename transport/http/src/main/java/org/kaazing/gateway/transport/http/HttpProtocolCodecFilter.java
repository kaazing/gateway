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
package org.kaazing.gateway.transport.http;

import org.apache.mina.core.future.IoFuture;
import org.apache.mina.core.future.IoFutureListener;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.core.write.WriteRequest;
import org.apache.mina.filter.codec.ProtocolCodecFactory;
import org.apache.mina.filter.codec.ProtocolDecoder;
import org.apache.mina.filter.codec.ProtocolEncoder;
import org.kaazing.gateway.resource.address.ResourceAddress;
import org.kaazing.gateway.transport.BridgeSession;
import org.kaazing.gateway.transport.ProtocolCodecFilter;
import org.kaazing.gateway.transport.bridge.Message;
import org.kaazing.mina.core.buffer.IoBufferEx;

//TODO: Lift this logic to make it generic at the protocol codec filter with Message objects
public class HttpProtocolCodecFilter extends ProtocolCodecFilter {
    public HttpProtocolCodecFilter(ProtocolCodecFactory factory) {
        super(factory);
    }

    public HttpProtocolCodecFilter(ProtocolEncoder encoder,
                                   ProtocolDecoder decoder) {
        super(encoder, decoder);
    }

    public HttpProtocolCodecFilter(Class<? extends ProtocolEncoder> encoderClass,
                                   Class<? extends ProtocolDecoder> decoderClass) {
        super(encoderClass, decoderClass);
    }


    @Override
    public void filterWrite(NextFilter nextFilter,
                            IoSession session,
                            WriteRequest writeRequest) throws Exception {

        // this logic (an empty message in the message cache) is currently only used to close out an
        // httpxe write request when it has been copied down to the http session already -
        // we need to close out the higher level write without really encoding anything.
        if (writeRequest.getMessage() instanceof  Message) {
            Message message = (Message) writeRequest.getMessage();
            String nextProtocol = BridgeSession.LOCAL_ADDRESS.get(session).getOption(ResourceAddress.NEXT_PROTOCOL);
            if (message.hasCache()) {
                IoBufferEx cachedProtocolBuffer = message.getCache().get(nextProtocol);
                if (cachedProtocolBuffer != null) {
                    if(cachedProtocolBuffer.capacity()==0) {
                        // We must commit this http session if the child is committing but not closing, to ensure
                        // the http headers get written immediately (before data is sent or the session is closed)
                        HttpSession child = HttpAcceptor.SESSION_KEY.get(session);
                        if (session instanceof HttpAcceptSession && child != null && child.isCommitting()
                                && !child.isClosing()) {
                            ((HttpAcceptSession)session).commit().addListener(
                                    new IoFutureListener<IoFuture>() {
                                        @Override
                                        public void operationComplete(IoFuture future) {
                                            writeRequest.getFuture().setWritten();
                                            nextFilter.messageSent(session, writeRequest);
                                        }
                                    });
                        }
                        else {
                            writeRequest.getFuture().setWritten();
                            nextFilter.messageSent(session, writeRequest);
                        }
                        return;
                    }
                 }
            }
        }
        super.filterWrite(nextFilter,session,writeRequest);

    }
}

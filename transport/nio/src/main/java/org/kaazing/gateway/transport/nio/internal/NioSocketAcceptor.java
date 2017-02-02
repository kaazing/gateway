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
package org.kaazing.gateway.transport.nio.internal;

import static java.util.concurrent.Executors.newCachedThreadPool;
import static org.kaazing.gateway.util.InternalSystemProperty.DEBUG_NIOWORKER_POOL;
import static org.kaazing.gateway.util.InternalSystemProperty.TCP_BACKLOG;
import static org.kaazing.gateway.util.InternalSystemProperty.TCP_IP_TOS;
import static org.kaazing.gateway.util.InternalSystemProperty.TCP_KEEP_ALIVE;
import static org.kaazing.gateway.util.InternalSystemProperty.TCP_MAXIMUM_READ_BUFFER_SIZE;
import static org.kaazing.gateway.util.InternalSystemProperty.TCP_MINIMUM_READ_BUFFER_SIZE;
import static org.kaazing.gateway.util.InternalSystemProperty.TCP_NO_DELAY;
import static org.kaazing.gateway.util.InternalSystemProperty.TCP_PROCESSOR_COUNT;
import static org.kaazing.gateway.util.InternalSystemProperty.TCP_READ_BUFFER_SIZE;
import static org.kaazing.gateway.util.InternalSystemProperty.TCP_RECEIVE_BUFFER_SIZE;
import static org.kaazing.gateway.util.InternalSystemProperty.TCP_REUSE_ADDRESS;
import static org.kaazing.gateway.util.InternalSystemProperty.TCP_SEND_BUFFER_SIZE;
import static org.kaazing.gateway.util.InternalSystemProperty.TCP_SO_LINGER;
import static org.kaazing.gateway.util.InternalSystemProperty.TCP_WRITE_TIMEOUT;

import java.util.Collection;
import java.util.Properties;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.mina.core.future.IoFuture;
import org.apache.mina.core.service.IoHandler;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.core.session.IoSessionInitializer;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChildChannelStateEvent;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.SimpleChannelUpstreamHandler;
import org.jboss.netty.channel.socket.Worker;
import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory;
import org.jboss.netty.channel.socket.nio.NioSocketChannel;
import org.jboss.netty.channel.socket.nio.NioWorker;
import org.jboss.netty.channel.socket.nio.WorkerPool;
import org.jboss.netty.util.ExternalResourceReleasable;
import org.jboss.netty.util.internal.ExecutorUtil;
import org.kaazing.gateway.resource.address.Protocol;
import org.kaazing.gateway.resource.address.ResourceAddress;
import org.kaazing.gateway.transport.BridgeSessionInitializer;
import org.kaazing.gateway.transport.NioBindException;
import org.kaazing.gateway.transport.nio.TcpExtension;
import org.kaazing.mina.core.service.IoAcceptorEx;
import org.kaazing.mina.netty.socket.nio.DefaultNioSocketChannelIoSessionConfig;
import org.kaazing.mina.netty.socket.nio.NioSocketChannelIoAcceptor;
import org.kaazing.mina.netty.util.threadlocal.VicariousThreadLocal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NioSocketAcceptor extends AbstractNioAcceptor {

    private static final String PROPERTY_NETTY_SELECT_TIMEOUT = "org.jboss.netty.selectTimeout";
    private static final long DEFAULT_SELECT_TIMEOUT_MILLIS = 10;
    private static final String LOGGER_NAME = String.format("transport.%s.accept", NioProtocol.TCP.name().toLowerCase());
    private static final Logger logger = LoggerFactory.getLogger(LOGGER_NAME);

    private final TcpExtensionFactory extensionFactory;

    static {
        // We must set the select timeout property before Netty class SelectorUtil gets loaded
        initSelectTimeout();
    }

    // Set select timeout to 10ms to mimick what we did in Mina mina-2.0.0-RC1f and later,
    // but also allow explicit setting of the Netty property (for internal testing purposes)
    static void initSelectTimeout() {
        String currentTimeout = System.getProperty(PROPERTY_NETTY_SELECT_TIMEOUT);
        if (currentTimeout == null || "".equals(currentTimeout)) {
            try {
                System.setProperty(PROPERTY_NETTY_SELECT_TIMEOUT, Long.toString(DEFAULT_SELECT_TIMEOUT_MILLIS));
            }
            catch (SecurityException e) {
                if (logger.isWarnEnabled()) {
                    logger.warn(String.format(
                            "Unable to set System property \"%s\" to %d due to %s, Gateway performance may be reduced",
                            PROPERTY_NETTY_SELECT_TIMEOUT, DEFAULT_SELECT_TIMEOUT_MILLIS, e));
                }
            }
        }
    }

    static final ThreadLocal<NioWorker> CURRENT_WORKER = new VicariousThreadLocal<>();

    private static final class SetCurrentWorkerTask implements Callable<NioWorker> {

        private final NioWorker worker;

        public SetCurrentWorkerTask(NioWorker worker) {
            this.worker = worker;
        }

        @Override
        public NioWorker call() throws Exception {
            CURRENT_WORKER.set(worker);
            return worker;
        }

    }

    private final AtomicReference<DistributedNioWorkerPool> currentWorkerPool = new AtomicReference<>();

    public NioSocketAcceptor(Properties configuration, TcpExtensionFactory extensionFactory) {
        super(configuration, LoggerFactory.getLogger(LOGGER_NAME));
        this.extensionFactory = extensionFactory;
    }

    NioSocketAcceptor(Properties configuration) {
        this(configuration, TcpExtensionFactory.newInstance());
    }

    @Override
    public void bind(final ResourceAddress address,
                     IoHandler handler,
                     BridgeSessionInitializer<? extends IoFuture> initializer) throws NioBindException {
        Collection<TcpExtension> extensions = extensionFactory.bind(address);
        BridgeSessionInitializer<? extends IoFuture> newInitializer = initializer;
        if (extensions.size() > 0) {
            newInitializer = new ExtensionsSessionInitializer(extensions, initializer);
        }
        super.bind(address, handler, newInitializer);
    }

    @Override
    public void dispose() {
        // Unset static state on the thread to allow gateway to be restarted in same thread
        currentWorkerPool.set(null);
        super.dispose();
    }

    @Override
    protected String getTransportName() {
        return "tcp";
    }

    @Override
    protected IoAcceptorEx initAcceptor(IoSessionInitializer<? extends IoFuture> initializer) {
        String readBufferSize = TCP_READ_BUFFER_SIZE.getProperty(configuration);
        String minimumReadBufferSize = TCP_MINIMUM_READ_BUFFER_SIZE.getProperty(configuration);
        String maximumReadBufferSize = TCP_MAXIMUM_READ_BUFFER_SIZE.getProperty(configuration);
        String writeTimeout = TCP_WRITE_TIMEOUT.getProperty(configuration);
        String keepAlive = TCP_KEEP_ALIVE.getProperty(configuration);
        boolean reuseAddress = Boolean.parseBoolean(TCP_REUSE_ADDRESS.getProperty(configuration));
        String tcpNoDelay = TCP_NO_DELAY.getProperty(configuration);
        String backlog = TCP_BACKLOG.getProperty(configuration);
        String receiveBufferSize = TCP_RECEIVE_BUFFER_SIZE.getProperty(configuration);
        String sendBufferSize = TCP_SEND_BUFFER_SIZE.getProperty(configuration);
        String linger = TCP_SO_LINGER.getProperty(configuration);
        String ipTypeOfService = TCP_IP_TOS.getProperty(configuration);

        NioSocketChannelIoAcceptor acceptor;

        WorkerPool<NioWorker> workerPool = initWorkerPool(logger, "TCP acceptor: {}", configuration);
		NioServerSocketChannelFactory serverChannelFactory = new NioServerSocketChannelFactory(
				Executors.newCachedThreadPool(),
				workerPool);
        acceptor = new NioSocketChannelIoAcceptor(new DefaultNioSocketChannelIoSessionConfig(),
                                                  serverChannelFactory,
                                                  new AffinityIoAcceptorChannelHandlerFactory());
    	acceptor.setIoSessionInitializer(initializer);

        // KG-8210: avoid hang on gateway shutdown, plus we don't want quiesce or unbind to disconnect everyone!
        acceptor.setCloseOnDeactivation(false);

        if ( "true".equals(keepAlive)) {
            acceptor.getSessionConfig().setKeepAlive(true);
            logger.debug("KEEP_ALIVE setting for TCP acceptor: {}", keepAlive);
        }

        if (writeTimeout != null) {
            acceptor.getSessionConfig().setWriteTimeout(Integer.parseInt(writeTimeout));
            logger.debug("WRITE_TIMEOUT setting for TCP acceptor: {}", writeTimeout);
        }
        else {
            acceptor.getSessionConfig().setWriteTimeout(0); // no timeout
        }

        if (tcpNoDelay != null) {
            acceptor.getSessionConfig().setTcpNoDelay(Boolean.parseBoolean(tcpNoDelay));
            logger.debug("TCP_NO_DELAY setting for TCP acceptor: {}", tcpNoDelay);
        }
        else {
            acceptor.getSessionConfig().setTcpNoDelay(true);
        }

        if (backlog != null) {
            acceptor.setBacklog(Integer.parseInt(backlog));
            logger.debug("BACKLOG setting for TCP acceptor: {}", backlog);
        }

        if (readBufferSize != null) {
            acceptor.getSessionConfig().setReadBufferSize(Integer.parseInt(readBufferSize));
            logger.debug("READ_BUFFER_SIZE setting for TCP acceptor: {}", readBufferSize);
        }

        if (minimumReadBufferSize != null) {
            acceptor.getSessionConfig().setMinReadBufferSize(Integer.parseInt(minimumReadBufferSize));
            logger.debug("MINIMUM_READ_BUFFER_SIZE setting for TCP acceptor: {}", minimumReadBufferSize);
        }

        if (maximumReadBufferSize != null) {
            acceptor.getSessionConfig().setMaxReadBufferSize(Integer.parseInt(maximumReadBufferSize));
            logger.debug("MAXIMUM_READ_BUFFER_SIZE setting for TCP acceptor: {}", maximumReadBufferSize);
        }

        if (receiveBufferSize != null) {
            acceptor.getSessionConfig().setReceiveBufferSize(Integer.parseInt(receiveBufferSize));
            logger.debug("SO RECEIVE BUFFER SIZE setting for TCP acceptor: {}", receiveBufferSize);
        }

        if (sendBufferSize != null) {
            acceptor.getSessionConfig().setSendBufferSize(Integer.parseInt(sendBufferSize));
            logger.debug("SO SEND BUFFER SIZE setting for TCP acceptor: {}", sendBufferSize);
        }

        if (linger != null) {
            acceptor.getSessionConfig().setSoLinger(Integer.parseInt(linger));
            logger.debug("SO Linger Interval for TCP acceptor: {}", linger);
        }

        if (ipTypeOfService != null) {
            acceptor.getSessionConfig().setTrafficClass(Integer.parseInt(ipTypeOfService));
            logger.debug("IP_TOS for TCP acceptor: {}", ipTypeOfService);
        }

        acceptor.setReuseAddress(reuseAddress);
        acceptor.getSessionConfig().setReuseAddress(reuseAddress);

        return acceptor;
    }


    public Worker[] getWorkers() {
        // KG-10074: if only non-TCP binds, worker pool may not yet be initialized (causing NPE below)
        initIfNecessary();
        return currentWorkerPool.get().workers;
    }

	WorkerPool<NioWorker> initWorkerPool(Logger logger, String message, Properties configuration) {
    	int workerCount = TCP_PROCESSOR_COUNT.getIntProperty(configuration);
        if (logger.isDebugEnabled()) {
            String processorCount = configuration.getProperty(TCP_PROCESSOR_COUNT.getPropertyName());
            if (processorCount != null) {
                logger.debug(message, String.format("worker count = %d, configured processor count = %s",
                         workerCount, processorCount));
            }
            else {
                logger.debug(message, "worker count = " + workerCount);
            }
        }
        DistributedNioWorkerPool workerPool = currentWorkerPool.get();
        if (workerPool == null) {
        	final boolean isDebug = "true".equals(DEBUG_NIOWORKER_POOL.getProperty(configuration));
        	if (isDebug) {
        		System.out.println("NioWorkerPool.DEBUG=true");
        	}
        	final ConcurrentMap<NioWorker, Thread> threadsByWorker = new ConcurrentHashMap<>();
        	workerPool = new DistributedNioWorkerPool(newCachedThreadPool(), workerCount) {
	        	@Override
	        	public NioWorker nextWorker() {
	        		NioWorker worker = CURRENT_WORKER.get();
	        		if (worker == null) {
	        			Thread currentThread = Thread.currentThread();
	        			String threadName = currentThread.getName();
	        			if (isDebug && !threadName.contains("boss")) {
	        				new Exception("Worker not found on non-Boss thread, outbound connect on non-IoThread?").fillInStackTrace().printStackTrace();
	        			}
	        			// No association for acceptor boss thread or out-bound connect on non-IoThread
	        			worker = super.nextWorker();
	        		}
	        		else if (isDebug) {
	        			// Connector connect thread has association from Acceptor worker thread
	        			assert worker != null;
	        			Thread currentThread = Thread.currentThread();
	        			String threadName = currentThread.getName();
	        			if (threadName.contains("boss")) {
	        				new Exception("Worker found unexpectedly on Boss thread").fillInStackTrace().printStackTrace();
	        			}
	        			Thread thread = threadsByWorker.get(worker);
	        			if (thread == null) {
	        				// remember association when first observed
							Thread newThread = currentThread;
	        				thread = threadsByWorker.putIfAbsent(worker, newThread);
	        				// handle race condition where 2 threads compete for association
	        				if (thread == null) {
	        					thread = newThread;
	        				}
	        			}

                        // verify association is consistent throughout
                        if (thread != currentThread) {
                            new Exception("Worker found unexpectedly aligned with different IoThread").fillInStackTrace().printStackTrace();
                        }
                    }

                    return worker;
                }
            };
            if (!currentWorkerPool.compareAndSet(null,  workerPool)) {
                // we lost the race with another thread doing acceptor.bind or connector.connect
                workerPool.releaseExternalResources();
                workerPool = currentWorkerPool.get();
            }
        }
        workerPool.incrementReferenceCount();
        return workerPool;
    }

	// distribute the connections evenly over the workers in the pool
	// based on the current number of active connections per worker
	// NOTE: this class needs to be thread safe because it can be called from multiple boss threads
    // when there is more than one bind
	private static class DistributedNioWorkerPool implements WorkerPool<NioWorker>, ExternalResourceReleasable {

		private final Executor workerExecutor;
		private final DistributedNioWorker[] workers;
		private final AtomicInteger referenceCount = new AtomicInteger(0);
	    private final AtomicInteger requestCount = new AtomicInteger(0);
		private final Logger logger = LoggerFactory.getLogger(LOGGER_NAME);

		public DistributedNioWorkerPool(Executor workerExecutor, int workerCount) {
			if (workerExecutor == null) {
				throw new NullPointerException("workerExecutor");
			}
			if (workerCount <= 0) {
				throw new IllegalArgumentException("workerCount (" + workerCount + ") must be a positive integer");
			}
			DistributedNioWorker[] workers = new DistributedNioWorker[workerCount];
			for (int i=0; i < workers.length; i++) {
				// we cannot allow shutdown on idle, otherwise worker may end up running on a different thread
				DistributedNioWorker worker = new DistributedNioWorker(workerExecutor);
				// we must set the current worker in the right thread local before any in-bound connections
				FutureTask<NioWorker> future = new FutureTask<>(new SetCurrentWorkerTask(worker));
				worker.executeInIoThread(future, /*alwaysAsync*/ true);
				try {
					NioWorker workerFromTask = future.get();
					assert (workerFromTask == worker);
				}
				catch (Exception e) {
					e.printStackTrace();
				}
				workers[i] = worker;
			}
			this.workers = workers;
			this.workerExecutor = workerExecutor;
		}

		public void incrementReferenceCount() {
		    referenceCount.incrementAndGet();
		}

		@Override
	    public void rebuildSelectors() {
	        for (DistributedNioWorker worker: workers) {
	            worker.rebuildSelector();
	        }
	    }

	    @Override
		public NioWorker nextWorker() {
			// break the tie differently on each call when connections are evenly distributed
            // Use round robin to guarantee we use each worker in turn when connections are evenly distributed.
            // This ensures all workers are used for backend connections from services doing connection fanout when
            // number of connections >= worker count.
		    int offset = requestCount.getAndIncrement() % workers.length;
			DistributedNioWorker available  = workers[offset];
			for (int i=offset + 1; i < workers.length; i++) {
				DistributedNioWorker worker = workers[i];
				int channelCount = available.channelCount.get();
				if (worker.channelCount.get() < channelCount) {
					available = worker;
				}
			}
			for (int i=0; i < offset; i++) {
				DistributedNioWorker worker = workers[i];
				int channelCount = available.channelCount.get();
				if (worker.channelCount.get() < channelCount) {
					available = worker;
				}
			}
            if (logger.isDebugEnabled()) {
                int used = 0;
                for (int i=0; i<workers.length; i++) {
                    if (workers[i]==available) {
                        used = i;
                        break;
                    }
                }
                logger.debug(String.format("nextWorker: returning worker #%d of %d", used+1, workers.length));
            }
			return available;
		}

	    @Override
	    public void releaseExternalResources() {
            // (KG-7319) Since NioSocketConnector and NioSocketAcceptor share the same worker pool we must only release
            // resources when there are no more active users. Note that during dispose, shutdown gets called (by Netty)
	        // followed by releaseExternalResources, so we only make the latter decrement the reference count.
            if (referenceCount.decrementAndGet() <= 0) {
	            shutdown();
	            ExecutorUtil.terminate(workerExecutor);
	        }
	    }

	    @Override
        public void shutdown() {
	        if (referenceCount.get() <= 0) {
    	        // (KG-5441) Must shutdown down workers so their threads can be stopped by the workerExecutor
    	        for (NioWorker worker: workers) {
    	            worker.shutdown();
    	        }
	        }
	    }

    }

    private static class DistributedNioWorker extends NioWorker {

        private final AtomicInteger channelCount;
        private final ChannelFutureListener closeListener;

        public DistributedNioWorker(Executor executor) {
            super(executor);

            this.channelCount = new AtomicInteger();
            this.closeListener = new ChannelFutureListener() {
                @Override
                public void operationComplete(ChannelFuture future) throws Exception {
                    // decrement the count for this channel's worker
                    channelCount.decrementAndGet();
                }
            };

        }

        public void childChannelOpen(NioSocketChannel childChannel) {
            // increment the channel count for this worker
            channelCount.incrementAndGet();
            // decrement the channel count for this worker on channel close
            childChannel.getCloseFuture().addListener(closeListener);
        }

    }

    private static class AffinityIoAcceptorChannelHandlerFactory extends SimpleChannelUpstreamHandler {

        private static final String MSG = "Exception caught in AffinityIoAcceptorChannelHandlerFactory.";

        @Override
        public void childChannelOpen(ChannelHandlerContext ctx, ChildChannelStateEvent e) throws Exception {
            NioSocketChannel childChannel = (NioSocketChannel) e.getChildChannel();
            DistributedNioWorker worker = (DistributedNioWorker) childChannel.getWorker();
            worker.childChannelOpen(childChannel);
            super.childChannelOpen(ctx, e);
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e) throws Exception {
            if (logger.isDebugEnabled()) {
                logger.warn(MSG, e.getCause());
            } else {
                logger.warn(MSG + e.getCause());
            }
        }
    }

    private static class ExtensionsSessionInitializer<T extends IoFuture> implements BridgeSessionInitializer<T> {
        private final Collection<TcpExtension> extensions;
        private final BridgeSessionInitializer<? extends IoFuture> wrapped;

        ExtensionsSessionInitializer(Collection<TcpExtension> extensions,
                                     BridgeSessionInitializer<? extends IoFuture> wrapped) {
            this.extensions = extensions;
            this.wrapped = wrapped;
        }

        @SuppressWarnings("unchecked")
        @Override
        public BridgeSessionInitializer<T> getParentInitializer(Protocol protocol) {
            return (BridgeSessionInitializer<T>) ((wrapped != null) ? wrapped.getParentInitializer(protocol) : null);
        }

        @SuppressWarnings("unchecked")
        @Override
        public void initializeSession(IoSession session, T future) {
            if ( wrapped != null ) {
                ((BridgeSessionInitializer<T>)wrapped).initializeSession(session,  future);
            }
            // Call extensions
            for (TcpExtension extension : extensions) {
                extension.initializeSession(session);
            }
        }

    }

}

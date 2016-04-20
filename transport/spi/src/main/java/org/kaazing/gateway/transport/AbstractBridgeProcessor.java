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
package org.kaazing.gateway.transport;

import static java.lang.String.format;

import java.net.SocketAddress;
import java.util.Collection;
import java.util.LinkedList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.mina.core.filterchain.IoFilterChain;
import org.apache.mina.core.future.IoFutureListener;
import org.apache.mina.core.future.WriteFuture;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.core.write.WriteRequest;
import org.apache.mina.core.write.WriteRequestQueue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.kaazing.mina.core.buffer.IoBufferAllocatorEx;
import org.kaazing.mina.core.buffer.IoBufferEx;
import org.kaazing.mina.core.service.AbstractIoProcessor;
import org.kaazing.mina.core.session.IoSessionEx;

public abstract class AbstractBridgeProcessor<T extends AbstractBridgeSession<?, ?>> extends AbstractIoProcessor<T> {

    private final ConcurrentHashMap<SocketAddress, IoSession> sessionMap;
	private final AtomicBoolean disposed;
    private static final Logger logger = LoggerFactory.getLogger(AbstractBridgeProcessor.class);

	public AbstractBridgeProcessor() {
		this.sessionMap = new ConcurrentHashMap<>();
		this.disposed = new AtomicBoolean();
	}

	@Override
	protected void add0(T session) {
		SocketAddress clientAddress = getUniqueAddress(session);
		sessionMap.putIfAbsent(clientAddress, session);
	}

    protected abstract SocketAddress getUniqueAddress(T session);

    public IoSession get(SocketAddress clientAddress) {
		return sessionMap.get(clientAddress);
    }

    @Override
    protected final void remove0(T session) {
        sessionMap.remove(getUniqueAddress(session));

        // KG-3458: must make sure parent.close gets done before fireSessionDestroyed to avoid possible hang which
        // can happen because fireSessionDestroyed for a wsnSession unbinds the HTTP accept for the revalidate handler.
        // If that's the last nio (tcp) level bind it will call IoServiceListenerSupport.fireServiceDeactivated which
        // cannot complete until all sessions are closed (including our parent session!)
        // do any internal remove handling
        try {
            removeInternal(session);
        } finally {
            doFireSessionDestroyed(session);
        }

    }

    protected void doFireSessionDestroyed(T session) {
        // TODO? look at write queue and get tail, get future and fire destroy when tail has been written
        //       or do fireSessionDestroyed from a listener on the parent session close future
        session.getService().getListeners().fireSessionDestroyed(session);
    }

    protected void removeInternal(T session) {
        IoSession parent = session.getParent();
        if (parent != null && !parent.isClosing()) {
        	parent.close(false);
        }
    }

    @Override
    protected final void flush0(T session) {
        flushInternal(session);
    }

    protected void flushInternal(T session) {
        IoSessionEx parent = session.getParent();
        if (parent == null) {
            return;
        }

        IoFilterChain filterChain = session.getFilterChain();
        WriteRequestQueue writeRequestQueue = session.getWriteRequestQueue();
        do {
            WriteRequest request = writeRequestQueue.poll(session);
            if (request == null) {
                break;
            }
            Object message = getMessageFromWriteRequest(session, request);
            if (message instanceof IoBufferEx) {
                IoBufferEx buf = (IoBufferEx) message;
                try {
                    // hold current remaining bytes so we know how much was written
                    int remaining = buf.remaining();

                    // drain the unwritten write requests to ensure that session.close(false)
                    // still triggers the session close future
                    if (parent.isClosing()) {
                        Collection<WriteRequest> unwritten = new LinkedList<>();
                        while (request != null) {
                            unwritten.add(request);
                            request = writeRequestQueue.poll(session);
                        }
                        // TODO: throw write to close session exception
                        // filterChain.fireExceptionCaught(new WriteToClosedSessionException(unwritten));
                        break;
                    }

                    // flush the buffer out to the parent
                    WriteFuture flushFuture = flushNow(session, parent, buf, filterChain, request);
                    if (flushFuture == null) {
                    	break;
                    }

                    // increment session written bytes
                    int written = remaining;

            		// increase is implemented in AbstractIoSession and not part of TransportSession or IoSession interface
                        if ( shouldAccountForWrittenBytes(session)) {
                            session.increaseWrittenBytes(written, System.currentTimeMillis());
                        }

                }
                catch (Exception e) {
                    request.getFuture().setException(e);
                    if (logger.isDebugEnabled()) {
                        logger.debug(format("Exception while writing message buffer '%s'.", buf), e);
                    } else {
                        logger.debug(format("Exception while writing message buffer '%s'.", buf));
                    }
                }
            }
            else {
                throw new IllegalStateException("Don't know how to handle message of type '" + message.getClass().getName() + "'.  Are you missing a protocol encoder?");
            }
        } while (true);
    }

    protected Object getMessageFromWriteRequest(T session, WriteRequest request) {
        return request.getMessage();
    }

    protected boolean shouldAccountForWrittenBytes(T session) {
        return true;
    }

	protected WriteFuture flushNow(T session, IoSessionEx parent, IoBufferEx buf, IoFilterChain filterChain, WriteRequest request) {
		IoBufferAllocatorEx<?> parentAllocator = parent.getBufferAllocator();
        IoBufferEx parentBuf = parentAllocator.wrap(buf.buf(), buf.flags());
	    return flushNowInternal(parent, parentBuf, buf, filterChain, request);
	}

	protected static WriteFuture flushNowInternal(IoSessionEx parent, Object message, IoBufferEx resetBuf, IoFilterChain filterChain, WriteRequest request) {
		WriteFuture parentFuture = parent.write(message);
		attachMessageSentInternal(filterChain, resetBuf, request, parentFuture);
		return parentFuture;
	}

	private static void setFutureWritten(IoFilterChain filterChain, WriteFuture future) {
        try {
            future.setWritten();
        } catch (Throwable t) {
            filterChain.fireExceptionCaught(t);
        }
	}

    private static void attachMessageSentInternal(final IoFilterChain filterChain, final IoBufferEx resetBuf, final WriteRequest request, WriteFuture future) {
        if (future.isDone()) {
			if (future.isWritten()) {
			    resetBuf.reset();
                // Complete the future without firing the (largely useless) messageSent event, to gain performance
			    setFutureWritten(filterChain, request.getFuture());
				//filterChain.fireMessageSent(request);
			} else {
				request.getFuture().setException(future.getException());
			}
		} else {
			future.addListener(new IoFutureListener<WriteFuture>() {
				@Override
				public void operationComplete(WriteFuture parentFuture) {
					if (parentFuture.isWritten()) {
		                resetBuf.reset();
                        // Complete the future without firing the (largely useless) messageSent event, to gain performance
		                setFutureWritten(filterChain, request.getFuture());
		                //filterChain.fireMessageSent(request);
					} else {
						request.getFuture().setException(parentFuture.getException());
					}
				}
			});
		}
    }

    @Override
	public void dispose() {
		if (!disposed.compareAndSet(false, true)) {
			throw new IllegalStateException("Already disposed");
		}
	}

	@Override
	public boolean isDisposed() {
		return disposed.get();
	}

	@Override
	public boolean isDisposing() {
		return disposed.get();
	}

	@Override
	protected final void updateTrafficControl0(T session) {

        if (!session.isWriteSuspended()) {
            flush(session);
        }

	    if (!session.isReadSuspended()) {
	        consume(session);
	    }

	}

	// opposite of flush that will be called when reads are resumed and should consume any pending reads
    protected void consume(T session) {
    }

}

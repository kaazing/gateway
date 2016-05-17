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
package org.kaazing.gateway.transport.pipe;

import java.util.Collection;
import java.util.LinkedList;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.filterchain.IoFilterChain;
import org.apache.mina.core.write.WriteRequest;
import org.apache.mina.core.write.WriteRequestQueue;
import org.apache.mina.core.write.WriteToClosedSessionException;
import org.kaazing.mina.core.service.AbstractIoProcessor;

public class NamedPipeProcessor extends AbstractIoProcessor<NamedPipeSession> {

	private volatile boolean disposing;

	@Override
	protected void add0(NamedPipeSession session) {
		// nothing to do
	}

	@Override
	protected void remove0(NamedPipeSession session) {
        session.getService().getListeners().fireSessionDestroyed(session);

        session.setRemoteSession(null);
	}

	@Override
	protected void flush0(NamedPipeSession session) {
		// two threads can call flush in parallel since
		// resuming read on the remote session can also trigger a flush
		// instead of synchronization, we use the "atomic-2-step" so that
		// neither thread is blocked, but all write requests are still flushed
		try {
			// step-1: first thread in performs a flush
			if (session.beginFlush()) {
				flushInternal(session);
			}
		}
		finally {
			// step-2: last thread out performs a flush
			if (session.endFlush()) {
				flushInternal(session);
			}
		}
	}

	@Override
	protected void updateTrafficControl0(NamedPipeSession session) {

		// handle resume write
		if (!session.isWriteSuspended()) {
			flush(session);
		}

		// handle resume read
		if (!session.isReadSuspended()) {
			NamedPipeSession remoteSession = session.getRemoteSession();
			if (remoteSession != null && !remoteSession.isWriteSuspended()) {
				// this requires processor.flush(session) to be thread-safe
				NamedPipeProcessor remoteProcessor = remoteSession.getProcessor();
				remoteProcessor.flush(remoteSession);
			}
		}
	}

	@Override
	public void dispose() {
		disposing = true;
	}

	@Override
	public boolean isDisposing() {
		return disposing;
	}

	@Override
	public boolean isDisposed() {
		// nothing to dispose
		return disposing;
	}

	private void flushInternal(NamedPipeSession session) {
		WriteRequestQueue writeRequestQueue = session.getWriteRequestQueue();
		IoFilterChain filterChain = session.getFilterChain();

        if (session.setFlushInternalStarted()) {

            try {

                // cannot write buffers to remote session if it cannot read them
                NamedPipeSession remoteSession = session.getRemoteSession();
                if (remoteSession == null || remoteSession.isReadSuspended()) {
                    return;
                }

                IoFilterChain remoteFilterChain = remoteSession.getFilterChain();

                do {
                    WriteRequest request = session.getCurrentWriteRequest();
                    if (request == null) {
                        request = writeRequestQueue.poll(session);
                        if (request == null) {
                            break;
                        }
                        session.setCurrentWriteRequest(request);
                    }
                    Object message = request.getMessage();
                    if (message instanceof IoBuffer) {
                        IoBuffer buf = (IoBuffer) request.getMessage();
                        try {
                            // hold current remaining bytes so we know how much was written
                            int writableBytes = buf.remaining();

                            if (writableBytes != 0) {

                                if (session.isClosing()) {
                                    Collection<WriteRequest> unwritten = new LinkedList<>();
                                    while (request != null) {
                                        unwritten.add(request);
                                        request = writeRequestQueue.poll(session);
                                    }
                                    if (!unwritten.isEmpty()) {
                                        for(WriteRequest wr: unwritten) {
                                            System.out.println(session.toString() + " " + wr.getOriginalRequest().getMessage());
                                        }
                                        WriteToClosedSessionException exception = new WriteToClosedSessionException(unwritten);
                                        filterChain.fireExceptionCaught(exception);
                                        return;
                                    }
                                }
                                IoBuffer dup = buf.duplicate();
                                remoteFilterChain.fireMessageReceived(dup);

                                // cleanup and fire message sent
                                session.setCurrentWriteRequest(null);
                                filterChain.fireMessageSent(request);

                                // increment session written bytes
                                session.increaseWrittenBytes(writableBytes, System.currentTimeMillis());
                            }
                            else {
                                // cleanup and fire message sent
                                session.setCurrentWriteRequest(null);
                                buf.reset();
                                filterChain.fireMessageSent(request);
                            }
                        }
                        catch (Exception e) {
                            request.getFuture().setException(e);
                        }
                    }
                    else {
                        throw new IllegalStateException("Don't know how to handle message of type '" + message.getClass().getName() + "'.  Are you missing a protocol encoder?");
                    }
                } while (true);



            } finally {
                session.setFlushInternalComplete();
            }
        }
	}
}


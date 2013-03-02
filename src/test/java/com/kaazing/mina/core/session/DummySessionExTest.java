/**
 * Copyright (c) 2007-2012, Kaazing Corporation. All rights reserved.
 */

package com.kaazing.mina.core.session;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.apache.mina.core.filterchain.IoFilterChain;
import org.apache.mina.core.service.IoHandlerAdapter;
import org.apache.mina.core.session.IoSession;
import org.junit.Test;

public class DummySessionExTest {

	@Test
	public void shouldNotBeReadSuspendedAfterThreadRealignment() {

		Thread alignment = new Thread();
		DummySessionEx session = new DummySessionEx(alignment);
		IoFilterChain filterChain = session.getFilterChain();
		filterChain.fireMessageReceived(new Object());
		assertFalse(session.isReadSuspended());
	}

	@Test
	public void shouldNotBeReadSuspendedWhenThreadAligned() {

		DummySessionEx session = new DummySessionEx();
		IoFilterChain filterChain = session.getFilterChain();
		filterChain.fireMessageReceived(new Object());
		assertFalse(session.isReadSuspended());
	}

	@Test
	public void shouldBeReadSuspendedAfterThreadRealignment() {

		Thread alignment = new Thread();
		DummySessionEx session = new DummySessionEx(alignment);
		session.setHandler(new IoHandlerAdapter() {

			@Override
			public void messageReceived(IoSession session, Object message)
					throws Exception {
				session.suspendRead();
			}
			
		});
		IoFilterChain filterChain = session.getFilterChain();
		filterChain.fireMessageReceived(new Object());
		assertTrue(session.isReadSuspended());
	}

	@Test
	public void shouldBeReadSuspendedWhenThreadAligned() {

		DummySessionEx session = new DummySessionEx();
		session.setHandler(new IoHandlerAdapter() {

			@Override
			public void messageReceived(IoSession session, Object message)
					throws Exception {
				session.suspendRead();
			}
			
		});
		IoFilterChain filterChain = session.getFilterChain();
		filterChain.fireMessageReceived(new Object());
		assertTrue(session.isReadSuspended());
	}
}

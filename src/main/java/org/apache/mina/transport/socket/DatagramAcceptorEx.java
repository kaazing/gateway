/**
 * Copyright (c) 2007-2012, Kaazing Corporation. All rights reserved.
 */

package org.apache.mina.transport.socket;

import com.kaazing.mina.core.service.IoServiceEx;

/**
 * This interface extends DatagramAcceptor in order to implement IoServiceEx
 */
public interface DatagramAcceptorEx extends DatagramAcceptor, IoServiceEx {

    @Override
    DatagramSessionConfigEx getSessionConfig();

}

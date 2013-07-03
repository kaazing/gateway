/**
 * Copyright (c) 2007-2012, Kaazing Corporation. All rights reserved.
 */

package org.apache.mina.transport.socket;

import com.kaazing.mina.core.service.IoAcceptorEx;

/**
 * This interface extends DatagramAcceptor in order to implement IoAcceptorEx
 */
public interface DatagramAcceptorEx extends DatagramAcceptor, IoAcceptorEx {

    @Override
    DatagramSessionConfigEx getSessionConfig();

}

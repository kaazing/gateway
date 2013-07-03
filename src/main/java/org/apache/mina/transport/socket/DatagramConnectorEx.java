/**
 * Copyright (c) 2007-2012, Kaazing Corporation. All rights reserved.
 */

package org.apache.mina.transport.socket;

import com.kaazing.mina.core.service.IoConnectorEx;

/**
 * This interface extends DatagramConnector in order to implement IoConnectorEx
 */
public interface DatagramConnectorEx extends DatagramConnector, IoConnectorEx {

    @Override
    DatagramSessionConfigEx getSessionConfig();

}

/**
 * Copyright (c) 2007-2012, Kaazing Corporation. All rights reserved.
 */

package com.kaazing.mina.core.session;

import org.apache.mina.core.session.IdleStatus;
import org.apache.mina.core.session.IoSessionConfig;


/**
 * Extended version of IoSessionConfig to add support for millisecond precision for idle timeouts.
*/   
public interface IoSessionConfigEx extends IoSessionConfig  {
    
    /**
     * New method added for millisecond precise idle times
     */
    public void setIdleTimeInMillis(IdleStatus status, long idleTimeMillis);
    
}

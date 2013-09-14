/**
 * Copyright (c) 2007-2013, Kaazing Corporation. All rights reserved.
 */

package com.kaazing.mina.core.write;

import org.apache.mina.core.write.WriteRequest;

/**
 * Extended version of WriteRequest to add support for mutating the
 * message during encoding to avoid undesirable allocation.
 */
public interface WriteRequestEx extends WriteRequest {

    void setMessage(Object message);
}

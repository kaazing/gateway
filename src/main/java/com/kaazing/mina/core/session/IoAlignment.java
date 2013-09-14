/**
 * Copyright (c) 2007-2013, Kaazing Corporation. All rights reserved.
 */

package com.kaazing.mina.core.session;

/**
 * Detection of thread alignment in AbstractIoSession.
*/
interface IoAlignment {

    // capture relationship with IoSessionEx method of same signature
    boolean isIoAligned();

}

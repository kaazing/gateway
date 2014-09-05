/**
 * Copyright (c) 2007-2014, Kaazing Corporation. All rights reserved.
 */

package com.kaazing.mina.netty.bootstrap;

import org.jboss.netty.util.ExternalResourceReleasable;

public interface Bootstrap extends ExternalResourceReleasable {

    void shutdown();

}

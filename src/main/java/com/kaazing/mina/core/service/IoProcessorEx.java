/**
 * Copyright (c) 2007-2013, Kaazing Corporation. All rights reserved.
 */

package com.kaazing.mina.core.service;

import org.apache.mina.core.service.IoProcessor;

import com.kaazing.mina.core.session.IoSessionEx;

// constrains type of session to IoSessionEx subclass
public interface IoProcessorEx<T extends IoSessionEx> extends IoProcessor<T>  {

}

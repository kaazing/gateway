/**
 * Copyright (c) 2007-2014 Kaazing Corporation. All rights reserved.
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.kaazing.gateway.transport.http.specification.httpxe;

import static org.kaazing.test.util.ITUtil.createRuleChain;

import org.apache.mina.core.service.IoHandler;
import org.apache.mina.core.session.IoSession;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.lib.concurrent.Synchroniser;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.kaazing.gateway.transport.http.HttpxeAcceptorRule;
import org.kaazing.k3po.junit.annotation.Specification;
import org.kaazing.k3po.junit.rules.K3poRule;

/**
 * Defines how httpxe will deal with http methods.
 */
public class RequestsIT {

    private final K3poRule k3po = new K3poRule().setScriptRoot("org/kaazing/specification/httpxe/requests");

    private final HttpxeAcceptorRule acceptor = new HttpxeAcceptorRule();

    private Mockery context;

    @Before
    public void initialize() {
        context = new Mockery() {
            {
                setImposteriser(ClassImposteriser.INSTANCE);
            }
        };
        context.setThreadingPolicy(new Synchroniser());
    }

    @Rule
    public final TestRule chain = createRuleChain(acceptor, k3po);

    @Test
    @Specification({
        "post.request.with.km.parameter.get/request"})
    public void shouldProcessPostRequestAsGetRequest() throws Exception {
    	final IoHandler acceptHandler = context.mock(IoHandler.class);
    	context.checking(new Expectations() {
    		{
    		oneOf(acceptHandler).sessionCreated(with(any(IoSession.class)));
    		oneOf(acceptHandler).sessionOpened(with(any(IoSession.class)));
    		}
    	});
    	acceptor.bind("httpxe://localhost:8000/path", acceptHandler);
        k3po.finish();
    }

    @Test
    @Ignore("not addressed yet")
    @Specification({
        "post.request.with.path.encoded.get/request"})
    public void shouldProcessPathEncodedPostRequestAsGetRequest() throws Exception {
        k3po.finish();
    }

    @Test
    @Ignore("not addressed yet")
    @Specification({
        "post.request.with.km.parameter.head/request"})
    public void shouldProcessPostRequestAsHeadRequest() throws Exception {
        k3po.finish();
    }

    @Test
    @Ignore("not addressed yet")
    @Specification({
        "post.request.with.path.encoded.head/request"})
    public void shouldProcessPathEncodedPostRequestAsHeadRequest() throws Exception {
        k3po.finish();
    }

    @Test
    @Ignore("not addressed yet")
    @Specification({
        "post.request.with.path.encoded.post/request"})
    public void shouldProcessPathEncodedGetRequestAsPostRequest() throws Exception {
        k3po.finish();
    }

    @Test
    @Ignore("not addressed yet")
    @Specification({
        "post.request.with.km.parameter.put/request"})
    public void shouldProcessPostRequestAsPutRequest() throws Exception {
        k3po.finish();
    }

    @Test
    @Ignore("not addressed yet")
    @Specification({
        "post.request.with.path.encoded.put/request"})
    public void shouldProcessPathEncodedPostRequestAsPutRequest() throws Exception {
        k3po.finish();
    }

    @Test
    @Ignore("not addressed yet")
    @Specification({
        "post.request.with.km.parameter.delete/request"})
    public void shouldProcessPostRequestAsDeleteRequest() throws Exception {
        k3po.finish();
    }

    @Test
    @Ignore("not addressed yet")
    @Specification({
        "post.request.with.path.encoded.delete/request"})
    public void shouldProcessPathEncodedPostRequestAsDeleteRequest() throws Exception {
        k3po.finish();
    }

    @Test
    @Ignore("not addressed yet")
    @Specification({
        "post.request.with.km.parameter.options/request"})
    public void shouldProcessPostRequestAsOptionsRequest() throws Exception {
        k3po.finish();
    }

    @Test
    @Ignore("not addressed yet")
    @Specification({
        "post.request.with.path.encoded.options/request"})
    public void shouldProcessPathEncodedPostRequestAsOptionsRequest() throws Exception {
        k3po.finish();
    }

    @Test
    @Ignore("not addressed yet")
    @Specification({
        "post.request.with.km.parameter.trace/request"})
    public void shouldProcessPostRequestAsTraceRequest() throws Exception {
        k3po.finish();
    }

    @Test
    @Ignore("not addressed yet")
    @Specification({
        "post.request.with.path.encoded.trace/request"})
    public void shouldProcessPathEncodedPostRequestAsTraceRequest() throws Exception {
        k3po.finish();
    }

    @Test
    @Ignore("not addressed yet")
    @Specification({
        "post.request.with.km.parameter.custom/request" })
    public void shouldProcessPostRequestAsCustomRequest() throws Exception {
        k3po.finish();
    }

    @Test
    @Ignore("not addressed yet")
    @Specification({
        "post.request.with.path.encoded.custom/request" })
    public void shouldProcessPathEncodedPostRequestAsCustomRequest() throws Exception {
        k3po.finish();
    }

    @Test
    @Ignore("not addressed yet")
    @Specification({
        "client.sends.httpxe.request/request" })
    public void shouldPassWithHttpxeReqestUsingHttpContentType() throws Exception {
        k3po.finish();
    }

    @Test
    @Ignore("not addressed yet")
    @Specification({
        "client.sends.httpxe.request.using.kct.parameter/request" })
    public void shouldPassWithHttpxeReqestUsingKctParamter() throws Exception {
        k3po.finish();
    }

}

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

package org.kaazing.gateway.service.http.directory;

import static org.junit.rules.RuleChain.outerRule;

import java.io.File;
import java.net.URI;

import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.kaazing.gateway.server.test.GatewayRule;
import org.kaazing.gateway.server.test.config.GatewayConfiguration;
import org.kaazing.gateway.server.test.config.builder.GatewayConfigurationBuilder;
import org.kaazing.k3po.junit.annotation.Specification;
import org.kaazing.k3po.junit.rules.K3poRule;

public class HttpDirectoryServiceIT {

    private static final String DIRECTORY_SERVICE_ACCEPT = "http://localhost:8000/";
    private static final String CROSS_ORIGIN_DIRECTORY_SERVICE_ACCEPT = "http://localhost:8001/";
    private static final String ASTRISK_ORIGIN_DIRECTORY_SERVICE_ACCEPT = "http://localhost:8002/";
    private static final String KEEPALIVE_DIRECTORY_SERVICE_ACCEPT = "http://localhost:8003/keepAlive";
    private static final String NO_SERVER_HEADER = "http://localhost:8004/";

    private final K3poRule robot = new K3poRule();

    private final GatewayRule gateway = new GatewayRule() {
        {
            // @formatter:off
            GatewayConfiguration configuration =
                    new GatewayConfigurationBuilder()
                        .webRootDirectory(new File("src/test/webapp"))
                            .service()
                                .accept(URI.create(KEEPALIVE_DIRECTORY_SERVICE_ACCEPT))
                                .type("directory")
                                .property("directory", "/public")
                                // We have to use this name (which is from TransportOptionNames) instead of "http.keepalive.timeout",
                                // see Gateway.camelCaseToDottedLowerCase.
                                .acceptOption("http.keepalive.timeout", "3") // seconds
                        .done()
                        .service()
                            .accept(URI.create(DIRECTORY_SERVICE_ACCEPT))
                            .type("directory")
                            .property("directory", "/public")
                            .property("welcome-file", "index.html")
                        .done()
                        .service()
                            .accept(URI.create(CROSS_ORIGIN_DIRECTORY_SERVICE_ACCEPT))
                            .type("directory")
                            .property("directory", "/public")
                                .crossOrigin().allowOrigin("http://localhost:8000")
                                .allowHeaders("x-websocket-protocol").allowMethods("GET")
                            .done()
                        .done()
                        .service()
                            .accept(URI.create(ASTRISK_ORIGIN_DIRECTORY_SERVICE_ACCEPT))
                            .type("directory")
                            .property("directory", "/public")
                                .crossOrigin().allowOrigin("*")
                            .done()
                        .done()
                            .service()
                            .accept(URI.create(NO_SERVER_HEADER))
                            .type("directory")
                            .acceptOption("http.server.header", "disabled")
                            .property("directory", "/public")
                            .property("welcome-file", "index.html")
                            .crossOrigin().allowOrigin("*").done()
                        .done()
                    .done();
            // @formatter:on
            init(configuration);
        }
    };

    @Rule
    public TestRule chain = outerRule(robot).around(gateway);

    @Specification("get.index.check.status.code.200")
    @Test(timeout = 8000)
    public void testGetIndexAndStatusCode200() throws Exception {
        robot.finish();
    }

    @Specification("no.server.header")
    @Test(timeout = 8000)
    public void testNoServerHeader() throws Exception {
        robot.finish();
    }

    @Specification("get.nonexistent.page.check.status.code.404")
    @Test(timeout = 8000)
    public void testGetNonexistantPageCheckStatusCode404() throws Exception {
        robot.finish();
    }

    @Specification("get.nonexistent.page.check.http.keepalive.timeout")
    @Test(timeout = 8000)
    // keepalive timeout is set at 3 secs so this should suffice to see the server disconnect
    public void testGetNonexistantPageCheckConnectionTimesOut() throws Exception {
        robot.finish();
    }

    @Specification("tcp.connect.and.close.to.directory.service")
    @Test(timeout = 8000)
    public void testTcpConnectAndClose() throws Exception {
        robot.finish();
    }

    /**
     * BUG for KG-7642 TODO @Ignore particular test in JIRA
     */
    @Ignore
    @Specification("tcp.connect.and.wait.for.close.to.directory.service")
    @Test(timeout = 35000)
    public void testTcpConnectAndWaitForClose() throws Exception {
        robot.finish();
    }

    @Specification("get.index.check.whole.response")
    @Test(timeout = 8000)
    public void testGetIndexCheckWholeResponse() throws Exception {
        robot.finish();
    }

    @Specification("post.large.data")
    @Test(timeout = 25000)
    public void testPostLargeData() throws Exception {
        robot.finish();
    }

    @Specification("get.forbidden.file.exists")
    @Test(timeout = 5000)
    public void testGetForbiddenFileExists() throws Exception {
        robot.finish();
    }

    // /////////////////// HOST HEADER ///////////////////////
    @Specification("host/host.empty.header.with.absolute.uri")
    @Test(timeout = 5000)
    public void testEmptyHostHeaderWithAbsoluteUri() throws Exception {
        robot.finish();
    }

    @Specification("host/host.empty.header.with.relative.uri")
    @Test(timeout = 5000)
    public void testEmptyHostHeaderWithRelativeUri() throws Exception {
        robot.finish();
    }

    @Specification("host/host.header.invalid")
    @Test(timeout = 5000)
    public void testInvalidHostHeader() throws Exception {
        robot.finish();
    }

    @Specification("host/host.header.not.present")
    @Test(timeout = 5000)
    public void testAbsentHostHeader() throws Exception {
        robot.finish();
    }

    @Specification("host/host.header.port.not.set")
    @Test(timeout = 5000)
    public void testAbsentPortInHostHeader() throws Exception {
        robot.finish();
    }

    @Specification("host/host.valid.header.with.absolute.uri")
    @Test(timeout = 5000)
    public void testValidHostHeaderWithAbsoluteUri() throws Exception {
        robot.finish();
    }

    @Specification("host/host.no.header.with.absolute.uri")
    @Test(timeout = 5000)
    public void testNoHostHeaderWithAbsoluteUri() throws Exception {
        robot.finish();
    }

    // ///////////////////// METHOD //////////////////////
    @Specification("method/method.connect")
    @Test(timeout = 5000)
    public void testConnectMethod() throws Exception {
        robot.finish();
    }

    @Specification("method/method.delete")
    @Test(timeout = 5000)
    public void testDeleteMethod() throws Exception {
        robot.finish();
    }

    @Specification("method/method.head")
    @Test(timeout = 5000)
    public void testHeadMethod() throws Exception {
        robot.finish();
    }

    @Specification("method/method.options")
    @Test(timeout = 5000)
    public void testOptionsMethod() throws Exception {
        robot.finish();
    }

    @Specification("method/method.bogus")
    @Test(timeout = 5000)
    public void testBogusMethod() throws Exception {
        robot.finish();
    }

    @Specification("method/method.post")
    @Test(timeout = 5000)
    public void testPostMethod() throws Exception {
        robot.finish();
    }

    // //////////////// URI ////////////////////////
    @Specification("uri.hex.encoded")
    @Test(timeout = 5000)
    public void testHexEncodedUri() throws Exception {
        robot.finish();
    }

    @Specification("uri.too.long")
    @Test(timeout = 5000)
    public void testUriTooLong() throws Exception {
        robot.finish();
    }

    @Specification("invalid.uri.with.space")
    @Test(timeout = 5000)
    public void testInvalidUri() throws Exception {
        robot.finish();
    }

    @Specification("uri.with.params")
    @Test(timeout = 5000)
    public void testUriWithParams() throws Exception {
        robot.finish();
    }

    // ////////////////// CROSS-ORIGIN ACCESS ////////////////////

    @Specification("origin/same.origin.constraint.not.set")
    @Test(timeout = 5000)
    public void testSameOriginConstraintNotSet() throws Exception {
        robot.finish();
    }

    @Specification("origin/different.origin.constraint.not.set")
    @Test(timeout = 5000)
    public void testDifferentOriginConstraintNotSet() throws Exception {
        robot.finish();
    }

    @Specification("origin/missing.origin.header.constraint.not.set")
    @Test(timeout = 5000)
    public void testMissingOriginHeaderConstraintNotSet() throws Exception {
        robot.finish();
    }

    @Specification("origin/wrong.host.constraint.set")
    @Test(timeout = 5000)
    public void testWrongHostConstraintSet() throws Exception {
        robot.finish();
    }

    @Specification("origin/wrong.port.constraint.set")
    @Test(timeout = 5000)
    public void testWrongPortConstraintSet() throws Exception {
        robot.finish();
    }

    @Specification("origin/missing.origin.header.constraint.set")
    @Test(timeout = 5000)
    public void testMissingOriginHeaderConstraintSet() throws Exception {
        robot.finish();
    }

    @Specification("origin/empty.origin.header.constraint.set")
    @Test(timeout = 5000)
    public void testEmptyOriginHeaderConstraintSet() throws Exception {
        robot.finish();
    }

    @Specification("origin/empty.origin.header.constraint.not.set")
    @Test(timeout = 5000)
    public void testEmptyOriginHeaderConstraintNotSet() throws Exception {
        robot.finish();
    }

    @Specification("origin/different.origin.constraint.asterisk")
    @Test(timeout = 5000)
    public void testDifferentOriginConstraintAsterisk() throws Exception {
        robot.finish();
    }

    @Specification("origin/empty.origin.header.constraint.asterisk")
    @Test(timeout = 5000)
    public void testEmptyOriginHeaderConstraintAsterisk() throws Exception {
        robot.finish();
    }

    @Specification("origin/missing.origin.header.constraint.asterisk")
    @Test(timeout = 5000)
    public void testMissingOriginHeaderConstraintAsterisk() throws Exception {
        robot.finish();
    }


    @Specification("origin/not.allowed.method")
    @Test(timeout = 5000)
    public void testNotAllowedMethodConstraintSet() throws Exception {
        robot.finish();
    }

    @Specification("origin/not.allowed.header")
    @Test(timeout = 5000)
    public void testNotAllowedHeaderConstraintSet() throws Exception {
        robot.finish();
    }

    @Specification("origin/allowed.method")
    @Test(timeout = 5000)
    public void testAllowedMethodConstraintSet() throws Exception {
        robot.finish();
    }

    @Specification("origin/allowed.header")
    @Test(timeout = 5000)
    public void testAllowedHeaderConstraintSet() throws Exception {
        robot.finish();
    }

}

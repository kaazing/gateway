/**
 * Copyright 2007-2016, Kaazing Corporation. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.kaazing.gateway.service.http.directory;

import static org.kaazing.test.util.ITUtil.createRuleChain;

import java.io.File;

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
    private static final String DIRECTORY_SERVICE_NO_SLASH = "http://localhost:8005/";
    private static final String DIRECTORY_SERVICE_DOT_SLASH = "http://localhost:8006/";
    private static final String DIRECTORY_SERVICE_NO_PATH = "http://localhost:8007/";
    private static final String DIRECTORY_SERVICE_WRONG_PATH = "http://localhost:8008/";

    private final K3poRule robot = new K3poRule();

    private final GatewayRule gateway = new GatewayRule() {
        {
            // @formatter:off
            GatewayConfiguration configuration =
                    new GatewayConfigurationBuilder()
                        .webRootDirectory(new File("src/test/webapp"))
                            .service()
                                .accept(KEEPALIVE_DIRECTORY_SERVICE_ACCEPT)
                                .type("directory")
                                .property("directory", "/public")
                                // We have to use this name (which is from TransportOptionNames) instead of "http.keepalive.timeout",
                                // see Gateway.camelCaseToDottedLowerCase.
                                .acceptOption("http.keepalive.timeout", "3") // seconds
                        .done()
                        .service()
                            .accept(DIRECTORY_SERVICE_ACCEPT)
                            .type("directory")
                            .property("directory", "/public")
                            .property("welcome-file", "index.html")
                        .done()
                        .service()
                            .accept(CROSS_ORIGIN_DIRECTORY_SERVICE_ACCEPT)
                            .type("directory")
                            .property("directory", "/public")
                                .crossOrigin().allowOrigin("http://localhost:8000")
                                .allowHeaders("x-websocket-protocol").allowMethods("GET")
                            .done()
                        .done()
                        .service()
                            .accept(ASTRISK_ORIGIN_DIRECTORY_SERVICE_ACCEPT)
                            .type("directory")
                            .property("directory", "/public")
                                .crossOrigin().allowOrigin("*")
                            .done()
                        .done()
                            .service()
                            .accept(NO_SERVER_HEADER)
                            .type("directory")
                            .acceptOption("http.server.header", "disabled")
                            .property("directory", "/public")
                            .property("welcome-file", "index.html")
                            .crossOrigin().allowOrigin("*").done()
                        .done()
                        .service()
                            .accept(DIRECTORY_SERVICE_NO_SLASH)
                            .type("directory")
                            .property("directory", "public")
                            .property("welcome-file", "index.html")
                        .done()
                        .service()
                            .accept(DIRECTORY_SERVICE_DOT_SLASH)
                            .type("directory")
                            .property("directory", "./public")
                            .property("welcome-file", "index.html")
                        .done()
                        .service()
                            .accept(DIRECTORY_SERVICE_NO_PATH)
                            .type("directory")
                            .property("directory", "")
                            .property("welcome-file", "index.html")
                        .done()
                        .service()
                            .accept(DIRECTORY_SERVICE_WRONG_PATH)
                            .type("directory")
                            .property("directory", ".public")
                            .property("welcome-file", "index.html")
                        .done()
                    .done();
            // @formatter:on
            init(configuration);
        }
    };

    @Rule
    public TestRule chain = createRuleChain(gateway, robot);

    @Specification("get.index.check.status.code.200")
    @Test
    public void testGetIndexAndStatusCode200() throws Exception {
        robot.finish();
    }

    @Specification("no.server.header")
    @Test
    public void testNoServerHeader() throws Exception {
        robot.finish();
    }

    @Specification("get.nonexistent.page.check.status.code.404")
    @Test
    public void testGetNonexistantPageCheckStatusCode404() throws Exception {
        robot.finish();
    }

    @Specification("get.nonexistent.page.check.http.keepalive.timeout")
    @Test
    // keepalive timeout is set at 3 secs so this should suffice to see the server disconnect
    public void testGetNonexistantPageCheckConnectionTimesOut() throws Exception {
        robot.finish();
    }

    @Specification("tcp.connect.and.close.to.directory.service")
    @Test
    public void testTcpConnectAndClose() throws Exception {
        robot.finish();
    }

    /**
     * BUG for KG-7642 TODO @Ignore particular test in JIRA
     */
    @Ignore
    @Specification("tcp.connect.and.wait.for.close.to.directory.service")
    @Test
    public void testTcpConnectAndWaitForClose() throws Exception {
        robot.finish();
    }

    @Specification("get.index.check.whole.response")
    @Test
    public void testGetIndexCheckWholeResponse() throws Exception {
        robot.finish();
    }

    @Specification("post.large.data")
    @Test
    public void testPostLargeData() throws Exception {
        robot.finish();
    }

    @Specification("get.forbidden.file.exists")
    @Test
    public void testGetForbiddenFileExists() throws Exception {
        robot.finish();
    }

    // /////////////////// HOST HEADER ///////////////////////

    @Specification("host/host.empty.header.with.relative.uri")
    @Test
    public void testEmptyHostHeaderWithRelativeUri() throws Exception {
        robot.finish();
    }

    @Specification("host/host.header.invalid")
    @Test
    public void testInvalidHostHeader() throws Exception {
        robot.finish();
    }

    @Specification("host/host.header.not.present")
    @Test
    public void testAbsentHostHeader() throws Exception {
        robot.finish();
    }

    @Specification("host/host.header.port.not.set")
    @Test
    public void testAbsentPortInHostHeader() throws Exception {
        robot.finish();
    }

    @Specification("host/host.valid.header.with.absolute.uri")
    @Test
    public void testValidHostHeaderWithAbsoluteUri() throws Exception {
        robot.finish();
    }

    @Specification("host/host.no.header.with.absolute.uri")
    @Test
    public void testNoHostHeaderWithAbsoluteUri() throws Exception {
        robot.finish();
    }

    // ///////////////////// METHOD //////////////////////
    @Specification("method/method.connect")
    @Test
    public void testConnectMethod() throws Exception {
        robot.finish();
    }

    @Specification("method/method.delete")
    @Test
    public void testDeleteMethod() throws Exception {
        robot.finish();
    }

    @Specification("method/method.head")
    @Test
    public void testHeadMethod() throws Exception {
        robot.finish();
    }

    @Specification("method/method.options")
    @Test
    public void testOptionsMethod() throws Exception {
        robot.finish();
    }

    @Specification("method/method.bogus")
    @Test
    public void testBogusMethod() throws Exception {
        robot.finish();
    }

    @Specification("method/method.post")
    @Test
    public void testPostMethod() throws Exception {
        robot.finish();
    }

    // //////////////// URI ////////////////////////
    @Specification("uri.hex.encoded")
    @Test
    public void testHexEncodedUri() throws Exception {
        robot.finish();
    }

    @Specification("uri.too.long")
    @Test
    public void testUriTooLong() throws Exception {
        robot.finish();
    }

    @Specification("invalid.uri.with.space")
    @Test
    public void testInvalidUri() throws Exception {
        robot.finish();
    }

    @Specification("uri.with.params")
    @Test
    public void testUriWithParams() throws Exception {
        robot.finish();
    }

    // ////////////////// CROSS-ORIGIN ACCESS ////////////////////

    @Specification("origin/same.origin.constraint.not.set")
    @Test
    public void testSameOriginConstraintNotSet() throws Exception {
        robot.finish();
    }

    @Specification("origin/different.origin.constraint.not.set")
    @Test
    public void testDifferentOriginConstraintNotSet() throws Exception {
        robot.finish();
    }

    @Specification("origin/missing.origin.header.constraint.not.set")
    @Test
    public void testMissingOriginHeaderConstraintNotSet() throws Exception {
        robot.finish();
    }

    @Specification("origin/wrong.host.constraint.set")
    @Test
    public void testWrongHostConstraintSet() throws Exception {
        robot.finish();
    }

    @Specification("origin/wrong.port.constraint.set")
    @Test
    public void testWrongPortConstraintSet() throws Exception {
        robot.finish();
    }

    @Specification("origin/missing.origin.header.constraint.set")
    @Test
    public void testMissingOriginHeaderConstraintSet() throws Exception {
        robot.finish();
    }

    @Specification("origin/empty.origin.header.constraint.set")
    @Test
    public void testEmptyOriginHeaderConstraintSet() throws Exception {
        robot.finish();
    }

    @Specification("origin/empty.origin.header.constraint.not.set")
    @Test
    public void testEmptyOriginHeaderConstraintNotSet() throws Exception {
        robot.finish();
    }

    @Specification("origin/different.origin.constraint.asterisk")
    @Test
    public void testDifferentOriginConstraintAsterisk() throws Exception {
        robot.finish();
    }

    @Specification("origin/empty.origin.header.constraint.asterisk")
    @Test
    public void testEmptyOriginHeaderConstraintAsterisk() throws Exception {
        robot.finish();
    }

    @Specification("origin/missing.origin.header.constraint.asterisk")
    @Test
    public void testMissingOriginHeaderConstraintAsterisk() throws Exception {
        robot.finish();
    }


    @Specification("origin/not.allowed.method")
    @Test
    public void testNotAllowedMethodConstraintSet() throws Exception {
        robot.finish();
    }

    @Specification("origin/not.allowed.header")
    @Test
    public void testNotAllowedHeaderConstraintSet() throws Exception {
        robot.finish();
    }

    @Specification("origin/allowed.method")
    @Test
    public void testAllowedMethodConstraintSet() throws Exception {
        robot.finish();
    }

    @Specification("origin/allowed.header")
    @Test
    public void testAllowedHeaderConstraintSet() throws Exception {
        robot.finish();
    }

    @Specification("directory.no.slash.code.200")
    @Test
    public void shouldFindResourceWithNoSlashInDirectory() throws Exception {
        robot.finish();
    }

    @Specification("directory.dot.slash.code.200")
    @Test
    public void shouldFindResourceWithDotSlashInDirectory() throws Exception {
        robot.finish();
    }

    @Specification("directory.no.path.code.200")
    @Test
    public void shouldFindRootWithNoPathInDirectory() throws Exception {
        robot.finish();
    }

    @Specification("directory.wrong.path.code.404")
    @Test
    public void shouldNotFindResourceWithWrongPathInDirectory() throws Exception {
        robot.finish();
    }
}

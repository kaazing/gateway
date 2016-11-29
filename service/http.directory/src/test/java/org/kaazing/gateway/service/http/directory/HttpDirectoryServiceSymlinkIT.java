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
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.FileSystemException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.AfterClass;
import org.junit.Assume;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.kaazing.gateway.server.test.GatewayRule;
import org.kaazing.gateway.server.test.config.GatewayConfiguration;
import org.kaazing.gateway.server.test.config.builder.GatewayConfigurationBuilder;
import org.kaazing.k3po.junit.annotation.Specification;
import org.kaazing.k3po.junit.rules.K3poRule;

public class HttpDirectoryServiceSymlinkIT {

    private static final String DIRECTORY_SERVICE_SYMLINK_INSIDE_FILE_FOLLOW = "http://localhost:8011/";
    private static final String DIRECTORY_SERVICE_SYMLINK_INSIDE_FILE_RESTRICTED = "http://localhost:8012/";
    private static final String DIRECTORY_SERVICE_SYMLINK_OUTSIDE_FILE_FOLLOW = "http://localhost:8015/";
    private static final String DIRECTORY_SERVICE_SYMLINK_OUTSIDE_FILE_RESTRICTED = "http://localhost:8016/";

    private static final String BASE_DIR = "src/test/webapp/";
    private static final String LINK_INSIDE_FILE = "/public/indexSymInside.html";
    private static final String TARGET_INSIDE_FILE = "/public/insideDir/index.html";
    private static final String LINK_OUTSIDE_FILE = "/public/indexSymOutside.html";
    private static final String TARGET_OUTSIDE_FILE = "/outsideDir/index.html";

    private static Path fileSymPathOutside = null;
    private static Path fileSymPathInside = null;

    private final K3poRule robot = new K3poRule();

    private final GatewayRule gateway = new GatewayRule() {
        {
            // @formatter:off
            GatewayConfiguration configuration =
                    new GatewayConfigurationBuilder()
                        .webRootDirectory(new File("src/test/webapp"))
                        .service()
                            .accept(DIRECTORY_SERVICE_SYMLINK_INSIDE_FILE_FOLLOW)
                            .type("directory")
                            .property("directory", "/public/")
                            .property("welcome-file", "indexSymInside.html")
                            .property("symbolic-links", "follow")
                        .done()
                        .service()
                            .accept(DIRECTORY_SERVICE_SYMLINK_INSIDE_FILE_RESTRICTED)
                            .type("directory")
                            .property("directory", "/public/")
                            .property("welcome-file", "indexSymInside.html")
                            .property("symbolic-links", "restricted")
                        .done()
                        .service()
                            .accept(DIRECTORY_SERVICE_SYMLINK_OUTSIDE_FILE_FOLLOW)
                            .type("directory")
                            .property("directory", "/public/")
                            .property("welcome-file", "indexSymOutside.html")
                            .property("symbolic-links", "follow")
                        .done()
                        .service()
                            .accept(DIRECTORY_SERVICE_SYMLINK_OUTSIDE_FILE_RESTRICTED)
                            .type("directory")
                            .property("directory", "/public/")
                            .property("welcome-file", "indexSymOutside.html")
                        .done()
                    .done();
            // @formatter:on
            init(configuration);
        }
    };

    @Rule
    public TestRule chain = createRuleChain(gateway, robot);

    @BeforeClass
    public static void createResourcesBefore() {
        fileSymPathInside = createSymlinkResources(LINK_INSIDE_FILE, TARGET_INSIDE_FILE);
        fileSymPathOutside = createSymlinkResources(LINK_OUTSIDE_FILE, TARGET_OUTSIDE_FILE);
    }

    private static Path createSymlinkResources(String linkPath, String targetPath) {
        try {
            Path newLink = new File(BASE_DIR, linkPath).getCanonicalFile().toPath();
            Path target = new File(BASE_DIR, targetPath).getCanonicalFile().toPath();
            return Files.createSymbolicLink(newLink, target);
        } catch (FileAlreadyExistsException existsException) {
            return new File(existsException.getFile()).toPath();
        } catch (FileSystemException accessDeniedException) {
            Assume.assumeTrue("User does not have OS access to create symlinks", false);
            return null;
        } catch (Exception ex) {
            return null;
        }
    }

    @AfterClass
    public static void cleanResourcesAfter() {
        try {
            Files.delete(fileSymPathOutside);
            Files.delete(fileSymPathInside);
        } catch (Exception x) {
            // no op
        }
    }

    @Specification("directory.service.symlink.follow.inside.file")
    @Test
    public void shouldFollowSymLinksInsideBaseFolder() throws Exception {
        robot.finish();
    }

    @Specification("directory.service.symlink.restricted.inside.file")
    @Test
    public void shouldFollowSymLinksIfRestrictedInsideBaseFolder() throws Exception {
        robot.finish();
    }

    @Specification("directory.service.symlink.follow.outside.file")
    @Test
    public void shouldFollowSymLinksOutsideBaseFolder() throws Exception {
        robot.finish();
    }

    @Specification("directory.service.symlink.restricted.outside.file")
    @Test
    public void shouldNotFollowSymLinksIfRestrictedOutsideBaseFolder() throws Exception {
        robot.finish();
    }

}

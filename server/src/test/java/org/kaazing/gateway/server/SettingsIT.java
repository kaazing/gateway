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
package org.kaazing.gateway.server;

import org.junit.Test;
import org.kaazing.gateway.server.impl.VersionUtils;

import java.io.File;
import java.io.IOException;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class SettingsIT {

    /**
     * Checks in the project's artifact that the manifest entries will be generated as expected by others (update.check).
     * Uses the jar found in the target, since in the integration-test the project's artifact is not in the classpath.
     */
    @Test
    public void shouldHaveCommunityProductEditionAndTitle() throws IOException {
        assertNull("Could use the classpath to introspect the jar.", VersionUtils.getGatewayProductEdition());
        File artifact = null;
        final File classDirectory = new File(SettingsIT.class.getProtectionDomain().getCodeSource().getLocation().getPath());
        final File targetDirectory = new File(classDirectory.getCanonicalPath() + "/../"); // target/test-classes
        for (File entry : targetDirectory.listFiles()) {
            if (entry.getName().startsWith("gateway.server") &&
                    entry.getName().endsWith(".jar")) {
                artifact = entry;
            }
        }
        assertNotNull("Artifact for gateway.server not found. Please run this test as an integration test.", artifact);
        JarFile jar = new JarFile(artifact);
        Manifest mf = jar.getManifest();
        Attributes attrs = mf.getMainAttributes();
        assertEquals("Kaazing Gateway", attrs.getValue("Implementation-Title"));
        assertEquals("Community.Gateway", attrs.getValue("Kaazing-Product"));
        jar.close();
    }
}


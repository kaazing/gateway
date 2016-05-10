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

import java.io.File;
import java.io.IOException;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class SettingsIT {

    /**
     * Checks in the projects's pom.xml that the manifest entries will be generated as expected by others (update.check)
     */
    @Test
    public void shouldHaveCommunityProductEditionAndTitle() throws IOException {
        File artefact = null;
        File target = new File("../server/target");
        for (File entry : target.listFiles()) {
            if (entry.getName().startsWith("gateway.server") &&
                    entry.getName().endsWith(".jar")) {
                artefact = entry;
            }
        }
        assertNotNull("Artefact for gateway.server not found. Please run this test as an integration test.", artefact);
        JarFile jar = new JarFile(artefact);
        Manifest mf = jar.getManifest();
        Attributes attrs = mf.getMainAttributes();
        assertEquals("Kaazing Gateway", attrs.getValue("Implementation-Title"));
        assertEquals("Community.Gateway", attrs.getValue("Kaazing-Product"));
    }
}

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
package org.kaazing.gateway.server.util.version;

import java.io.IOException;
import java.net.URL;
import java.util.Enumeration;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

/**
 * Class used for parsing the class path system property.
 */
public class ManifestReader {

    /**
     * Retrieves the class path entries.
     * @return the class path entries
     * @throws IOException
     */
    public Enumeration<URL> getManifestURLs() throws IOException {
        return getClass().getClassLoader().getResources("META-INF/MANIFEST.MF");
    }

    /**
     * Retrieves the jar file manifest attributes for a given class path entry
     * @param url - the class path entry
     * @return the manifest file attributes
     * @throws IOException
     */
    public Attributes getManifestAttributesFromURL(URL url) throws IOException {
        Manifest manifest = new Manifest(url.openStream());
        return manifest.getMainAttributes();
    }

}

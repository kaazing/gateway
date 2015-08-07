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

package org.kaazing.gateway.server.util.version;

import java.io.IOException;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

public final class ClassPathUtils {

    public static String[] getClassPathEntries() {
        return System.getProperty("java.class.path").split(System.getProperty("path.separator"));
    }

    public static Attributes getManifestAttributesFromClassPathEntry(String pathEntry) throws IOException {
        JarFile jar = new JarFile(pathEntry);
        Manifest mf = jar.getManifest();
        jar.close();
        return mf.getMainAttributes();
    }

    private ClassPathUtils() {
    }

}

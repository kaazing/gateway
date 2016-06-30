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
package org.kaazing.gateway.util.file;

import java.io.File;

/**
 * Utility routines for file management.
 */
public class FileUtils {

    protected FileUtils() {
    }

    /**
     * Given a File object, return the extension (portion after the final '.'), if any.
     * If no extension, returns null.  If the file parameter is null, returns null.
     * Does not check if the file actually exists or not.
     */
    public static String getFileExtension(File file) {
        if (file == null) {
            return null;
        }

        String fileName = file.getName();
        int dotPos = fileName.lastIndexOf('.');
        return dotPos == -1 ? null : fileName.substring(dotPos + 1);
    }
}

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
package org.kaazing.gateway.transport.http.resource.impl;

import static org.kaazing.gateway.resource.address.http.HttpResourceAddress.TEMP_DIRECTORY;
import static org.kaazing.gateway.util.file.FileUtils.getFileExtension;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.Map;

import org.kaazing.gateway.resource.address.ResourceAddress;
import org.kaazing.gateway.transport.http.HttpAcceptSession;
import org.kaazing.gateway.transport.http.HttpUtils;
import org.kaazing.gateway.transport.http.resource.HttpDynamicResource;

public final class HttpInjectedDynamicResource extends HttpDynamicResource {

    private static final Map<String, String> EMPTY_WRITE_HEADERS = Collections.emptyMap();
    
    private final long startTime;
    private final String resourcePath;
    private final Map<String, String> writeHeaders;

    @Override
    public void writeFile(HttpAcceptSession httpSession) throws IOException {

        // flush write headers
        if (!writeHeaders.isEmpty()) {
            for (String headerName : writeHeaders.keySet()) {
                String headerValue = writeHeaders.get(headerName);
                httpSession.addWriteHeader(headerName, headerValue);
            }
        }

        ResourceAddress localAddress = httpSession.getLocalAddress();
        File cacheDirectory = localAddress.getOption(TEMP_DIRECTORY);

        String resolvedResourcePath = resourcePath;
        if (resourcePath.endsWith(".js")) {
            resolvedResourcePath = resourcePath.replaceFirst("\\.js$", ".html");
        }
        File cacheFile = new File(cacheDirectory, resolvedResourcePath);
        File bridgeFile = supplyAsFile(cacheFile, startTime);

        // Note: per KG-866, we will be setting the content type even if the response
        // notes that the file hasn't been modified since last requested.
        String contentType = getContentType(getFileExtension(bridgeFile));
        if (contentType != null) {
            httpSession.setWriteHeader("Content-Type", contentType);
        }

        HttpUtils.writeIfModified(httpSession, bridgeFile);
    }
    
    HttpInjectedDynamicResource(String resourcePath) {
        this(resourcePath, EMPTY_WRITE_HEADERS);
    }
    
    HttpInjectedDynamicResource(String resourcePath, Map<String, String> writeHeaders) {
        this.resourcePath = resourcePath;
        this.writeHeaders = writeHeaders;
        this.startTime = System.currentTimeMillis();
    }


    private File supplyAsFile(File tempFile, long startTime) throws IOException {
        if (resourcePath.endsWith(".js")) {
            HttpUtils.supplyScriptAsHtml(tempFile, startTime, resourcePath);
        }
        else {
            HttpUtils.supplyFile(tempFile, startTime, resourcePath);
        }
        
        return tempFile;
    }

    // TODO: is this still necessary now that we are above HttpSession?
    private static String getContentType(String fileExtension) {
        if (fileExtension == null) {
            return null;
        }
                
        String contentType = null;
        
        fileExtension = fileExtension.toLowerCase();

        if ("html".equals(fileExtension)) {
            contentType = "text/html";
        }
        else if ("htm".equals(fileExtension)) {
            contentType = "text/html";
        }
        else if ("jar".equals(fileExtension)) {
            contentType = "application/java-archive";
        }
        else if ("js".equals(fileExtension)) {
            contentType = "text/javascript";
        }
        else if ("png".equals(fileExtension)) {
            contentType = "image/png";
        }
        else if ("gif".equals(fileExtension)) {
            contentType = "image/gif";
        }
        else if ("jpg".equals(fileExtension)) {
            contentType = "image/jpeg";
        }
        else if ("jpeg".equals(fileExtension)) {
            contentType = "image/jpeg";
        }
        else if ("css".equals(fileExtension)) {
            contentType = "text/css";
        }
        else if ("swf".equals(fileExtension)) {
            contentType = "application/x-shockwave-flash";
        }
        else if ("xap".equals(fileExtension)) {
            contentType = "application/x-silverlight-app";
        }
        else if ("htc".equals(fileExtension)) {
            contentType = "text/x-component";
        }
        else if ("jnlp".equals(fileExtension)) {
            contentType = "application/x-java-jnlp-file";
        }
        else if ("manifest".equals(fileExtension)) {
            contentType = "text/cache-manifest";
        }
        else if ("appcache".equals(fileExtension)) {
            contentType = "text/cache-manifest";
        }
        else if ("vtt".equals(fileExtension)) {
            contentType = "text/vtt";
        }
        else if ("aspx".equals(fileExtension)) {
            contentType = "text/html";
        }
        else if ("apk".equals(fileExtension)) {
            contentType = "application/vnd.android.package-archive";
        }

        return contentType;
    }

}

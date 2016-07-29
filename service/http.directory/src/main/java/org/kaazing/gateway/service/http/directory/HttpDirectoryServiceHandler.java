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

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;

import org.kaazing.gateway.service.ServiceContext;
import org.kaazing.gateway.service.ServiceProperties;
import org.kaazing.gateway.service.http.directory.cachecontrol.CacheControlHandler;
import org.kaazing.gateway.service.http.directory.cachecontrol.PatternCacheControl;
import org.kaazing.gateway.service.http.directory.cachecontrol.PatternMatcherUtils;
import org.kaazing.gateway.transport.IoHandlerAdapter;
import org.kaazing.gateway.transport.http.HttpAcceptSession;
import org.kaazing.gateway.transport.http.HttpHeaders;
import org.kaazing.gateway.transport.http.HttpMethod;
import org.kaazing.gateway.transport.http.HttpSession;
import org.kaazing.gateway.transport.http.HttpStatus;
import org.kaazing.gateway.transport.http.HttpUtils;
import org.kaazing.gateway.util.file.FileUtils;
import org.kaazing.mina.core.buffer.IoBufferAllocatorEx;
import org.kaazing.mina.core.buffer.IoBufferEx;

class HttpDirectoryServiceHandler extends IoHandlerAdapter<HttpAcceptSession> {

    private ServiceContext serviceContext;
    private File baseDir;
    private String welcomeFile;
    private File errorPagesDir;
    private boolean indexes;

    private List<PatternCacheControl> patterns;
    private Map<String, CacheControlHandler> urlCacheControlMap = new ConcurrentHashMap<>();

    private static final DateFormat RFC822_FORMAT_PATTERN =
            new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz", Locale.ENGLISH);
    private static final String SYMLINK_RESTRICTED = "restricted";

    static {
        RFC822_FORMAT_PATTERN.setTimeZone(TimeZone.getTimeZone("GMT"));
    }

    HttpDirectoryServiceHandler() {
    }

    ServiceContext getServiceContext() {
        return serviceContext;
    }

    void setServiceContext(ServiceContext serviceContext) {
        this.serviceContext = serviceContext;
    }

    void setBaseDir(File baseDir) {
        this.baseDir = baseDir;
    }

    File getBaseDir() {
        return baseDir;
    }

    void setWelcomeFile(String welcomeFile) {
        this.welcomeFile = welcomeFile;
    }

    String getWelcomeFile() {
        return welcomeFile;
    }

    void setErrorPagesDir(File errorPagesDir) {
        this.errorPagesDir = errorPagesDir;
    }

    boolean usingIndexes() {
        return indexes;
    }

    void setIndexes(boolean indexes) {
        this.indexes = indexes;
    }

    void setPatterns(List<PatternCacheControl> patterns) {
        this.patterns = patterns;
    }

    void emptyUrlCacheControlMap() {
        urlCacheControlMap.clear();
    }

    @Override
    public void doSessionCreated(HttpAcceptSession session) throws Exception {
        // NOOP no license check needed
    }

    @Override
    public void doSessionClosed(HttpAcceptSession session) throws Exception {
        // NOOP no license check needed
    }

    @Override
    protected void doExceptionCaught(HttpAcceptSession session, Throwable cause) throws Exception {
        // trigger sessionClosed to update connection capabilities accordingly
        session.close(true);
    }

    @Override
    protected void doSessionOpened(HttpAcceptSession session) throws Exception {
        // Get the method used for this request; if not a GET, refuse the
        // request (KG-1233).
        // (KG-11211) Enabling HEAD method too.
        HttpMethod method = session.getMethod();
        if (method != HttpMethod.GET && method != HttpMethod.HEAD) {
            reportError(session, HttpStatus.CLIENT_METHOD_NOT_ALLOWED);
            session.close(false);
            return;
        }

        // get relative path from service path
        String pathInfo = session.getPathInfo().getPath();

        // construct file reference from configured base directory
        File requestFile = new File(baseDir, "/" + pathInfo);
        // check if this is a directory reference
        if (requestFile.isDirectory()) {
            String requestPath = session.getRequestURI().getPath();

            // redirect to include trailing slash, if not present
            // this is important to resolve relative links correctly
            if (!requestPath.endsWith("/")) {
                session.setStatus(HttpStatus.REDIRECT_FOUND);
                // TODO: add queryString back for redirect
                session.setWriteHeader("Location", requestPath + "/");
                session.close(false);
                return;
            }
        }

        // if file is not under baseDir report an access denied error and close session
        boolean underBaseDir = false;
        File baseDirCannonical = baseDir.getCanonicalFile();

        for (File candidate = requestFile.getCanonicalFile(); candidate != null; candidate = candidate.getParentFile()) {
            if (candidate.equals(baseDirCannonical)) {
                underBaseDir = true;
                break;
            }
        }

        if (!underBaseDir) {
            reportError(session, HttpStatus.CLIENT_BAD_REQUEST);
            session.close(false);
            return;
        }

        // Make another check for the file being a directory, return the welcomeFile
        // or a directory listing as appropriate. This is done in a separate set
        // from the redirect to allow bad requests to be detected before possibly
        // generating a directory listing.
        if (requestFile.isDirectory()) {
            boolean generateIndex = usingIndexes();
            if (welcomeFile != null) {
                File testWelcomeFile = new File(requestFile, welcomeFile);

                // If the welcome file exists or the service is not generating
                // directory listings, then set the request file to the welcome
                // file. In the case where directory listings aren't being
                // generated, the Gateway will return a NOT_FOUND for a non-existent
                // welcome file.
                if (testWelcomeFile.exists() || !generateIndex) {
                    requestFile = testWelcomeFile;
                    generateIndex = false;
                }
            }

            // if there is no configured welcome file on the service, or no existing
            // welcome file in the directory then we can generate a listing
            if (generateIndex) {
                session.setWriteHeader("Content-Type", "text/html");
                ByteBuffer nioBuf = DirectoryListingUtils.createDirectoryListing(pathInfo, baseDir, requestFile);
                IoBufferAllocatorEx<?> allocator = session.getBufferAllocator();
                IoBufferEx buf = allocator.wrap(nioBuf);
                session.write(buf);
                session.close(false);
                return;
            }
        }

        // if file does not exist then report error and close session
        if (!requestFile.exists()) {
            reportError(session, HttpStatus.CLIENT_NOT_FOUND);
            session.close(false);
            return;
        }

        ServiceProperties properties = serviceContext.getProperties();
        String followSymlink = properties.get("symbolic-links");
        if (followSymlink == null) {
            followSymlink = SYMLINK_RESTRICTED;
        }

        if (Files.isSymbolicLink(requestFile.toPath())) {
            Path targetPath = Files.readSymbolicLink(requestFile.toPath());
            boolean symLinkUnderBaseDir = targetPath.startsWith(baseDirCannonical.getPath());
            if (SYMLINK_RESTRICTED.equals(followSymlink) && !symLinkUnderBaseDir) {
                reportError(session, HttpStatus.CLIENT_NOT_FOUND);
                session.close(false);
                return;
            }
        }

        String requestPath = requestFile.getPath().replaceAll("\\\\", "/");
        addCacheControl(session, requestFile, requestPath);

        // check to see if the file has been modified since the last request
        String etag = HttpUtils.getETagHeaderValue(requestFile);
        boolean modified = HttpUtils.hasBeenModified(session, etag, requestFile);
        if (!modified) {
            // file has not been modified so set status and close session
            session.setWriteHeader("ETag", etag);
            session.setWriteHeader("Last-Modified", RFC822_FORMAT_PATTERN.format(requestFile.lastModified()));
            session.setStatus(HttpStatus.REDIRECT_NOT_MODIFIED);
            session.close(false);
            return;
        }

        // add cached content file headers.
        HttpUtils.addLastModifiedHeader(session, requestFile);

        session.setWriteHeader("ETag", etag);

        // add the content type, based on file extension.
        String contentType = serviceContext.getContentType(FileUtils.getFileExtension(requestFile));
        if (contentType != null) {
            session.setWriteHeader("Content-Type", contentType);
        }

        // get io buffer for file
        IoBufferAllocatorEx<?> allocator = session.getBufferAllocator();
        IoBufferEx buf = HttpUtils.getBufferForFile(allocator, requestFile);

        // add content length
        session.setWriteHeader(HttpHeaders.HEADER_CONTENT_LENGTH, Integer.toString(buf.remaining()));

        // write buffer and close session
        session.write(buf);
        session.close(false);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("HttpDirectoryServiceHandler [baseDir=").append(baseDir);
        if (welcomeFile != null) {
            sb.append(",welcomeFile=").append(welcomeFile);
        }
        if (errorPagesDir != null) {
            sb.append(",errorPagesDir=").append(errorPagesDir);
        }
        sb.append("]");
        return sb.toString();
    }

    private void reportError(HttpAcceptSession session, HttpStatus status) throws IOException {
        session.setStatus(status);
        if (errorPagesDir != null && errorPagesDir.exists()) {
            String errorFileName = Integer.toString(status.code()) + ".html";
            File errorContentFile = new File(errorPagesDir, errorFileName);
            if (errorContentFile.exists() && errorContentFile.canRead()) {
                IoBufferAllocatorEx<?> allocator = session.getBufferAllocator();
                IoBufferEx buf = HttpUtils.getBufferForFile(allocator, errorContentFile);
                session.setWriteHeader(HttpHeaders.HEADER_CONTENT_LENGTH, Integer.toString(buf.remaining()));
                session.write(buf);
            }
        }

    }

    /**
     * Matches the file URL with the most specific pattern and caches this information in a map
     * Sets cache-control and expires headers
     * @param session
     * @param requestFile
     * @param requestPath
     */
    private void addCacheControl(HttpAcceptSession session, File requestFile, String requestPath) {
        CacheControlHandler cacheControlHandler = urlCacheControlMap.computeIfAbsent(requestPath, 
                path -> patterns.stream()
                     .filter(patternCacheControl -> PatternMatcherUtils.caseInsensitiveMatch(requestPath, patternCacheControl.getPattern()))
                     .findFirst()
                     .map(patternCacheControl -> new CacheControlHandler(requestFile, patternCacheControl))
                     .orElse(null)
        );

        if (cacheControlHandler != null) {
            addCacheControlHeader(session, requestFile, cacheControlHandler);
        }
    }

    private static final void addCacheControlHeader(HttpSession session, File requestFile,
        CacheControlHandler cacheControlHandler) {
        cacheControlHandler.resetState();
        session.setWriteHeader("Cache-Control", cacheControlHandler.getCacheControlHeader());
        addExpiresHeader(session, cacheControlHandler.getExpiresHeader());
    }

    private static void addExpiresHeader(HttpSession session, long time) {
        session.setWriteHeader("Expires", RFC822_FORMAT_PATTERN.format(time));
    }

    static class DirectoryListingUtils {
        private static final String[] SIZE_UNITS = new String[] { " bytes", " KB", " MB", " GB", " TB", " PB", " EB", " ZB" };
        private static final String HEADER_BOILERPLATE = "<!DOCTYPE HTML><html><head><meta http-equiv=\"Content-Type\" content=\"text/html; charset=utf-8\"/><title>Directory listing for ";
        private static final String BODY_BOILERPLATE = "</title></head><body><h1>Index of ";
        private static final String TABLE_BOILERPLATE = "<table><thead><tr><th>Filename</th><th class=\"number\">Size</th><th class=\"date\">Last Modified Date</th></tr></thead><tbody>";
        private static final String FOOTER_BOILERPLATE = "</tbody></table><address>Powered by Kaazing WebSocket Gateway</address></body></html>";
        private static final String STYLE_BOILERPLATE = "</h1><style type=\"text/css\">\n\nbody {\n    font-family: Arial, Helvetica, sans-serif;\n" +
                                                        "}\n\na {\n    color: #ff6600;\n    text-decoration: none;\n}\n\na:hover {\n    text-decoration: underline;\n" +
                                                        "}\n\ntable {\n    border-spacing: 0px;\n    border-bottom: solid 1px #fcdac2;\n}\n\n" +
                                                        "th {\n    text-align: left;\n    border-top: solid 1px #fcdac2;\n    border-bottom: solid 1px #fcdac2;\n" +
                                                        "    background-color: #fcf1de;\n}\n\nth {\n    padding-left: 40px;\n    padding-top: 6px;\n    padding-bottom: 6px;\n" +
                                                        "}\n\ntd {\n    padding-left: 40px;\n    padding-top: 3px;\n    padding-bottom: 3px;\n}\n\n" +
                                                        "th:first-child, td:first-child {\n    padding-left: 5px;\n}\n\nth:last-child, td:last-child {\n" +
                                                        "    padding-right: 5px;\n}\n\ntr:nth-child(even) {\n    background-color: #fcf6eb;\n}\n\n" +
                                                        "td.number, th.number, td.date, th.date {\n    text-align: right;\n}\n\naddress {\n" +
                                                        "    color: #999;\n    font-size: smaller;\n    margin-top: 6px;\n}\n\n</style>";
        private static final DateFormat df = new SimpleDateFormat("dd-MMM-yyyy HH:mm");

        public static ByteBuffer createDirectoryListing(String requestPath, File baseDir, File directory) throws Exception {
            StringBuffer sb = new StringBuffer();
            sb.append(HEADER_BOILERPLATE);

            String pathName = getPathName(baseDir, directory);
            if ("".equals(pathName)) {
                pathName = "/";
            }

            sb.append(pathName);

            sb.append(BODY_BOILERPLATE);
            sb.append(pathName);
            sb.append(STYLE_BOILERPLATE); // there's some static CSS to include in the page, add it now

            sb.append(TABLE_BOILERPLATE);

            if (!"/".equals(pathName)) {
                // add a link to the parent directory
                sb.append("<tr><td><a href=\"" + getParentPath(requestPath) +  "\">.. (Parent Directory)</a></td><td</td><td></td></tr>");
            }

            File[] files = sortFiles(directory.listFiles());

            // build the HTML response
            if (files != null) {
                for (File file : files) {
                    sb.append(createDirectoryListRow(file));
                }
            }

            sb.append(FOOTER_BOILERPLATE);
            return ByteBuffer.wrap(sb.toString().getBytes());
        }

        private static File[] sortFiles(File[] unsortedFiles) {
            if ((unsortedFiles == null) || (unsortedFiles.length == 0)) {
                return null;
            }

            TreeMap<String, File> subDirMap = new TreeMap<>();
            TreeMap<String, File> fileMap = new TreeMap<>();

            // When creating a listing, directories are grouped first, then
            // files.  Each group is sorted separately.
            for (File file : unsortedFiles) {
                if (file.isDirectory()) {
                    subDirMap.put(file.getName(), file);
                } else {
                    fileMap.put(file.getName(), file);
                }
            }

            int count = 0;
            File[] sortedFiles = new File[unsortedFiles.length];
            for (String key : subDirMap.keySet()) {
                sortedFiles[count++] = subDirMap.get(key);
            }
            for (String key : fileMap.keySet()) {
                sortedFiles[count++] = fileMap.get(key);
            }

            return sortedFiles;
        }

        private static String createDirectoryListRow(File file) {
            StringBuffer sb = new StringBuffer();
            if (file.isDirectory()) {
                // if it's a directory, append a slash to the name to indicate such, don't show a file size
                sb.append("<tr><td><a href=\"" + file.getName() + "/\">" + file.getName() + "/</a>" +
                        "</td><td></td><td class=\"date\">" + getDateString(file.lastModified())  + "</td></tr>");
            } else {
                sb.append("<tr><td><a href=\"" + file.getName() + "\">" + file.getName() + "</a>" +
                        "</td><td class=\"number\">" + getFileSizeString((double)file.length()) +
                        "</td><td class=\"date\">" + getDateString(file.lastModified())  + "</td></tr>");
            }
            return sb.toString();
        }

        // Return the relative path of the target file in relation to this directory service's
        // base directory.  Since the base directory is the "root" in this case, we just want
        // paths under this root.
        private static String getPathName(File baseDir, File targetFile) throws Exception {
            File canonicalFile = targetFile.getCanonicalFile();
            File baseDirCanonical = baseDir.getCanonicalFile();

            if (canonicalFile.equals(baseDirCanonical)) {
                return "";
            } else {
                return getPathName(baseDirCanonical, canonicalFile.getParentFile()) + "/" + canonicalFile.getName();
            }
        }

        private static String getParentPath(String path) {
            String parentPath = path;
            if (parentPath.endsWith("/")) {
                parentPath = parentPath.substring(0, parentPath.length()-1);
            }

            int lastSlashIndex = parentPath.lastIndexOf("/");
            if (lastSlashIndex != -1) {
                // add 1 to include the last slash which skips a redirect when following the link
                return parentPath.substring(0, lastSlashIndex+1);
            }
            return parentPath;
        }

        // Return a string representation of the given number of bytes.  This makes an attempt to
        // use the largest units possible to display the file size while still having a size greater
        // that 1 in those units.  For example, 400 is displayed at "400 bytes" but 4096 is displayed
        // as "4 KB."
        private static String getFileSizeString(double fileSize) {
            int count = 0;
            while (fileSize > 1024) {
                fileSize = fileSize / 1024;
                count++;
            }

            DecimalFormat format = new DecimalFormat("0.0");
            return format.format(fileSize) + SIZE_UNITS[count];
        }

        private static String getDateString(long timestamp) {
            return df.format(new Date(timestamp));
        }
    }
}

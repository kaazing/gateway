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
package org.kaazing.gateway.util.aws;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.security.SignatureException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.StringTokenizer;
import java.util.TimeZone;
import java.util.TreeMap;
import java.util.TreeSet;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

public final class AwsUtils {
    private AwsUtils() {
    }

    private static final Logger LOG = LoggerFactory.getLogger(AwsUtils.class);

    private static final String UTF8_CHARSET = "UTF-8";
    private static final String HMAC_SHA1_ALGORITHM = "HmacSHA1";
    private static final String HMAC_SHA256_ALGORITHM = "HmacSHA256";

    /**
     * Returns the AccountId of the user who is running the instance.
     *
     * @return String       representing the AccountId or the owner-id
     * @throws java.io.IOException  if failed to retrieve the AccountId using the
     *                      Cloud infrastructure
     */
    public static String getAccountId() throws IOException {
        // Get the MAC address of the machine.
        String macUrl = getMetadataUrl() + "/network/interfaces/macs/";
        String mac = invokeUrl(macUrl).trim();

        // Use the MAC address to obtain the owner-id or the
        // AWS AccountId.
        String idUrl = macUrl + mac + "owner-id";
        String acctId = invokeUrl(idUrl).trim();

        assert  acctId != null;
        return acctId;
    }

    /**
     * Returns the URL that is used to fetch the image metadata. Based on
     * the Cloud Vendor/Provider, this will be different.
     *
     * @return String    URL to be used to retrieve image metadata
     */
    public static String getMetadataUrl() {
        return "http://169.254.169.254/latest/meta-data";
    }

    /**
     * Returns the region in which the instance is running.
     *
     * @return String       representing the region where the instance is
     *                      running
     * @throws java.io.IOException  if failed to retrieve the region information
     *                      using the Cloud infrastructure
     */
    public static String getRegion() throws IOException {
        String url = getMetadataUrl() + "/placement/availability-zone";
        String zone = invokeUrl(url);
        zone = zone.trim();

        // In case of AWS, the zone includes an extra character
        // at the end such as "us-east-1a", "us-east-1b", "eu-west-1a",
        // etc. We have to strip that last character to get the
        // correct region.
        String region = zone.substring(0, zone.length() - 1);
        assert region != null;
        return region;
    }

    /**
     * Returns the name of the security group from the list that is
     * obtained from the resource vendor. An instance may belong to multiple
     * security groups. And, the list of security groups obtained from the
     * vendor may not be ordered. If the vendor supports the notion of
     * a default security group, then that should be returned. Otherwise,
     * the implementation will be vendor-specific.
     *
     * @return
     * @throws java.io.IOException
     */
    public static String getSecurityGroupName() throws IOException {
        // For AWS, we are returning the first security group from the list
        // that is obtained by querying the meta-data.

        String url = getMetadataUrl() + "/security-groups";
        String groups = invokeUrl(url);

        if ((groups == null) || (groups.trim().length() == 0)) {
            String msg = "No security-group assigned to the instance";
            throw new IllegalStateException(msg);
        }

        StringTokenizer tokenizer = new StringTokenizer(groups, "\n");
        return tokenizer.nextToken(); // We only need the first one.
    }

    /**
     * Returns the local address (IPv4) of the instance.  The local address
     * is defined to be
     *       Public IP address if launched with direct addressing; private IP
     *       address if launched with public addressing.
     *
     * @return local IP address (IPv4) of the instance
     * @throws java.io.IOException
     */
    public static String getLocalIPv4() throws IOException {
        String url = getMetadataUrl() + "/local-ipv4";
        String localIPv4 = invokeUrl(url);

        if ((localIPv4 == null) || (localIPv4.trim().length() == 0)) {
            String msg = "No local IPv4 assigned to the instance";
            throw new IllegalStateException(msg);
        }

        return localIPv4.trim();
    }

    /**
     * Returns the URL that is used to fetch the instance's user-data.
     * Based on the Cloud Vendor/Provider, this will be different.
     *
     * @return String    URL to be used to retrieve image metadata
     */
    public static String getUserdataUrl() {
        return "http://169.254.169.254/latest/user-data";
    }

    /**
     * Creates a "SignatureVersion 1" based signed request. The requestURI
     * parameter includes the <protocol>://<endpoint>/<uri> in it. This
     * method will ONLY add "Timestamp", "AWSAccessKeyId", "SignatureVersion"
     * and "Signature" parameters. It's the callers responsibility to include
     * other parameters in the params map. The other parameters will be specific
     * to the the REST API that is being invoked. So, the caller should include
     * parameters such as "Action", etc.
     *
     * @param uri                format will <protocol>://<endpoint>/<resource>
     * @param params             Map with parameters such as "Action", etc.
     * @param awsAccessKeyId     AccessKeyId of the caller to create a signature
     * @param awsSecretKey       SecretKey of the caller to create the signature
     * @return String that is a completely signed URL based on
     *         "SignatureVersion 1" scheme
     * @throws java.security.SignatureException
     */
    public static String getVersion1SignedRequest(String              uri,
                                                  Map<String, String> params,
                                                  String              awsAccessKeyId,
                                                  String              awsSecretKey)
           throws SignatureException {
        if ((params == null)         ||
            (awsAccessKeyId == null) ||
            (awsSecretKey == null)   ||
            (uri == null)) {
            throw new IllegalArgumentException("Null parameter passed in");
        }

        params.put("AWSAccessKeyId", awsAccessKeyId);
        params.put("SignatureVersion", "1");
        params.put("Timestamp", getTimestamp());

        String stringToSign = getV1StringToSign(params);
        String signature = createV1Signature(stringToSign,
                                             awsSecretKey,
                                             HMAC_SHA1_ALGORITHM);
        params.put("Signature", signature);


        // Encode the query parameter values and construct the
        // query string.
        StringBuffer queryString = new StringBuffer();
        for (Map.Entry<String, String> entry : params.entrySet()) {
            String value = entry.getValue();
            String encValue;

            try {
                encValue = URLEncoder.encode(value, UTF8_CHARSET);
            } catch (UnsupportedEncodingException e) {
                encValue = value;
            }

            String separator = (queryString.length() == 0) ? "?" : "&";
            queryString.append(separator);
            queryString.append(entry.getKey() + "=" + encValue);
        }

        return uri + queryString.toString();
    }

    /**
     * This method returns a complete signed request using HmaSHA256 algorithm
     * using the "SignatureVersion 2" scheme. Currently, this method creates
     * "SignatureVersion 2" based signed requests ONLY for the HTTP "GET"
     * method. It's the callers the responsibility such as "Action" and such
     * based on the REST API that they are trying to exercise. This method adds
     * "SignatureVersion", "SignatureMethod", "AWSAccessKeyId", "Timestamp", and
     * "Signature" parameters to the request.
     *
     * @param requestMethod     Only "GET at this point.
     * @param endpoint          endpoint or the host
     * @param requestURI        following the endpoint up until the query params
     * @param params            Map of name-value pairs containing params such
     *                          as "Action", etc.
     * @param awsAccessKeyId    AccessKeyId of the caller to create a signature
     * @param awsSecretKey      SecretKey of the caller to create the signature
     * @return String   the complete URL with proper encoding and the Signature
     *                  query param appended
     * @throws java.security.SignatureException
     */
    public static String getVersion2SignedRequest(String              requestMethod,
                                                  String              protocol,
                                                  String              endpoint,
                                                  String              requestURI,
                                                  Map<String, String> params,
                                                  String              awsAccessKeyId,
                                                  String              awsSecretKey)
            throws SignatureException {
        if ((requestMethod == null)  ||
            (protocol == null)       ||
            (endpoint == null)       ||
            (requestURI == null)     ||
            (params == null)         ||
            (awsAccessKeyId == null) ||
            (awsSecretKey == null)) {
            throw new IllegalArgumentException("Null parameter passed in");
        }

        params.put("AWSAccessKeyId", awsAccessKeyId);
        params.put("SignatureMethod", HMAC_SHA256_ALGORITHM);
        params.put("SignatureVersion", "2");
        params.put("Timestamp", getTimestamp());

        String canonicalQS = getV2CanonicalizedQueryString(params);
        String stringToSign = requestMethod + "\n" +
                              endpoint      + "\n" +
                              requestURI    + "\n" +
                              canonicalQS;
        String signature = createSignature(stringToSign,
                                           awsSecretKey,
                                           HMAC_SHA256_ALGORITHM);
        String request = protocol + "://" + endpoint + requestURI
                             + "?" + canonicalQS
                             + "&Signature=" + signature;

        return request;
    }

    /**
     * This is a generic method to invoke the REST API. The URL that is passed
     * in should result in a signed request with the "Signature" query parameter
     * included. It's the caller's responsibility to deal with the return value
     * which may be a simple string or a XML response.
     *
     * @param url           represents a signed request with "Signature" param
     * @return String       representing the outcome of the REST API call
     * @throws java.io.IOException  is thrown if the connection times out or the query
     *                      params are incorrectly specified
     */
    public static String invokeUrl(String url) throws IOException {
        if (url == null) {
            throw new IllegalArgumentException("Null parameter passed in");
        }

        URL               urlObj;
        InputStream       inStream = null;
        String            response = null;
        HttpURLConnection connection = null;

        try {
          // Create the HttpURLConnection.
          urlObj = new URL(url);
          connection = (HttpURLConnection) urlObj.openConnection();

          // Only need HTTP GET.
          connection.setRequestMethod("GET");

          // Set 2seconds timeout interval.
          connection.setConnectTimeout(2 * 1000);
          connection.setReadTimeout(2 * 1000);

          connection.connect();

          // Read the output from the server.

          try {
              inStream = connection.getInputStream();
              // System.out.println("Return Code: " + connection.getResponseCode());
          }
          catch (IOException ex) {
              // Check the error stream for additional information that can
              // be useful to address the issue. If there is nothing in the
              // response body, HttpURLConnection.getErrorStream() returns a
              // null.
              inStream = connection.getErrorStream();
              if (inStream == null) {
                  // If connection.getErrorStream() is null, just use the
                  // message from the exception.
                  response = ex.getMessage();
              }
              else {
                  // Otherwise, get the error stream content and use it as
                  // a message for the new IOException instance that wraps the
                  // original IOException instance.
                  response = getResponse(inStream);
                  inStream.close();
                  inStream = null; // To deal with the check in finally.
              }

              throw new IOException(url + "\n" + response, ex);
          }

          response = getResponse(inStream);
        }
        finally {
            if (inStream != null) {
              try {
                inStream.close();
              }
              catch (IOException ioe) {
                // Swallow this exception.
              }
           }

            if (connection != null) {
                connection.disconnect();
            }
        }

        return response;
    }

    /**
     * Indicates whether the Gateway is currently deployed in AWS environment
     * by returning true. Otherwise, it returns false.
     *
     * It could either be DevPay or non-DevPay license.
     *
     * @return boolean
     */
    public static boolean isDeployedToAWS() {
        try {
            // Ping the AWS-specific meta-data URL to figure out whether the
            // Gateway is deployed in a AWS Cloud environment.
            invokeUrl("http://169.254.169.254/latest/meta-data");
        }
        catch (IOException ex) {
            return false;
        }

        return true;
    }

    /**
     * Returns a Document(DOM) object representing the parsed XML.
     *
     * @param xmlStr
     * @return Document
     */
    public static Document parseXMLResponse(String xmlStr) {
        if ((xmlStr == null) || (xmlStr.length() == 0)) {
            return null;
        }

        // FIXME: error handling for the DOM parsing...
        try {
            DocumentBuilder db = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            return db.parse(new ByteArrayInputStream(xmlStr.getBytes()));
        } catch (ParserConfigurationException | IOException | SAXException pcex) {
            //ignore
        }

        return null;
    }

    // ------------------------- Private Methods -----------------------------
    private static String createSignature(String     stringToSign,
                                          String     awsSecretKey,
                                          String     algorithm)
            throws SignatureException {

        assert stringToSign != null;
        assert awsSecretKey != null;
        assert algorithm != null;

        String signature;

        try {
            byte[] secretyKeyBytes = awsSecretKey.getBytes(UTF8_CHARSET);
            SecretKeySpec secretKeySpec = new SecretKeySpec(secretyKeyBytes,
                                                            algorithm);
            Mac mac = Mac.getInstance(algorithm);
            mac.init(secretKeySpec);

            byte[] data = stringToSign.getBytes(UTF8_CHARSET);

            byte[] rawHmac = mac.doFinal(data);
            signature = rfc3986Conformance(new String(Codec.base64Encode(rawHmac)));
        } catch (Exception e) {
            throw new SignatureException("Failed to generate HMAC : " +
                                                               e.getMessage());
        }

        return signature;
    }

    private static String getResponse(InputStream in) {
        if (in == null) {
            return null;
        }

        InputStreamReader inReader = new InputStreamReader(in);
        BufferedReader reader = new BufferedReader(inReader);
        StringBuilder strBuilder = new StringBuilder();

        try {
            String line;
            while ((line = reader.readLine()) != null) {
                strBuilder.append(line + "\n");
            }
        }
        catch (Exception ex) {
            return null;
        }
        finally {
            if (reader != null) {
                try {
                    reader.close();
                }
                catch (IOException ioe) {
                    // Swallow this exception.
                }
            }
        }

        String response = (strBuilder.length() > 0) ? strBuilder.toString() :
                                                      null;
        return response;
    }

    private static String createV1Signature(String stringToSign, String awsSecretKey, String algorithm)
            throws SignatureException {
        String signature;

        if ((stringToSign == null) ||
            (awsSecretKey == null) ||
            (algorithm == null)) {
            return null;
        }

        try {
            byte[] secretyKeyBytes = awsSecretKey.getBytes();
            SecretKeySpec secretKeySpec = new SecretKeySpec(secretyKeyBytes, algorithm);
            Mac mac = Mac.getInstance(algorithm);
            mac.init(secretKeySpec);

            byte[] data = stringToSign.getBytes();

            byte[] rawHmac = mac.doFinal(data);
            signature = Codec.base64Encode(rawHmac);
        } catch (Exception e) {
            throw new SignatureException("Failed to generate HMAC : " + e.getMessage());
        }

        return signature;
    }

    private static String getTimestamp() {
        Calendar   cal = Calendar.getInstance();
        DateFormat dfm = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
        dfm.setTimeZone(TimeZone.getTimeZone("GMT"));
        return dfm.format(cal.getTime());
    }

    private static String getV1StringToSign(Map<String, String> paramMap) {
        assert paramMap != null;

        Set<String> sortedKeys = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        sortedKeys.addAll(paramMap.keySet());

        // Don't include "Signature" in the string to sign.
        sortedKeys.remove("Signature");

        StringBuilder stringBuilder = new StringBuilder();
        for (String key : sortedKeys) {
            stringBuilder.append(key);
            stringBuilder.append(paramMap.get(key));
        }

        return stringBuilder.toString();
    }

    private static String getV2CanonicalizedQueryString(Map<String, String> params) {
        assert params != null && !params.isEmpty();

        SortedMap<String, String> sortedMap = new TreeMap<>(params);

        // Remove "Signature" parameter, if added.
        sortedMap.remove("Signature");

        StringBuffer buffer = new StringBuffer();
        Iterator<Map.Entry<String, String>> iter = sortedMap.entrySet().iterator();

        while (iter.hasNext()) {
            Map.Entry<String, String> kvpair = iter.next();
            buffer.append(rfc3986Conformance(kvpair.getKey()));
            buffer.append("=");
            buffer.append(rfc3986Conformance(kvpair.getValue()));
            if (iter.hasNext()) {
                buffer.append("&");
            }
        }

        return buffer.toString();
    }

    // Based on RFC 3986 and AWS doc, further encode certain characters.
    private static String rfc3986Conformance(String s) {
        assert s != null;

        String out;

        if (s == null) {
            return null;
        }

        try {
            out = URLEncoder.encode(s, UTF8_CHARSET).replace("+", "%20")
                    .replace("*", "%2A").replace("%7E", "~");
        } catch (UnsupportedEncodingException e) {
            out = s;
        }
        return out;
    }
}

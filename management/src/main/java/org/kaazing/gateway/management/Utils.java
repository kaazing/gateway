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
package org.kaazing.gateway.management;

import java.util.List;
import java.util.concurrent.Future;

import org.kaazing.gateway.resource.address.ResourceAddress;
import org.kaazing.gateway.resource.address.uri.URIUtils;
import org.kaazing.gateway.transport.AbstractBridgeAcceptor;
import org.kaazing.gateway.transport.AbstractBridgeConnector;
import org.kaazing.gateway.transport.wseb.WsebSession;
import org.kaazing.gateway.transport.wsn.WsnSession;
import org.kaazing.mina.core.service.IoAcceptorEx;
import org.kaazing.mina.core.service.IoConnectorEx;
import org.kaazing.mina.core.service.IoServiceEx;
import org.kaazing.mina.core.session.IoSessionEx;
import org.snmp4j.smi.AbstractVariable;
import org.snmp4j.smi.Null;
import org.snmp4j.smi.OctetString;

/**
 * Utilities useful at a couple levels of the management support. In particular, both the SessionManagementBean and the
 * ServiceManagementBean want to send information about the type, family and direction (accept/connect/...) of a given session
 * over to the Command Center.
 */
public class Utils {

    protected Utils() { }

    // For letting management decide if a session is a WS type or something else.
    public enum ManagementSessionType {
        NATIVE, EMULATED, OTHER
    }

    public static final String QUOTE = "\"";
    public static final String COMMA = ",";

    public static final String ACCEPT_DIRECTION = "ACCEPT";
    public static final String CONNECT_DIRECTION = "CONNECT";
    public static final String UNKNOWN_DIRECTION = "<unknown>";

    /**
     * Create a descriptive string for the type of connection that this session has been created on. This is generally just used
     * in the Command Center. The following is the same as the insides of ResourceAddress.getProtocol(), which is private. NOTE:
     * we're using BridgeSession.LOCAL_ADDRESS.get(session) to retrieve the address to send to management.
     */
    public static String getSessionTypeName(ResourceAddress address) {
        String externalURI = address.getExternalURI();
        return URIUtils.getScheme(externalURI);
    }

    /**
     * Determine the session 'direction' (accept/connect/???). We need this to be determined just once, so that it hangs around
     * after the session is closed.
     */
    public static String getSessionDirection(IoSessionEx session) {
        IoServiceEx service = session.getService();
        String connectionDirection;

        if (service instanceof IoAcceptorEx || service instanceof AbstractBridgeAcceptor) {
            connectionDirection = ACCEPT_DIRECTION;
        } else if (service instanceof IoConnectorEx || service instanceof AbstractBridgeConnector) {
            connectionDirection = CONNECT_DIRECTION;
        } else {
            connectionDirection = UNKNOWN_DIRECTION;
        }

        return connectionDirection;
    }

    public static String getCauseString(Throwable cause) {
        StringBuffer sb = new StringBuffer();
        Throwable t = cause;
        while (t != null) {
            String className = t.getClass().getName();
            String message = t.getMessage();
            sb.append(className + ((message == null) ? "" : ("=" + message)));
            sb.append('\n');
            t = t.getCause();
        }
        return sb.toString();
    }

    // For testing, assert whether we are or are not on an IO thread now.
    // flag = true means we expect that we ARE on the given thread, false
    // means we expect that we are NOT.
    public static void assertIOThread(Thread t, boolean flag) {
        if (flag) {
            assert t.equals(Thread.currentThread()) : "Management NOT on IO thread when expected";  // XXX testing
        } else {
            assert !(t.equals(Thread.currentThread())) : "Management on IO thread when not expected";  // XXX testing
        }
    }

    /**
     * Given futures from the various ThreadServiceStats, wait for them to finish and add their numeric values together. The
     * Futures are all Future<Object> so we don't have to have lots of different ones, so we coerce types here.
     *
     * @param futures
     * @return
     */
    // This must NOT run on any IO worker thread
    public static long sumFutures(List<Future<Object>> futures) {
        long total = 0;

        for (Future<Object> f : futures) {
            try {
                total += (Long) f.get();
            } catch (Exception ignore) {
                System.out.println("### sumFutures got exception!");
            }
        }

        return total;
    }

    public static ManagementSessionType getManagementSessionType(IoSessionEx session) {
        if (session instanceof WsnSession) {
            return ManagementSessionType.NATIVE;
        } else if (session instanceof WsebSession) {
            return ManagementSessionType.EMULATED;
        } else {
            return ManagementSessionType.OTHER;
        }
    }

    /**
     * Given an array of strings, return a string that equals the output of creating a JSONArray and then stringifying it.
     *
     * @param values
     * @return
     */
    public static String makeJSONArrayString(Object[] values) {
        // Since they won't change during a run, compute the JSON value for the
        // summary data fields (an array where all field names are wrapped in quotes
        StringBuilder buf = new StringBuilder();
        int numVals = values.length;

        buf.append("[");
        for (int i = 0; i < numVals; i++) {
            buf.append(QUOTE);
            buf.append(values[i].toString());
            buf.append(QUOTE);
            if (i < numVals - 1) {
                buf.append(COMMA);
            }
        }
        buf.append("]");

        return buf.toString();
    }

    /**
     * Convert a server-side string into either an OctetString (if the string is non-null) or Null.instance ("the value of this
     * OID is null") for transmission. This is so the client side can tell the difference between real nulls and empty values.
     *
     * @param s
     * @return
     */
    public static AbstractVariable stringToVariable(String s) {
        return s == null ? Null.instance : new OctetString(s);
    }

    /**
     * Convert a server-side byte array into either an OctetString (if the array is non-null) or Null.instance ("the value of
     * this OID is null") for transmission. This is so the client side can tell the difference between real nulls and empty
     * values.
     *
     * @param s
     * @return
     */
    public static AbstractVariable byteArrayToVariable(byte[] b) {
        return b == null ? Null.instance : new OctetString(b);
    }

    /**
     * For logging/debug, return the last part of the class name of an object
     */
    public static String getClassName(Object obj) {
        String className = obj.getClass().getName();
        className = className.substring(className.lastIndexOf('.') + 1);
        return className;
    }
}

/**
 * Copyright (c) 2007-2012, Kaazing Corporation. All rights reserved.
 */

package org.kaazing.gateway.server.context.resolve.utils;

import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import org.kaazing.gateway.util.GL;

/**
 * Utils class providing needed methods for performing network interface resolution
 *
 */
public class ResolutionUtils {
    private static Enumeration<NetworkInterface> networkInterfaces = null;
    // static block resolving the network interfaces
    static {
        try {
            networkInterfaces = NetworkInterface.getNetworkInterfaces();
        } catch (SocketException socketEx) {
            GL.debug("server", "Unable to resolve device URIs, processing URIs without device resolution.");
        }
    }

    /**
     * Method performing String to URI resolution
     * @param uri
     * @param includeIPv6
     * @return
     */
   public static List<URI> resolveStringUriToURIList(String uri, boolean includeIPv6) {
        // The URI might be a device name/port, e.g. @eth0:5942 or [@eth0:1]:5942, so make sure to
        // resolve device names before continuing.
        List<URI> resolvedDeviceURIs = new ArrayList<URI>();
        if (networkInterfaces != null) {
            if (uri.contains("://") && (uri.lastIndexOf(':') > 0)) {
                String[] schemeAndHost = uri.split("://");
                if (schemeAndHost.length == 2) {
                    String host = schemeAndHost[1].substring(0, schemeAndHost[1].lastIndexOf(':'));
                    String port = schemeAndHost[1].substring(schemeAndHost[1].lastIndexOf(':') + 1);
                    List<String> resolvedAddresses = resolveDeviceAddress(host, includeIPv6);
                    for (String resolvedAddress : resolvedAddresses) {
                        resolvedDeviceURIs.add(URI.create(schemeAndHost[0] + "://" + resolvedAddress + ":" + port));
                    }
                }
            }
        }

        if (resolvedDeviceURIs.isEmpty()) {
            resolvedDeviceURIs.add(URI.create(uri));
        }
        return resolvedDeviceURIs;
    }

   /**
    * Methos performing device address resolution
    * @param deviceName
    * @param includeIPv6
    * @return
    */
   private static List<String> resolveDeviceAddress(String deviceName, boolean includeIPv6) {
        List<String> resolvedAddresses = new ArrayList<String>();
        if (deviceName.startsWith("[@") && deviceName.endsWith("]")) {
            deviceName = deviceName.substring(2, deviceName.lastIndexOf(']'));
        } else if (deviceName.startsWith("@")) {
            deviceName = deviceName.substring(1);
        }

        while (networkInterfaces.hasMoreElements()) {
            NetworkInterface networkInterface = networkInterfaces.nextElement();
            if (deviceName.equals(networkInterface.getName())) {
                Enumeration<InetAddress> inetAddresses = networkInterface.getInetAddresses();
                while (inetAddresses.hasMoreElements()) {
                    InetAddress inetAddress = inetAddresses.nextElement();
                    if (inetAddress instanceof Inet6Address) {
                        if (!includeIPv6) {
                            continue;
                        }
                        String inet6HostAddress = inetAddress.getHostAddress();
                        resolvedAddresses.add(String.format("[%s]", inet6HostAddress));
                    } else {
                        resolvedAddresses.add(inetAddress.getHostAddress());
                    }
                }
            }

            // add an internal URI for any sub interfaces that match the hostAddress
            Enumeration<NetworkInterface> subInterfaces = networkInterface.getSubInterfaces();
            if ((subInterfaces != null) && subInterfaces.hasMoreElements()) {
                resolvedAddresses.addAll(resolveDeviceAddress(deviceName, includeIPv6));
            }
        }

        return resolvedAddresses;
    }
}

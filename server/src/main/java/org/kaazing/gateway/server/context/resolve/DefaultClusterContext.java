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
package org.kaazing.gateway.server.context.resolve;

import static org.kaazing.gateway.server.context.resolve.DefaultServiceContext.BALANCER_MAP_NAME;
import static org.kaazing.gateway.server.context.resolve.DefaultServiceContext.MEMBERID_BALANCER_MAP_NAME;

import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;
import java.util.logging.Level;
import java.util.logging.LogRecord;

import org.kaazing.gateway.resource.address.ResolutionUtils;
import org.kaazing.gateway.server.collections.ClusterCollectionsFactory;
import org.kaazing.gateway.service.cluster.ClusterConnectOptionsContext;
import org.kaazing.gateway.service.cluster.ClusterContext;
import org.kaazing.gateway.service.cluster.MemberId;
import org.kaazing.gateway.service.cluster.MembershipEventListener;
import org.kaazing.gateway.service.collections.CollectionsFactory;
import org.kaazing.gateway.util.GL;
import org.kaazing.gateway.util.Utils;
import org.kaazing.gateway.util.aws.AwsUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hazelcast.config.AwsConfig;
import com.hazelcast.config.Config;
import com.hazelcast.config.JoinConfig;
import com.hazelcast.config.MapConfig;
import com.hazelcast.config.MulticastConfig;
import com.hazelcast.config.NetworkConfig;
import com.hazelcast.config.TcpIpConfig;
import com.hazelcast.core.Cluster;
import com.hazelcast.core.EntryEvent;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import com.hazelcast.core.ITopic;
import com.hazelcast.core.Member;
import com.hazelcast.core.MemberAttributeEvent;
import com.hazelcast.core.MembershipEvent;
import com.hazelcast.core.MembershipListener;
import com.hazelcast.logging.LogEvent;
import com.hazelcast.logging.LogListener;
import com.hazelcast.logging.LoggingService;
import com.hazelcast.map.listener.EntryAddedListener;
import com.hazelcast.map.listener.EntryEvictedListener;
import com.hazelcast.map.listener.EntryRemovedListener;
import com.hazelcast.map.listener.EntryUpdatedListener;

/**
 * ClusterContext for KEG
 * <p/>
 * <br>Balancer data<ol> <li> HttpBalancerService.MEMBERID_BALANCER_MAP_NAME: <ul><li> List of balanced URIs for one member
 * <li>Key: Cluster member id <li>Value: Map(key: balancerURI, value: acceptURIs) </ul> <li>HttpBalancerService.BALANCER_MAP_NAME
 * <ul><li> List of balanced URIs for whole cluster <li>Key: balanceURI <li>Value: acceptURIs </ul> </ol>
 */
public class DefaultClusterContext implements ClusterContext, LogListener {

    private static final String CLUSTER_LOG_FORMAT = "HAZELCAST: [%s] - %s";
    private static final String INSTANCE_KEY_MAP = "instanceKeyMap";

    private static final String HAZELCAST_SOCKET_BIND_ANY_PROPERTY = "hazelcast.socket.bind.any";
    private static final String HAZELCAST_SHUTDOWNHOOK_ENABLED_PROPERTY = "hazelcast.shutdownhook.enabled";
    private static final String HAZELCAST_PHONE_HOME_ENABLED_PROPERTY = "hazelcast.phone.home.enabled";
    private static final String HAZELCAST_VERSION_CHECK_ENABLED_PROPERTY = "hazelcast.version.check.enabled";

    private final Logger logger = LoggerFactory.getLogger(GL.CLUSTER_LOGGER_NAME);

    private final String localInstanceKey = Utils.randomHexString(16);

    private CollectionsFactory collectionsFactory;
    private List<MemberId> localInterfaces = new ArrayList<>();
    private final List<MemberId> clusterMembers = new ArrayList<>();
    private final List<MembershipEventListener> membershipEventListeners = new ArrayList<>();
    private MemberId localNodeId;
    private final String clusterName;
    private HazelcastInstance clusterInstance;
    private final ClusterConnectOptionsContext connectOptions;
    private final AtomicBoolean clusterInitialized = new AtomicBoolean(false);

    public DefaultClusterContext(String name,
                                 List<MemberId> interfaces,
                                 List<MemberId> members) {
        this(name, interfaces, members, null);
    }

    public DefaultClusterContext(String name,
                                 List<MemberId> interfaces,
                                 List<MemberId> members,
                                 ClusterConnectOptionsContext connectOptions) {
        this.clusterName = name;
        this.localInterfaces.addAll(interfaces);
        this.clusterMembers.addAll(members);
        this.connectOptions = connectOptions;
    }

    @Override
    public void start() {

        // Check that we have either localInterfaces or clusterMembers
        if (localInterfaces.size() + clusterMembers.size() == 0) {
            // if no local interfaces
            if (localInterfaces.size() == 0) {
                GL.info(GL.CLUSTER_LOGGER_NAME, "No network interfaces specified in the gateway configuration");
                throw new IllegalArgumentException("No network interfaces specified in the gateway configuration");
            }

            // if no members
            if (clusterMembers.size() == 0) {
                GL.info(GL.CLUSTER_LOGGER_NAME, "No cluster members specified in the gateway configuration");
                throw new IllegalArgumentException("No cluster members specified in the gateway configuration");
            }
        }

        try {
            // from ha.xml and then add members interfaces
            Config config = initializeHazelcastConfig();

            initializeCluster(config);

            GL.info(GL.CLUSTER_LOGGER_NAME, "Cluster Member started: IP Address: {}; Port: {}; id: {}",
                    localNodeId.getHost(), localNodeId.getPort(), localNodeId.getId());
        } catch (Exception e) {
            GL.error(GL.CLUSTER_LOGGER_NAME, "Unable to initialize cluster due to an exception: {}", e);
            throw new RuntimeException(String.format("Unable to initialize cluster due to an exception: %s", e), e);
        }
    }

    @Override
    public void dispose() {
        if (clusterInstance != null) {
            // KG-5837: do not call Hazelcast.shutdownAll() since that will hobble all in-process gateways
            clusterInstance.getLifecycleService().shutdown();
        }
    }

    private Config initializeHazelcastConfig() throws Exception {

        Config hazelCastConfig = new Config();

        hazelCastConfig.getGroupConfig().setName(getClusterName());
        hazelCastConfig.getGroupConfig().setPassword("5942");
        hazelCastConfig.setProperty(HAZELCAST_PHONE_HOME_ENABLED_PROPERTY, "false");
        hazelCastConfig.setProperty(HAZELCAST_VERSION_CHECK_ENABLED_PROPERTY, "false");

        MapConfig mapConfig = hazelCastConfig.getMapConfig("serverSessions");
        mapConfig.setBackupCount(3);

        MapConfig sharedBalancerMapConfig = hazelCastConfig.getMapConfig(BALANCER_MAP_NAME);
        sharedBalancerMapConfig.setBackupCount(3);
        MapConfig memberBalancerMapConfig = hazelCastConfig.getMapConfig(MEMBERID_BALANCER_MAP_NAME);
        memberBalancerMapConfig.setBackupCount(3);

        // TO turn off logging in hazelcast API.
        // Note: must use Logger.getLogger, not LogManager.getLogger
        java.util.logging.Logger logger = java.util.logging.Logger.getLogger("com.hazelcast");
        logger.setLevel(Level.OFF);

        NetworkConfig networkConfig = new NetworkConfig();

        // disable port auto increment
        networkConfig.setPortAutoIncrement(false);

        // The first accepts port is the port used by all network interfaces.
        int clusterPort = (localInterfaces.size() > 0) ? localInterfaces.get(0).getPort() : -1;

        // initialize hazelcast
        if (clusterPort != -1) {
            networkConfig.setPort(clusterPort);
        }

        for (MemberId localInterface : localInterfaces) {
            String protocol = localInterface.getProtocol();
            if ("udp".equalsIgnoreCase(protocol) || "aws".equalsIgnoreCase(protocol)) {
                throw new IllegalArgumentException("Cannot accept on a multicast or aws address, use unicast address starting " +
                        "with tcp://");
            }

            // TODO: add IPv6 support when needed, as recent Hazelcast version have support for it
            // NOTE: The version of Hazelcast(1.9.4.8) that was being used did not support IPv6 address. The Hazelcast library
            //       threw NumberFormatException when IPv6 address was specified as an interface to bind to.
            String hostAddress = localInterface.getHost();
            InetAddress address = getResolvedAddressFromHost(hostAddress);

            if (address instanceof Inet6Address) {
                throw new IllegalArgumentException("ERROR: Cluster member accept url - '" + localInterface.toString() +
                        "' consists of IPv6 address which is not supported. Use Ipv4 address instead.");
            }

            // convertHostToIP method is used in order to address situations in which network interface syntax is present
            String hostConvertedToIP = convertHostToIP(localInterface.getHost());
            networkConfig.getInterfaces().addInterface(hostConvertedToIP);

            if (localInterface.getPort() != clusterPort) {
                throw new IllegalArgumentException("Port numbers on the network interfaces in <accept> do not match");
            }
        }

        boolean usingMulticast = false;

        JoinConfig joinConfig = networkConfig.getJoin();
        MulticastConfig multicastConfig = joinConfig.getMulticastConfig();

        // Disable multicast to avoid using the default multicast address 224.2.2.3:54327.
        // In this way, new cluster members joining the cluster won't accidentally join
        // due to having configured the default multicast address.  If multicast adresses are mentioned below,
        // we enable it.  See KG-6045 for an example of an accidental multicast cluster connection.
        multicastConfig.setEnabled(false);

        TcpIpConfig tcpIpConfig = joinConfig.getTcpIpConfig();
        List<InetSocketAddress> multicastAddresses = new ArrayList<>();
        List<InetSocketAddress> unicastAddresses = new ArrayList<>();
        MemberId awsMember = null;

        for (MemberId member : clusterMembers) {
            switch (member.getProtocol()) {
                case "udp":
                    // convertHostToIP method is used in order to address situations in which network interface syntax is present
                    multicastAddresses.add(new InetSocketAddress(convertHostToIP(member.getHost()), member.getPort()));
                    break;
                case "tcp":
                    // convertHostToIP method is used in order to address situations in which network interface syntax is present
                    unicastAddresses.add(new InetSocketAddress(convertHostToIP(member.getHost()), member.getPort()));
                    break;
                case "aws":
                    awsMember = member;

                    // There should be only one <connect> tag when AWS is being
                    // used. We have already validated that in
                    // GatewayContextResolver.processClusterMembers() method.
                    break;
            }
        }

        if (awsMember == null) {
            // Gateway is running in an on-premise env.

            int multicastAddressesCount = multicastAddresses.size();
            if (multicastAddressesCount > 1) {
                throw new IllegalArgumentException("Conflicting multicast discovery addresses in cluster configuration");
            } else if (multicastAddressesCount > 0) {
                if (AwsUtils.isDeployedToAWS()) {
                    throw new IllegalArgumentException("Multicast cluster configuration not supported on AWS, use " +
                            "aws://security-group/<security-group-name> in connect tag");
                }
                multicastConfig.setEnabled(true);
                InetSocketAddress multicastAddress = multicastAddresses.get(0);
                multicastConfig.setMulticastGroup(multicastAddress.getHostName());
                multicastConfig.setMulticastPort(multicastAddress.getPort());
            }

            if (unicastAddresses.size() > 0) {
                tcpIpConfig.setEnabled(!usingMulticast);
                for (InetSocketAddress unicastAddress : unicastAddresses) {
                    tcpIpConfig.addMember(
                            String.format("%s:%s", unicastAddress.getAddress().getHostAddress(), unicastAddress.getPort()));
                }
            }

            // Check if address specified to bind to is a wildcard address. If it is a wildcard address,
            // do not override the property PROP_SOCKET_BIND_ANY. If not, set the PROP_SOCKET_BIND_ANY to false so
            // that Hazelcast won't discard the interface specified to bind to.
            boolean useAnyAddress = false;

            // Check to see if the address specified is the wildcard address
            for (String networkInterface : networkConfig.getInterfaces().getInterfaces()) {
                InetAddress address = getResolvedAddressFromHost(networkInterface);
                if (address.isAnyLocalAddress()) {
                    // this will prevent PROP_SOCKET_BIND_ANY property from being overridden to false
                    // so that Hazelcast can bind to wildcard address
                    useAnyAddress = true;
                    break;
                }
            }

            // Override the PROP_SOCKET_BIND_ANY to false if the address to bind to is not wildcard address
            // Explicitly enable the interface so that Hazelcast will pick the one specified
            if (!useAnyAddress) {
                networkConfig.getInterfaces().setEnabled(true);
                hazelCastConfig.setProperty(HAZELCAST_SOCKET_BIND_ANY_PROPERTY, "false");
            }
        } else {
            // Gateway is running in the AWS/Cloud env.

            // Get rid of the leading slash "/" in the path.
            String path = awsMember.getPath();
            String groupName = null;

            if (path != null) {
                if ((path.indexOf('/') == 0) && (path.length() > 1)) {
                    groupName = path.substring(1, path.length());
                }
            }

            // If the groupName is not specified, then we will get it from
            // the meta-data.
            if ((groupName == null) || (groupName.length() == 0)) {
                groupName = AwsUtils.getSecurityGroupName();
            }

            multicastConfig.setEnabled(false);
            tcpIpConfig.setEnabled(false);
            AwsConfig awsConfig = joinConfig.getAwsConfig();
            awsConfig.setEnabled(true);
            awsConfig.setAccessKey(connectOptions.getAwsAccessKeyId());
            awsConfig.setSecretKey(connectOptions.getAwsSecretKey());
            awsConfig.setRegion(AwsUtils.getRegion());
            awsConfig.setSecurityGroupName(groupName);

            // KG-7725: Hazelcast wants to bind on an interface, and if the Gateway doesn't
            // tell it which ones to pick it'll pick one on its own.  Make sure interfaces
            // are explicitly enabled, and grab the local IP address since this will be the
            // IP address used to do discovery of other Gateways in the given security group.
            // Otherwise an elastic IP associated with the instance might cause Hazelcast to
            // pick a different IP address to listen on than the one used to connect to other
            // members, meaning where Gateways are listening is *not* where Gateways try to
            // connect to other Gateways.  In those situations the cluster does not form.
            String localIPv4 = AwsUtils.getLocalIPv4();
            networkConfig.getInterfaces().setEnabled(true);
            networkConfig.getInterfaces().clear();

            networkConfig.getInterfaces().addInterface(localIPv4);

            // KG-12825: Override the property PROP_SOCKET_BIND_ANY and set it to false so that
            //           Hazelcast does not discard the interface explicitly specified to bind to.
            hazelCastConfig.setProperty(HAZELCAST_SOCKET_BIND_ANY_PROPERTY, "false");
        }

        hazelCastConfig.setNetworkConfig(networkConfig);

        // Override the shutdown hook in Hazelcast so that the connection counts can be correctly maintained.
        // The cluster instance should be shutdown by the Gateway, so there should be no need for the default
        // Hazelcast shutdown hook.
        hazelCastConfig.setProperty(HAZELCAST_SHUTDOWNHOOK_ENABLED_PROPERTY, "false");

        return hazelCastConfig;
    }

    /**
     * Method returning IP from host
     * @param host
     * @return
     * @throws UnknownHostException
     */
    private String convertHostToIP(String host) throws UnknownHostException {
        return getResolvedAddressFromHost(host).getHostAddress();
    }

    @SuppressWarnings("unused")
    private List<String> processInterfaceOrMemberEntry(String entry) {
        if (entry == null) {
            return null;
        }

        ArrayList<String> addresses = new ArrayList<>();
        int starIndex = entry.indexOf('*');
        int dashIndex = entry.indexOf('-');
        if (starIndex == -1 && dashIndex == -1) {
            addresses.add(entry);
            return addresses;
        }
        String[] parts = entry.split("\\.");
        if (parts.length != 4) {
            throw new IllegalArgumentException("Invalid wildcard in the entry for cluster configuration: " + entry);
        }

        // Prevent wildcards in the first part
        if (parts[0].contains("*") && parts[0].contains("-")) {
            throw new IllegalArgumentException(
                    "Invalid wildcard in the entry for cluster configuration, first part of the address cannot contain a " +
                            "wildcard: "
                            + entry);
        }

        String part1 = parts[0];

        String[] part2s = processEntryPart(entry, parts[1]);
        String[] part3s = processEntryPart(entry, parts[2]);
        String[] part4s = processEntryPart(entry, parts[3]);

        for (String part2 : part2s) {
            for (String part3 : part3s) {
                for (String part4 : part4s) {
                    addresses.add(part1 + "." + part2 + "." + part3 + "." + part4);
                }
            }
        }

        return addresses;

    }

    /**
     * Previous to network interface syntax support, only InetAddress.getByName(hostAddress) was used for when
     * returning IPs based on host name. This basically did a InetAddress.getAllByName(hostAddress)[0]
     * Consequently, only the first IP address was returned
     * A similar approach has been used with added NetworkIntrfaceSyntax support, where only the first IP
     * is returned for a localInterface
     * Same approach is used also for cluster members
     * Method returning resolved addresses from host
     * @param hostAddress
     * @return
     * @throws UnknownHostException
     */
    private InetAddress getResolvedAddressFromHost(String hostAddress) throws UnknownHostException {
        // TODO: Previous to network interface syntax support, only InetAddress.getByName(hostAddress) was used for when
        // returning IPs based on host name. This basically did a InetAddress.getAllByName(hostAddress)[0]
        // Consequently, only the first IP address was returned
        // A similar approach has been used with added NetworkIntrfaceSyntax support, where only the first IP
        // is returned for a localInterface
        // Same approach is used also for cluster members
        InetAddress address;
        Collection<InetAddress> addresses = ResolutionUtils.getAllByName(hostAddress, false);
        if (addresses.isEmpty()) {
            address = InetAddress.getByName(hostAddress);
        }
        else {
            address = addresses.iterator().next();
        }
        return address;
    }

    private String[] processEntryPart(String entry, String ipPart) {

        // No wild cards
        if (!ipPart.contains("*") && !ipPart.contains("-")) {
            String[] resolvedParts = new String[1];
            resolvedParts[0] = ipPart;
            return resolvedParts;
        }

        // process *
        if (ipPart.equals("*")) {
            String[] resolvedParts = new String[256];
            for (int i = 0; i < 256; i++) {
                resolvedParts[i] = String.valueOf(i);
            }
            return resolvedParts;
        }

        // process ranges
        if (ipPart.contains("-")) {
            String[] rangeParts = ipPart.split("-");
            if (rangeParts.length != 2) {
                throw new IllegalArgumentException("Invalid wildcard in the entry for cluster configuration: " + entry);
            }

            int start = Integer.parseInt(rangeParts[0]);
            int end = Integer.parseInt(rangeParts[1]);
            String[] resolvedParts = new String[end - start + 1];
            for (int i = start; i <= end; i++) {
                resolvedParts[i] = String.valueOf(i);
            }
            return resolvedParts;
        }

        throw new IllegalArgumentException("Invalid wildcard in the entry for cluster configuration: " + entry);
    }

    @Override
    public MemberId getLocalMember() {
        return this.localNodeId;
    }

    @Override
    public Collection<MemberId> getMemberIds() {
        Map<MemberId, String> instanceKeyMap = getCollectionsFactory().getMap(INSTANCE_KEY_MAP);
        return instanceKeyMap.keySet();
    }

    private MemberId getMemberId(Member member) {
        InetSocketAddress inetSocketAddress = member.getSocketAddress();
        String hostname = inetSocketAddress.getHostName();
        if (!inetSocketAddress.isUnresolved()) {
            String ipAddr = inetSocketAddress.getAddress().getHostAddress();
            hostname = ipAddr;
            GL.debug(GL.CLUSTER_LOGGER_NAME, "getMemberId: Hostname: {}; IP Address: {}", hostname, ipAddr);
        }
        return new MemberId("tcp", hostname, inetSocketAddress.getPort());
    }

    private MembershipListener membershipListener = new MembershipListener() {

        @Override
        public void memberAdded(MembershipEvent membershipEvent) {
            MemberId newMemberId = getMemberId(membershipEvent.getMember());
            GL.info(GL.CLUSTER_LOGGER_NAME, "Cluster member {} is now online", newMemberId.getId());
            fireMemberAdded(newMemberId);
            GL.info(GL.CLUSTER_LOGGER_NAME, "Member Added");
            logClusterStateAtInfoLevel();
        }

        @Override
        public void memberRemoved(MembershipEvent membershipEvent) {
            MemberId removedMember = getMemberId(membershipEvent.getMember());
            GL.info(GL.CLUSTER_LOGGER_NAME, "Cluster member {} has gone down", removedMember);

            // Clean up the member's instanceKey
            Map<MemberId, String> instanceKeyMap = getCollectionsFactory().getMap(INSTANCE_KEY_MAP);
            instanceKeyMap.remove(removedMember);

            // cleanup balancer URIs for the member that went down
            Map<MemberId, Map<String, List<String>>> memberIdBalancerUriMap =
                    getCollectionsFactory().getMap(MEMBERID_BALANCER_MAP_NAME);
            if (memberIdBalancerUriMap == null) {
                throw new IllegalStateException("MemberId to BalancerMap is null");
            }

            IMap<String, TreeSet<String>> sharedBalanceUriMap = getCollectionsFactory().getMap(BALANCER_MAP_NAME);
            if (sharedBalanceUriMap == null) {
                throw new IllegalStateException("Shared balanced URIs map is null");
            }

            Map<String, List<String>> memberBalancedUrisMap = memberIdBalancerUriMap.remove(removedMember);
            if (memberBalancedUrisMap != null) {
                GL.debug(GL.CLUSTER_LOGGER_NAME, "Cleaning up balancer cluster state for member {}", removedMember);
                try {
                    for (String key : memberBalancedUrisMap.keySet()) {
                        GL.debug(GL.CLUSTER_LOGGER_NAME, "URI Key: {}", key);
                        List<String> memberBalancedUris = memberBalancedUrisMap.get(key);
                        TreeSet<String> globalBalancedUris;
                        TreeSet<String> newGlobalBalancedUris;
                        do {
                            globalBalancedUris = sharedBalanceUriMap.get(key);
                            newGlobalBalancedUris = new TreeSet<>(globalBalancedUris);
                            for (String memberBalancedUri : memberBalancedUris) {
                                GL.debug(GL.CLUSTER_LOGGER_NAME, "Attempting to removing Balanced URI : {}", memberBalancedUri);
                                newGlobalBalancedUris.remove(memberBalancedUri);
                            }
                        } while (!sharedBalanceUriMap.replace(key, globalBalancedUris, newGlobalBalancedUris));

                        GL.debug(GL.CLUSTER_LOGGER_NAME,
                                "Removed balanced URIs for cluster member {}, new global list: {}", removedMember,
                                newGlobalBalancedUris);
                    }
                } catch (Exception e) {
                    throw new IllegalStateException("Unable to remove the balanced URIs served by the member going down from " +
                            "global map");
                }
            }

            fireMemberRemoved(removedMember);
            GL.info(GL.CLUSTER_LOGGER_NAME, "Member Removed");
            logClusterStateAtInfoLevel();
        }

        @Override
        public void memberAttributeChanged(MemberAttributeEvent memberAttributeEvent) {
            // Member attributes are not used 
        }
    };

    @Override
    public String getInstanceKey(MemberId memberId) {
        if (memberId == localNodeId) {
            return this.localInstanceKey;  // quicker, and works with CLIENT_MODE, too.
        }

        Map<MemberId, String> instanceKeyMap = getCollectionsFactory().getMap(INSTANCE_KEY_MAP);
        return instanceKeyMap.get(memberId);
    }

    // cluster collections

    @Override
    public Lock getLock(String name) {
        return clusterInstance.getLock(name);
    }

    @Override
    public void addMembershipEventListener(MembershipEventListener eventListener) {
        if (eventListener != null) {
            membershipEventListeners.add(eventListener);
            GL.debug(GL.CLUSTER_LOGGER_NAME, "MemberShipEventListener Added");
        }
    }

    @Override
    public void removeMembershipEventListener(MembershipEventListener eventListener) {
        if (eventListener != null) {
            membershipEventListeners.remove(eventListener);
        }
    }

    @Override
    public String getClusterName() {
        return this.clusterName;
    }

    @Override
    public List<MemberId> getAccepts() {
        return localInterfaces;
    }

    @Override
    public List<MemberId> getConnects() {
        return clusterMembers;
    }

    @Override
    public ClusterConnectOptionsContext getConnectOptions() {
        return connectOptions;
    }

    @Override
    /*
     * Logs cluster state and balancer service maps contents when ha logging is enabled at trace level !
     */
    public void logClusterState() {
        logClusterMembers();
        logBalancerMap();
    }

    @Override
    /*
     * Logs cluster state at Info Level, recommended use only for startup or methods that are called only one time when
     * cluster state changes !
     */
    public void logClusterStateAtInfoLevel() {
        if (clusterInstance != null) {
            Cluster cluster = clusterInstance.getCluster();
            if (cluster != null) {
                GL.info(GL.CLUSTER_LOGGER_NAME, "Current cluster members:");
                Set<Member> currentMembers = clusterInstance.getCluster().getMembers();
                for (Member currentMember : currentMembers) {
                    MemberId memberId = getMemberId(currentMember);
                    GL.info(GL.CLUSTER_LOGGER_NAME, "      member: {}", memberId);
                }
            }
        }
        GL.info(GL.CLUSTER_LOGGER_NAME, "Current shared balancer map:");
        Map<String, Set<String>> balancerMap = getCollectionsFactory().getMap(BALANCER_MAP_NAME);
        for (String balanceURI : balancerMap.keySet()) {
            Set<String> balanceTargets = balancerMap.get(balanceURI);
            GL.info(GL.CLUSTER_LOGGER_NAME, "     balance URI: {}    target list: {}", balanceURI, balanceTargets);
        }

    }
    private void logClusterMembers() {
        // log current cluster state on TRACE level
        if (clusterInstance != null) {
            Cluster cluster = clusterInstance.getCluster();
            if (cluster != null) {
                GL.trace(GL.CLUSTER_LOGGER_NAME, "Current cluster members:");
                Set<Member> currentMembers = clusterInstance.getCluster().getMembers();
                for (Member currentMember : currentMembers) {
                    MemberId memberId = getMemberId(currentMember);
                    GL.trace(GL.CLUSTER_LOGGER_NAME, "      member: {}", memberId);
                }
            }
        }
    }

    private void logBalancerMap() {
        GL.trace(GL.CLUSTER_LOGGER_NAME, "Current members of balancer map:");
        Map<MemberId, Map<String, List<String>>> memberIdBalancerUriMap =
                              getCollectionsFactory().getMap(MEMBERID_BALANCER_MAP_NAME);
        for (MemberId memberID : memberIdBalancerUriMap.keySet()) {
            GL.trace(GL.CLUSTER_LOGGER_NAME, " MemberID {}", memberID);
            Map<String, List<String>> balanceURIMap = memberIdBalancerUriMap.get(memberID);
            for (String balanceURI : balanceURIMap.keySet()) {
                List<String> balanceTargets = balanceURIMap.get(balanceURI);
                GL.trace(GL.CLUSTER_LOGGER_NAME, "     balance URI: {}    target list: {}", balanceURI, balanceTargets);
            }

        }
        GL.trace(GL.CLUSTER_LOGGER_NAME, "Current shared balancer map::");
        Map<String, Set<String>> balancerMap = getCollectionsFactory().getMap(BALANCER_MAP_NAME);
        for (String balanceURI : balancerMap.keySet()) {
            Set<String> balanceTargets = balancerMap.get(balanceURI);
            GL.trace(GL.CLUSTER_LOGGER_NAME, "     balance URI: {}    target list: {}", balanceURI, balanceTargets);
        }
    }

    /**
     * Fire member added event
     */
    private void fireMemberAdded(MemberId newMember) {
        GL.debug(GL.CLUSTER_LOGGER_NAME, "Firing member added for : {}", newMember);
        for (MembershipEventListener listener : membershipEventListeners) {
            try {
                listener.memberAdded(newMember);
            } catch (Throwable e) {
                GL.error(GL.CLUSTER_LOGGER_NAME, "Error in member added event {}", e);
            }
        }
    }

    /**
     * Fire member removed event
     */
    private void fireMemberRemoved(MemberId exMember) {
        GL.debug(GL.CLUSTER_LOGGER_NAME, "Firing member removed for: {}", exMember);
        for (MembershipEventListener listener : membershipEventListeners) {
            try {
                listener.memberRemoved(exMember);
            } catch (Throwable e) {
                GL.error(GL.CLUSTER_LOGGER_NAME, "Error in member removed event {}", e);
            }
        }
    }

    @Override
    public CollectionsFactory getCollectionsFactory() {
        initializeCluster(null);
        return this.collectionsFactory;
    }

    private void initializeCluster(Config config) {
        if (clusterInitialized.compareAndSet(false, true)) {
            clusterInstance = Hazelcast.newHazelcastInstance(config);
            if (clusterInstance == null) {
                throw new RuntimeException("Unable to initialize the cluster");
            }
            Cluster cluster = clusterInstance.getCluster();
            cluster.addMembershipListener(this.membershipListener);

            // Register a listener for Hazelcast logging events
            LoggingService loggingService = clusterInstance.getLoggingService();
            loggingService.addLogListener(Level.FINEST, this);

            this.collectionsFactory = new ClusterCollectionsFactory(clusterInstance);
            localNodeId = getMemberId(cluster.getLocalMember());

            IMap<MemberId, String> instanceKeyMap = collectionsFactory.getMap(INSTANCE_KEY_MAP);
            instanceKeyMap.put(localNodeId, localInstanceKey);

            IMap<String, Collection<String>> balancerMap = collectionsFactory.getMap(BALANCER_MAP_NAME);
            addBalancerMapEntryListeners(balancerMap);
        }
    }

    private void addBalancerMapEntryListeners(IMap<String, Collection<String>> balancerMap) {
        balancerMap.addEntryListener(new EntryAddedListener<String, Collection<String>>() {
            @Override
            public void entryAdded(EntryEvent<String, Collection<String>> event) {
                GL.trace(GL.CLUSTER_LOGGER_NAME, "New entry for balance URI: {}   value: {}", event.getKey(), event.getValue());
            }
        }, true);

        balancerMap.addEntryListener(new EntryEvictedListener<String, Collection<String>>() {
            @Override
            public void entryEvicted(EntryEvent<String, Collection<String>> evictedEntryEvent) {
                throw new RuntimeException("Balancer map entries should not be evicted, only added or removed.");
            }
        }, true);

        balancerMap.addEntryListener(new EntryRemovedListener<String, Collection<String>>() {
            @Override
            public void entryRemoved(EntryEvent<String, Collection<String>> removedEntryEvent) {
                GL.trace(GL.CLUSTER_LOGGER_NAME, "Entry removed for balance URI: {}   value: {}", removedEntryEvent
                        .getKey(), removedEntryEvent.getValue());
            }
        }, true);

        balancerMap.addEntryListener(new EntryUpdatedListener<String, Collection<String>>() {
            @Override
            public void entryUpdated(EntryEvent<String, Collection<String>> updatedEntryEvent) {
                GL.trace(GL.CLUSTER_LOGGER_NAME, "Entry updated for balance URI: {}   value: {}", updatedEntryEvent
                        .getKey(), updatedEntryEvent.getValue());
            }
        }, true);

    }

    @Override
    public void log(LogEvent logEvent) {
        Member member = logEvent.getMember();
        LogRecord record = logEvent.getLogRecord();

        Level level = record.getLevel();
        if (level.equals(Level.SEVERE)) {
            logger.error(String.format(CLUSTER_LOG_FORMAT, member, record.getMessage()));

        } else if (level.equals(Level.WARNING)) {
            logger.warn(String.format(CLUSTER_LOG_FORMAT, member, record.getMessage()));

        } else if (level.equals(Level.INFO)) {
            logger.info(String.format(CLUSTER_LOG_FORMAT, member, record.getMessage()));

        } else if (level.equals(Level.FINE)) {
            if (logger.isDebugEnabled()) {
                logger.debug(String.format(CLUSTER_LOG_FORMAT, member, record.getMessage()));
            }

        } else if (level.equals(Level.FINER) ||
                level.equals(Level.FINEST)) {
            if (logger.isTraceEnabled()) {
                logger.trace(String.format(CLUSTER_LOG_FORMAT, member, record.getMessage()));
            }
        }
    }

    @Override
    public <E> ITopic<E> getTopic(String name) {
        return this.collectionsFactory.getTopic(name);
    }
}

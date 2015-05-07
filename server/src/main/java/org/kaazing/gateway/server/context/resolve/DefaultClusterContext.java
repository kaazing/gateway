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

package org.kaazing.gateway.server.context.resolve;

import com.hazelcast.config.AwsConfig;
import com.hazelcast.config.Config;
import com.hazelcast.config.Join;
import com.hazelcast.config.MapConfig;
import com.hazelcast.config.MulticastConfig;
import com.hazelcast.config.NetworkConfig;
import com.hazelcast.config.TcpIpConfig;
import com.hazelcast.core.Cluster;
import com.hazelcast.core.EntryEvent;
import com.hazelcast.core.EntryListener;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import com.hazelcast.core.IdGenerator;
import com.hazelcast.core.Member;
import com.hazelcast.core.MembershipEvent;
import com.hazelcast.core.MembershipListener;
import com.hazelcast.impl.GroupProperties;
import com.hazelcast.logging.LogEvent;
import com.hazelcast.logging.LogListener;
import com.hazelcast.logging.LoggingService;
import com.hazelcast.nio.Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import org.kaazing.gateway.server.messaging.buffer.ClusterMemoryMessageBufferFactory;
import org.kaazing.gateway.server.messaging.collections.ClusterCollectionsFactory;
import org.kaazing.gateway.service.cluster.BalancerMapListener;
import org.kaazing.gateway.service.cluster.ClusterConnectOptionsContext;
import org.kaazing.gateway.service.cluster.ClusterContext;
import org.kaazing.gateway.service.cluster.ClusterMessaging;
import org.kaazing.gateway.service.cluster.InstanceKeyListener;
import org.kaazing.gateway.service.cluster.MemberId;
import org.kaazing.gateway.service.cluster.MembershipEventListener;
import org.kaazing.gateway.service.cluster.ReceiveListener;
import org.kaazing.gateway.service.cluster.SendListener;
import org.kaazing.gateway.service.messaging.buffer.MessageBufferFactory;
import org.kaazing.gateway.service.messaging.collections.CollectionsFactory;
import org.kaazing.gateway.util.GL;
import org.kaazing.gateway.util.Utils;
import org.kaazing.gateway.util.aws.AwsUtils;
import org.kaazing.gateway.util.scheduler.SchedulerProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import static org.kaazing.gateway.server.context.resolve.DefaultServiceContext.BALANCER_MAP_NAME;
import static org.kaazing.gateway.server.context.resolve.DefaultServiceContext.MEMBERID_BALANCER_MAP_NAME;

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

    // This is also used in DefaultServiceContext
    static final String CLUSTER_LOGGER_NAME = "ha";
    private final Logger logger = LoggerFactory.getLogger(CLUSTER_LOGGER_NAME);

    private final String localInstanceKey = Utils.randomHexString(16);

    private MessageBufferFactory messageBufferFactory;
    private CollectionsFactory collectionsFactory;
    private List<MemberId> localInterfaces = new ArrayList<>();
    private final List<MemberId> clusterMembers = new ArrayList<>();
    private final List<MembershipEventListener> membershipEventListeners = new ArrayList<>();
    private final List<InstanceKeyListener> instanceKeyListeners = new ArrayList<>();
    private final List<BalancerMapListener> balancerMapListeners = new ArrayList<>();
    private ClusterMessaging clusterMessaging;
    private MemberId localNodeId;
    private final String clusterName;
    private HazelcastInstance clusterInstance;
    private final SchedulerProvider schedulerProvider;
    private final ClusterConnectOptionsContext connectOptions;
    private final AtomicBoolean clusterInitialized = new AtomicBoolean(false);

    public DefaultClusterContext(String name,
                                 List<MemberId> interfaces,
                                 List<MemberId> members,
                                 SchedulerProvider schedulerProvider) {
        this(name, interfaces, members, schedulerProvider, null);
    }

    public DefaultClusterContext(String name,
                                 List<MemberId> interfaces,
                                 List<MemberId> members,
                                 SchedulerProvider schedulerProvider,
                                 ClusterConnectOptionsContext connectOptions) {
        this.clusterName = name;
        this.localInterfaces.addAll(interfaces);
        this.clusterMembers.addAll(members);
        this.schedulerProvider = schedulerProvider;
        this.connectOptions = connectOptions;
    }

    @Override
    public void start() {

        // Check that we have either localInterfaces or clusterMembers
        if (localInterfaces.size() + clusterMembers.size() == 0) {
            // if no local interfaces
            if (localInterfaces.size() == 0) {
                GL.info(CLUSTER_LOGGER_NAME, "No network interfaces specified in the gateway configuration");
                throw new IllegalArgumentException("No network interfaces specified in the gateway configuration");
            }

            // if no members
            if (clusterMembers.size() == 0) {
                GL.info(CLUSTER_LOGGER_NAME, "No cluster members specified in the gateway configuration");
                throw new IllegalArgumentException("No cluster members specified in the gateway configuration");
            }
        }

        try {
            // from ha.xml and then add members interfaces
            Config config = initializeHazelcastConfig();

            initializeCluster(config);

            GL.info(CLUSTER_LOGGER_NAME, "Cluster Member started: IP Address: {}; Port: {}; id: {}",
                    localNodeId.getHost(), localNodeId.getPort(), localNodeId.getId());
        } catch (Exception e) {
            GL.error(CLUSTER_LOGGER_NAME, "Unable to initialize cluster due to an exception: {}", e);
            throw new RuntimeException(String.format("Unable to initialize cluster due to an exception: %s", e), e);
        }
    }

    @Override
    public void dispose() {
        // If we're in client mode, then we may not have this clusterMessaging
        // object.  So don't try to destroy it if it is not there at all
        // (KG-3496).
        if (clusterMessaging != null) {
            clusterMessaging.destroy();
        }

        if (clusterInstance != null) {
            // KG-5837: do not call Hazelcast.shutdownAll() since that will hobble all in-process gateways
            clusterInstance.getLifecycleService().shutdown();
        }
    }

    private Config initializeHazelcastConfig() throws Exception {

        Config hazelCastConfig = new Config();

        hazelCastConfig.getGroupConfig().setName(getClusterName());
        hazelCastConfig.getGroupConfig().setPassword("5942");

        MapConfig mapConfig = hazelCastConfig.getMapConfig("serverSessions");
        mapConfig.setBackupCount(3);

        MapConfig sharedBalancerMapConfig = hazelCastConfig.getMapConfig(BALANCER_MAP_NAME);
        sharedBalancerMapConfig.setBackupCount(Integer.MAX_VALUE);
        MapConfig memberBalancerMapConfig = hazelCastConfig.getMapConfig(MEMBERID_BALANCER_MAP_NAME);
        memberBalancerMapConfig.setBackupCount(Integer.MAX_VALUE);

        // disable port auto increment
        hazelCastConfig.setPortAutoIncrement(false);

        // The first accepts port is the port used by all network interfaces.
        int clusterPort = (localInterfaces.size() > 0) ? localInterfaces.get(0).getPort() : -1;

        // TO turn off logging in hazelcast API.
        // Note: must use Logger.getLogger, not LogManager.getLogger
        java.util.logging.Logger logger = java.util.logging.Logger.getLogger("com.hazelcast");
        logger.setLevel(Level.OFF);

        // initialize hazelcast
        if (clusterPort != -1) {
            hazelCastConfig.setPort(clusterPort);
        }

        NetworkConfig networkConfig = new NetworkConfig();

        for (MemberId localInterface : localInterfaces) {
            String protocol = localInterface.getProtocol();
            if ("udp".equalsIgnoreCase(protocol) || "aws".equalsIgnoreCase(protocol)) {
                throw new IllegalArgumentException("Cannot accept on a multicast or aws address, use unicast address starting " +
                        "with tcp://");
            }

            // NOTE: The version of Hazelcast(1.9.4.8) that is being used does not support IPv6 address. The Hazelcast library
            //       throws NumberFormatException when IPv6 address is specified as an interface to bind to.
            String hostAddress = localInterface.getHost();
            InetAddress address = InetAddress.getByName(hostAddress);
            if (address instanceof Inet6Address) {
                throw new IllegalArgumentException("ERROR: Cluster member accept url - '" + localInterface.toString() +
                        "' consists of IPv6 address which is not supported. Use Ipv4 address instead.");
            }

            networkConfig.getInterfaces().addInterface(localInterface.getHost());

            if (localInterface.getPort() != clusterPort) {
                throw new IllegalArgumentException("Port numbers on the network interfaces in <accept> do not match");
            }
        }

        boolean usingMulticast = false;

        Join joinConfig = networkConfig.getJoin();
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
            if (member.getProtocol().equals("udp")) {
                multicastAddresses.add(new InetSocketAddress(member.getHost(), member.getPort()));
            } else if (member.getProtocol().equals("tcp")) {
                unicastAddresses.add(new InetSocketAddress(member.getHost(), member.getPort()));
            } else if (member.getProtocol().equals("aws")) {
                awsMember = member;

                // There should be only one <connect> tag when AWS is being
                // used. We have already validated that in
                // GatewayContextResolver.processClusterMembers() method.
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
                    tcpIpConfig.addAddress(new Address(unicastAddress));
                }
            }

            // Check if address specified to bind to is a wildcard address. If it is a wildcard address,
            // do not override the property PROP_SOCKET_BIND_ANY. If not, set the PROP_SOCKET_BIND_ANY to false so
            // that Hazelcast won't discard the interface specified to bind to.
            boolean useAnyAddress = false;

            // Check to see if the address specified is the wildcard address
            for (String networkInterface : networkConfig.getInterfaces().getInterfaces()) {
                InetAddress address = InetAddress.getByName(networkInterface);
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
                hazelCastConfig.setProperty(GroupProperties.PROP_SOCKET_BIND_ANY, "false");
            }
        } else {
            // Gateway is running in the AWS/Cloud env.

            // Get rid of the leading slash "/" in the path.
            String path = awsMember.getPath();
            String groupName = null;

            if (path != null) {
                if ((path.indexOf("/") == 0) && (path.length() > 1)) {
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
            hazelCastConfig.setProperty(GroupProperties.PROP_SOCKET_BIND_ANY, "false");
        }

        hazelCastConfig.setNetworkConfig(networkConfig);

        // Override the shutdown hook in Hazelcast so that the connection counts can be correctly maintained.
        // The cluster instance should be shutdown by the Gateway, so there should be no need for the default
        // Hazelcast shutdown hook.
        hazelCastConfig.setProperty(GroupProperties.PROP_SHUTDOWNHOOK_ENABLED, "false");

        return hazelCastConfig;
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

        for (int i = 0; i < part2s.length; i++) {
            for (int j = 0; j < part3s.length; j++) {
                for (int k = 0; k < part4s.length; k++) {
                    addresses.add(part1 + "." + part2s[i] + "." + part3s[j] + "." + part4s[k]);
                }
            }
        }

        return addresses;

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
        InetSocketAddress inetSocketAddress = member.getInetSocketAddress();
        String hostname = inetSocketAddress.getHostName();
        if (!inetSocketAddress.isUnresolved()) {
            String ipAddr = inetSocketAddress.getAddress().getHostAddress();
            hostname = ipAddr;
            GL.debug(CLUSTER_LOGGER_NAME, "getMemberId: Hostname: {}; IP Address: {}", hostname, ipAddr);
        }
        return new MemberId("tcp", hostname, inetSocketAddress.getPort());
    }

    private MembershipListener membershipListener = new MembershipListener() {

        @Override
        public void memberAdded(MembershipEvent membershipEvent) {
            MemberId newMemberId = getMemberId(membershipEvent.getMember());
            GL.info(CLUSTER_LOGGER_NAME, "Cluster member {} is now online", newMemberId.getId());
            fireMemberAdded(newMemberId);
            logClusterMembers();
        }

        @Override
        public void memberRemoved(MembershipEvent membershipEvent) {
            MemberId removedMember = getMemberId(membershipEvent.getMember());
            GL.info(CLUSTER_LOGGER_NAME, "Cluster member {} has gone down", removedMember);

            // Clean up the member's instanceKey
            Map<MemberId, String> instanceKeyMap = getCollectionsFactory().getMap(INSTANCE_KEY_MAP);
            instanceKeyMap.remove(removedMember);

            // cleanup balancer URIs for the member that went down
            Map<MemberId, Map<URI, List<URI>>> memberIdBalancerUriMap =
                    getCollectionsFactory().getMap(MEMBERID_BALANCER_MAP_NAME);
            if (memberIdBalancerUriMap == null) {
                throw new IllegalStateException("MemberId to BalancerMap is null");
            }

            IMap<URI, Set<URI>> sharedBalanceUriMap = getCollectionsFactory().getMap(BALANCER_MAP_NAME);
            if (sharedBalanceUriMap == null) {
                throw new IllegalStateException("Shared balanced URIs map is null");
            }

            Map<URI, List<URI>> memberBalancedUrisMap = memberIdBalancerUriMap.remove(removedMember);
            if (memberBalancedUrisMap != null) {
                GL.debug(CLUSTER_LOGGER_NAME, "Cleaning up balancer cluster state for member {}", removedMember);
                try {
                    for (URI key : memberBalancedUrisMap.keySet()) {
                        GL.debug(CLUSTER_LOGGER_NAME, "URI Key: {}", key);
                        List<URI> memberBalancedUris = memberBalancedUrisMap.get(key);
                        Set<URI> globalBalancedUris = null;
                        Set<URI> newGlobalBalancedUris = null;
                        do {
                            globalBalancedUris = sharedBalanceUriMap.get(key);
                            newGlobalBalancedUris = new HashSet<>(globalBalancedUris);
                            for (URI memberBalancedUri : memberBalancedUris) {
                                GL.debug(CLUSTER_LOGGER_NAME, "Attempting to removing Balanced URI : {}", memberBalancedUri);
                                newGlobalBalancedUris.remove(memberBalancedUri);
                            }
                        } while (!sharedBalanceUriMap.replace(key, globalBalancedUris, newGlobalBalancedUris));

                        GL.debug(CLUSTER_LOGGER_NAME, "Removed balanced URIs for cluster member {}, new global list: {}",
                                removedMember, newGlobalBalancedUris);
                    }
                } catch (Exception e) {
                    throw new IllegalStateException("Unable to remove the balanced URIs served by the member going down from " +
                            "global map");
                }
            }

            fireMemberRemoved(removedMember);
            logClusterMembers();
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

    private EntryListener<MemberId, String> instanceKeyEntryListener = new EntryListener<MemberId, String>() {
        // WE're supporting the idea of 'instance keys' (i.e. random strings that are supposed
        // to be unique per instance of a gateway) solely for management to be able to tell the
        // difference between two instances of a gateway accessed through the same management URL.
        // The Console supports displaying history, and it needs to know when reconnecting to a
        // given management URL whether or not the configuration might have changed. Because
        // member IDs generally don't change (for now they're based on the cluster accept URL),
        // they're a bad indicator of an instance stopping and being restarted. Thus the need for the
        // instance key. When a member is added or removed, the instanceKey is also added or
        // removed, and we can trigger events for management to update their cluster state.
        @Override
        public void entryAdded(EntryEvent<MemberId, String> newEntryEvent) {
            fireInstanceKeyAdded(newEntryEvent.getValue());
        }

        @Override
        public void entryEvicted(EntryEvent<MemberId, String> evictedEntryEvent) {
            throw new RuntimeException("Instance keys should not be evicted, only added or removed.");
        }

        @Override
        public void entryRemoved(EntryEvent<MemberId, String> removedEntryEvent) {
            fireInstanceKeyRemoved(removedEntryEvent.getValue());
        }

        @Override
        public void entryUpdated(EntryEvent<MemberId, String> updatedEntryEvent) {
            throw new RuntimeException("Instance keys can not be updated, only added or removed.");
        }
    };

    private EntryListener<URI, Collection<URI>> balancerMapEntryListener = new EntryListener<URI, Collection<URI>>() {
        @Override
        public void entryAdded(EntryEvent<URI, Collection<URI>> newEntryEvent) {
            GL.trace(CLUSTER_LOGGER_NAME, "New entry for balance URI: {}   value: {}", newEntryEvent.getKey(), newEntryEvent
                    .getValue());
            fireBalancerEntryAdded(newEntryEvent);
        }

        @Override
        public void entryEvicted(EntryEvent<URI, Collection<URI>> evictedEntryEvent) {
            throw new RuntimeException("Balancer map entries should not be evicted, only added or removed.");
        }

        @Override
        public void entryRemoved(EntryEvent<URI, Collection<URI>> removedEntryEvent) {
            GL.trace(CLUSTER_LOGGER_NAME, "Entry removed for balance URI: {}   value: {}", removedEntryEvent
                    .getKey(), removedEntryEvent.getValue());
            fireBalancerEntryRemoved(removedEntryEvent);
        }

        @Override
        public void entryUpdated(EntryEvent<URI, Collection<URI>> updatedEntryEvent) {
            GL.trace(CLUSTER_LOGGER_NAME, "Entry updated for balance URI: {}   value: {}", updatedEntryEvent
                    .getKey(), updatedEntryEvent.getValue());
            fireBalancerEntryUpdated(updatedEntryEvent);
        }
    };

    // cluster collections

    @Override
    public Lock getLock(Object obj) {
        return clusterInstance.getLock(obj);
    }

    @Override
    public IdGenerator getIdGenerator(String name) {
        return clusterInstance.getIdGenerator(name);
    }

    // cluster communication

    @Override
    public void addReceiveQueue(String name) {
        if (clusterMessaging != null) {
            clusterMessaging.addReceiveQueue(name);
        }
    }

    @Override
    public void addReceiveTopic(String name) {
        if (clusterMessaging != null) {
            clusterMessaging.addReceiveTopic(name);
        }
    }

    @Override
    public void send(Object msg, SendListener listener, MemberId member) {
        if (clusterMessaging != null) {
            clusterMessaging.send(msg, listener, member);
        }
    }

    @Override
    public void send(Object msg, SendListener listener, String name) {
        if (clusterMessaging != null) {
            clusterMessaging.send(msg, listener, name);
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public Object send(Object msg, MemberId member) throws Exception {
        if (clusterMessaging != null) {
            return clusterMessaging.send(msg, member);
        }

        return null;
    }

    @SuppressWarnings("unchecked")
    @Override
    public Object send(Object msg, String name) throws Exception {
        if (clusterMessaging != null) {
            return clusterMessaging.send(msg, name);
        }

        return null;
    }

    @Override
    public <T> void setReceiver(Class<T> type, ReceiveListener<T> receiveListener) {
        if (clusterMessaging != null) {
            clusterMessaging.setReceiver(type, receiveListener);
        }
    }

    @Override
    public <T> void removeReceiver(Class<T> type) {
        if (clusterMessaging != null) {
            clusterMessaging.removeReceiver(type);
        }
    }

    @Override
    public void addMembershipEventListener(MembershipEventListener eventListener) {
        if (eventListener != null) {
            membershipEventListeners.add(eventListener);
        }
    }

    @Override
    public void removeMembershipEventListener(MembershipEventListener eventListener) {
        if (eventListener != null) {
            membershipEventListeners.remove(eventListener);
        }
    }

    @Override
    public void addInstanceKeyListener(InstanceKeyListener instanceKeyListener) {
        if (instanceKeyListener != null) {
            instanceKeyListeners.add(instanceKeyListener);
        }
    }

    @Override
    public void removeInstanceKeyListener(InstanceKeyListener instanceKeyListener) {
        if (instanceKeyListener != null) {
            instanceKeyListeners.remove(instanceKeyListener);
        }
    }

    @Override
    public void addBalancerMapListener(BalancerMapListener balancerMapListener) {
        if (balancerMapListener != null) {
            balancerMapListeners.add(balancerMapListener);
        }
    }

    @Override
    public void removeBalancerMapListener(BalancerMapListener balancerMapListener) {
        if (balancerMapListener != null) {
            balancerMapListeners.remove(balancerMapListener);
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
    public MessageBufferFactory getMessageBufferFactory() {
        return messageBufferFactory;
    }

    @Override
    public void logClusterState() {
        logClusterMembers();
        logBalancerMap();
    }

    private void logClusterMembers() {
        // log current cluster state on TRACE level
        if (clusterInstance != null) {
            Cluster cluster = clusterInstance.getCluster();
            if (cluster != null) {
                GL.trace(CLUSTER_LOGGER_NAME, "Current cluster members:");
                Set<Member> currentMembers = clusterInstance.getCluster().getMembers();
                for (Member currentMember : currentMembers) {
                    MemberId memberId = getMemberId(currentMember);
                    GL.trace(CLUSTER_LOGGER_NAME, "      member: {}", memberId);
                }
            }
        }
    }

    private void logBalancerMap() {
        GL.trace(CLUSTER_LOGGER_NAME, "Current balancer map:");
        Map<URI, Set<URI>> balancerMap = getCollectionsFactory().getMap(BALANCER_MAP_NAME);
        for (URI balanceURI : balancerMap.keySet()) {
            Set<URI> balanceTargets = balancerMap.get(balanceURI);
            GL.trace(CLUSTER_LOGGER_NAME, "     balance URI: {}    target list: {}", balanceURI, balanceTargets);
        }
    }

    /**
     * Fire member added event
     */
    private void fireMemberAdded(MemberId newMember) {
        GL.debug(CLUSTER_LOGGER_NAME, "Firing member added for : {}", newMember);
        for (MembershipEventListener listener : membershipEventListeners) {
            try {
                listener.memberAdded(newMember);
            } catch (Throwable e) {
                GL.error(CLUSTER_LOGGER_NAME, "Error in member added event {}", e);
            }
        }
    }

    /**
     * Fire member removed event
     */
    private void fireMemberRemoved(MemberId exMember) {
        GL.debug(CLUSTER_LOGGER_NAME, "Firing member removed for: {}", exMember);
        for (MembershipEventListener listener : membershipEventListeners) {
            try {
                listener.memberRemoved(exMember);
            } catch (Throwable e) {
                GL.error(CLUSTER_LOGGER_NAME, "Error in member removed event {}", e);
            }
        }
    }

    /**
     * Fire instanceKeyAdded event
     */
    private void fireInstanceKeyAdded(String instanceKey) {
        GL.debug(CLUSTER_LOGGER_NAME, "Firing instanceKeyAdded for: {}", instanceKey);
        for (InstanceKeyListener listener : instanceKeyListeners) {
            try {
                listener.instanceKeyAdded(instanceKey);
            } catch (Throwable e) {
                GL.error(CLUSTER_LOGGER_NAME, "Error in instanceKeyAdded event {}", e);
            }
        }
    }

    /**
     * Fire instanceKeyRemoved event
     */
    private void fireInstanceKeyRemoved(String instanceKey) {
        GL.debug(CLUSTER_LOGGER_NAME, "Firing instanceKeyRemoved for: {}", instanceKey);
        for (InstanceKeyListener listener : instanceKeyListeners) {
            try {
                listener.instanceKeyRemoved(instanceKey);
            } catch (Throwable e) {
                GL.error(CLUSTER_LOGGER_NAME, "Error in instanceKeyRemoved event {}", e);
            }
        }
    }

    /**
     * Fire balancerEntryAdded event
     */
    private void fireBalancerEntryAdded(EntryEvent<URI, Collection<URI>> entryEvent) {
        URI balancerURI = entryEvent.getKey();
        GL.debug(CLUSTER_LOGGER_NAME, "Firing balancerEntryAdded for: {}", balancerURI);
        for (BalancerMapListener listener : balancerMapListeners) {
            try {
                listener.balancerEntryAdded(balancerURI, entryEvent.getValue());
            } catch (Throwable e) {
                GL.error(CLUSTER_LOGGER_NAME, "Error in balancerEntryAdded event {}", e);
            }
        }
    }

    /**
     * Fire balancerEntryRemoved event
     */
    private void fireBalancerEntryRemoved(EntryEvent<URI, Collection<URI>> entryEvent) {
        URI balancerURI = entryEvent.getKey();
        GL.debug(CLUSTER_LOGGER_NAME, "Firing balancerEntryRemoved for: {}", balancerURI);
        for (BalancerMapListener listener : balancerMapListeners) {
            try {
                listener.balancerEntryRemoved(balancerURI, entryEvent.getValue());
            } catch (Throwable e) {
                GL.error(CLUSTER_LOGGER_NAME, "Error in balancerEntryRemoved event {}", e);
            }
        }
    }

    /**
     * Fire balancerEntryUpdated event
     */
    private void fireBalancerEntryUpdated(EntryEvent<URI, Collection<URI>> entryEvent) {
        URI balancerURI = entryEvent.getKey();
        GL.debug(CLUSTER_LOGGER_NAME, "Firing balancerEntryUpdated for: {}", balancerURI);
        for (BalancerMapListener listener : balancerMapListeners) {
            try {
                listener.balancerEntryUpdated(balancerURI, entryEvent.getValue());
            } catch (Throwable e) {
                GL.error(CLUSTER_LOGGER_NAME, "Error in balancerEntryUpdated event {}", e);
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
            this.messageBufferFactory = new ClusterMemoryMessageBufferFactory(clusterInstance);
            localNodeId = getMemberId(cluster.getLocalMember());
            clusterMessaging = new ClusterMessaging(this, clusterInstance, schedulerProvider);

            IMap<MemberId, String> instanceKeyMap = collectionsFactory.getMap(INSTANCE_KEY_MAP);
            instanceKeyMap.put(localNodeId, localInstanceKey);
            instanceKeyMap.addEntryListener(instanceKeyEntryListener, true);

            IMap<URI, Collection<URI>> balancerMap = collectionsFactory.getMap(BALANCER_MAP_NAME);
            balancerMap.addEntryListener(balancerMapEntryListener, true);
        }
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
}

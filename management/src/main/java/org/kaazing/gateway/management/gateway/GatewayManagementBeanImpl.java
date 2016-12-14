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
package org.kaazing.gateway.management.gateway;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.write.WriteRequest;
import org.apache.mina.util.CopyOnWriteMap;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.kaazing.gateway.management.AbstractManagementBean;
import org.kaazing.gateway.management.ClusterManagementListener;
import org.kaazing.gateway.management.ManagementService;
import org.kaazing.gateway.management.Utils.ManagementSessionType;
import org.kaazing.gateway.management.context.ManagementContext;
import org.kaazing.gateway.management.update.check.ManagementUpdateCheck;
import org.kaazing.gateway.management.update.check.ManagementUpdateCheckFactory;
import org.kaazing.gateway.server.context.GatewayContext;
import org.kaazing.gateway.server.impl.VersionUtils;
import org.kaazing.gateway.service.cluster.ClusterContext;
import org.kaazing.gateway.service.cluster.MemberId;
import org.kaazing.gateway.service.cluster.MembershipEventListener;
import org.kaazing.gateway.service.collections.CollectionsFactory;
import org.kaazing.gateway.service.http.balancer.HttpBalancerService;
import org.kaazing.mina.netty.util.threadlocal.VicariousThreadLocal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hazelcast.core.EntryEvent;
import com.hazelcast.core.EntryListener;
import com.hazelcast.core.MapEvent;

/**
 * Implementation of the management 'data' bean for a session. This just contains the data. Wrappers for different management
 * protocols define the use of those data.
 */
public class GatewayManagementBeanImpl extends AbstractManagementBean
        implements GatewayManagementBean, MembershipEventListener, EntryListener<MemberId, Collection<String>> {

    private static final Logger logger = LoggerFactory.getLogger(GatewayManagementBeanImpl.class);

    // Each IO worker thread gets a ThreadServiceStats object via get().
    private final ThreadLocal<ThreadGatewayStats> gatewayStats =
            new VicariousThreadLocal<ThreadGatewayStats>() {
                @Override
                protected ThreadGatewayStats initialValue() {
                    ThreadGatewayStats stats = new ThreadGatewayStats();
                    gatewayStatsMap.put(Thread.currentThread(), stats);
                    return stats;
                }
            };

    // Map of the per-thread thread-local stats objects. Keyed on thread ID.
    private final CopyOnWriteMap<Thread, ThreadGatewayStats> gatewayStatsMap = new CopyOnWriteMap<>();

    // A dynamic Gateway "id".  For customer usefulness, we'll make this
    // the string <hostname>:<pid>, where <hostname> is the hostname of the
    // processor this gateway process is running on, and <pid> is the process ID
    // of the gateway process.  These can both be determined here.
    private final String hostAndPid;
    private GatewayContext gatewayContext;

    // fields from VersionInfo
    private String productTitle;
    private String productBuild;
    private String productEdition;

    private final long startTime;
    private ClusterContext clusterContext;
    private final List<ClusterManagementListener> clusterManagementListeners;


    // Keep a unique index number for each gateway instance, as we can use
    // it in SNMP for an OID, and it might be useful elsewhere if we decide
    // we want to use it in place of some map key or something.  The SNMP
    // support for sessions also depends on knowing this value.
    private static final AtomicInteger maxGatewayIndex = new AtomicInteger(1);
    private final int id;

    private final ManagementUpdateCheck updateChecker;

    public GatewayManagementBeanImpl(ManagementContext managementContext,
                                     GatewayContext gatewayContext,
                                     String hostAndPid) {
        super(managementContext,
                managementContext.getGatewaySummaryDataNotificationInterval(),
                SUMMARY_DATA_FIELD_LIST);

        // FIXME:  every gateway ends up with id = 1 based on the next line of code, instead id should be from some cluster
        // info...
        this.id = maxGatewayIndex.getAndIncrement();  // may use in various wrappers
        this.hostAndPid = hostAndPid;
        this.startTime = System.currentTimeMillis();
        this.clusterManagementListeners = new ArrayList<>();
        this.gatewayContext = gatewayContext;

        this.productTitle = VersionUtils.getGatewayProductTitle();
        this.productBuild = VersionUtils.getGatewayProductVersionBuild();
        this.productEdition = VersionUtils.getGatewayProductEdition();

        ManagementUpdateCheck updateCheckerLookup;
        try {
            // TODO, force update check should really only be available from the service bean and then with fallback with console
            // going directly to the source itself.  Before we do that we should refactor the SNMP tables to allow for services
            // to offer componentized services
            updateCheckerLookup = ManagementUpdateCheckFactory.newManagementUpdateCheckFactory().newUpdateCheck("http");
        } catch (IllegalArgumentException e) {
            updateCheckerLookup = null;
        }
        this.updateChecker = updateCheckerLookup;
    }

    @Override
    public int getId() {
        return id;
    }

    @Override
    public String getHostAndPid() {
        return hostAndPid;
    }

    @Override
    public String getProductTitle() {
        return productTitle;
    }

    @Override
    public String getProductBuild() {
        return productBuild;
    }

    @Override
    public String getProductEdition() {
        return productEdition;
    }

    @Override
    public long getTotalCurrentSessions() {
        long total = 0;
        for (ThreadGatewayStats stats : gatewayStatsMap.values()) {
            total += stats.getTotalCurrentSessions();
        }

        return total;
    }

    @Override
    public long getTotalBytesReceived() {
        long total = 0;
        for (ThreadGatewayStats stats : gatewayStatsMap.values()) {
            total += stats.getTotalBytesReceived();
        }

        return total;
    }

    @Override
    public long getTotalBytesSent() {
        long total = 0;
        for (ThreadGatewayStats stats : gatewayStatsMap.values()) {
            total += stats.getTotalBytesSent();
        }

        return total;
    }

    @Override
    public long getTotalExceptions() {
        long total = 0;
        for (ThreadGatewayStats stats : gatewayStatsMap.values()) {
            total += stats.getTotalExceptions();
        }

        return total;
    }

    @Override
    public long getUptime() {
        return System.currentTimeMillis() - startTime;
    }

    @Override
    public long getStartTime() {
        return startTime;
    }

    @Override
    public String getInstanceKey() {
        // NOTE: the ClusterContext pointed to here is always present, even if we're a singleton
        // instead of defining a <cluster> element in the config. The clusterContext that's a
        // member variable of this bean is set from outside, and is not set unless we have a
        // real cluster config.
        ClusterContext context = gatewayContext.getCluster();
        String instanceKey = context.getInstanceKey(context.getLocalMember());
        return instanceKey;
    }

    @Override
    public void setClusterContext(ClusterContext clusterContext) {
        this.clusterContext = clusterContext;
        if (clusterContext != null) {
            clusterContext.addMembershipEventListener(this);
        }
    }

    @Override
    public String getClusterMembers() {
        if (clusterContext == null) {
            return "";
        }

        CollectionsFactory factory = clusterContext.getCollectionsFactory();
        Collection<MemberId> memberIds = clusterContext.getMemberIds();
        Map<MemberId, Map<String, List<String>>> memberIdBalancerMap = factory
                .getMap(HttpBalancerService.MEMBERID_BALANCER_MAP_NAME);

        JSONObject jsonObj = new JSONObject();

        try {
            for (MemberId memberId : memberIds) {
                String instanceKey = clusterContext.getInstanceKey(memberId);
                Map<String, List<String>> balancerURIMap = memberIdBalancerMap.get(memberId);

                if (balancerURIMap != null) {
                    JSONObject uriMap = new JSONObject();

                    for (String balancerURI : balancerURIMap.keySet()) {
                        List<String> balanceeURIs = balancerURIMap.get(balancerURI);
                        JSONArray jsonArray = new JSONArray();
                        for (String balanceeURI : balanceeURIs) {
                            jsonArray.put(balanceeURI);
                        }
                        uriMap.put(balancerURI, jsonArray);
                    }

                    jsonObj.put(instanceKey, uriMap);
                } else {
                    jsonObj.put(instanceKey, JSONObject.NULL);
                }
            }
        } catch (JSONException ex) {
            // We know the values are valid, we should not be able to get to here.
            throw new RuntimeException("Error inserting balancer URIs for cluster members into JSON object");
        }

        return jsonObj.toString();
    }

    @Override
    public String getManagementServiceMap() {
        if (clusterContext == null) {
            return "";
        }

        CollectionsFactory factory = clusterContext.getCollectionsFactory();
        Map<MemberId, Collection<String>> managementServices = factory.getMap(ManagementService.MANAGEMENT_SERVICE_MAP_NAME);
        if ((managementServices == null) || managementServices.isEmpty()) {
            return "";
        }

        JSONObject jsonObj = new JSONObject();

        try {
            for (MemberId member : managementServices.keySet()) {
                String instanceKey = clusterContext.getInstanceKey(member);

                JSONArray jsonArray = new JSONArray();

                Collection<String> acceptURIs = managementServices.get(member);

                if (acceptURIs != null) {
                    for (String acceptURI : acceptURIs) {
                        jsonArray.put(acceptURI);
                    }
                }

                jsonObj.put(instanceKey, jsonArray);
            }
        } catch (JSONException ex) {
            // We know the values are valid, we should not be able to get to here.
            throw new RuntimeException("Error inserting acceptURIs for management services into JSON array");
        }

        return jsonObj.toString();
    }

    @Override
    public String getClusterBalancerMap() {
        if (clusterContext == null) {
            return "";
        }

        CollectionsFactory factory = clusterContext.getCollectionsFactory();
        Map<String, Collection<String>> balancers = factory.getMap(HttpBalancerService.BALANCER_MAP_NAME);
        if ((balancers == null) || balancers.isEmpty()) {
            return "";
        }

        JSONObject jsonObj = new JSONObject();

        try {
            for (String uri : balancers.keySet()) {

                Collection<String> balancees = balancers.get(uri);
                if (balancees != null && balancees.size() > 0) {
                    JSONArray jsonArray = new JSONArray();

                    for (String balanceeURI : balancees) {
                        jsonArray.put(balanceeURI);
                    }

                    jsonObj.put(uri, jsonArray);
                } else {
                    jsonObj.put(uri, JSONObject.NULL);
                }
            }
        } catch (JSONException ex) {
            // We know the values are valid, we should not be able to get to here.
            throw new RuntimeException("Error inserting balanceeURIs for balancerURIs into JSON array");
        }

        return jsonObj.toString();
    }

    @Override
    public void addClusterManagementListener(ClusterManagementListener listener) {
        listener.setGatewayBean(this);
        clusterManagementListeners.add(listener);
    }

    @Override
    public String getSummaryData() {
        JSONArray jsonArray = null;

        try {
            Object[] vals = new Object[SUMMARY_DATA_FIELD_LIST.length];

            vals[SUMMARY_DATA_TOTAL_CURRENT_SESSIONS_INDEX] = 0L;
            vals[SUMMARY_DATA_TOTAL_BYTES_RECEIVED_INDEX] = 0L;
            vals[SUMMARY_DATA_TOTAL_BYTES_SENT_INDEX] = 0L;
            vals[SUMMARY_DATA_TOTAL_EXCEPTIONS_INDEX] = 0L;

            for (ThreadGatewayStats stats : gatewayStatsMap.values()) {
                stats.collectSummaryValues(vals);
            }

            jsonArray = new JSONArray(vals);
        } catch (JSONException ex) {
            // We should never be able to get here, as the summary data values are all legal
        }

        return jsonArray.toString();
    }

    private List<GatewayManagementListener> getManagementListeners() {
        return managementContext.getGatewayManagementListeners();
    }

    @Override
    public void memberAdded(MemberId newMember) {
        // Removed the listener 'membershipChanged' notification to instanceKeyAdded
    }

    @Override
    public void memberRemoved(MemberId removedMember) {
        CollectionsFactory factory = clusterContext.getCollectionsFactory();
        Map<MemberId, Collection<String>> managementServiceUriMap = factory.
                getMap(ManagementService.MANAGEMENT_SERVICE_MAP_NAME);
        managementServiceUriMap.remove(removedMember);
    }

    @Override
    public void entryAdded(EntryEvent<MemberId, Collection<String>> event) {
        MemberId memberId = event.getKey();
        String instanceKey = clusterContext.getInstanceKey(memberId);
        for (ClusterManagementListener listener : clusterManagementListeners) {
            listener.managementServicesChanged("add", instanceKey, event.getValue());
        }
    }

    @Override
    public void entryEvicted(EntryEvent<MemberId, Collection<String>> event) {
        // this listener is here to track when new management services are added, so we can ignore this
    }

    @Override
    public void entryRemoved(EntryEvent<MemberId, Collection<String>> event) {
        // this listener is here to track when new management services are added, so we can ignore this
    }

    @Override
    public void entryUpdated(EntryEvent<MemberId, Collection<String>> event) {
        // this listener is here to track when new management services are added, so we can ignore this
    }

    @Override
    public void mapCleared(MapEvent event) {
        // this listener is here to track when new management services are added, so we can ignore this
    }

    @Override
    public void mapEvicted(MapEvent event) {
        // this listener is here to track when new management services are added, so we can ignore this
    }

    // Implement various methods needed by the strategy objects.

    // This must run ON the IO thread
    @Override
    public void doSessionCreated(final long sessionId, final ManagementSessionType managementSessionType) throws Exception {
        ThreadGatewayStats stats = gatewayStats.get();
        stats.doSessionCreated();
    }

    /**
     * Notify the management listeners on a sessionCreated.
     * <p/>
     * NOTE: this starts on the IO thread, but runs a task OFF the thread.
     */
    @Override
    public void doSessionCreatedListeners(final long sessionId, final ManagementSessionType managementSessionType) {
        runManagementTask(new Runnable() {
            @Override
            public void run() {
                try {
                    // The particular management listeners change on strategy, so get them here.
                    for (final GatewayManagementListener listener : getManagementListeners()) {
                        listener.doSessionCreated(GatewayManagementBeanImpl.this, sessionId);
                    }

                    markChanged();  // mark ourselves as changed, possibly tell listeners
                } catch (Exception ex) {
                    logger.warn("Error during sessionCreated gateway listener notifications:", ex);
                }
            }
        });
    }

    // This must run ON the IO thread
    @Override
    public void doSessionClosed(final long sessionId, final ManagementSessionType managementSessionType) throws Exception {
        ThreadGatewayStats stats = gatewayStats.get();
        stats.doSessionClosed();
    }

    /**
     * Notify the management listeners on a sessionClosed.
     * <p/>
     * NOTE: this starts on the IO thread, but runs a task OFF the thread.
     */
    @Override
    public void doSessionClosedListeners(final long sessionId, final ManagementSessionType managementSessionType) {
        runManagementTask(new Runnable() {
            @Override
            public void run() {
                try {
                    // The particular management listeners change on strategy, so get them here.
                    for (final GatewayManagementListener listener : getManagementListeners()) {
                        listener.doSessionClosed(GatewayManagementBeanImpl.this, sessionId);
                    }

                    markChanged();  // mark ourselves as changed, possibly tell listeners
                } catch (Exception ex) {
                    logger.warn("Error during sessionClosed gateway listener notifications:", ex);
                }
            }
        });
    }

    // This must run ON the IO thread
    @Override
    public void doMessageReceived(final long sessionId, final long sessionReadBytes, final Object message) throws Exception {
        if (message instanceof ByteBuffer) {
            ThreadGatewayStats stats = gatewayStats.get();
            stats.doMessageReceived((IoBuffer) message);
        }
    }

    /**
     * Notify the management listeners on a messageReceived.
     * <p/>
     * NOTE: this starts on the IO thread, but runs a task OFF the thread.
     */
    @Override
    public void doMessageReceivedListeners(final long sessionId, final long sessionReadBytes, final Object message) {
        runManagementTask(new Runnable() {
            @Override
            public void run() {
                try {
                    // The particular management listeners change on strategy, so get them here.
                    for (final GatewayManagementListener listener : getManagementListeners()) {
                        listener.doMessageReceived(GatewayManagementBeanImpl.this, sessionId);
                    }

                    markChanged();  // mark ourselves as changed, possibly tell listeners
                } catch (Exception ex) {
                    logger.warn("Error during messageReceived gateway listener notifications:", ex);
                }
            }
        });
    }


    // This must run ON the IO thread
    @Override
    public void doFilterWrite(final long sessionId, final long sessionWrittenBytes, final WriteRequest writeRequest) throws
            Exception {
        Object message = writeRequest.getMessage();

        if (message instanceof IoBuffer) {
            ThreadGatewayStats stats = gatewayStats.get();
            stats.doFilterWrite((IoBuffer) message);
        }
    }

    /**
     * Notify the management listeners on a filterWrite.
     * <p/>
     * NOTE: this starts on the IO thread, but runs a task OFF the thread.
     */
    @Override
    public void doFilterWriteListeners(final long sessionId, final long sessionWrittenBytes, final WriteRequest writeRequest) {
        runManagementTask(new Runnable() {
            @Override
            public void run() {
                try {
                    // The particular management listeners change on strategy, so get them here.
                    for (final GatewayManagementListener listener : getManagementListeners()) {
                        listener.doFilterWrite(GatewayManagementBeanImpl.this, sessionId);
                    }

                    markChanged();  // mark ourselves as changed, possibly tell listeners
                } catch (Exception ex) {
                    logger.warn("Error during filterWrite gateway listener notifications:", ex);
                }
            }
        });
    }

    // This must run ON the IO thread
    @Override
    public void doExceptionCaught(final long sessionId, final Throwable cause) throws Exception {
        ThreadGatewayStats stats = gatewayStats.get();
        stats.doExceptionCaught();
    }

    /**
     * Notify the management listeners on a filterWrite.
     * <p/>
     * NOTE: this starts on the IO thread, but runs a task OFF the thread.
     */
    @Override
    public void doExceptionCaughtListeners(final long sessionId, final Throwable cause) {
        runManagementTask(new Runnable() {
            @Override
            public void run() {
                try {
                    // The particular management listeners change on strategy, so get them here.
                    for (final GatewayManagementListener listener : getManagementListeners()) {
                        listener.doExceptionCaught(GatewayManagementBeanImpl.this, sessionId);
                    }

                    markChanged();  // mark ourselves as changed, possibly tell listeners
                } catch (Exception ex) {
                    logger.warn("Error during exceptionCaught gateway listener notifications:", ex);
                }
            }
        });
    }

    /**
     * Ongoing service statistics. There is an instance of this class per worker thread, stored as a ThreadLocal on the thread,
     * with a reference to it in a CopyOnWriteMap stored here in ServiceManagementBeanImpl so we can do insertions of stats
     * objects into the map without locks.
     */
    private class ThreadGatewayStats {

        private long totalCurrentSessions;
        private long totalBytesReceived;
        private long totalBytesSent;
        private long totalExceptions;

        /**
         * Given a session, extract relevant counts and update them locally.
         *
         * @param session
         * @return
         */
        // This must only be called ON an IO thread (the one equal to this structure)
        public long doSessionCreated() {
            totalCurrentSessions++;
            return totalCurrentSessions;
        }

        public long doSessionClosed() {
            totalCurrentSessions--;
            return totalCurrentSessions;
        }

        public long doMessageReceived(IoBuffer buf) {
            totalBytesReceived += buf.remaining();
            return totalBytesReceived;
        }

        public long doFilterWrite(IoBuffer buf) {
            totalBytesSent += buf.remaining();
            return totalBytesSent;
        }

        public long doExceptionCaught() {
            totalExceptions++;
            return totalExceptions;
        }

        // For use by the 'summation' methods All of these try to create a future to run on
        // the IO worker thread associated with the map.

        // This runs OFF any IO worker thread
        public long getTotalCurrentSessions() {
            return totalCurrentSessions;
        }

        // This runs OFF any IO worker thread
        public long getTotalBytesReceived() {
            return totalBytesReceived;
        }

        // This runs OFF any IO worker thread
        public long getTotalBytesSent() {
            return totalBytesSent;
        }

        // This runs OFF any IO worker thread
        public long getTotalExceptions() {
            return totalExceptions;
        }

        // This runs OFF any IO worker thread. The final list of summary values from a service
        // actually includes data we do not keep in the thread-specific area. See ServiceManagementBeanImpl for those.
        public void collectSummaryValues(Object[] vals) {
            vals[GatewayManagementBean.SUMMARY_DATA_TOTAL_CURRENT_SESSIONS_INDEX] =
                    ((Long) vals[GatewayManagementBean.SUMMARY_DATA_TOTAL_CURRENT_SESSIONS_INDEX]) + totalCurrentSessions;
            vals[GatewayManagementBean.SUMMARY_DATA_TOTAL_BYTES_RECEIVED_INDEX] =
                    ((Long) vals[GatewayManagementBean.SUMMARY_DATA_TOTAL_BYTES_RECEIVED_INDEX]) + totalBytesReceived;
            vals[GatewayManagementBean.SUMMARY_DATA_TOTAL_BYTES_SENT_INDEX] =
                    ((Long) vals[GatewayManagementBean.SUMMARY_DATA_TOTAL_BYTES_SENT_INDEX]) + totalBytesSent;
            vals[GatewayManagementBean.SUMMARY_DATA_TOTAL_EXCEPTIONS_INDEX] =
                    ((Long) vals[GatewayManagementBean.SUMMARY_DATA_TOTAL_EXCEPTIONS_INDEX]) + totalExceptions;
            vals[GatewayManagementBean.SUMMARY_DATA_LATEST_UPDATEABLE_GATEWAY_VERSION_INDEX] = getAvailableUpdateVersion();
        }
    }

    @Override
    public String getAvailableUpdateVersion() {
        String version = "";
        if (updateChecker != null) {
            version = updateChecker.getAvailableUpdateVersion();
        }
        return version;
    }

    @Override
    public ManagementUpdateCheck getUpdateCheck() {
        return updateChecker;
    }

    @Override
    public void forceUpdateVersionCheck() {
        if (updateChecker != null) {
            updateChecker.checkForUpdate();
        }
    }

}

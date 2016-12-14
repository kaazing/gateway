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
package org.kaazing.gateway.management.service;

import static java.lang.String.format;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.security.Principal;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.write.WriteRequest;
import org.apache.mina.util.CopyOnWriteMap;
import org.json.JSONArray;
import org.json.JSONException;
import org.kaazing.gateway.management.AbstractManagementBean;
import org.kaazing.gateway.management.ManagementBean;
import org.kaazing.gateway.management.Utils;
import org.kaazing.gateway.management.Utils.ManagementSessionType;
import org.kaazing.gateway.management.context.DefaultManagementContext;
import org.kaazing.gateway.management.gateway.GatewayManagementBean;
import org.kaazing.gateway.management.session.SessionManagementBean;
import org.kaazing.gateway.security.RealmContext;
import org.kaazing.gateway.server.Gateway;
import org.kaazing.gateway.service.Service;
import org.kaazing.gateway.service.ServiceContext;
import org.kaazing.gateway.service.proxy.ProxyService;
import org.kaazing.gateway.service.proxy.ServiceConnectManager;
import org.kaazing.gateway.transport.AbstractBridgeSession;
import org.kaazing.mina.core.session.IoSessionEx;
import org.kaazing.mina.netty.util.threadlocal.VicariousThreadLocal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Interface that defines the data and access methods that will be supported by all management protocols (e.g., JMX, SNMP) for a
 * given service. Various lifecycle methods are also provided, to allow things like initialization and cleanup on start/stop.
 * NOTE: there are certain methods that must be publicly callable, but are not actually supposed to be exposed to the users.
 */
public interface ServiceManagementBean extends ManagementBean {

    String[] SUMMARY_DATA_FIELD_LIST = new String[]{"serviceConnected", "totalBytesReceived",
            "totalBytesSent", "totalCurrentSessions", "totalCurrentNativeSessions", "totalCurrentEmulatedSessions",
            "totalCumulativeSessions", "totalCumulativeNativeSessions", "totalCumulativeEmulatedSessions",
            "totalExceptionCount", "latestException", "latestExceptionTime", "lastSuccessfulConnectTime",
            "lastFailedConnectTime", "lastHeartbeatPingResult", "lastHeartbeatPingTimestamp", "heartbeatPingCount",
            "heartbeatPingSuccesses", "heartbeatPingFailures", "heartbeatRunning", "notificationsEnabled"};

    int SUMMARY_DATA_SERVICE_CONNECTED_INDEX = 0;
    int SUMMARY_DATA_TOTAL_BYTES_RECEIVED_INDEX = 1;
    int SUMMARY_DATA_TOTAL_BYTES_SENT_INDEX = 2;
    int SUMMARY_DATA_TOTAL_CURRENT_SESSIONS_INDEX = 3;
    int SUMMARY_DATA_TOTAL_CURRENT_NATIVE_SESSIONS_INDEX = 4;
    int SUMMARY_DATA_TOTAL_CURRENT_EMULATED_SESSIONS_INDEX = 5;
    int SUMMARY_DATA_TOTAL_CUMULATIVE_SESSIONS_INDEX = 6;
    int SUMMARY_DATA_TOTAL_CUMULATIVE_NATIVE_SESSIONS_INDEX = 7;
    int SUMMARY_DATA_TOTAL_CUMULATIVE_EMULATED_SESSIONS_INDEX = 8;
    int SUMMARY_DATA_TOTAL_EXCEPTION_COUNT_INDEX = 9;
    int SUMMARY_DATA_LATEST_EXCEPTION_INDEX = 10;
    int SUMMARY_DATA_LATEST_EXCEPTION_TIME_INDEX = 11;
    int SUMMARY_DATA_LAST_SUCCCESSFUL_CONNECT_TIME_INDEX = 12;
    int SUMMARY_DATA_LAST_FAILED_CONNECT_TIME_INDEX = 13;
    int SUMMARY_DATA_LAST_HEARTBEAT_PING_RESULT_INDEX = 14;
    int SUMMARY_DATA_LAST_HEARTBEAT_PING_TIMESTAMP_INDEX = 15;
    int SUMMARY_DATA_HEARTBEAT_PING_COUNT_INDEX = 16;
    int SUMMARY_DATA_HEARTBEAT_PING_SUCCESSES_INDEX = 17;
    int SUMMARY_DATA_HEARTBEAT_PING_FAILURES_INDEX = 18;
    int SUMMARY_DATA_HEARTBEAT_RUNNING_INDEX = 19;
    int SUMMARY_DATA_NOTIFICATIONS_ENABLED_INDEX = 20;

    GatewayManagementBean getGatewayManagementBean();

    int getId();

    String getServiceType();

    long getCumulativeSessionCount();

    long getCumulativeNativeSessionCount();

    long getCumulativeEmulatedSessionCount();

    long getCurrentSessionCount();

    long getCurrentNativeSessionCount();

    long getCurrentEmulatedSessionCount();

    long getExceptionCount();

    String getLatestException();

    long getLatestExceptionTime();

    void clearCumulativeSessionsCount();

    long getTotalBytesReceivedCount();

    long getTotalBytesSentCount();

    long[] collectCurrentSessionCounts();

    // A map of sessionID to user principals for the associated session
    Map<Long, Map<String, String>> getLoggedInSessions();

    Map<String, String> getUserPrincipals(Long sessionId);

    IoSessionEx getSession(long sessionId);

    ServiceContext getServiceContext();

    Set<Class<Principal>> getUserPrincipalClasses();

    String getServiceName();

    /**
     * Convenience routine so we can get a string value for the IP address of a session. Used both for new sessions and session
     * open-close.
     *
     * @param session
     * @return
     */
    String getSessionRemoteAddress(IoSessionEx session);

    // -----------------------------------------------------------------
    // For proxy style services the following methods should return data
    // -----------------------------------------------------------------

    long getLastSuccessfulConnectTime();

    long getLastFailedConnectTime();

    boolean getLastHeartbeatPingResult();

    long getLastHeartbeatPingTimestamp();

    int getHeartbeatPingCount();

    int getHeartbeatPingSuccessesCount();

    int getHeartbeatPingFailuresCount();

    boolean isServiceConnected();

    boolean isHeartbeatRunning();

    // -----------------------------------------------------------------
    // end of proxy-style service data
    // -----------------------------------------------------------------

    // Now some lifecycle methods, generally called from a particular
    // management interface through the beans for that protocol, for
    // example the ServiceMXBean for a given service.
    void start() throws Exception;

    void stop() throws Exception;

    void restart() throws Exception;

    // Methods that we expose as part of event handling at the service level to
    // allow us to modify the service management bean before it's handed to the
    // managementProcessors for protocol-specific handling.

    void doSessionCreated(long sessionId, ManagementSessionType managementSessionType) throws Exception;

    void doSessionCreatedListeners(final long sessionId, final ManagementSessionType managementSessionType);

    void doSessionClosed(long sessionId, ManagementSessionType managementSessionType) throws Exception;

    void doSessionClosedListeners(final long sessionId, final ManagementSessionType managementSessionType);

    void doMessageReceived(long sessionId, long sessionReadBytes, Object message) throws Exception;

    void doMessageReceivedListeners(final long sessionId, final long sessionReadBytes, final Object message);

    void doFilterWrite(long sessionId, long sessionWrittenBytes, WriteRequest writeRequest) throws Exception;

    void doFilterWriteListeners(final long sessionId,
                                       final long sessionWrittenBytes,
                                       final WriteRequest writeRequest);

    String doExceptionCaught(long sessionId, Throwable cause) throws Exception;

    void doExceptionCaughtListeners(final long sessionId, final Throwable cause);

    void storeSessionManagementBean(SessionManagementBean sessionBean);

    SessionManagementBean getSessionManagementBean(long sessionId);

    SessionManagementBean removeSessionManagementBean(long sessionId);

    void enableNotifications(boolean notificationsEnabled);

    boolean areNotificationsEnabled();

    void addUserPrincipals(IoSessionEx session, Map<String, String> userPrincipals);

    void removeUserPrincipals(IoSessionEx session);

    class DefaultServiceManagementBean extends AbstractManagementBean implements ServiceManagementBean {
        // Each IO worker thread gets a ThreadServiceStats object via get().
        private final ThreadLocal<ThreadServiceStats> serviceStats = new VicariousThreadLocal<ThreadServiceStats>() {
            @Override
            protected ThreadServiceStats initialValue() {
                ThreadServiceStats stats = new ThreadServiceStats();
                serviceStatsMap.put(Thread.currentThread(), stats);
                return stats;
            }
        };

        // ------------------------------------
        // Main class member vars start here
        // ------------------------------------

        private static final Logger logger = LoggerFactory.getLogger(DefaultServiceManagementBean.class);
        private static final Logger gatewayStartupLogger = LoggerFactory.getLogger(Gateway.class);

        // Map of the per-thread thread-local stats objects. Keyed on thread ID.
        private final CopyOnWriteMap<Thread, ThreadServiceStats> serviceStatsMap =
                new CopyOnWriteMap<>();

        // Keep a unique index number for each service instance, as we can use
        // it in SNMP for an OID, and it might be useful elsewhere if we decide
        // we want to use it in place of some map key or something. The SNMP
        // support for sessions also depends on knowing this value.
        // private static final AtomicInteger maxServiceIndex = new AtomicInteger(1);

        private final int index;

        private boolean notificationsEnabled;


        private final GatewayManagementBean gatewayManagementBean;
        private final ServiceContext serviceContext;
        private final ServiceConnectManager serviceConnectManager;

        private final Set<Class<Principal>> userPrincipalClasses;

        /*
         * Various bundles (e.g. Stomp JMS and perhaps AMQP or another one) that need to do authentication handling can
         * potentially get in a situation where they first having called 'sessionCreated', or b) Somehow call
         * 'sessionClosed' more than once. We want to prevent both scenarios, so to do so we'll have a flag that is set
         * during sessionCreated, then cleared after the first time through sessionClosed.
         */
        private Set<Long> sessionCreatedFlag = new HashSet<>();

        /**
         * Constructor. The reason we pass in the managementProcessorList is that during sessionClosed, there are things we want
         * to do with the various ManagementProcessors (like notifications) that use the new values of counts in the bean. The
         * problem is that if some other session closes at roughly the same time, we'll have a race condition on reporting the
         * 'current session count' value. The best way to fix that is to get the new value as we change the old one, then send
         * that value to the management processors, rather than forcing them to fetch the current one later and assume that it is
         * correct.
         *
         * @param address
         * @param gatewayManagementBean
         * @param serviceContext
         * @param beanId
         */
        protected DefaultServiceManagementBean(GatewayManagementBean gatewayManagementBean,
                                               ServiceContext serviceContext) {
            super(gatewayManagementBean.getManagementContext(), gatewayManagementBean.getManagementContext()
                    .getServiceSummaryDataNotificationInterval(), SUMMARY_DATA_FIELD_LIST);

            this.index = DefaultManagementContext.getNextServiceIndex(serviceContext);

            this.gatewayManagementBean = gatewayManagementBean;
            this.serviceContext = serviceContext;

            Service service = serviceContext.getService();
            if (service instanceof ProxyService) {
                ProxyService proxyService = (ProxyService) service;
                this.serviceConnectManager = proxyService.getServiceConnectManager();
            } else {
                this.serviceConnectManager = null;
            }

            userPrincipalClasses = new HashSet<>();
            RealmContext realmContext = serviceContext.getServiceRealm();
            if (realmContext != null) {
                String[] upc = realmContext.getUserPrincipalClasses();
                if (upc != null) {
                    for (String className : upc) {
                        Class<?> untypedClass;
                        try {
                            untypedClass = org.kaazing.gateway.util.Utils.loadClass(className);
                            if ( !(Principal.class.isAssignableFrom(untypedClass)) ) {
                                String message = className + " is not of type Principal";
                                gatewayStartupLogger.error(message);
                                throw new IllegalArgumentException(message);
                            }
                        }
                        catch (ClassNotFoundException | NoSuchFieldException | IllegalAccessException e) {
                            String message = format("Unable to load user principal class \"%s\" due to exception \"%s\"",
                                    className, e.toString());
                            if (gatewayStartupLogger.isDebugEnabled()) {
                                // Include stack trace
                                gatewayStartupLogger.error(message, e);
                            }
                            else {
                                gatewayStartupLogger.error(message);
                            }
                            throw new IllegalArgumentException(message);
                        }
                        @SuppressWarnings("unchecked")
                        Class<Principal> principalClass = (Class<Principal>) untypedClass;
                        userPrincipalClasses.add(principalClass);
                    }
                }
            }
        }

        @Override
        public GatewayManagementBean getGatewayManagementBean() {
            return gatewayManagementBean;
        }

        private List<ServiceManagementListener> getManagementListeners() {
            return managementContext.getServiceManagementListeners();
        }

        @Override
        public String getServiceType() {
            return serviceContext.getServiceType();
        }

        @Override
        public int getId() {
            return index;
        }

        @Override
        public Set<Class<Principal>> getUserPrincipalClasses() {
            return userPrincipalClasses;
        }

        @Override
        public void enableNotifications(boolean notificationsEnabled) {
            this.notificationsEnabled = notificationsEnabled;
        }

        @Override
        public boolean areNotificationsEnabled() {
            return notificationsEnabled;
        }

        // XXX This runs OFF the IO thread
        @Override
        public void clearCumulativeSessionsCount() {
            for (ThreadServiceStats stats : serviceStatsMap.values()) {
                stats.clearCumulativeSessionCount();
            }
        }

        // XXX This runs OFF the IO thread
        @Override
        public long getCurrentSessionCount() {
            long total = 0;
            for (ThreadServiceStats stats : serviceStatsMap.values()) {
                total += stats.getCurrentSessionCount();
            }

            return total;
        }

        // XXX This runs OFF the IO thread
        @Override
        public long getCurrentNativeSessionCount() {
            long total = 0;
            for (ThreadServiceStats stats : serviceStatsMap.values()) {
                total += stats.getCurrentNativeSessionCount();
            }

            return total;
        }

        // XXX This runs OFF the IO thread
        @Override
        public long getCurrentEmulatedSessionCount() {
            long total = 0;
            for (ThreadServiceStats stats : serviceStatsMap.values()) {
                total += stats.getCurrentEmulatedSessionCount();
            }

            return total;
        }

        // XXX This runs OFF the IO thread
        @Override
        public long getCumulativeSessionCount() {
            long total = 0;
            for (ThreadServiceStats stats : serviceStatsMap.values()) {
                total += stats.getCumulativeSessionCount();
            }

            return total;
        }

        // XXX This runs OFF the IO thread
        @Override
        public long getCumulativeNativeSessionCount() {
            long total = 0;
            for (ThreadServiceStats stats : serviceStatsMap.values()) {
                total += stats.getCumulativeNativeSessionCount();
            }

            return total;
        }

        // XXX This runs OFF the IO thread
        @Override
        public long getCumulativeEmulatedSessionCount() {
            long total = 0;
            for (ThreadServiceStats stats : serviceStatsMap.values()) {
                total += stats.getCumulativeEmulatedSessionCount();
            }

            return total;
        }

        // XXX This runs OFF the IO thread
        @Override
        public long getExceptionCount() {
            long total = 0;
            for (ThreadServiceStats stats : serviceStatsMap.values()) {
                total += stats.getExceptionCount();
            }

            return total;
        }

        // XXX This runs OFF the IO thread
        @Override
        public String getLatestException() {
            Object[] exceptionData = {-1, null};

            for (ThreadServiceStats stats : serviceStatsMap.values()) {
                stats.collectLatestException(exceptionData);
            }

            return (String) exceptionData[1];
        }

        // XXX This runs OFF the IO thread
        @Override
        public long getLatestExceptionTime() {
            Object[] exceptionData = {-1, null};

            for (ThreadServiceStats stats : serviceStatsMap.values()) {
                stats.collectLatestException(exceptionData);
            }

            return (Long) exceptionData[0];
        }

        // XXX This runs OFF the IO thread
        @Override
        public long getTotalBytesReceivedCount() {
            long total = 0;
            for (ThreadServiceStats stats : serviceStatsMap.values()) {
                total += stats.getTotalBytesReceivedCount();
            }

            return total;
        }

        // XXX This runs OFF the IO thread
        @Override
        public long getTotalBytesSentCount() {
            long total = 0;
            for (ThreadServiceStats stats : serviceStatsMap.values()) {
                total += stats.getTotalBytesSentCount();
            }

            return total;
        }

        @Override
        public long[] collectCurrentSessionCounts() {
            long[] counts = {0, 0};
            for (ThreadServiceStats stats : serviceStatsMap.values()) {
                counts[0] = counts[0] + stats.getCurrentSessionCount();
                counts[1] = counts[1] + stats.getCumulativeSessionCount();
            }
            return counts;
        }

        // XXX This runs OFF the IO thread
        @Override
        public String getSummaryData() {

            //long start = System.nanoTime();

            JSONArray jsonArray = null;

            try {
                Object[] vals = new Object[SUMMARY_DATA_FIELD_LIST.length];

                vals[SUMMARY_DATA_SERVICE_CONNECTED_INDEX] = isServiceConnected();
                vals[SUMMARY_DATA_TOTAL_BYTES_RECEIVED_INDEX] = 0L;
                vals[SUMMARY_DATA_TOTAL_BYTES_SENT_INDEX] = 0L;
                vals[SUMMARY_DATA_TOTAL_CURRENT_SESSIONS_INDEX] = 0L;
                vals[SUMMARY_DATA_TOTAL_CURRENT_NATIVE_SESSIONS_INDEX] = 0L;
                vals[SUMMARY_DATA_TOTAL_CURRENT_EMULATED_SESSIONS_INDEX] = 0L;
                vals[SUMMARY_DATA_TOTAL_CUMULATIVE_SESSIONS_INDEX] = 0L;
                vals[SUMMARY_DATA_TOTAL_CUMULATIVE_NATIVE_SESSIONS_INDEX] = 0L;
                vals[SUMMARY_DATA_TOTAL_CUMULATIVE_EMULATED_SESSIONS_INDEX] = 0L;
                vals[SUMMARY_DATA_TOTAL_EXCEPTION_COUNT_INDEX] = 0L;
                vals[SUMMARY_DATA_LATEST_EXCEPTION_INDEX] = null;
                vals[SUMMARY_DATA_LATEST_EXCEPTION_TIME_INDEX] = 0L;
                vals[SUMMARY_DATA_LAST_SUCCCESSFUL_CONNECT_TIME_INDEX] = getLastSuccessfulConnectTime();
                vals[SUMMARY_DATA_LAST_FAILED_CONNECT_TIME_INDEX] = getLastFailedConnectTime();
                vals[SUMMARY_DATA_LAST_HEARTBEAT_PING_RESULT_INDEX] = getLastHeartbeatPingResult();
                vals[SUMMARY_DATA_LAST_HEARTBEAT_PING_TIMESTAMP_INDEX] = getLastHeartbeatPingTimestamp();
                vals[SUMMARY_DATA_HEARTBEAT_PING_COUNT_INDEX] = getHeartbeatPingCount();
                vals[SUMMARY_DATA_HEARTBEAT_PING_SUCCESSES_INDEX] = getHeartbeatPingSuccessesCount();
                vals[SUMMARY_DATA_HEARTBEAT_PING_FAILURES_INDEX] = getHeartbeatPingFailuresCount();
                vals[SUMMARY_DATA_HEARTBEAT_RUNNING_INDEX] = isHeartbeatRunning();
                vals[SUMMARY_DATA_NOTIFICATIONS_ENABLED_INDEX] = areNotificationsEnabled();

                for (ThreadServiceStats stats : serviceStatsMap.values()) {
                    stats.collectSummaryValues(vals);
                }

                jsonArray = new JSONArray(vals);
            } catch (JSONException ex) {
                // We should never be able to get here, as the summary data values are all legal
            }


            String val = jsonArray.toString();

            // long stop = System.nanoTime();
            // System.out.println("### Gathering summaries for SVC ID " + getId() +
            // " took " + ((stop - start) / 1000) + " us for " + val.length() + " chars [" + val + "]");

            return val;
        }

        // XXX This runs OFF the IO thread
        @Override
        public Map<Long, Map<String, String>> getLoggedInSessions() {
            Map<Long, Map<String, String>> sessions = new HashMap<>();

            for (ThreadServiceStats stats : serviceStatsMap.values()) {
                stats.collectLoggedInSessions(sessions);
            }

            return sessions;
        }

        // XXX This runs OFF the IO thread (???)
        @Override
        public Map<String, String> getUserPrincipals(Long sessionId) {
            // return (loggedInSessions != null ? loggedInSessions.get(sessionId) : null);

            return null; // XXX FIX THIS!
        }

        // XXX This runs OFF the IO thread
        @Override
        public long getLastSuccessfulConnectTime() {
            return serviceConnectManager != null ? serviceConnectManager.getLastSuccessfulConnectTime() : 0;
        }

        // XXX This runs OFF the IO thread
        @Override
        public long getLastFailedConnectTime() {
            return serviceConnectManager != null ? serviceConnectManager.getLastFailedConnectTime() : 0;
        }

        // XXX This runs OFF the IO thread
        @Override
        public boolean getLastHeartbeatPingResult() {
            return serviceConnectManager != null ? serviceConnectManager.getLastHeartbeatPingResult() : false;
        }

        // XXX This runs OFF the IO thread
        @Override
        public long getLastHeartbeatPingTimestamp() {
            return serviceConnectManager != null ? serviceConnectManager.getLastHeartbeatPingTimestamp() : 0;
        }

        // XXX This runs OFF the IO thread
        @Override
        public int getHeartbeatPingCount() {
            return serviceConnectManager != null ? serviceConnectManager.getHeartbeatPingCount() : 0;
        }

        // XXX This runs OFF the IO thread
        @Override
        public int getHeartbeatPingSuccessesCount() {
            return serviceConnectManager != null ? serviceConnectManager.getHeartbeatPingSuccessesCount() : 0;
        }

        // XXX This runs OFF the IO thread
        @Override
        public int getHeartbeatPingFailuresCount() {
            return serviceConnectManager != null ? serviceConnectManager.getHeartbeatPingFailuresCount() : 0;
        }

        // XXX This runs OFF the IO thread
        @Override
        public boolean isServiceConnected() {
            return serviceConnectManager != null && serviceConnectManager.isServiceConnected();
        }

        // XXX This runs OFF the IO thread
        @Override
        public boolean isHeartbeatRunning() {
            return serviceConnectManager != null && serviceConnectManager.isHeartbeatRunning();
        }

        @Override
        public IoSessionEx getSession(long sessionId) {
            return serviceContext.getActiveSession(sessionId);
        }

        @Override
        public String getSessionRemoteAddress(IoSessionEx session) {
            // if we happen to have a bridge session, go up until we don't
            // WsebSessions do not, for some reason, and it looks like their
            // reader and writer are null, too.
            while (session instanceof AbstractBridgeSession) {
                IoSessionEx parentSession = ((AbstractBridgeSession<?, ?>) session).getParent();
                if (parentSession == null) {
                    break;
                }
                session = parentSession;
            }

            SocketAddress remoteAddress = session.getRemoteAddress();
            if (remoteAddress != null) {
                if (remoteAddress instanceof InetSocketAddress) {
                    return ((InetSocketAddress) remoteAddress).getAddress().toString();
                } else {
                    return remoteAddress.toString();
                }
            }

            return "";
        }

        // Now some lifecycle methods, generally called from a particular
        // management interface through the beans for that protocol, for
        // example the ServiceMXBean for a given service.

        @Override
        public void start() throws Exception {
            serviceContext.start();
        }

        @Override
        public void stop() throws Exception {
            serviceContext.stop();
        }

        @Override
        public void restart() throws Exception {
            serviceContext.stop();
            serviceContext.start();
        }

        @Override
        public ServiceContext getServiceContext() {
            return serviceContext;
        }

        // Handle service-level calls from the management filter's Strategy object.
        // ALL REQUESTS WILL BE ON ONE OR ANOTHER OF THE IO THREADS, SO MUST NOT BLOCK!

        /**
         * When a session is created, create the thread-local copy of ThreadServiceStats and put the same object into the
         * serviceStatsMap so we can reference it here. Since a given session is pinned to a given worker thread and we use a
         * CopyOnWriteMap for serviceStatsMap, this is thread-safe.
         */
        // This must run ON the IO thread
        @Override
        public void doSessionCreated(long sessionId, ManagementSessionType managementSessionType) throws Exception {
            ThreadServiceStats stats = serviceStats.get();
            stats.doSessionCreated(sessionId, managementSessionType);
            sessionCreatedFlag.add(sessionId);
        }

        /**
         * Notify the management listeners when a session is created.
         * <p/>
         * NOTE: this starts on the IO thread, but runs a task OFF the thread.
         */
        @Override
        public void doSessionCreatedListeners(final long sessionId, final ManagementSessionType managementSessionType) {

            // Call the listeners OFF the IO thread.
            runManagementTask(new Runnable() {
                @Override
                public void run() {
                    try {
                        // We need to gather current stats to send out notifications
                        final long[] counts = collectCurrentSessionCounts();

                        List<ServiceManagementListener> serviceListeners = getManagementListeners();
                        for (final ServiceManagementListener listener : serviceListeners) {
                            listener.doSessionCreated(DefaultServiceManagementBean.this, counts[0], counts[1]);
                        }

                        markChanged();
                    } catch (Exception ex) {
                        logger.warn("Error during doSessionCreated service listener notifications:", ex);
                    }
                }
            });

        }

        // This must run ON the IO thread
        @Override
        public void doSessionClosed(long sessionId, ManagementSessionType managementSessionType) throws Exception {
            if (sessionCreatedFlag.remove(sessionId)) {
                ThreadServiceStats stats = serviceStats.get();
                stats.doSessionClosed(sessionId, managementSessionType);
            }
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
                        // We need to gather current stats to send out notifications
                        final long[] counts = collectCurrentSessionCounts();

                        List<ServiceManagementListener> serviceListeners = getManagementListeners();
                        for (final ServiceManagementListener listener : serviceListeners) {
                            listener.doSessionClosed(DefaultServiceManagementBean.this, sessionId, counts[0]);
                        }

                        markChanged();
                    } catch (Exception ex) {
                        logger.warn("Error during doSessionClosed service listener notifications:", ex);
                    }
                }
            });
        }

        // This must run ON the IO thread
        @Override
        public void doMessageReceived(long sessionId, long sessionReadBytes, Object message) throws Exception {
            ThreadServiceStats stats = serviceStats.get();
            stats.addToBytesReceived(sessionId, sessionReadBytes);
        }

        /**
         * Notify the management listeners on a messageReceived.
         * <p/>
         * NOTE: this starts on the IO thread, but runs a task OFF the thread.
         */
        @Override
        public  void doMessageReceivedListeners(final long sessionId, final long sessionReadBytes, final Object message) {
            runManagementTask(new Runnable() {
                @Override
                public void run() {
                    try {
                        if (message instanceof IoBuffer) {
                            IoBuffer buf = (IoBuffer) message;
                            ByteBuffer byteBuffer = buf.buf();

                            List<ServiceManagementListener> serviceListeners = getManagementListeners();
                            for (final ServiceManagementListener listener : serviceListeners) {
                                listener.doMessageReceived(DefaultServiceManagementBean.this, sessionId, byteBuffer);
                            }
                        }

                        markChanged();
                    } catch (Exception ex) {
                        logger.warn("Error during doMessageReceived service listener notifications:", ex);
                    }
                }
            });
        }

        // This must run ON the IO thread
        @Override
        public void doFilterWrite(long sessionId, long sessionWrittenBytes, WriteRequest writeRequest) throws Exception {
            ThreadServiceStats stats = serviceStats.get();
            stats.addToBytesSent(sessionId, sessionWrittenBytes);
        }

        /**
         * Notify the management listeners on a filterWrite.
         * <p/>
         * NOTE: this starts on the IO thread, but runs a task OFF the thread.
         */
        @Override
        public void doFilterWriteListeners(final long sessionId,
                                           final long sessionWrittenBytes,
                                           final WriteRequest writeRequest) {

            final Object message = writeRequest.getMessage();

            runManagementTask(new Runnable() {
                @Override
                public void run() {
                    try {
                        if (message instanceof IoBuffer) {
                            IoBuffer buf = (IoBuffer) message;
                            ByteBuffer byteBuffer = buf.buf();

                            List<ServiceManagementListener> serviceListeners = getManagementListeners();
                            for (final ServiceManagementListener listener : serviceListeners) {
                                listener.doFilterWrite(DefaultServiceManagementBean.this, sessionId, byteBuffer);
                            }
                        }

                        markChanged();
                    } catch (Exception ex) {
                        logger.warn("Error during doFilterWrite service listener notifications:", ex);
                    }
                }
            });
        }

        // This must run ON the IO thread
        @Override
        public String doExceptionCaught(long sessionId, Throwable cause) throws Exception {
            long time = System.currentTimeMillis();
            final String exceptionMessage = Utils.getCauseString(cause);

            ThreadServiceStats stats = serviceStats.get();
            stats.setLatestException(exceptionMessage, time);
            return exceptionMessage;
        }

        /**
         * Notify the management listeners on a doExceptionCaught
         * <p/>
         * NOTE: this starts on the IO thread, but runs a task OFF the thread.
         */
        @Override
        public void doExceptionCaughtListeners(final long sessionId, final Throwable cause) {
            runManagementTask(new Runnable() {
                @Override
                public void run() {
                    try {
                        final String exceptionMessage = Utils.getCauseString(cause);

                        List<ServiceManagementListener> serviceListeners = getManagementListeners();
                        for (final ServiceManagementListener listener : serviceListeners) {
                            listener.doExceptionCaught(DefaultServiceManagementBean.this, sessionId, exceptionMessage);
                        }

                        markChanged();
                    } catch (Exception ex) {
                        logger.warn("Error during doExceptionCaught service listener notifications:", ex);
                    }
                }
            });

        }

        // This must run ON the IO thread
        @Override
        public void storeSessionManagementBean(SessionManagementBean sessionBean) {
            ThreadServiceStats stats = serviceStats.get();
            stats.storeSessionManagementBean(sessionBean);
        }

        @Override
        public SessionManagementBean getSessionManagementBean(long sessionId) {
            ThreadServiceStats stats = serviceStats.get();
            return stats.getSessionManagementBean(sessionId);
        }

        @Override
        public SessionManagementBean removeSessionManagementBean(long sessionId) {
            ThreadServiceStats stats = serviceStats.get();
            return stats.removeSessionManagementBean(sessionId);
        }

        // The following must run ON the IO thread (the one associated with the session)
        @Override
        public void addUserPrincipals(IoSessionEx session, Map<String, String> userPrincipals) {
            ThreadServiceStats stats = serviceStats.get();
            stats.addLoggedInSession(session, userPrincipals);
        }

        // The following must run ON an IO worker thread (the one associated with the session)
        @Override
        public void removeUserPrincipals(IoSessionEx session) {
            ThreadServiceStats stats = serviceStats.get();
            stats.removeLoggedInSession(session);
        }

        /**
         * Ongoing service statistics. There is an instance of this class per worker thread, stored as a ThreadLocal on the
         * thread, with a reference to it in a CopyOnWriteMap stored here in DefaultServiceManagementBean so we can do insertions
         * of stats objects into the map without locks.
         */
        private class ThreadServiceStats {

            // A map of threads that are contributing to the various totals. Rather than
            // store something on the IoSessionEx as an attribute, we'll do so here. We
            // know this is safe because the calls to manipulate the map must be done on
            // an IO thread, so there is no contention to deal with.
            private SessionMap sessionBeans = new SessionMap();

            // Thread-specific overall counts.
            //
            // NOTE: because these values are thread-specific, they'll only ever be UPDATED
            // by one single thread. The only guys that will READ them are either the
            // management stuff collating a single value or the summary notifications.
            // For now, because things are a single writer, we're going to assume we do
            // not need Atomic values to store them or Futures to read them, as the read
            // will only be off by at most one update. We might have to go to Atomics later
            // if that turns out to be a problem.
            private long currentSessionCount;
            private long currentNativeSessionCount;
            private long currentEmulatedSessionCount;
            private long cumulativeSessionCount;
            private long cumulativeNativeSessionCount;
            private long cumulativeEmulatedSessionCount;
            private long exceptionCount;
            private String latestException;
            private long latestExceptionTime;

            // the following are 'assembled' values that we're calculating as we go along.
            private long totalBytesSentCount;
            private long totalBytesReceivedCount;

            // A map of session ID to an associated set of user principals. Note that we
            // need to convert the ID to something else (e.g. session MBean name) when
            // returned to the user from a call through the service protocol-specific object.
            private final Map<Long, Map<String, String>> loggedInSessions = new HashMap<>();

            // Optimization to keep track on a per-session basis (within the thread), of the total
            // bytes sent and received so we can report a total sent and received for the service.
            // We do this on a per-session basis so we can quickly compute a delta between
            // the previous count for a session and the current count, allowing us to update
            // a total for the thread easily and quickly. Since a given thread is only ever
            // working on a single session at a time, and sessions are pinned to the thread,
            // we can operate without locks.
            private final ByteCountMap bytesSentCountBySession = new ByteCountMap();
            private final ByteCountMap bytesReceivedCountBySession = new ByteCountMap();

            /**
             * Given a session, extract relevant counts and update them locally. NOTE: because we're a singlw thread, it doesn't
             * really help to return the new values, as other threads will be getting stuff, too.
             *
             * @param session
             * @return
             */
            // This must only be called ON an IO thread (the one equal to this structure)
            void doSessionCreated(long sessionId, ManagementSessionType managementSessionType) {
                currentSessionCount++;
                cumulativeSessionCount++;

                if (managementSessionType.equals(ManagementSessionType.NATIVE)) {
                    currentNativeSessionCount++;
                    cumulativeNativeSessionCount++;
                } else if (managementSessionType.equals(ManagementSessionType.EMULATED)) {
                    currentEmulatedSessionCount++;
                    cumulativeEmulatedSessionCount++;
                }
            }

            void doSessionClosed(long sessionId, ManagementSessionType managementSessionType) {
                currentSessionCount--;

                if (managementSessionType.equals(ManagementSessionType.NATIVE)) {
                    currentNativeSessionCount--;
                } else if (managementSessionType.equals(ManagementSessionType.EMULATED)) {
                    currentEmulatedSessionCount--;
                }

                bytesSentCountBySession.remove(sessionId);
                bytesReceivedCountBySession.remove(sessionId);
                loggedInSessions.remove(sessionId);
            }

            void storeSessionManagementBean(SessionManagementBean sessionBean) {
                sessionBeans.put(sessionBean.getId(), sessionBean);
            }

            SessionManagementBean getSessionManagementBean(long sessionId) {
                SessionMapEntry entry = sessionBeans.get(sessionId);
                if (entry != null) {
                    return entry.bean;
                }

                return null;
            }

            SessionManagementBean removeSessionManagementBean(long sessionId) {
                SessionMapEntry entry = sessionBeans.remove(sessionId);
                if (entry != null) {
                    return entry.bean;
                }

                return null;
            }

            // For use by the 'summation' methods All of these try to create a future to run on
            // the IO worker thread associated with the map.

            // This runs OFF any IO worker thread.
            // See comment above about not needing a Future or Atomic.
            long getCurrentSessionCount() {
                return currentSessionCount;
            }

            // This runs OFF any IO worker thread
            // See comment above about not needing a Future or Atomic.
            long getCurrentNativeSessionCount() {
                return currentNativeSessionCount;
            }

            // This runs OFF any IO worker thread
            // See comment above about not needing a Future or Atomic.
            long getCurrentEmulatedSessionCount() {
                return currentEmulatedSessionCount;
            }

            // This runs OFF any IO worker thread
            // See comment above about not needing a Future or Atomic.
            long getCumulativeSessionCount() {
                return cumulativeSessionCount;
            }

            // This runs OFF any IO worker thread
            // See comment above about not needing a Future or Atomic.
            long getCumulativeNativeSessionCount() {
                return cumulativeNativeSessionCount;
            }

            // This runs OFF any IO worker thread
            // See comment above about not needing a Future or Atomic.
            long getCumulativeEmulatedSessionCount() {
                return cumulativeEmulatedSessionCount;
            }

            // This runs OFF any IO worker thread.
            // XXX Should we be doing this as a Runnsble on the associated thread?
            // This is only called explicitly from JMX or SNMP--not internally by
            // the gateway for any particular reason.
            void clearCumulativeSessionCount() {
                cumulativeSessionCount = 0;
                cumulativeNativeSessionCount = 0;
                cumulativeEmulatedSessionCount = 0;
            }

            // This runs OFF any IO worker thread
            // See comment above about not needing a Future or Atomic.
            long getExceptionCount() {
                return exceptionCount;
            }

            // This must run ON the stats current worker thread.
            void setLatestException(final String exception, final long exceptionTime) {
                exceptionCount++;
                latestException = exception;
                latestExceptionTime = exceptionTime;
            }

            // This runs OFF any IO worker thread
            // See comment above about not needing a Future or Atomic.
            //
            // This method is different than the 'counts' methods above, in that it assumes it is being called
            // as part of calling several consecutive stats objects and adds/updates the incoming totals with
            // its local data, rather than just returning the data. This makes it simpler for the caller.
            void collectLatestException(Object[] exceptionData) {
                if (latestExceptionTime > ((Long) exceptionData[0])) {
                    // XXX It is possible that things change between the first and second statements below1
                    exceptionData[0] = latestExceptionTime;
                    exceptionData[1] = latestException;
                }
            }

            // This runs OFF any IO worker thread
            // See comment above about not needing a Future or Atomic.
            long getTotalBytesSentCount() {
                return totalBytesSentCount;
            }

            // This runs OFF any IO worker thread
            // See comment above about not needing a Future or Atomic.
            long getTotalBytesReceivedCount() {
                return totalBytesReceivedCount;
            }

            // This runs OFF any IO worker thread
            // See comment above about not needing a Future or Atomic.
            void collectLoggedInSessions(Map<Long, Map<String, String>> vals) {
                vals.putAll(loggedInSessions);
            }

            // This runs OFF any IO worker thread. The final list of summary values from a service
            // actually includes data we do not keep in the thread-specific area. See DefaultServiceManagementBean for
            // those.
            //
            // This method is different than the 'counts' methods above, in that it assumes it is being called
            // as part of calling several consecutive stats objects and adds/updates the incoming totals with
            // its local data, rather than just returning the data. This makes it simpler for the caller.
            void collectSummaryValues(Object[] vals) {
                // 'vals' must be of size ServiceManagementBean.SUMMARY_DATA_FIELD_LIST.length;
                // XXX it is possible that things change between accesses below. Without a global
                // lock I'm not sure we can guarantee anything, but we'll be as close as possible.
                vals[ServiceManagementBean.SUMMARY_DATA_TOTAL_BYTES_RECEIVED_INDEX] =
                        ((Long) vals[ServiceManagementBean.SUMMARY_DATA_TOTAL_BYTES_RECEIVED_INDEX])
                                + totalBytesReceivedCount;
                vals[ServiceManagementBean.SUMMARY_DATA_TOTAL_BYTES_SENT_INDEX] =
                        ((Long) vals[ServiceManagementBean.SUMMARY_DATA_TOTAL_BYTES_SENT_INDEX])
                                + totalBytesSentCount;
                vals[ServiceManagementBean.SUMMARY_DATA_TOTAL_CURRENT_SESSIONS_INDEX] =
                        ((Long) vals[ServiceManagementBean.SUMMARY_DATA_TOTAL_CURRENT_SESSIONS_INDEX])
                                + currentSessionCount;
                vals[ServiceManagementBean.SUMMARY_DATA_TOTAL_CURRENT_NATIVE_SESSIONS_INDEX] =
                        ((Long) vals[ServiceManagementBean.SUMMARY_DATA_TOTAL_CURRENT_NATIVE_SESSIONS_INDEX])
                                + currentNativeSessionCount;
                vals[ServiceManagementBean.SUMMARY_DATA_TOTAL_CURRENT_EMULATED_SESSIONS_INDEX] =
                        ((Long) vals[ServiceManagementBean.SUMMARY_DATA_TOTAL_CURRENT_EMULATED_SESSIONS_INDEX])
                                + currentEmulatedSessionCount;
                vals[ServiceManagementBean.SUMMARY_DATA_TOTAL_CUMULATIVE_SESSIONS_INDEX] =
                        ((Long) vals[ServiceManagementBean.SUMMARY_DATA_TOTAL_CUMULATIVE_SESSIONS_INDEX])
                                + cumulativeSessionCount;
                vals[ServiceManagementBean.SUMMARY_DATA_TOTAL_CUMULATIVE_NATIVE_SESSIONS_INDEX] =
                        ((Long) vals[ServiceManagementBean.SUMMARY_DATA_TOTAL_CUMULATIVE_NATIVE_SESSIONS_INDEX])
                                + cumulativeNativeSessionCount;
                vals[ServiceManagementBean.SUMMARY_DATA_TOTAL_CUMULATIVE_EMULATED_SESSIONS_INDEX] =
                        ((Long) vals[ServiceManagementBean.SUMMARY_DATA_TOTAL_CUMULATIVE_EMULATED_SESSIONS_INDEX])
                                + cumulativeEmulatedSessionCount;
                vals[ServiceManagementBean.SUMMARY_DATA_TOTAL_EXCEPTION_COUNT_INDEX] =
                        ((Long) vals[ServiceManagementBean.SUMMARY_DATA_TOTAL_EXCEPTION_COUNT_INDEX])
                                + exceptionCount;

                Long exceptionTime = (Long) vals[ServiceManagementBean.SUMMARY_DATA_LATEST_EXCEPTION_TIME_INDEX];
                if (exceptionTime == null || latestExceptionTime > exceptionTime) {
                    vals[ServiceManagementBean.SUMMARY_DATA_LATEST_EXCEPTION_INDEX] = latestException;
                    vals[ServiceManagementBean.SUMMARY_DATA_LATEST_EXCEPTION_TIME_INDEX] = latestExceptionTime;
                }
            }

            // The following is to run ON the IO thread
            void addToBytesSent(long sessionId, long sessionWrittenBytes) {
                ByteCountMapEntry prevByteCount = bytesSentCountBySession.get(sessionId);
                long delta = prevByteCount == null ? sessionWrittenBytes : sessionWrittenBytes
                        - prevByteCount.byteCount;
                bytesSentCountBySession.put(sessionId, sessionWrittenBytes);
                totalBytesSentCount += delta;
            }

            // The following is to run ON the IO thread
            void addToBytesReceived(long sessionId, long sessionReadBytes) {
                ByteCountMapEntry prevByteCount = bytesReceivedCountBySession.get(sessionId);
                long delta = prevByteCount == null ? sessionReadBytes : sessionReadBytes - prevByteCount.byteCount;
                bytesReceivedCountBySession.put(sessionId, sessionReadBytes);
                totalBytesReceivedCount += delta;
            }

            // The following is to run ON the IO thread
            void addLoggedInSession(IoSessionEx session, Map<String, String> userPrincipals) {
                loggedInSessions.put(session.getId(), userPrincipals);
            }

            // The following is to run ON the IO thread
            void removeLoggedInSession(IoSessionEx session) {
                loggedInSessions.remove(session.getId());
            }
        }

        // an implementation of a map that avoids auto-boxing longs to Longs and thus avoids creation of little
        // bits of garbage that then need to be garbage collected;
        private final class SessionMap {
            private int arraySize;
            private int mapSize;
            private SessionMapEntry[] data;

            private SessionMap() {
                arraySize = 64;
                mapSize = 0;
                data = new SessionMapEntry[arraySize];
            }

            private void put(long id, SessionManagementBean bean) {
                int index = (int) (id % arraySize);
                SessionMapEntry entry = data[index];
                if (entry != null) {
                    SessionMapEntry prevEntry = null;
                    while (entry != null) {
                        if (entry.id == id) {
                            entry.setBean(bean);
                            return;
                        }
                        prevEntry = entry;
                        entry = entry.next;
                    }

                    // The end of the chain was reached, add the new entry to the end of the chain
                    prevEntry.next = new SessionMapEntry(id, bean);
                } else {
                    data[index] = new SessionMapEntry(id, bean);
                }

                mapSize++;

                // This amounts to a load factor of 1, so when the number of entries reaches the
                // size of the array, the map is resized.
                if (mapSize > arraySize) {
                    resize();
                }
            }

            private void resize() {
                SessionMapEntry[] oldData = data;
                arraySize *= 2;
                data = new SessionMapEntry[arraySize];

                for (SessionMapEntry anOldData : oldData) {
                    SessionMapEntry entry = anOldData;
                    while (entry != null) {
                        int index = (int) (entry.id % arraySize);
                        SessionMapEntry newEntry = data[index];
                        if (newEntry != null) {
                            while (newEntry.next != null) {
                                newEntry = newEntry.next;
                            }
                            newEntry.next = entry;
                        } else {
                            data[index] = entry;
                        }

                        SessionMapEntry prevEntry = entry;
                        entry = entry.next;

                        prevEntry.next = null;
                    }
                }
            }

            private SessionMapEntry get(long id) {
                int index = (int) (id % arraySize);
                SessionMapEntry entry = data[index];
                while (entry != null) {
                    if (entry.id == id) {
                        return entry;
                    }
                    entry = entry.next;
                }

                return null;
            }

            private SessionMapEntry remove(long id) {
                int index = (int) (id % arraySize);
                SessionMapEntry entry = data[index];
                if (entry != null) {
                    if (entry.id == id) {
                        data[index] = entry.next;
                        entry.next = null;
                    } else {
                        SessionMapEntry prevEntry = entry;
                        while ((entry != null) && (entry.id != id)) {
                            prevEntry = entry;
                            entry = entry.next;
                        }

                        if (entry != null) {
                            prevEntry.next = entry.next;
                            entry.next = null;
                        }
                    }

                    if (entry != null) {
                        mapSize--;
                    }
                    return entry;
                }

                return null;
            }
        }

        private final class SessionMapEntry {
            private long id;
            private SessionManagementBean bean;
            private SessionMapEntry next;

            private SessionMapEntry(long id, SessionManagementBean bean) {
                this.id = id;
                this.bean = bean;
                this.next = null;
            }

            // allow the value of an entry to be changed to support the put(K, V) semantics where K already exists in
            // the
            // map
            private void setBean(SessionManagementBean bean) {
                this.bean = bean;
            }
        }

        private final class ByteCountMap {
            private int arraySize;
            private int mapSize;
            private ByteCountMapEntry[] data;

            private ByteCountMap() {
                arraySize = 64;
                mapSize = 0;
                data = new ByteCountMapEntry[arraySize];
            }

            private void put(long id, long byteCount) {
                int index = (int) (id % arraySize);
                ByteCountMapEntry entry = data[index];
                if (entry != null) {
                    ByteCountMapEntry prevEntry = null;
                    while (entry != null) {
                        if (entry.id == id) {
                            entry.setByteCount(byteCount);
                            return;
                        }
                        prevEntry = entry;
                        entry = entry.next;
                    }

                    // The end of the chain was reached, add the new entry to the end of the chain
                    prevEntry.next = new ByteCountMapEntry(id, byteCount);
                } else {
                    data[index] = new ByteCountMapEntry(id, byteCount);
                }

                mapSize++;

                // This amounts to a load factor of 1, so when the number of entries reaches the
                // size of the array, the map is resized.
                if (mapSize > arraySize) {
                    resize();
                }
            }

            private void resize() {
                ByteCountMapEntry[] oldData = data;
                arraySize *= 2;
                data = new ByteCountMapEntry[arraySize];

                for (ByteCountMapEntry anOldData : oldData) {
                    ByteCountMapEntry entry = anOldData;
                    while (entry != null) {
                        int index = (int) (entry.id % arraySize);
                        ByteCountMapEntry newEntry = data[index];
                        if (newEntry != null) {
                            while (newEntry.next != null) {
                                newEntry = newEntry.next;
                            }
                            newEntry.next = entry;
                        } else {
                            data[index] = entry;
                        }

                        ByteCountMapEntry prevEntry = entry;
                        entry = entry.next;

                        prevEntry.next = null;
                    }
                }
            }

            private ByteCountMapEntry get(long id) {
                int index = (int) (id % arraySize);
                ByteCountMapEntry entry = data[index];
                while (entry != null) {
                    if (entry.id == id) {
                        return entry;
                    }
                    entry = entry.next;
                }

                return null;
            }

            private ByteCountMapEntry remove(long id) {
                int index = (int) (id % arraySize);
                ByteCountMapEntry entry = data[index];
                if (entry != null) {
                    if (entry.id == id) {
                        data[index] = entry.next;
                        entry.next = null;
                    } else {
                        ByteCountMapEntry prevEntry = entry;
                        while ((entry != null) && (entry.id != id)) {
                            prevEntry = entry;
                            entry = entry.next;
                        }

                        if (entry != null) {
                            prevEntry.next = entry.next;
                            entry.next = null;
                        }
                    }

                    if (entry != null) {
                        mapSize--;
                    }
                    return entry;
                }

                return null;
            }
        }

        private final class ByteCountMapEntry {
            private long id;
            private long byteCount;
            private ByteCountMapEntry next;

            private ByteCountMapEntry(long id, long byteCount) {
                this.id = id;
                this.byteCount = byteCount;
                this.next = null;
            }

            // allow the value of an entry to be changed to support the put(K, V) semantics where K already exists in
            // the
            // map
            private void setByteCount(long byteCount) {
                this.byteCount = byteCount;
            }
        }

        @Override
        public String getServiceName() {
            return serviceContext.getServiceName();
        }
    }

}

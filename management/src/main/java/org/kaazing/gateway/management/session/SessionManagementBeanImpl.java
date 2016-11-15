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
package org.kaazing.gateway.management.session;

import java.security.Principal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.security.auth.Subject;
import org.apache.mina.core.future.CloseFuture;
import org.apache.mina.core.write.WriteRequest;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.kaazing.gateway.management.AbstractManagementBean;
import org.kaazing.gateway.management.Utils;
import org.kaazing.gateway.management.service.ServiceManagementBean;
import org.kaazing.gateway.transport.BridgeSession;
import org.kaazing.mina.core.session.IoSessionEx;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.kaazing.gateway.transport.ws.AbstractWsBridgeSession.LAST_ROUND_TRIP_LATENCY;
import static org.kaazing.gateway.transport.ws.AbstractWsBridgeSession.LAST_ROUND_TRIP_LATENCY_TIMESTAMP;

/**
 * Implementation of the management 'data' bean for a session. This just contains the data. Wrappers for different management
 * protocols define the use of those data.
 */
public class SessionManagementBeanImpl extends AbstractManagementBean implements SessionManagementBean {

    private static final long CLOSE_TIMEOUT_MS = 100;

    private static final Logger logger = LoggerFactory.getLogger(SessionManagementBeanImpl.class);

    private final ServiceManagementBean serviceManagementBean;
    private final IoSessionEx session;
    private final String sessionTypeName;  // descriptive string
    private final String sessionDirection;

    private Map<String, String> userPrincipals;  // value and class name for each user principal

    private boolean notificationsEnabled;

    public SessionManagementBeanImpl(ServiceManagementBean serviceManagementBean,
                                     IoSessionEx session) {
        super(serviceManagementBean.getGatewayManagementBean().getManagementContext(),
                serviceManagementBean.getGatewayManagementBean().getManagementContext()
                        .getSessionSummaryDataNotificationInterval(),
                SUMMARY_DATA_FIELD_LIST);

        this.serviceManagementBean = serviceManagementBean;
        this.session = session;
        this.sessionTypeName = Utils.getSessionTypeName(BridgeSession.LOCAL_ADDRESS.get(session));
        this.sessionDirection = Utils.getSessionDirection(session);
    }

    @Override
    public ServiceManagementBean getServiceManagementBean() {
        return serviceManagementBean;
    }

    private List<SessionManagementListener> getManagementListeners() {
        return managementContext.getSessionManagementListeners();
    }

    @Override
    public IoSessionEx getSession() {
        return session;
    }

    @Override
    public Map<String, String> getUserPrincipalMap() {
        return userPrincipals;
    }

    // The following is intended to run ON the IO thread
    @Override
    public void setUserPrincipals(Map<String, String> userPrincipals) {
        this.userPrincipals = userPrincipals;
        this.serviceManagementBean.addUserPrincipals(session, userPrincipals);
    }

    @Override
    public void close() {
        CloseFuture future = session.close(false);
        try {
            future.await(CLOSE_TIMEOUT_MS);
        } catch (InterruptedException ex) {
            // it's fine to return--there isn't really any "error" here.
        }
    }

    @Override
    public void closeImmediately() {
        CloseFuture future = session.close(true);
        try {
            future.await(CLOSE_TIMEOUT_MS);
        } catch (InterruptedException ex) {
            // it's fine to return--there isn't really any "error" here.
        }
    }

    @Override
    public long getId() {
        return session.getId();
    }

    @Override
    public long getReadBytes() {
        return session.getReadBytes();
    }

    @Override
    public double getReadBytesThroughput() {
        session.updateThroughput(System.currentTimeMillis(), false);
        return session.getReadBytesThroughput();
    }

    @Override
    public long getWrittenBytes() {
        return session.getWrittenBytes();
    }

    @Override
    public double getWrittenBytesThroughput() {
        session.updateThroughput(System.currentTimeMillis(), false);
        return session.getWrittenBytesThroughput();
    }

    // In contrast to getPrincipals, this is for sending a JSON value back.
    @Override
    public String getUserPrincipals() {
        if (userPrincipals == null) {
            return null;
        }

        JSONObject jsonObj = new JSONObject(userPrincipals);

        return jsonObj.toString();
    }

    @Override
    public String getSummaryData() {
        JSONArray jsonArray = null;

        try {
            Object[] vals = new Object[SUMMARY_DATA_FIELD_LIST.length];

            vals[SUMMARY_DATA_READ_BYTES_INDEX] = getReadBytes();
            vals[SUMMARY_DATA_READ_BYTES_THPT_INDEX] = getReadBytesThroughput();
            vals[SUMMARY_DATA_WRITTEN_BYTES_INDEX] = getWrittenBytes();
            vals[SUMMARY_DATA_WRITTEN_BYTES_THPT_INDEX] = getWrittenBytesThroughput();

            jsonArray = new JSONArray(vals);
        } catch (JSONException ex) {
            // We should never be able to get here, as the summary data values are all legal
        }

        String val = jsonArray.toString();
        return val;
    }

    @Override
    public void enableNotifications(boolean notificationsEnabled) {
        this.notificationsEnabled = notificationsEnabled;
    }

    @Override
    public boolean areNotificationsEnabled() {
        return notificationsEnabled;
    }

    @Override
    public void incrementExceptionCount() {

    }

    @Override
    public long getCreateTime() {
        return session.getCreationTime();
    }

    @Override
    public String getRemoteAddress() {
        String remoteAddress = serviceManagementBean.getSessionRemoteAddress(this.session);
        return remoteAddress;
    }

    @Override
    public String getSessionTypeName() {
        return sessionTypeName;
    }

    @Override
    public String getSessionDirection() {
        return sessionDirection;
    }

    //---------------------------------------------------------------
    // Implement various methods used by the management strategies
    // THESE ARE ALL CALLED ON AN IO THREAD. WE MUST NOT BLOCK!
    //---------------------------------------------------------------

    @Override
    public void doSessionCreated() throws Exception {
        // establish the user principals.
        // XXX There's a question about what to do if they are changed on revalidate
        Set<Class<Principal>> userPrincipalClasses = serviceManagementBean.getUserPrincipalClasses();

        if (userPrincipalClasses != null && !userPrincipalClasses.isEmpty()) {
            Map<String, String> userPrincipals = new HashMap<>();
            Subject subject = session.getSubject();
            if (subject != null) {
                Set<Principal> principals = subject.getPrincipals();
                for (Principal principal : principals) {
                    String principalName = principal.getName();
                    for (Class<Principal> userPrincipal : userPrincipalClasses) {
                        if (userPrincipal.isInstance(principal)) {
                            userPrincipals.put(principalName, principal.getClass().getName());
                        }
                    }
                }

                // The following MUST run ON the IO thread.
                setUserPrincipals(userPrincipals);
            }
        }
    }

    /**
     * Notify the management listeners on a sessionCreated.
     * <p/>
     * NOTE: this starts on the IO thread, but runs a task OFF the thread.
     */
    @Override
    public void doSessionCreatedListeners() {
        runManagementTask(new Runnable() {
            @Override
            public void run() {
                try {
                    // The particular management listeners change on strategy, so get them here.
                    for (final SessionManagementListener listener : getManagementListeners()) {
                        listener.doSessionCreated(SessionManagementBeanImpl.this);
                    }

                    // XXX Should we include a 'markChanged()' here?
                } catch (Exception ex) {
                    logger.warn("Error during doSessionCreated session listener notifications:", ex);
                }
            }
        });
    }

    @Override
    public void doSessionClosed() throws Exception {
        serviceManagementBean.removeUserPrincipals(session);
    }

    /**
     * Notify the management listeners on a filterWrite.
     * <p/>
     * NOTE: this starts on the IO thread, but runs a task OFF the thread.
     */
    @Override
    public void doSessionClosedListeners() {
        runManagementTask(new Runnable() {
            @Override
            public void run() {
                try {
                    // The particular management listeners change on strategy, so get them here.
                    for (final SessionManagementListener listener : getManagementListeners()) {
                        listener.doSessionClosed(SessionManagementBeanImpl.this);
                    }

                    // XXX should there be a markChanged() here because the session status is now closed?
                    // Or is that covered by the fact we generate a session-closed message?
                } catch (Exception ex) {
                    logger.warn("Error during doSessionClosed session listener notifications:", ex);
                }
            }
        });
    }

    @Override
    public void doMessageReceived(final Object message) throws Exception {
    }

    /**
     * Notify the management listeners on a messageReceived.
     * <p/>
     * NOTE: this starts on the IO thread, but runs a task OFF the thread.
     */
    @Override
    public void doMessageReceivedListeners(final Object message) {
        runManagementTask(new Runnable() {
            @Override
            public void run() {
                try {
                    List<SessionManagementListener> sessionListeners = getManagementListeners();
                    for (final SessionManagementListener listener : sessionListeners) {
                        listener.doMessageReceived(SessionManagementBeanImpl.this, message);
                    }

                    markChanged();
                } catch (Exception ex) {
                    logger.warn("Error during doMessageReceived session listener notifications:", ex);
                }
            }
        });
    }

    @Override
    public void doFilterWrite(final WriteRequest writeRequest) throws Exception {
    }

    /**
     * Notify the management listeners on a messageReceived.
     * <p/>
     * NOTE: this starts on the IO thread, but runs a task OFF the thread.
     */
    @Override
    public void doFilterWriteListeners(final WriteRequest writeRequest) {

        final Object message = writeRequest.getMessage();
        WriteRequest originalRequest = writeRequest.getOriginalRequest();
        final Object originalMessage = originalRequest != null ? originalRequest.getMessage() : null;

        runManagementTask(new Runnable() {
            @Override
            public void run() {
                try {
                    List<SessionManagementListener> sessionListeners = getManagementListeners();
                    for (final SessionManagementListener listener : sessionListeners) {
                        listener.doFilterWrite(SessionManagementBeanImpl.this, message, originalMessage);
                    }

                    markChanged();
                } catch (Exception ex) {
                    logger.warn("Error during doFilterWrite session listener notifications:", ex);
                }
            }
        });
    }

    @Override
    public void doExceptionCaught(final Throwable cause) throws Exception {

    }

    /**
     * Notify the management listeners on a messageReceived.
     * <p/>
     * NOTE: this starts on the IO thread, but runs a task OFF the thread.
     */
    @Override
    public void doExceptionCaughtListeners(final Throwable cause) {
        runManagementTask(new Runnable() {
            @Override
            public void run() {
                try {
                    List<SessionManagementListener> sessionListeners = getManagementListeners();
                    for (final SessionManagementListener listener : sessionListeners) {
                        listener.doExceptionCaught(SessionManagementBeanImpl.this, cause);
                    }

                    markChanged();
                } catch (Exception ex) {
                    logger.warn("Error during doExceptionCaught session listener notifications:", ex);
                }
            }
        });
    }

    @Override
    public long getLastRoundTripLatency() {
        IoSessionEx session = getSession();
        while (session != null) {
            Long latency = LAST_ROUND_TRIP_LATENCY.get(session);
            if (latency != null) {
                return latency;
            }
            if (session instanceof BridgeSession) {
                session = ((BridgeSession) session).getParent();
            } else {
                break;
            }
        }
        return -1;
    }

    @Override
    public long getLastRoundTripLatencyTimestamp() {
        IoSessionEx session = getSession();
        while (session != null) {
            Long latency = LAST_ROUND_TRIP_LATENCY_TIMESTAMP.get(session);
            if (latency != null) {
                return latency;
            }
            if (session instanceof BridgeSession) {
                session = ((BridgeSession) session).getParent();
            } else {
                break;
            }
        }
        return -1;
    }

}

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

package org.kaazing.gateway.management.snmp;

import java.io.IOException;
import java.net.URI;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Properties;
import java.util.concurrent.Callable;
import java.util.concurrent.ScheduledExecutorService;
import org.json.JSONArray;
import org.json.JSONObject;
import org.kaazing.gateway.management.ClusterManagementListener;
import org.kaazing.gateway.management.ManagementServiceHandler;
import org.kaazing.gateway.management.SummaryDataListener;
import org.kaazing.gateway.management.Utils;
import org.kaazing.gateway.management.config.ClusterConfigurationBean;
import org.kaazing.gateway.management.config.NetworkConfigurationBean;
import org.kaazing.gateway.management.config.RealmConfigurationBean;
import org.kaazing.gateway.management.config.SecurityConfigurationBean;
import org.kaazing.gateway.management.config.ServiceConfigurationBean;
import org.kaazing.gateway.management.config.ServiceDefaultsConfigurationBean;
import org.kaazing.gateway.management.context.ManagementContext;
import org.kaazing.gateway.management.gateway.GatewayManagementBean;
import org.kaazing.gateway.management.gateway.GatewayManagementListener;
import org.kaazing.gateway.management.service.ServiceManagementBean;
import org.kaazing.gateway.management.service.ServiceManagementListener;
import org.kaazing.gateway.management.session.SessionManagementBean;
import org.kaazing.gateway.management.session.SessionManagementListener;
import org.kaazing.gateway.management.snmp.mib.CpuManagementMIB;
import org.kaazing.gateway.management.snmp.mib.GatewayConfigurationMIB;
import org.kaazing.gateway.management.snmp.mib.GatewayManagementMIB;
import org.kaazing.gateway.management.snmp.mib.JVMManagementMIB;
import org.kaazing.gateway.management.snmp.mib.MIBConstants;
import org.kaazing.gateway.management.snmp.mib.NicManagementMIB;
import org.kaazing.gateway.management.snmp.mib.ServiceManagementMIB;
import org.kaazing.gateway.management.snmp.mib.SessionManagementMIB;
import org.kaazing.gateway.management.snmp.mib.SystemManagementMIB;
import org.kaazing.gateway.management.snmp.transport.ManagementAddress;
import org.kaazing.gateway.management.snmp.transport.ManagementTDomainAddressFactory;
import org.kaazing.gateway.management.snmp.transport.ManagementTcpTransport;
import org.kaazing.gateway.management.snmp.transport.ManagementTransport;
import org.kaazing.gateway.management.snmp.transport.ManagementUdpTransport;
import org.kaazing.gateway.management.system.CpuListManagementBean;
import org.kaazing.gateway.management.system.CpuManagementBean;
import org.kaazing.gateway.management.system.HostManagementBean;
import org.kaazing.gateway.management.system.JvmManagementBean;
import org.kaazing.gateway.management.system.NicListManagementBean;
import org.kaazing.gateway.management.system.NicManagementBean;
import org.kaazing.gateway.resource.address.ResourceAddress;
import org.kaazing.gateway.server.context.resolve.DefaultServiceContext;
import org.kaazing.gateway.service.ServiceContext;
import org.kaazing.gateway.transport.BridgeSession;
import org.kaazing.gateway.transport.IoHandlerAdapter;
import org.kaazing.gateway.transport.io.IoMessage;
import org.kaazing.gateway.transport.ws.AbstractWsBridgeSession;
import org.kaazing.gateway.transport.ws.extension.WsExtension;
import org.kaazing.gateway.transport.ws.extension.WsExtensionParameter;
import org.kaazing.gateway.transport.wseb.WsebSession;
import org.kaazing.gateway.transport.wsn.WsnSession;
import org.kaazing.gateway.transport.wsr.WsrSession;
import org.kaazing.gateway.util.ws.WebSocketWireProtocol;
import org.kaazing.mina.core.buffer.IoBufferEx;
import org.kaazing.mina.core.session.IoSessionEx;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.snmp4j.CommandResponderEvent;
import org.snmp4j.MessageDispatcher;
import org.snmp4j.MessageDispatcherImpl;
import org.snmp4j.PDU;
import org.snmp4j.Session;
import org.snmp4j.Snmp;
import org.snmp4j.Target;
import org.snmp4j.TransportMapping;
import org.snmp4j.agent.CommandProcessor;
import org.snmp4j.agent.DefaultMOContextScope;
import org.snmp4j.agent.DefaultMOQuery;
import org.snmp4j.agent.DefaultMOServer;
import org.snmp4j.agent.DuplicateRegistrationException;
import org.snmp4j.agent.MOGroup;
import org.snmp4j.agent.MOQuery;
import org.snmp4j.agent.MOScope;
import org.snmp4j.agent.MOServer;
import org.snmp4j.agent.ManagedObject;
import org.snmp4j.agent.NotificationOriginator;
import org.snmp4j.agent.ProxyForwarder;
import org.snmp4j.agent.RequestHandler;
import org.snmp4j.agent.io.DefaultMOPersistenceProvider;
import org.snmp4j.agent.io.ImportModes;
import org.snmp4j.agent.io.MOInput;
import org.snmp4j.agent.io.MOInputFactory;
import org.snmp4j.agent.io.MOPersistenceProvider;
import org.snmp4j.agent.io.MOServerPersistence;
import org.snmp4j.agent.io.prop.PropertyMOInput;
import org.snmp4j.agent.mo.DefaultMOFactory;
import org.snmp4j.agent.mo.MOFactory;
import org.snmp4j.agent.mo.snmp.NotificationLogMib;
import org.snmp4j.agent.mo.snmp.NotificationLogMib.NlmConfigLogEntryRow;
import org.snmp4j.agent.mo.snmp.NotificationOriginatorImpl;
import org.snmp4j.agent.mo.snmp.ProxyForwarderImpl;
import org.snmp4j.agent.mo.snmp.RowStatus;
import org.snmp4j.agent.mo.snmp.SNMPv2MIB;
import org.snmp4j.agent.mo.snmp.SnmpCommunityMIB;
import org.snmp4j.agent.mo.snmp.SnmpFrameworkMIB;
import org.snmp4j.agent.mo.snmp.SnmpNotificationMIB;
import org.snmp4j.agent.mo.snmp.SnmpProxyMIB;
import org.snmp4j.agent.mo.snmp.SnmpTargetMIB;
import org.snmp4j.agent.mo.snmp.StorageType;
import org.snmp4j.agent.mo.snmp.SysUpTime;
import org.snmp4j.agent.mo.snmp.UsmMIB;
import org.snmp4j.agent.mo.snmp.VacmMIB;
import org.snmp4j.agent.mo.snmp4j.Snmp4jConfigMib;
import org.snmp4j.agent.mo.snmp4j.Snmp4jLogMib;
import org.snmp4j.agent.mo.util.MOTableSizeLimit;
import org.snmp4j.agent.mo.util.VariableProvider;
import org.snmp4j.agent.request.Request;
import org.snmp4j.agent.request.RequestStatus;
import org.snmp4j.agent.request.SnmpRequest;
import org.snmp4j.agent.request.SubRequest;
import org.snmp4j.agent.request.SubRequestIterator;
import org.snmp4j.agent.security.VACM;
import org.snmp4j.mp.MPv1;
import org.snmp4j.mp.MPv2c;
import org.snmp4j.mp.MPv3;
import org.snmp4j.mp.SnmpConstants;
import org.snmp4j.security.SecurityModels;
import org.snmp4j.security.SecurityProtocols;
import org.snmp4j.security.USM;
import org.snmp4j.smi.Address;
import org.snmp4j.smi.Counter64;
import org.snmp4j.smi.Integer32;
import org.snmp4j.smi.Null;
import org.snmp4j.smi.OID;
import org.snmp4j.smi.OctetString;
import org.snmp4j.smi.UnsignedInteger32;
import org.snmp4j.smi.Variable;
import org.snmp4j.smi.VariableBinding;
import org.snmp4j.util.PDUFactory;
import org.snmp4j.util.WorkerPool;

/**
 * The Kaazing service that implements SNMP management support.
 * <p/>
 * Kaazing's SNMP support is based on the SNMP4J open-source library under the Apache 2.0 license. To see the full text of the
 * license, please see the Kaazing third-party licenses file.
 */
class SnmpManagementServiceHandler extends IoHandlerAdapter<IoSessionEx> implements VariableProvider, ManagementServiceHandler {
    private static final Logger logger = LoggerFactory.getLogger(SnmpManagementServiceHandler.class);

    public static final String NOTIF_TARGET_PARAM_NAME = "KaazingData";
    private static final String SESSION_CLOSED_OTHER_END_MSG = "Session was closed at other end";
    // lots of simultaneous management requests.

    // For performance, I need to pass this to the agent
    protected final ServiceContext serviceContext;
    private final ManagementContext managementContext;

    private final ManagementTransport transportMapping;
    private final MOServer server;
    private final KaazingConfigManager agent;
    private final ScheduledExecutorService notifScheduler; // FIXME: for sending notifications... not copied from SNMPAgent yet
    private final ScheduledExecutorService summaryDataScheduler;


    public SnmpManagementServiceHandler(ServiceContext serviceContext, ManagementContext managementContext) {
        this.serviceContext = serviceContext;
        this.managementContext = managementContext;
        this.managementContext.addGatewayManagementListener(new SNMPGatewayManagementListener());
        this.managementContext.addServiceManagementListener(new SNMPServiceManagementListener());
        this.managementContext.addSessionManagementListener(new SNMPSessionManagementListener());

        transportMapping = new ManagementTransport(managementContext);

        server = new DefaultMOServer();
        MOServer[] moServers = new MOServer[]{server};

        final Properties props = KaazingSNMPAgentProperties.getProperties();
        MOInputFactory configurationFactory = new MOInputFactory() {
            public MOInput createMOInput() {
                return new PropertyMOInput(props, SnmpManagementServiceHandler.this);
            }
        };

        MessageDispatcher messageDispatcher = new MessageDispatcherImpl();
        //        List<String> addressList = Arrays.asList(args.get("address").split(";"));
        //        addListenAddresses(messageDispatcher, addressList);
        //        agent = new AgentConfigManager(new OctetString(MPv3.createLocalEngineID()),
        messageDispatcher.addTransportMapping(transportMapping);
        messageDispatcher.addTransportMapping(new ManagementTcpTransport(serviceContext, managementContext));
        messageDispatcher.addTransportMapping(new ManagementUdpTransport(serviceContext, managementContext));
        agent = new KaazingConfigManager(new OctetString(MPv3.createLocalEngineID()),
                messageDispatcher,
                null,
                moServers,
                                         /*ThreadPool.create("SNMPAgent", 3)*/null /*null worker pool so that commands execute
                                          in the NIOProcessor thread handling the request, response, notification*/,
                configurationFactory,
                new DefaultMOPersistenceProvider(moServers, ""));
        notifScheduler = serviceContext.getSchedulerProvider().getScheduler("SNMPNotif", true);
        summaryDataScheduler = serviceContext.getSchedulerProvider().getScheduler("SNMPSummaryData", false);

        agent.initialize();
        agent.configure(); // FIXME:  configuration should be through gateway-config.xml, not SampleAgent.properties
        agent.launch();
    }

    @Override
    public ServiceContext getServiceContext() {
        return serviceContext;
    }

    @Override
    protected void doSessionCreated(IoSessionEx session) throws Exception {
        // processed on IO thread.
        managementContext.incrementManagementSessionCount();

        super.doSessionCreated(session);
    }

    /**
     * Closing a session for the management service itself.
     */
    @Override
    protected void doSessionClosed(IoSessionEx session) throws Exception {
        // processed on IO thread
        managementContext.decrementManagementSessionCount();

        // Clean up the entry in the notification table for this session, if there is one.
        OctetString name = generateNotificationTargetAddressName(session.getId());
        agent.getSnmpTargetMIB().removeTargetAddress(name);

        super.doSessionClosed(session);
    }

    @Override
    protected void doMessageReceived(IoSessionEx session, Object message) throws Exception {
        // processed on IO thread
        if (message instanceof IoBufferEx) {
            ByteBuffer buf = ((IoBufferEx) message).buf();
            transportMapping.processMessage(new ManagementAddress(session), buf);
        }
        super.doMessageReceived(session, message);
    }

    // We don't need to implement doFilterWrite, because nothing sends from the back through
    // the management service (of course, the management service itself sends notifications).

    @Override
    protected void doExceptionCaught(IoSessionEx session, Throwable cause) throws Exception {
        // processed on IO thread

        // there is currently an issue in WsnAcceptor when we close the session from the
        // client side. It throws an exception 'Session was closed at other end' that will
        // get fixed at some point, but not in 4.0.2. To avoid polluting the log, screen that
        // exception out here.
        String message = cause.getMessage();
        if (message == null || !message.equals(SESSION_CLOSED_OTHER_END_MSG)) {
            if (logger.isDebugEnabled()) {
                logger.debug("Exception caught in SNMP session: ", cause);
            }
        }
        // Do NOT call super, as it is the one that prints a 'please implement' message.
    }

    protected void sendNotification(final OID oid, final VariableBinding[] variableBindings) {
//        sNotifCount++;
        /*Future<Object> notifFuture = */
        notifScheduler.submit(new Callable<Object>() {
            @Override
            public Object call() {
//                sNotifCallCount++;
                return agent.getNotificationOriginator().notify(new OctetString(), oid, variableBindings);
            }
        });
        // if we care about the response, call get
        //notifFuture.get();
    }

    public OctetString generateNotificationTargetAddressName(long sessionId) {
        OctetString targetAddressName = new OctetString(Long.toString(sessionId));
        return targetAddressName;
    }

    public Variable getVariable(String name) {
        OID oid;
        OctetString context = null;
        int pos = name.indexOf(':');
        if (pos >= 0) {
            context = new OctetString(name.substring(0, pos));
            oid = new OID(name.substring(pos + 1, name.length()));
        } else {
            oid = new OID(name);
        }
        final DefaultMOContextScope scope = new DefaultMOContextScope(context, oid, true, oid, true);
        MOQuery query = new DefaultMOQuery(scope, false, this);
        ManagedObject mo = server.lookup(query);
        if (mo != null) {
            final VariableBinding vb = new VariableBinding(oid);
            final RequestStatus status = new RequestStatus();
            SubRequest req = new SubRequest() {
                private boolean completed;
                private MOQuery query;

                public boolean hasError() {
                    return false;
                }

                public void setErrorStatus(int errorStatus) {
                    status.setErrorStatus(errorStatus);
                }

                public int getErrorStatus() {
                    return status.getErrorStatus();
                }

                public RequestStatus getStatus() {
                    return status;
                }

                public MOScope getScope() {
                    return scope;
                }

                public VariableBinding getVariableBinding() {
                    return vb;
                }

                public Request getRequest() {
                    return null;
                }

                public Object getUndoValue() {
                    return null;
                }

                public void setUndoValue(Object undoInformation) {
                }

                public void completed() {
                    completed = true;
                }

                public boolean isComplete() {
                    return completed;
                }

                public void setTargetMO(ManagedObject managedObject) {
                }

                public ManagedObject getTargetMO() {
                    return null;
                }

                public int getIndex() {
                    return 0;
                }

                public void setQuery(MOQuery query) {
                    this.query = query;
                }

                public MOQuery getQuery() {
                    return query;
                }

                public SubRequestIterator repetitions() {
                    return null;
                }

                public void updateNextRepetition() {
                }

                public Object getUserObject() {
                    return null;
                }

                public void setUserObject(Object userObject) {
                }

            };
            mo.get(req);
            return vb.getVariable();
        }
        return null;
    }

//    public OID addGatewayBean(GatewayManagementBean gatewayBean) {
//        return agent.addGatewayBean(gatewayBean);
//    }

    public void removeGatewayBean(OID oid) {
        agent.removeGatewayBean(oid);
    }

    public void removeServiceBean(OID oid) {
        agent.removeServiceBean(oid);
    }

//    public OID addSessionBean(SessionManagementBean sessionBean) {
//        return agent.addSessionBean(sessionBean);
//    }

    public void removeSessionBean(OID oid) {
        agent.removeSessionBean(oid);
    }

    /**
     * The Kaazing SNMP 'agent'.
     */
    class KaazingConfigManager {

        public static final int STATE_CREATED = 0;
        public static final int STATE_INITIALIZED = 10;
        public static final int STATE_CONFIGURED = 20;
        public static final int STATE_RESTORED = 30;
        public static final int STATE_SUSPENDED = 35;
        public static final int STATE_RUNNING = 40;
        public static final int STATE_UNSAVED_CHANGES = 45;
        public static final int STATE_SAVED = 50;
        public static final int STATE_SHUTDOWN = -1;

        private CommandProcessor agent;
        // private WorkerPool workerPool;

        private VACM vacm;
        private USM usm;
        private MOServer[] servers;
        private Session snmpSession;
        private MessageDispatcher dispatcher;
        private OctetString engineID;
        private ProxyForwarder proxyForwarder;
        private NotificationOriginator notificationOriginator;

        private MOInputFactory configuration;
        private MOPersistenceProvider persistenceProvider;
        private int persistenceImportMode = ImportModes.UPDATE_CREATE;

        // mandatory standard MIBs
        private SNMPv2MIB snmpv2MIB;
        private SnmpTargetMIB targetMIB;
        private SnmpCommunityMIB communityMIB;
        private SnmpNotificationMIB notificationMIB;
        private SnmpFrameworkMIB frameworkMIB;
        private UsmMIB usmMIB;
        private VacmMIB vacmMIB;
        private GatewayManagementMIB gatewayManagementMIB;
        private ServiceManagementMIB serviceManagementMIB;
        private SessionManagementMIB sessionManagementMIB;
        private GatewayConfigurationMIB gatewayConfigurationMIB;
        private JVMManagementMIB jvmManagementMIB;
        private SystemManagementMIB systemManagementMIB;
        private CpuManagementMIB cpuManagementMIB;
        private NicManagementMIB nicManagementMIB;

        // optional standard MIBs
        protected SnmpProxyMIB proxyMIB;

        // optional SNMP4J MIBs
        private Snmp4jLogMib snmp4jLogMIB;
        private Snmp4jConfigMib snmp4jConfigMIB;
        private NotificationLogMib notificationLogMIB;

        private UnsignedInteger32 notificationLogDefaultLimit = new UnsignedInteger32(100);
        private UnsignedInteger32 notificationLogGlobalLimit = new UnsignedInteger32(1000);
        private UnsignedInteger32 notificationLogGlobalAge = new UnsignedInteger32(0);

        protected MOFactory moFactory = DefaultMOFactory.getInstance();

        protected OctetString sysDescr = new OctetString("Kaazing WebSocket Gateway " +
        /* VersionInfo.getVersion()+ */" [powered by SNMP4J v" + org.snmp4j.version.VersionInfo.getVersion() + "]"
                + " - " + System.getProperty("os.name", "") + " - " + System.getProperty("os.arch") + " - "
                + System.getProperty("os.version"));
        protected OID sysOID = new OID("1.3.6.1.4.1.4976.10");
        protected Integer32 sysServices = new Integer32(72);

        protected OctetString defaultContext;

        protected MOTableSizeLimit tableSizeLimit;

        /**
         * Creates a SNMP agent configuration which can be run by calling {@link #run()} later.
         *
         * @param agentsOwnEngineID    the authoritative engine ID of the agent.
         * @param messageDispatcher    the MessageDispatcher to use. The message dispatcher must be configured outside, i.e.
         *                             transport mappings have to be added before this constructor is being called.
         * @param vacm                 a view access control model. Typically, this parameter is set to <code>null</code> to use
         *                             the default VACM associated with the <code>VacmMIB</code>.
         * @param moServers            the managed object server(s) that serve the managed objects available to this agent.
         * @param workerPool           the <code>WorkerPool</code> to be used to process incoming request.
         * @param configurationFactory a <code>MOInputFactory</code> that creates a <code>MOInput</code> stream with containing
         *                             serialized ManagedObject information with the agent's configuration or <code>null</code>
         *                             otherwise.
         * @param persistenceProvider  the primary <code>MOPersistenceProvider</code> to be used to load and store persistent
         *                             MOs.
         * @param engineBootsProvider  the provider of engine boots counter.
         */
        public KaazingConfigManager(OctetString agentsOwnEngineID, MessageDispatcher messageDispatcher, VACM vacm,
                                    MOServer[] moServers, WorkerPool workerPool, MOInputFactory configurationFactory,
                                    MOPersistenceProvider persistenceProvider) {
            this.engineID = agentsOwnEngineID;
            this.dispatcher = messageDispatcher;
            this.vacm = vacm;
            this.servers = moServers;
            // this.workerPool = workerPool;
            this.configuration = configurationFactory;
            this.persistenceProvider = persistenceProvider;
        }

        /**
         * Creates a SNMP agent configuration which can be run by calling {@link #run()} later.
         *
         * @param agentsOwnEngineID    the authoritative engine ID of the agent.
         * @param messageDispatcher    the MessageDispatcher to use. The message dispatcher must be configured outside, i.e.
         *                             transport mappings have to be added before this constructor is being called.
         * @param vacm                 a view access control model. Typically, this parameter is set to <code>null</code> to use
         *                             the default VACM associated with the <code>VacmMIB</code>.
         * @param moServers            the managed object server(s) that serve the managed objects available to this agent.
         * @param workerPool           the <code>WorkerPool</code> to be used to process incoming request.
         * @param configurationFactory a <code>MOInputFactory</code> that creates a <code>MOInput</code> stream with containing
         *                             serialized ManagedObject information with the agent's configuration or <code>null</code>
         *                             otherwise.
         * @param persistenceProvider  the primary <code>MOPersistenceProvider</code> to be used to load and store persistent
         *                             MOs.
         * @param engineBootsProvider  the provider of engine boots counter.
         * @param moFactory            the {@link MOFactory} to be used to create {@link ManagedObject}s created by this config
         *                             manager. If <code>null</code> the {@link DefaultMOFactory} will be used.
         */
        public KaazingConfigManager(OctetString agentsOwnEngineID, MessageDispatcher messageDispatcher, VACM vacm,
                                    MOServer[] moServers, WorkerPool workerPool, MOInputFactory configurationFactory,
                                    MOPersistenceProvider persistenceProvider, MOFactory moFactory) {
            this(agentsOwnEngineID, messageDispatcher, vacm, moServers, workerPool, configurationFactory,
                    persistenceProvider);
            this.moFactory = (moFactory == null) ? this.moFactory : moFactory;
        }

        /**
         * Returns the VACM used by this agent config manager.
         *
         * @return the VACM instance of this agent.
         */
        public VACM getVACM() {
            return vacm;
        }

        /**
         * Returns the SNMPv2-MIB implementation used by this config manager.
         *
         * @return the SNMPv2MIB instance of this agent.
         */
        public SNMPv2MIB getSNMPv2MIB() {
            return snmpv2MIB;
        }

        /**
         * Returns the SNMP-TARGET-MIB implementation used by this config manager.
         *
         * @return the SnmpTargetMIB instance of this agent.
         */
        public SnmpTargetMIB getSnmpTargetMIB() {
            return targetMIB;
        }

        /**
         * Returns the SNMP-NOTIFICATION-MIB implementation used by this config manager.
         *
         * @return the SnmpNotificationMIB instance of this agent.
         */
        public SnmpNotificationMIB getSnmpNotificationMIB() {
            return notificationMIB;
        }

        /**
         * Returns the SNMP-COMMUNITY-MIB implementation used by this config manager.
         *
         * @return the SnmpCommunityMIB instance of this agent.
         */
        public SnmpCommunityMIB getSnmpCommunityMIB() {
            return communityMIB;
        }

        /**
         * Returns the NOTIFICATION-LOG-MIB implementation used by this config manager.
         *
         * @return the NotificationLogMib instance of this agent.
         */
        public NotificationLogMib getNotificationLogMIB() {
            return notificationLogMIB;
        }

        /**
         * Returns the SNMP4J-LOG-MIB implementation used by this config manager.
         *
         * @return the Snmp4jLogMib instance of this agent.
         */
        public Snmp4jLogMib getSnmp4jLogMIB() {
            return snmp4jLogMIB;
        }

        /**
         * Returns the SNMP4J-CONFIG-MIB implementation used by this config manager.
         *
         * @return the Snmp4jConfigMib instance of this agent.
         */
        public Snmp4jConfigMib getSnmp4jConfigMIB() {
            return snmp4jConfigMIB;
        }

        /**
         * Launch the agent by registering and launching (i.e., set to listen mode) transport mappings.
         */
        protected void launch() {
            if (tableSizeLimit != null) {
                for (int i = 0; i < servers.length; i++) {
                    DefaultMOServer.unregisterTableRowListener(servers[i], tableSizeLimit);
                    DefaultMOServer.registerTableRowListener(servers[i], tableSizeLimit);
                }
            }
            dispatcher.removeCommandResponder(agent);
            dispatcher.addCommandResponder(agent);
            registerTransportMappings();
            // try {
            // launchTransportMappings();
            // }
            // catch (IOException ex) {
            // String txt =
            // "Could not put all transport mappings in listen mode: "+
            // ex.getMessage();
            // logger.error(txt, ex);
            // runState.addError(new ErrorDescriptor(txt, runState.getState(),
            // STATE_RUNNING, ex));
            // }
            fireLaunchNotifications();
        }

        /**
         * Fire notifications after agent start, i.e. sending a coldStart trap.
         */
        protected void fireLaunchNotifications() {
            if (notificationOriginator != null) {
                notificationOriginator.notify(new OctetString(), SnmpConstants.coldStart, new VariableBinding[0]);
            }
        }

        /**
         * Shutdown the agent by closing the internal SNMP session - including the transport mappings provided through the
         * configured {@link MessageDispatcher} and then store the agent state to persistent storage (if available).
         */
        public void shutdown() {
            try {
                snmpSession.close();
                snmpSession = null;
            } catch (IOException ex) {
                logger.warn("Failed to close SNMP session: " + ex.getMessage());
            }
            // saveState();
            if (tableSizeLimit != null) {
                for (int i = 0; i < servers.length; i++) {
                    DefaultMOServer.unregisterTableRowListener(servers[i], tableSizeLimit);
                }
            }
            unregisterMIBs(null);
        }

        public void initSnmp4jLogMIB() {
            snmp4jLogMIB = new Snmp4jLogMib();
        }

        public void initSnmp4jConfigMIB(MOPersistenceProvider[] persistenceProvider) {
            snmp4jConfigMIB = new Snmp4jConfigMib(snmpv2MIB.getSysUpTime());
            snmp4jConfigMIB.setSnmpCommunityMIB(communityMIB);
            if (this.persistenceProvider != null) {
                snmp4jConfigMIB.setPrimaryProvider(this.persistenceProvider);
            }
            if (persistenceProvider != null) {
                for (int i = 0; i < persistenceProvider.length; i++) {
                    if (persistenceProvider[i] != this.persistenceProvider) {
                        snmp4jConfigMIB.addPersistenceProvider(persistenceProvider[i]);
                    }
                }
            }
        }

        protected void initNotificationLogMIB(VACM vacm, SnmpNotificationMIB notifyMIB) {
            notificationLogMIB = new NotificationLogMib(moFactory, vacm, notifyMIB);
            // init default log
            NlmConfigLogEntryRow row = (NlmConfigLogEntryRow) notificationLogMIB.getNlmConfigLogEntry().createRow(
                    new OID(new int[]{0}),
                    new Variable[]{new OctetString(), notificationLogDefaultLimit,
                            new Integer32(NotificationLogMib.NlmConfigLogAdminStatusEnum.enabled), new Integer32(),
                            new Integer32(StorageType.permanent), new Integer32(RowStatus.active)});
            notificationLogMIB.getNlmConfigLogEntry().addRow(row);
            notificationLogMIB.getNlmConfigGlobalAgeOut().setValue(notificationLogGlobalAge);
            notificationLogMIB.getNlmConfigGlobalEntryLimit().setValue(notificationLogGlobalLimit);
            if (notificationOriginator instanceof NotificationOriginatorImpl) {
                ((NotificationOriginatorImpl) notificationOriginator).removeNotificationLogListener(notificationLogMIB);
                ((NotificationOriginatorImpl) notificationOriginator).addNotificationLogListener(notificationLogMIB);
            }
        }

        protected void initSecurityModels() {
            usm = createUSM();
            SecurityModels.getInstance().addSecurityModel(usm);
            frameworkMIB = new SnmpFrameworkMIB(usm, dispatcher.getTransportMappings());
        }

        protected void initMessageDispatcherWithMPs(MessageDispatcher mp) {
            // Notice in the following that we're overriding the standard PDUFactory.
            // That's so we can create our own PDUs, specifically to support GetSubtree
            // PDU and variants. The version shown here is the same as the MPv2c
            // default 'incomingPDUFactory', but with a Kaazing-specific PDU.

            // XXX Need to fix the following so that V3 stuff works--it really
            // needs a KaazingPDUFactory that implements things for it's normal
            // case (see DefaultPDUFactory for the implementation to copy and change.)
            PDUFactory pduFactory =
                    new PDUFactory() {
                        public PDU createPDU(Target target) {
                            return new KaazingPDU();
                        }
                    };

            mp.addMessageProcessingModel(new MPv1());
            mp.addMessageProcessingModel(new MPv2c(pduFactory));
            MPv3 mpv3 = new MPv3(agent.getContextEngineID().getValue(), pduFactory);
            mp.addMessageProcessingModel(mpv3);
        }

        @SuppressWarnings("unchecked")
        protected void registerTransportMappings() {
            ArrayList<TransportMapping> l = new ArrayList<>(dispatcher.getTransportMappings());
            for (Iterator<TransportMapping> it = l.iterator(); it.hasNext();) {
                TransportMapping tm = it.next();
                tm.removeTransportListener(dispatcher);
                tm.addTransportListener(dispatcher);
            }
        }

        // protected void launchTransportMappings() throws IOException {
        // launchTransportMappings(dispatcher.getTransportMappings());
        // }

        /**
         * Puts a list of transport mappings into listen mode.
         * @param transportMappings a list of {@link TransportMapping} instances.
         * @throws IOException if a transport cannot listen to incoming messages.
         */
        // protected void launchTransportMappings(Collection transportMappings)
        // throws IOException
        // {
        // ArrayList l = new ArrayList(transportMappings);
        // for (Iterator it = l.iterator(); it.hasNext();) {
        // TransportMapping tm = (TransportMapping) it.next();
        // if (!tm.isListening()) {
        // tm.listen();
        // }
        // }
        // }

        /**
         * Closes a list of transport mappings.
         *
         * @param transportMappings a list of {@link TransportMapping} instances.
         * @throws IOException if a transport cannot be closed.
         */
        protected void stopTransportMappings(Collection<TransportMapping> transportMappings) throws IOException {
            ArrayList<TransportMapping> l = new ArrayList<>(transportMappings);
            for (Iterator<TransportMapping> it = l.iterator(); it.hasNext();) {
                TransportMapping tm = it.next();
                if (tm.isListening()) {
                    tm.close();
                }
            }
        }

        /**
         * Save the state of the agent persistently - if necessary persistent storage is available.
         */
        // FIXME: This could be a way to get historical data...
        // public void saveState() {
        // if (persistenceProvider != null) {
        // try {
        // persistenceProvider.store(persistenceProvider.getDefaultURI());
        // runState.advanceState(STATE_SAVED);
        // }
        // catch (IOException ex) {
        // String txt = "Failed to save agent state: "+ex.getMessage();
        // logger.error(txt, ex);
        // runState.addError(new ErrorDescriptor(txt, runState.getState(),
        // STATE_SAVED, ex));
        // }
        // }
        // }

        /**
         * Restore a previously persistently saved state - if available.
         * @return <code>true</code> if the agent state could be restored successfully, <code>false</code> otherwise.
         */
        // public boolean restoreState() {
        // if (persistenceProvider != null) {
        // try {
        // persistenceProvider.restore(persistenceProvider.getDefaultURI(),
        // persistenceImportMode);
        // runState.advanceState(STATE_RESTORED);
        // return true;
        // }
        // catch (FileNotFoundException fnf) {
        // String txt = "Saved agent state not found: "+fnf.getMessage();
        // logger.warn(txt);
        // }
        // catch (IOException ex) {
        // String txt = "Failed to load agent state: "+ex.getMessage();
        // logger.error(txt, ex);
        // runState.addError(new ErrorDescriptor(txt, runState.getState(),
        // STATE_RESTORED, ex));
        // }
        // }
        // return false;
        // }

        /**
         * Configures components and managed objects.
         */
        public void configure() {
            if (configuration != null) {
                MOInput config = configuration.createMOInput();
                if (config == null) {
                    logger.debug("No configuration returned by configuration factory " + configuration);
                    return;
                }
                MOServerPersistence serverPersistence = new MOServerPersistence(servers);
                try {
                    serverPersistence.loadData(config);
                } catch (IOException ex) {
                    String txt = "Failed to load agent configuration: " + ex.getMessage();
                    logger.error(txt, ex);
                    // runState.addError(new ErrorDescriptor(txt, runState.getState(),
                    // STATE_CONFIGURED, ex));
                    throw new RuntimeException(txt, ex);
                } finally {
                    try {
                        config.close();
                    } catch (IOException ex1) {
                        logger.warn("Failed to close config input stream: " + ex1.getMessage());
                    }
                }
            }
            // runState.advanceState(STATE_CONFIGURED);
        }

        protected void initMandatoryMIBs() {
            targetMIB = new SnmpTargetMIB(dispatcher);
            targetMIB.addDefaultTDomains();
            targetMIB.addSupportedTDomain(ManagementTDomainAddressFactory.KaazingTransportDomain,
                    new ManagementTDomainAddressFactory((DefaultServiceContext) serviceContext));
            snmpv2MIB = new SNMPv2MIB(getSysDescr(), getSysOID(), getSysServices());
            notificationMIB = new SnmpNotificationMIB();
            vacmMIB = new VacmMIB(servers);
            usmMIB = new UsmMIB(usm, getSupportedSecurityProtocols());
            usm.addUsmUserListener(usmMIB);
            communityMIB = new SnmpCommunityMIB(targetMIB);
            gatewayManagementMIB = new GatewayManagementMIB(managementContext, moFactory);
            serviceManagementMIB = new ServiceManagementMIB(managementContext, moFactory);
            sessionManagementMIB = new SessionManagementMIB(managementContext, moFactory);
            gatewayConfigurationMIB = new GatewayConfigurationMIB(moFactory);
            jvmManagementMIB = new JVMManagementMIB(managementContext, moFactory);
            systemManagementMIB = new SystemManagementMIB(managementContext, moFactory);
            cpuManagementMIB = new CpuManagementMIB(managementContext, moFactory);
            nicManagementMIB = new NicManagementMIB(managementContext, moFactory);
        }

        protected void linkCounterListener() {
            agent.removeCounterListener(snmpv2MIB);
            agent.addCounterListener(snmpv2MIB);
            usm.getCounterSupport().removeCounterListener(snmpv2MIB);
            usm.getCounterSupport().addCounterListener(snmpv2MIB);
        }

        /**
         * Gets the set of security protocols supported by this agent configuration.
         *
         * @return {@link SecurityProtocols#getInstance()} by default after initialization by {@link
         * SecurityProtocols#addDefaultProtocols()}.
         */
        protected SecurityProtocols getSupportedSecurityProtocols() {
            SecurityProtocols.getInstance().addDefaultProtocols();
            return SecurityProtocols.getInstance();
        }

        /**
         * Creates the USM used by this agent configuration.
         *
         * @return an USM initialized by the engine boots from the <code>engineBootsProvider</code> and <code>engineID</code>.
         */
        protected USM createUSM() {
            return new USM(getSupportedSecurityProtocols(), engineID, 1 /* hardcoded engineBoots == 1 since we don't support
            restart */);
        }

        /**
         * Gets the system services ID which can be modified by altering its value.
         *
         * @return 72 by default.
         */
        public Integer32 getSysServices() {
            return sysServices;
        }

        /**
         * Gets the system OID which can be modified by altering its value.
         *
         * @return an OID - by default the SNMP4J root OID is returned.
         */
        public OID getSysOID() {
            return sysOID;
        }

        /**
         * Returns the sysDescr.0 value for this agent which can be modified by altering its value.
         *
         * @return an OctetString describing the node of the form
         * <p/>
         * <pre>
         * SNMP4J-Agent version [SNMP4J-version] -
         *         <os.name> - <os.arch> - <os.version>
         * </pre>
         * <p/>
         * .
         */
        public OctetString getSysDescr() {
            return sysDescr;
        }

        /**
         * Gets the sysUpTime.0 instance for the default context.
         *
         * @return a <code>SysUpTime</code> instance.
         */
        public SysUpTime getSysUpTime() {
            return snmpv2MIB.getSysUpTime();
        }

        /**
         * Returns the notification originator of this agent configuration. To get the (multi-threaded) {@link
         * NotificationOriginator} of the agent, use {@link #getAgentNotificationOriginator} instead.
         *
         * @return a <code>NotificationOriginator</code> instance.
         */
        public NotificationOriginator getNotificationOriginator() {
            return notificationOriginator;
        }

        /**
         * Returns the notification originator of the agent. Use this method to get a {@link NotificationOriginator} for sending
         * your notifications.
         *
         * @return the <code>NotificationOriginator</code> instance.
         */
        public NotificationOriginator getAgentNotificationOriginator() {
            return agent.getNotificationOriginator();
        }

        /**
         * Sets the notification originator of this agent configuration.
         *
         * @param notificationOriginator a <code>NotificationOriginator</code> instance.
         */
        public void setNotificationOriginator(NotificationOriginator notificationOriginator) {
            this.notificationOriginator = notificationOriginator;
            if (agent != null) {
                agent.setNotificationOriginator(notificationOriginator);
            }
        }

        private VACM vacm() {
            if (vacm != null) {
                return vacm;
            }
            return vacmMIB;
        }

        public void initialize() {
            snmpSession = createSnmpSession(dispatcher);
            if (engineID == null) {
                engineID = new OctetString(MPv3.createLocalEngineID());
            }
            agent = createCommandProcessor(engineID);

            // agent.setWorkerPool(workerPool);
            initSecurityModels();
            initMessageDispatcherWithMPs(dispatcher);
            initMandatoryMIBs();
            linkCounterListener();
            // use VACM-MIB as VACM by default
            agent.setVacm(vacm());
            for (int i = 0; i < servers.length; i++) {
                agent.addMOServer(servers[i]);
            }
            agent.setCoexistenceProvider(communityMIB);
            if (notificationOriginator == null) {
                notificationOriginator = createNotificationOriginator();
            }
            agent.setNotificationOriginator(notificationOriginator);
            // Use CommandProcessor instead notificationOriginator to send informs non
            // blocking.
            snmpv2MIB.setNotificationOriginator(agent);


            initOptionalMIBs();

            try {
                registerMIBs(getDefaultContext());
            } catch (DuplicateRegistrationException drex) {
                logger.error("Duplicate MO registration: " + drex.getMessage(), drex);
            }
            // runState.advanceState(STATE_INITIALIZED);
        }

        /**
         * Sets the table size limits for the tables in this agent. If this method is called while the agent's registration is
         * being changed, a <code>ConcurrentModificationException</code> might be thrown.
         *
         * @param sizeLimits a set of properties as defined by {@link MOTableSizeLimit}.
         */
        public void setTableSizeLimits(Properties sizeLimits) {
            if ((tableSizeLimit != null) && (servers != null)) {
                for (int i = 0; i < servers.length; i++) {
                    DefaultMOServer.unregisterTableRowListener(servers[i], tableSizeLimit);
                }
            }
            tableSizeLimit = new MOTableSizeLimit(sizeLimits);
            // if (getState() == STATE_RUNNING) {
            for (int i = 0; i < servers.length; i++) {
                DefaultMOServer.registerTableRowListener(servers[i], tableSizeLimit);
            }
            // }
        }

        /**
         * Sets the table size limit for the tables in this agent. If this method is called while the agent's registration is
         * being changed, a <code>ConcurrentModificationException</code> might be thrown.
         *
         * @param sizeLimit the maximum size (numer of rows) of tables allowed for this agent.
         */
        public void setTableSizeLimit(int sizeLimit) {
            if ((tableSizeLimit != null) && (servers != null)) {
                for (int i = 0; i < servers.length; i++) {
                    DefaultMOServer.unregisterTableRowListener(servers[i], tableSizeLimit);
                }
            }
            tableSizeLimit = new MOTableSizeLimit(sizeLimit);
            // if (getState() == STATE_RUNNING) {
            for (int i = 0; i < servers.length; i++) {
                DefaultMOServer.registerTableRowListener(servers[i], tableSizeLimit);
            }
            // }
        }

        protected void initOptionalMIBs() {
            initSnmp4jLogMIB();
            initSnmp4jConfigMIB(null);
            if ((vacm() != null) && (notificationMIB != null)) {
                initNotificationLogMIB(vacm(), notificationMIB);
            }
        }

        /**
         * Returns the default context - which is the context that is used by the base agent to register its MIB objects. By
         * default it is <code>null</code> which causes the objects to be registered virtually for all contexts. In that case,
         * subagents for example my not register their own objects under the same subtree(s) in any context. To allow subagents
         * to register their own instances of those MIB modules, an empty <code>OctetString</code> should be used as default
         * context instead.
         *
         * @return <code>null</code> or an <code>OctetString</code> (normally the empty string) denoting the context used for
         * registering default MIBs.
         */
        public OctetString getDefaultContext() {
            return defaultContext;
        }

        /**
         * This method can be overwritten by a subagent to specify the contexts each MIB module (group) will be registered to.
         *
         * @param mibGroup       a group of {@link ManagedObject}s (i.e., a MIB module).
         * @param defaultContext the context to be used by default (i.e., the <code>null</code> context)
         * @return the context for which the module should be registered.
         */
        protected OctetString getContext(MOGroup mibGroup, OctetString defaultContext) {
            return defaultContext;
        }

        /**
         * Register the initialized MIB modules in the specified context of the agent.
         *
         * @param context the context to register the internal MIB modules. This should be <code>null</code> by default.
         * @throws DuplicateRegistrationException if some of the MIB modules registration regions conflict with already
         *                                        registered regions.
         */
        protected void registerMIBs(OctetString context) throws DuplicateRegistrationException {
            MOServer server = agent.getServer(context);
            targetMIB.registerMOs(server, getContext(targetMIB, context));
            notificationMIB.registerMOs(server, getContext(notificationMIB, context));
            vacmMIB.registerMOs(server, getContext(vacmMIB, context));
            usmMIB.registerMOs(server, getContext(usmMIB, context));
            snmpv2MIB.registerMOs(server, getContext(snmpv2MIB, context));
            frameworkMIB.registerMOs(server, getContext(frameworkMIB, context));
            communityMIB.registerMOs(server, getContext(communityMIB, context));
            gatewayManagementMIB.registerMOs(server, getContext(gatewayManagementMIB, context));
            serviceManagementMIB.registerMOs(server, getContext(serviceManagementMIB, context));
            sessionManagementMIB.registerMOs(server, getContext(sessionManagementMIB, context));
            gatewayConfigurationMIB.registerMOs(server, getContext(gatewayConfigurationMIB, context));
            jvmManagementMIB.registerMOs(server, getContext(jvmManagementMIB, context));
            systemManagementMIB.registerMOs(server, getContext(systemManagementMIB, context));
            cpuManagementMIB.registerMOs(server, getContext(cpuManagementMIB, context));
            nicManagementMIB.registerMOs(server, getContext(nicManagementMIB, context));

            if (snmp4jLogMIB != null) {
                snmp4jLogMIB.registerMOs(server, getContext(snmp4jLogMIB, context));
            }
            if (snmp4jConfigMIB != null) {
                snmp4jConfigMIB.registerMOs(server, getContext(snmp4jConfigMIB, context));
            }
            if (proxyMIB != null) {
                proxyMIB.registerMOs(server, getContext(proxyMIB, context));
            }
            if (notificationLogMIB != null) {
                notificationLogMIB.registerMOs(server, getContext(notificationLogMIB, context));
            }
        }

        /**
         * Unregister the initialized MIB modules from the default context of the agent.
         *
         * @param context the context where the MIB modules have been previously registered.
         */
        protected void unregisterMIBs(OctetString context) {
            MOServer server = agent.getServer(context);
            targetMIB.unregisterMOs(server, getContext(targetMIB, context));
            notificationMIB.unregisterMOs(server, getContext(notificationMIB, context));
            vacmMIB.unregisterMOs(server, getContext(vacmMIB, context));
            usmMIB.unregisterMOs(server, getContext(usmMIB, context));
            snmpv2MIB.unregisterMOs(server, getContext(snmpv2MIB, context));
            frameworkMIB.unregisterMOs(server, getContext(frameworkMIB, context));
            communityMIB.unregisterMOs(server, getContext(communityMIB, context));
            gatewayManagementMIB.unregisterMOs(server, getContext(gatewayManagementMIB, context));
            serviceManagementMIB.unregisterMOs(server, getContext(serviceManagementMIB, context));
            sessionManagementMIB.unregisterMOs(server, getContext(sessionManagementMIB, context));
            gatewayConfigurationMIB.unregisterMOs(server, getContext(gatewayConfigurationMIB, context));

            jvmManagementMIB.unregisterMOs(server, getContext(jvmManagementMIB, context));
            systemManagementMIB.unregisterMOs(server, getContext(systemManagementMIB, context));
            cpuManagementMIB.unregisterMOs(server, getContext(cpuManagementMIB, context));
            nicManagementMIB.unregisterMOs(server, getContext(nicManagementMIB, context));

            if (snmp4jLogMIB != null) {
                snmp4jLogMIB.unregisterMOs(server, getContext(snmp4jLogMIB, context));
            }
            if (snmp4jConfigMIB != null) {
                snmp4jConfigMIB.unregisterMOs(server, getContext(targetMIB, context));
            }
            if (proxyMIB != null) {
                proxyMIB.unregisterMOs(server, getContext(proxyMIB, context));
            }
            if (notificationLogMIB != null) {
                notificationLogMIB.unregisterMOs(server, getContext(notificationLogMIB, context));
            }
        }

        public void setupProxyForwarder() {
            proxyForwarder = createProxyForwarder(agent);
        }

        protected NotificationOriginator createNotificationOriginator() {
            return new NotificationOriginatorImpl(snmpSession, vacm(), snmpv2MIB.getSysUpTime(), targetMIB, notificationMIB);
        }

        /**
         * Creates and registers the default proxy forwarder application ({@link ProxyForwarderImpl}).
         *
         * @param agent the command processor that uses the proxy forwarder.
         * @return a ProxyForwarder instance.
         */
        protected ProxyForwarder createProxyForwarder(CommandProcessor agent) {
            proxyMIB = new SnmpProxyMIB();
            ProxyForwarderImpl pf = new ProxyForwarderImpl(snmpSession, proxyMIB, targetMIB);
            agent.addProxyForwarder(pf, null, ProxyForwarder.PROXY_TYPE_ALL);
            pf.addCounterListener(snmpv2MIB);
            return proxyForwarder;
        }

        /**
         * Creates the command processor.  We implement our own so that we can provide new PDUs and still invoke some of the
         * default CommandProcessor methods.
         *
         * @param engineID the engine ID of the agent.
         * @return a new CommandProcessor instance.
         */
        protected CommandProcessor createCommandProcessor(OctetString engineID) {
            CommandProcessor cp = new KaazingCommandProcessor(engineID);
            return cp;
        }

        /**
         * Creates the SNMP session to be used for this agent.
         *
         * @param dispatcher the message dispatcher to be associated with the session.
         * @return a SNMP session (a {@link Snmp} instance by default).
         */
        protected Session createSnmpSession(MessageDispatcher dispatcher) {
            return new Snmp(dispatcher);
        }

        /**
         * Sets the import mode for the {@link MOPersistenceProvider}.
         *
         * @param importMode one of the import modes defined by {@link ImportModes}.
         */
        public void setPersistenceImportMode(int importMode) {
            this.persistenceImportMode = importMode;
        }

        /**
         * Returns the currently active import mode for the {@link MOPersistenceProvider}.
         *
         * @return one of the import modes defined by {@link ImportModes}.
         */
        public int getPersistenceImportMode() {
            return persistenceImportMode;
        }

        public OID addGatewayBean(GatewayManagementBean gatewayBean) {
            OID gatewayOID = gatewayManagementMIB.addGatewayBean(gatewayBean);

            // We don't have a separate call to add the productTitle info that
            // really lives in gatewayManagementBean, so we'll do it here.
            gatewayConfigurationMIB.addVersionInfo(gatewayBean);

            return gatewayOID;
        }

        public void removeGatewayBean(OID gatewayOID) {
            gatewayManagementMIB.removeGatewayBean(gatewayOID);
        }

        public OID addServiceBean(ServiceManagementBean serviceBean) {
            return serviceManagementMIB.addServiceBean(serviceBean);
        }

        public void removeServiceBean(OID oid) {
            serviceManagementMIB.removeServiceBean(oid);
        }

        public OID addSessionBean(SessionManagementBean sessionBean) {
            OID sessionOID = sessionManagementMIB.addSessionBean(sessionBean);
            return sessionOID;
        }

        public void removeSessionBean(OID oid) {
            sessionManagementMIB.removeSessionBean(oid);
        }

        // TODO: implement removeClusterConfigurationBean????
        public void addClusterConfigurationBean(ClusterConfigurationBean clusterConfigurationBean) {
            gatewayConfigurationMIB.addClusterConfiguration(clusterConfigurationBean);
        }

        // TODO: implement removeNetworkConfigurationBean????
        public void addNetworkConfigurationBean(NetworkConfigurationBean networkConfigurationBean) {
            gatewayConfigurationMIB.addNetworkConfiguration(networkConfigurationBean);
        }

        // TODO: implement removeSecurityBean????
        public void addSecurityBean(SecurityConfigurationBean securityBean) {
            gatewayConfigurationMIB.addSecurityConfiguration(securityBean);
        }

        // TODO: implement removeRealmBean????
        public void addRealmBean(RealmConfigurationBean realmBean) {
            gatewayConfigurationMIB.addRealmConfiguration(realmBean);
        }

        // TODO: implement removeServiceConfigurationBean????
        public void addServiceConfigurationBean(ServiceConfigurationBean serviceConfigurationBean) {
            gatewayConfigurationMIB.addServiceConfiguration(serviceConfigurationBean);
        }

        public void addServiceDefaultsConfigurationBean(ServiceDefaultsConfigurationBean serviceDefaultsConfigurationBean) {
            gatewayConfigurationMIB.addServiceDefaultsConfiguration(serviceDefaultsConfigurationBean);
        }

        public void addVersionInfo(GatewayManagementBean gatewayManagementBean) {
            gatewayConfigurationMIB.addVersionInfo(gatewayManagementBean);
        }

        public void addSystemManagementBean(HostManagementBean systemManagementBean) {
            systemManagementMIB.addSystemManagementBean(systemManagementBean);
        }

        public void addCpuListManagementBean(CpuListManagementBean cpuListManagementBean) {
            cpuManagementMIB.addCpuListManagementBean(cpuListManagementBean);
        }

        public void addNicListManagementBean(NicListManagementBean nicListManagementBean) {
            nicManagementMIB.addNicListManagementBean(nicListManagementBean);
        }

        public void addCpuManagementBean(CpuManagementBean cpuManagementBean) {
            cpuManagementMIB.addCpuManagementBean(cpuManagementBean);
        }

        public void addNicManagementBean(NicManagementBean nicManagementBean) {
            nicManagementMIB.addNicManagementBean(nicManagementBean);
        }

        public void addJvmManagementBean(JvmManagementBean jvmManagementBean) {
            jvmManagementMIB.addJvmManagementBean(jvmManagementBean);
        }

        /**
         * A subclass of CommandProcessor so we can both declare our own RequestHandlers and take advantage of the
         * processNextSubRequest in the main CommandProcessor, which is where the real work happens.
         */
        class KaazingCommandProcessor extends CommandProcessor {

            private final KaazingSubscribeNotifHandler notifHandler = new KaazingSubscribeNotifHandler();
            private final GetSubtreeHandler getSubtreeHandler = new GetSubtreeHandler();

            public KaazingCommandProcessor(OctetString contextEngineID) {
                super(contextEngineID);
                addPduHandler(getSubtreeHandler);
                addPduHandler(notifHandler);
            }

            /**
             * Support for handling subscribe requests for Kaazing notifications.
             */
            class KaazingSubscribeNotifHandler implements RequestHandler {

                @Override
                public void processPdu(Request request, MOServer server) {
                    // if there is a valid management address, use the wrapped IoSession to create a subscription to
                    // notifications...
                    // FIXME:  add more fields to the request to so that specific notifications can be subscribed to
                    Object source = request.getSource();
                    if (source instanceof CommandResponderEvent) {
                        CommandResponderEvent commandEvent = (CommandResponderEvent) source;

                        // process a Kaazing notification subscription request.
                        // XXX Is this right? Originally this was checked by
                        // addr being a ManagementAddress.
                        Address addr = commandEvent.getPeerAddress();
                        if (addr instanceof ManagementAddress) {
                            ManagementAddress managementAddress = (ManagementAddress) addr;
                            IoSessionEx session = managementAddress.getSession();

                            OctetString name = generateNotificationTargetAddressName(session
                                    .getId());  // so we have different 'names' for each connection
                            OID transportDomain = ManagementTDomainAddressFactory.KaazingTransportDomain;
                            OctetString address = new OctetString(Long.toString(session.getId()));
                            OctetString tagList = new OctetString("notify");
                            OctetString params =
                                    new OctetString(NOTIF_TARGET_PARAM_NAME); // N.B. this is the target param entry name,
                                    // this is the key name to tie everything together
                            int storageType = 4;
                            SnmpManagementServiceHandler.this.agent.getSnmpTargetMIB()
                                    .addTargetAddress(name, transportDomain, address, 250, 1, tagList, params, storageType);
                        }
                    }
                }

                @Override
                public boolean isSupported(int pduType) {
                    return pduType == KaazingPDU.KAAZING_NOTIFICATION_SUBSCRIPTION;
                }
            }

            /**
             * Support for handling 'getSubtree' requests.  This is based on the SNMP4J class GetNextHandler.
             */
            class GetSubtreeHandler implements RequestHandler {

                public void processPdu(Request request, MOServer server) {
                    // this check is the CommandProcessor private static method 'initRequestPhase'.
                    if (request.getPhase() == Request.PHASE_INIT) {
                        request.nextPhase();
                    }

                    OctetString context = request.getContext();

                    SnmpRequest req = (SnmpRequest) request;
//                    CommandResponderEvent requestEvent = req.getInitiatingEvent();

                    try {
                        // The subrequests are the individual OIDs in the original request.  We are
                        // doing a separate 'getSubtree' on each.
                        SubRequestIterator it = (SubRequestIterator) request.iterator();

                        while (it.hasNext()) {
                            SubRequest sreq = it.nextSubRequest();

                            // The initial set of subrequests associated with the
                            // request have been allocated ahead of time, when the
                            // request was created. The set of response VariableBinding objects
                            // were also created at that time.  We're just going to be adding
                            // some new variable bindings to the response by faking out the processor
                            // and cloning the results we get each time. Contrary to SNMP4J's normal
                            // processing, we don't have a SubRequest defined for each VarBinding.
                            if (!sreq.isComplete()) {

                                // Adjust the scope so we start with the incoming OID as the
                                // beginning of the range to return, and go to the next peer
                                // at the same or higher level.
                                DefaultMOContextScope scope = (DefaultMOContextScope) sreq.getScope();
                                OID lowerBound = (OID) scope.getLowerBound().clone();
                                scope.setLowerIncluded(false);  // Note that we DO NOT include the requested OIDs!
                                OID upperBound = lowerBound.nextPeer();
                                scope.setUpperBound(upperBound);
                                scope.setUpperIncluded(false); // so we don't overrun

                                // 1st process the 'real' subrequest passed in.
                                processNextSubRequest(request, server, context, sreq);

                                if (sreq.getStatus().getErrorStatus() == PDU.noError) {
                                    // Copy that VariableBinding aside, because we cannot replace the
                                    // var binding in the request, and we're going to override the
                                    // value with new values as we get each subsequent value.
                                    VariableBinding initialVB = (VariableBinding) sreq.getVariableBinding().clone();

                                    // Now use that to repeatedly request the 'next' element until we get nothing.
                                    scope.setLowerIncluded(false);
                                    boolean first = true;

                                    while (!sreq.getVariableBinding().getVariable().equals(Null.endOfMibView)) {
                                        sreq.getStatus().setPhaseComplete(false);
                                        sreq.getStatus().setProcessed(false);
                                        sreq.setQuery(null);
                                        scope.setLowerBound(sreq.getVariableBinding().getOid());
                                        if (!first) {
                                            VariableBinding vb = (VariableBinding) sreq.getVariableBinding().clone();
                                            ((KaazingPDU) req.getResponse()).add(vb);
                                        }
                                        first = false;
                                        processNextSubRequest(request, server, context, sreq);
                                        if (sreq.getStatus().getErrorStatus() != PDU.noError) {
                                            break;
                                        }
                                    }

                                    sreq.getVariableBinding().setOid(initialVB.getOid());
                                    sreq.getVariableBinding().setVariable(initialVB.getVariable());
                                    sreq.getStatus().setPhaseComplete(true);
                                    sreq.getStatus().setProcessed(true);
                                }
                            }
                        }
                    } catch (NoSuchElementException nsex) {
                        if (logger.isDebugEnabled()) {
                            logger.debug("GETSUBTREE request response PDU size limit reached");
                        }
                    }
                }

                public boolean isSupported(int pduType) {
                    return pduType == KaazingPDU.GETSUBTREE;
                }
            }
        }
    }

    @Override
    public void addGatewayManagementBean(final GatewayManagementBean gatewayManagementBean) {

        agent.addGatewayBean(gatewayManagementBean);
        //FIXME: we should store the OID if we want to remove the gateway bean from the agent...

        // set up the summary-data listener. This was originally pushed to the management
        // thread, but it's faster done directly.
        OID dataOID = ((OID) MIBConstants.oidGatewayEntry.clone())
                .append(MIBConstants.colGatewaySummaryData)
                .append(gatewayManagementBean.getId());

        OID notificationOID = MIBConstants.oidGatewaySummaryDataEvent;

        gatewayManagementBean.addSummaryDataListener(new SNMPSummaryDataListener(dataOID, notificationOID));

        gatewayManagementBean.addClusterManagementListener(new SNMPClusterListener());
    }

    @Override
    public void addServiceManagementBean(final ServiceManagementBean serviceManagementBean) {
        agent.addServiceBean(serviceManagementBean);

        // set up the summary-data listener. This was originally pushed to the management
        // thread, but it's faster done directly.
        OID dataOID = ((OID) MIBConstants.oidServiceEntry.clone()).append(MIBConstants.colServiceSummaryData)
                .append(serviceManagementBean.getGatewayManagementBean().getId())
                .append(serviceManagementBean.getId());

        OID notificationOID = MIBConstants.oidServiceSummaryDataNotification;

        serviceManagementBean.addSummaryDataListener(new SNMPSummaryDataListener(dataOID, notificationOID));
    }

    @Override
    public void addSessionManagementBean(final SessionManagementBean sessionManagementBean) {
        agent.addSessionBean(sessionManagementBean);

        // set up the summary-data listener. This was originally pushed to the management
        // thread, but it's faster done directly.
        ServiceManagementBean serviceBean = sessionManagementBean.getServiceManagementBean();
        OID dataOID = ((OID) MIBConstants.oidSessionEntry.clone()).append(MIBConstants.colSessionSummaryData)
                .append(serviceBean.getGatewayManagementBean().getId())
                .append(serviceBean.getId())
                .append((int) sessionManagementBean.getId());

        OID notificationOID = MIBConstants.oidSessionSummaryDataNotification;

        sessionManagementBean.addSummaryDataListener(new SNMPSummaryDataListener(dataOID, notificationOID));
    }

    @Override
    public void removeSessionManagementBean(SessionManagementBean sessionManagementBean) {
        ServiceManagementBean serviceBean = sessionManagementBean.getServiceManagementBean();
        GatewayManagementBean gatewayBean = serviceBean.getGatewayManagementBean();
        OID sessionIndexOID = new OID(new int[]{gatewayBean.getId(), serviceBean.getId(), (int) sessionManagementBean.getId()});
        agent.removeSessionBean(sessionIndexOID);
    }

    @Override
    public void addClusterConfigurationBean(ClusterConfigurationBean configurationBean) {
        agent.addClusterConfigurationBean(configurationBean);
    }

    @Override
    public void addNetworkConfigurationBean(NetworkConfigurationBean configurationBean) {
        agent.addNetworkConfigurationBean(configurationBean);
    }

    @Override
    public void addSecurityConfigurationBean(SecurityConfigurationBean configurationBean) {
        agent.addSecurityBean(configurationBean);
    }

    @Override
    public void addRealmConfigurationBean(RealmConfigurationBean configurationBean) {
        agent.addRealmBean(configurationBean);
    }

    @Override
    public void addServiceConfigurationBean(ServiceConfigurationBean configurationBean) {
        agent.addServiceConfigurationBean(configurationBean);
    }

    @Override
    public void addServiceDefaultsConfigurationBean(ServiceDefaultsConfigurationBean configurationBean) {
        agent.addServiceDefaultsConfigurationBean(configurationBean);
    }

    @Override
    public void addVersionInfo(GatewayManagementBean managementBean) {
        agent.addVersionInfo(managementBean);
    }

    @Override
    public void addSystemManagementBean(final HostManagementBean managementBean) {
        agent.addSystemManagementBean(managementBean);

        // Run setting up the summary data schedule OFF any IO thread, if we happen to be on one now.
        managementContext.runManagementTask(new Runnable() {
            public void run() {
                OID dataOID = ((OID) MIBConstants.oidSystemSummaryData.clone())
                        .append(managementBean.getGatewayManagementBean().getId());

                OID notificationOID = MIBConstants.oidSystemSummaryDataNotification;

                managementBean.addSummaryDataListener(new SNMPSummaryDataListener(dataOID, notificationOID));
            }
        });
    }

    @Override
    public void addCpuListManagementBean(final CpuListManagementBean managementBean) {
        agent.addCpuListManagementBean(managementBean);

        // Run setting up the summary data schedule OFF any IO thread, if we happen to be on one now.
        managementContext.runManagementTask(new Runnable() {
            public void run() {
                OID dataOID = ((OID) MIBConstants.oidCpuListSummaryData.clone())
                        .append(managementBean.getGatewayManagementBean().getId());

                OID notificationOID = MIBConstants.oidCpuListSummaryDataNotification;

                managementBean.addSummaryDataListener(new SNMPSummaryDataListener(dataOID, notificationOID));
            }
        });
    }

    @Override
    public void addCpuManagementBean(final CpuManagementBean managementBean, String hostAndPid) {
        agent.addCpuManagementBean(managementBean);

        // We do not directly support notifications on individual CPUs.
    }

    @Override
    public void addNicListManagementBean(final NicListManagementBean managementBean) {
        agent.addNicListManagementBean(managementBean);

        // Run setting up the summary data schedule OFF any IO thread, if we happen to be on one now.
        managementContext.runManagementTask(new Runnable() {
            public void run() {
                OID dataOID = ((OID) MIBConstants.oidNicListSummaryData.clone())
                        .append(managementBean.getGatewayManagementBean().getId());

                OID notificationOID = MIBConstants.oidNicListSummaryDataNotification;

                managementBean.addSummaryDataListener(new SNMPSummaryDataListener(dataOID, notificationOID));
            }
        });
    }

    @Override
    public void addNicManagementBean(final NicManagementBean managementBean, String hostAndPid) {
        agent.addNicManagementBean(managementBean);

        // We do not directly support notifications on individual NICs.
    }


    @Override
    public void addJvmManagementBean(final JvmManagementBean managementBean) {
        agent.addJvmManagementBean(managementBean);

        // Run setting up the summary data schedule OFF any IO thread, if we happen to be on one now.
        managementContext.runManagementTask(new Runnable() {
            public void run() {
                OID dataOID = ((OID) MIBConstants.oidJvmSummaryData.clone())
                        .append(managementBean.getGatewayManagementBean().getId());

                OID notificationOID = MIBConstants.oidJvmSummaryDataNotification;

                managementBean.addSummaryDataListener(new SNMPSummaryDataListener(dataOID, notificationOID));
            }
        });
    }

    private final class SNMPSummaryDataListener implements SummaryDataListener {

        private final OID summaryDataOID;
        private final OID summaryDataNotificationOID;

        public SNMPSummaryDataListener(OID summaryDataOID, OID summaryDataNotificationOID) {
            this.summaryDataOID = summaryDataOID;
            this.summaryDataNotificationOID = summaryDataNotificationOID;
        }

        @Override
        public void sendSummaryData(String summaryData) {
            VariableBinding[] variables = new VariableBinding[1];
            variables[0] = new VariableBinding(summaryDataOID, new OctetString(summaryData));
            sendNotification(summaryDataNotificationOID, variables);
        }
    }

    private final class SNMPGatewayManagementListener implements GatewayManagementListener {

        @Override
        public void doSessionCreated(final GatewayManagementBean gatewayBean,
                                     final long sessionId) throws Exception {
            // for the moment we don't sent gateway-level management notifications on sessionCreated.
        }


        @Override
        public void doSessionClosed(final GatewayManagementBean gatewayBean,
                                    final long sessionId) throws Exception {
            // for the moment we don't sent gateway-level management notifications on sessionClosed.
        }

        @Override
        public void doMessageReceived(final GatewayManagementBean gatewayBean,
                                      final long sessionId) throws Exception {
            // for the moment we don't sent gateway-level management notifications on messageReceived.
        }

        @Override
        public void doFilterWrite(final GatewayManagementBean gatewayBean,
                                  final long sessionId) throws Exception {
            // for the moment we don't sent gateway-level management notifications on filterWrite.
        }

        @Override
        public void doExceptionCaught(final GatewayManagementBean gatewayBean,
                                      final long sessionId) throws Exception {
            // for the moment we don't sent gateway-level management notifications on exceptionCaught.
        }
    }

    private final class SNMPServiceManagementListener implements ServiceManagementListener {

        @Override
        public void doSessionCreated(final ServiceManagementBean serviceBean,
                                     final long newCurrentSessionCount,
                                     final long newTotalSessionCount) {
            // SNMP does its stuff in SessionManagementListener.doSessionCreated().
        }


        @Override
        public void doSessionClosed(final ServiceManagementBean serviceBean,
                                    final long sessionId,
                                    final long newCurrentSessionCount) throws Exception {
            if (serviceBean.areNotificationsEnabled()) {
                int gatewayId = serviceBean.getGatewayManagementBean().getId();
                int serviceId = serviceBean.getId();

                OID serviceOID = new OID(new int[]{gatewayId, serviceId});

                // TODO We should really design the particular set of notifications that we
                // want to expose, and the data that go with them.
                OID notificationOID = MIBConstants.oidServiceDisconnectionNotification;
                VariableBinding[] variables = new VariableBinding[2];

                // Send the current session count (no need for total, as it didn't change.)
                OID currSessionCountOID = ((OID) MIBConstants.oidServiceEntry.clone())
                        .append(MIBConstants.colServiceCurrentSessionCount)
                        .append(serviceOID);
                variables[0] = new VariableBinding(currSessionCountOID, new Counter64(newCurrentSessionCount));

                // Add a variable to turn the session 'status' field to 'off'.
                OID sessionOID = new OID(new int[]{gatewayId, serviceId, (int) sessionId});

                variables[1] = new VariableBinding(((OID) MIBConstants.oidSessionEntry.clone())
                        .append(MIBConstants.colSessionCloseSession)
                        .append(sessionOID),
                        new Integer32(0));
                sendNotification(notificationOID, variables);

                // TODO Should we be sending a notification about loggedInSessions, too, as we did for JMX?
            }
        }

        @Override
        public void doMessageReceived(final ServiceManagementBean serviceBean,
                                      final long sessionId,
                                      final ByteBuffer message) throws Exception {
            // TODO:  If we are tracing messages through this service, send this message out as a notification
        }

        @Override
        public void doFilterWrite(final ServiceManagementBean serviceBean,
                                  final long sessionId,
                                  final ByteBuffer writeMessage) throws Exception {
            // TODO:  If we are tracing messages through this service, send this message out as a notification
        }

        @Override
        public void doExceptionCaught(final ServiceManagementBean serviceBean,
                                      final long sessionId,
                                      final String exceptionMessage) throws Exception {
        }
    }

    private final class SNMPSessionManagementListener implements SessionManagementListener {

        /**
         * Send a notification about the session being opened. We set things up so that this is considered to be a service-level
         * operation, but all the real data is in the session level, so for consistency and locality, we're going to process it
         * in here, though we do check the SERVICE-level notifications-enabled flag.
         */
        @Override
        public void doSessionCreated(final SessionManagementBean sessionBean) throws Exception {

            ServiceManagementBean serviceBean = sessionBean.getServiceManagementBean();

            if (serviceBean.areNotificationsEnabled()) {

                // send the new session count notification for the given service.

                // XXX We should really design the particular set of notifications that we
                // want to expose, and the data that go with them.

                JSONObject sessionData = new JSONObject();

                IoSessionEx session = sessionBean.getSession();

                sessionData.put("sessionId", session.getId());
                sessionData.put("serviceId", serviceBean.getId());
                sessionData.put("createTime", session.getCreationTime());
                sessionData.put("localAddress", session.getLocalAddress());
                sessionData.put("remoteAddress", serviceBean.getSessionRemoteAddress(session));

                ResourceAddress address = BridgeSession.LOCAL_ADDRESS.get(session);
                sessionData.put("sessionTypeName", Utils.getSessionTypeName(address));
                sessionData.put("sessionDirection", Utils.getSessionDirection(session));

                Map<String, String> userPrincipals = sessionBean.getUserPrincipalMap();

                if (userPrincipals != null) {
                    sessionData.put("principals", userPrincipals);
                }

                // Allow some protocol-specific attributes but at a known attribute name.
                // Initially we'll only have AbstractWsBridgeSessions.
                JSONObject protocolAttributes = null;

                if (session instanceof AbstractWsBridgeSession) {
                    AbstractWsBridgeSession wsBridgeSession = (AbstractWsBridgeSession) session;
    /*
     * We could gather a bunch of different session and parent session and other
     * attributes here. However, there is no really obvious way to do this
     * in a generic way without sending over lots of stringified object-instance
     * information that won't mean much to the client. Until we figure out a specific
     * set of attributes (like 'wsExtensions' below), we're not going to send
     * any other bridge-session attributes.

                    IoSession parentSession = wsBridgeSession.getParent();
                    if (parentSession != null) {
                        Set<Object> parentAttrKeys = parentSession.getAttributeKeys();
                        if (parentAttrKeys != null) {
                            for (Object parentAttrKey : parentAttrKeys) {
                                Object attr = parentSession.getAttribute(parentAttrKey);
                                sessionData.put(parentAttrKey.toString(), attr);
                            }
                        }
                    }
                    Set<Object> attrKeys = wsBridgeSession.getAttributeKeys();
                    if (attrKeys != null) {
                        for (Object attrKey : attrKeys) {
                            Object attr = wsBridgeSession.getAttribute(attrKey);
                            sessionData.put(attrKey.toString(), attr);
                        }
                    }
    */
                    protocolAttributes = new JSONObject();

                    List<WsExtension> extensions = wsBridgeSession.getWsExtensions().asList();
                    if (extensions != null && extensions.size() > 0) {
                        JSONObject jsonObj = new JSONObject();

                        for (WsExtension extension : extensions) {
                            String token = extension.getExtensionToken();

                            JSONArray paramsArray = null;

                            if (extension.hasParameters()) {
                                paramsArray = new JSONArray();

                                for (WsExtensionParameter param : extension.getParameters()) {
                                    String name = param.getName();
                                    String value = param.getValue();

                                    // if value == null, it's a param without a name (the value
                                    // is actually in the name;
                                    if (value == null) {
                                        paramsArray.put(name.toString());
                                    } else {
                                        paramsArray.put(name.toString() + "=" + value.toString());
                                    }
                                }
                            }

                            jsonObj.put(token, paramsArray);
                        }

                        protocolAttributes.put("extensions", jsonObj);
                    }

                    if (session instanceof WsnSession) {
                        WsnSession wsnSession = (WsnSession) session;
                        WebSocketWireProtocol protocolVersion = wsnSession.getVersion();
                        protocolAttributes.put("protocolVersion", protocolVersion.toString());
                    } else if (session instanceof WsebSession) {
                        WsebSession wsebSession = (WsebSession) session;
                        ResourceAddress readAddr = wsebSession.getReadAddress();
                        ResourceAddress writeAddr = wsebSession.getWriteAddress();
                        if (readAddr != null) {
                            protocolAttributes.put("readAddress", readAddr);
                        }
                        if (writeAddr != null) {
                            protocolAttributes.put("writeAddress", writeAddr);
                        }
                    } else if (session instanceof WsrSession) {
                        WsrSession wsrSession = (WsrSession) session;
                        ResourceAddress rtmpAddress = wsrSession.getRtmpAddress();
                        protocolAttributes.put("rtmpAddress", rtmpAddress);
                    }
                }

                if (protocolAttributes != null) {
                    sessionData.put("protocolAttributes", protocolAttributes);
                }

                OID notificationOID = MIBConstants.oidServiceConnectionNotification;
                VariableBinding[] variables = new VariableBinding[1];
                variables[0] = new VariableBinding(notificationOID, new OctetString(sessionData.toString()));

                sendNotification(notificationOID, variables);
            }
        }

        @Override
        public void doSessionClosed(final SessionManagementBean sessionBean) throws Exception {
            // Nothing to do, now that the management listeners are in DefaultManagementContext.
        }

        @Override
        public void doMessageReceived(final SessionManagementBean sessionBean, Object message) throws Exception {
            if (sessionBean.areNotificationsEnabled()) {
                if (message instanceof IoBufferEx) {
                    StringBuffer sb = new StringBuffer();
                    sb.append("Message Received: ");
                    IoBufferEx buffer = (IoBufferEx) message;
                    byte[] bytes = buffer.array();
                    int arrayOffset = buffer.arrayOffset();
                    for (int i = buffer.position(); i < buffer.limit(); i++) {
                        sb.append(String.format("%02X ", bytes[i + arrayOffset]));
                    }

                    ServiceManagementBean serviceBean = sessionBean.getServiceManagementBean();
                    GatewayManagementBean gatewayBean = serviceBean.getGatewayManagementBean();
                    OID sessionOID = new OID(new int[]{gatewayBean.getId(), serviceBean.getId(), (int) sessionBean.getId()});

                    OID notificationOID = MIBConstants.oidSessionMessageReceivedNotification;

                    VariableBinding[] variables = new VariableBinding[2];
                    variables[0] = new VariableBinding(((OID) MIBConstants.oidSessionEntry.clone())
                            .append(MIBConstants.colSessionId).append(sessionOID),
                            new Counter64(sessionBean.getId()));
                    variables[1] = new VariableBinding(notificationOID, new OctetString(sb.toString()));
                    sendNotification(notificationOID, variables);
                }
            }
        }

        @Override
        public void doFilterWrite(final SessionManagementBean sessionBean,
                                  final Object message,
                                  final Object originalMessage) throws Exception {
            if (sessionBean.areNotificationsEnabled()) {
                if (message instanceof IoBufferEx) {
                    StringBuffer sb = new StringBuffer();
                    sb.append("Message Sent: ");

                    IoBufferEx buffer = null;
                    if (originalMessage != null) {
                        if (originalMessage instanceof IoBufferEx) {
                            buffer = (IoBufferEx) originalMessage;
                        } else if (originalMessage instanceof IoMessage) {
                            buffer = ((IoMessage) originalMessage).getBuffer();
                        }
                    } else {
                        buffer = (IoBufferEx) message;
                    }

                    byte[] bytes = buffer.array();
                    int arrayOffset = buffer.arrayOffset();
                    for (int i = buffer.position(); i < buffer.limit(); i++) {
                        sb.append(String.format("%02X ", bytes[i + arrayOffset]));
                    }

                    ServiceManagementBean serviceBean = sessionBean.getServiceManagementBean();
                    GatewayManagementBean gatewayBean = serviceBean.getGatewayManagementBean();
                    OID sessionOID = new OID(new int[]{gatewayBean.getId(), serviceBean.getId(), (int) sessionBean.getId()});

                    OID notificationOID = MIBConstants.oidSessionFilterWriteNotification;

                    VariableBinding[] variables = new VariableBinding[2];
                    variables[0] = new VariableBinding(((OID) MIBConstants.oidSessionEntry.clone())
                            .append(MIBConstants.colSessionId).append(sessionOID),
                            new Counter64(sessionBean.getId()));
                    variables[1] = new VariableBinding(notificationOID, new OctetString(sb.toString()));
                    sendNotification(notificationOID, variables);
                }
            }
        }

        @Override
        public void doExceptionCaught(final SessionManagementBean sessionBean, Throwable cause) {
        }
    }

    private class SNMPClusterListener implements ClusterManagementListener {

        private GatewayManagementBean gatewayBean;

        public void setGatewayBean(GatewayManagementBean gatewayBean) {
            this.gatewayBean = gatewayBean;
        }

        @Override
        public void membershipChanged(String changeType, String instanceKey) {
            OID gatewayOID = new OID(new int[]{1}); // hardcoded to 1 for now

            // Since we have no data yet on the ID that's joining or leaving, just
            // send the instanceKey of the member that's changed. Let the client-side
            // deal with determining changes to the overall balanced-services state.
            // At least in the join case, no services are actually running yet.
            OID notificationOID = MIBConstants.oidClusterMembershipEvent;
            OID membershipEventTypeOID = MIBConstants.oidClusterMembershipEventType;
            OID instanceKeyOID = ((OID) MIBConstants.oidGatewayEntry.clone())
                    .append(MIBConstants.colInstanceKey)
                    .append(gatewayOID);


            VariableBinding[] variables = new VariableBinding[2];
            variables[0] = new VariableBinding(membershipEventTypeOID, new OctetString(changeType));
            variables[1] = new VariableBinding(instanceKeyOID, new OctetString(instanceKey));

            sendNotification(notificationOID, variables);
        }

        @Override
        public void managementServicesChanged(String changeType, String instanceKey, Collection<URI> managementServiceAccepts) {
            OID gatewayOID = new OID(new int[]{1}); // hardcoded to 1 for now

            OID notificationOID = MIBConstants.oidClusterManagementServiceEvent;
            OID mgmtSvcNotificationEventTypeOID = MIBConstants.oidClusterManagementServiceEventType;
            OID mgmtSvcNotificationEventURIsOID = MIBConstants.oidClusterManagementServiceEventURIs;
            OID instanceKeyOID = ((OID) MIBConstants.oidGatewayEntry.clone())
                    .append(MIBConstants.colInstanceKey)
                    .append(gatewayOID);

            StringBuffer sb = new StringBuffer();
            String managementServiceAcceptsValue = "";
            if (managementServiceAccepts != null) {
                for (URI managementServiceAccept : managementServiceAccepts) {
                    sb.append(managementServiceAccept.toString() + '\n');
                }
                if (sb.length() > 1) {
                    managementServiceAcceptsValue = sb.substring(0, sb.length() - 1); // trim final \n char
                }
            }

            VariableBinding[] variables = new VariableBinding[3];
            variables[0] = new VariableBinding(mgmtSvcNotificationEventTypeOID, new OctetString(changeType));
            variables[1] = new VariableBinding(instanceKeyOID, new OctetString(instanceKey));
            variables[2] = new VariableBinding(mgmtSvcNotificationEventURIsOID, new OctetString(managementServiceAcceptsValue));
            sendNotification(notificationOID, variables);
        }

        @Override
        public void balancerMapChanged(String changeType, URI balancerURI, Collection<URI> balanceeURIs) {
            OID gatewayOID = new OID(new int[]{1}); // hardcoded to 1 for now

            OID notificationOID = MIBConstants.oidClusterBalancerMapEvent;
            OID balancerMapNotificationEventTypeOID = MIBConstants.oidClusterBalancerMapEventType;
            OID balancerMapNotificationBalancerUriOID = MIBConstants.oidClusterBalancerMapEventBalancerURI;
            OID balancerMapNotificationBalanceeUrisOID = MIBConstants.oidClusterBalancerMapEventBalanceeURIs;

            StringBuffer sb = new StringBuffer();
            String balanceeURIsValue = "";
            if (balanceeURIs != null) {
                for (URI balanceeURI : balanceeURIs) {
                    sb.append(balanceeURI.toString() + '\n');
                }
                if (sb.length() > 1) {
                    balanceeURIsValue = sb.substring(0, sb.length() - 1); // trim final \n char
                }
            }

            VariableBinding[] variables = new VariableBinding[3];
            variables[0] = new VariableBinding(balancerMapNotificationEventTypeOID, new OctetString(changeType));
            variables[1] = new VariableBinding(balancerMapNotificationBalancerUriOID, new OctetString(balancerURI.toString()));
            variables[2] = new VariableBinding(balancerMapNotificationBalanceeUrisOID, new OctetString(balanceeURIsValue));
            sendNotification(notificationOID, variables);

        }
    }

    private static class KaazingSNMPAgentProperties {
        private static Properties agentProperties;
        private static Properties tableSizeLimits;

        static {
            agentProperties = new Properties();
            agentProperties.setProperty("snmp4j.agent.cfg.contexts", "");
            agentProperties.setProperty("snmp4j.agent.cfg.oid.1.3.6.1.2.1.1.2.0", "{o}1.3.6.1.4.1.4976");
            agentProperties.setProperty("snmp4j.agent.cfg.oid.1.3.6.1.2.1.1.4.0", "{s}System Administrator");
            agentProperties.setProperty("snmp4j.agent.cfg.oid.1.3.6.1.2.1.1.6.0", "{s}<edit location>");
            agentProperties.setProperty("snmp4j.agent.cfg.oid.1.3.6.1.2.1.1.7.0", "{i}10");
            agentProperties.setProperty("snmp4j.agent.cfg.oid.1.3.6.1.2.1.1.9.1", "1:2");
            agentProperties.setProperty("snmp4j.agent.cfg.index.1.3.6.1.2.1.1.9.1.0", "{o}1");
            agentProperties.setProperty("snmp4j.agent.cfg.value.1.3.6.1.2.1.1.9.1.0.0", "{o}1.3.6.1.4.1.4976.10.1.1.100.4.1");
            agentProperties.setProperty("snmp4j.agent.cfg.value.1.3.6.1.2.1.1.9.1.0.1", "");

            // --------------
            //    VACM MIB
            // --------------
            // security2Group
            agentProperties.setProperty("snmp4j.agent.cfg.oid.1.3.6.1.6.3.16.1.2.1", "5:3");
            agentProperties.setProperty("snmp4j.agent.cfg.index.1.3.6.1.6.3.16.1.2.1.0", "{o}2.6.'public'");
            agentProperties.setProperty("snmp4j.agent.cfg.value.1.3.6.1.6.3.16.1.2.1.0.0", "{s}v1v2cgroup");
            agentProperties.setProperty("snmp4j.agent.cfg.value.1.3.6.1.6.3.16.1.2.1.0.1", "{i}4");
            agentProperties.setProperty("snmp4j.agent.cfg.value.1.3.6.1.6.3.16.1.2.1.0.2", "{i}1");
            agentProperties.setProperty("snmp4j.agent.cfg.index.1.3.6.1.6.3.16.1.2.1.1", "{o}3.6.'SHADES'");
            agentProperties.setProperty("snmp4j.agent.cfg.value.1.3.6.1.6.3.16.1.2.1.1.0", "{s}v3group");
            agentProperties.setProperty("snmp4j.agent.cfg.value.1.3.6.1.6.3.16.1.2.1.1.1", "{i}4");
            agentProperties.setProperty("snmp4j.agent.cfg.value.1.3.6.1.6.3.16.1.2.1.1.2", "{i}1");
            agentProperties.setProperty("snmp4j.agent.cfg.index.1.3.6.1.6.3.16.1.2.1.2", "{o}1.6.'public'");
            agentProperties.setProperty("snmp4j.agent.cfg.value.1.3.6.1.6.3.16.1.2.1.2.0", "{s}v1v2cgroup");
            agentProperties.setProperty("snmp4j.agent.cfg.value.1.3.6.1.6.3.16.1.2.1.2.1", "{i}4");
            agentProperties.setProperty("snmp4j.agent.cfg.value.1.3.6.1.6.3.16.1.2.1.2.2", "{i}1");
            agentProperties.setProperty("snmp4j.agent.cfg.index.1.3.6.1.6.3.16.1.2.1.3", "{o}3.3.'SHA'");
            agentProperties.setProperty("snmp4j.agent.cfg.value.1.3.6.1.6.3.16.1.2.1.3.0", "{s}v3group");
            agentProperties.setProperty("snmp4j.agent.cfg.value.1.3.6.1.6.3.16.1.2.1.3.1", "{i}4");
            agentProperties.setProperty("snmp4j.agent.cfg.value.1.3.6.1.6.3.16.1.2.1.3.2", "{i}1");
            agentProperties.setProperty("snmp4j.agent.cfg.index.1.3.6.1.6.3.16.1.2.1.4", "{o}3.5.'unsec'");
            agentProperties.setProperty("snmp4j.agent.cfg.value.1.3.6.1.6.3.16.1.2.1.4.0", "{s}v3group");
            agentProperties.setProperty("snmp4j.agent.cfg.value.1.3.6.1.6.3.16.1.2.1.4.1", "{i}4");
            agentProperties.setProperty("snmp4j.agent.cfg.value.1.3.6.1.6.3.16.1.2.1.4.2", "{i}1");

            // access
            agentProperties.setProperty("snmp4j.agent.cfg.oid.1.3.6.1.6.3.16.1.4.1", "5:6");
            agentProperties.setProperty("snmp4j.agent.cfg.index.1.3.6.1.6.3.16.1.4.1.0", "{o}10.'v1v2cgroup'.0.2.1");
            agentProperties.setProperty("snmp4j.agent.cfg.value.1.3.6.1.6.3.16.1.4.1.0.0", "{i}1");
            agentProperties.setProperty("snmp4j.agent.cfg.value.1.3.6.1.6.3.16.1.4.1.0.1", "{s}unrestrictedReadView");
            agentProperties.setProperty("snmp4j.agent.cfg.value.1.3.6.1.6.3.16.1.4.1.0.2", "{s}unrestrictedWriteView");
            agentProperties.setProperty("snmp4j.agent.cfg.value.1.3.6.1.6.3.16.1.4.1.0.3", "{s}unrestrictedNotifyView");
            agentProperties.setProperty("snmp4j.agent.cfg.value.1.3.6.1.6.3.16.1.4.1.0.4", "{i}4");
            agentProperties.setProperty("snmp4j.agent.cfg.value.1.3.6.1.6.3.16.1.4.1.0.5", "{i}1");
            agentProperties.setProperty("snmp4j.agent.cfg.index.1.3.6.1.6.3.16.1.4.1.1", "{o}7.'v3group'.0.3.3");
            agentProperties.setProperty("snmp4j.agent.cfg.value.1.3.6.1.6.3.16.1.4.1.1.0", "{i}1");
            agentProperties.setProperty("snmp4j.agent.cfg.value.1.3.6.1.6.3.16.1.4.1.1.1", "{s}unrestrictedReadView");
            agentProperties.setProperty("snmp4j.agent.cfg.value.1.3.6.1.6.3.16.1.4.1.1.2", "{s}unrestrictedWriteView");
            agentProperties.setProperty("snmp4j.agent.cfg.value.1.3.6.1.6.3.16.1.4.1.1.3", "{s}unrestrictedNotifyView");
            agentProperties.setProperty("snmp4j.agent.cfg.value.1.3.6.1.6.3.16.1.4.1.1.4", "{i}4");
            agentProperties.setProperty("snmp4j.agent.cfg.value.1.3.6.1.6.3.16.1.4.1.1.5", "{i}1");
            agentProperties.setProperty("snmp4j.agent.cfg.index.1.3.6.1.6.3.16.1.4.1.2", "{o}10.'v1v2cgroup'.0.1.1");
            agentProperties.setProperty("snmp4j.agent.cfg.value.1.3.6.1.6.3.16.1.4.1.2.0", "{i}1");
            agentProperties.setProperty("snmp4j.agent.cfg.value.1.3.6.1.6.3.16.1.4.1.2.1", "{s}unrestrictedReadView");
            agentProperties.setProperty("snmp4j.agent.cfg.value.1.3.6.1.6.3.16.1.4.1.2.2", "{s}unrestrictedWriteView");
            agentProperties.setProperty("snmp4j.agent.cfg.value.1.3.6.1.6.3.16.1.4.1.2.3", "{s}unrestrictedNotifyView");
            agentProperties.setProperty("snmp4j.agent.cfg.value.1.3.6.1.6.3.16.1.4.1.2.4", "{i}4");
            agentProperties.setProperty("snmp4j.agent.cfg.value.1.3.6.1.6.3.16.1.4.1.2.5", "{i}1");
            agentProperties.setProperty("snmp4j.agent.cfg.index.1.3.6.1.6.3.16.1.4.1.3", "{o}7.'v3group'.0.3.2");
            agentProperties.setProperty("snmp4j.agent.cfg.value.1.3.6.1.6.3.16.1.4.1.3.0", "{i}1");
            agentProperties.setProperty("snmp4j.agent.cfg.value.1.3.6.1.6.3.16.1.4.1.3.1", "{s}unrestrictedReadView");
            agentProperties.setProperty("snmp4j.agent.cfg.value.1.3.6.1.6.3.16.1.4.1.3.2", "{s}unrestrictedWriteView");
            agentProperties.setProperty("snmp4j.agent.cfg.value.1.3.6.1.6.3.16.1.4.1.3.3", "{s}unrestrictedNotifyView");
            agentProperties.setProperty("snmp4j.agent.cfg.value.1.3.6.1.6.3.16.1.4.1.3.4", "{i}4");
            agentProperties.setProperty("snmp4j.agent.cfg.value.1.3.6.1.6.3.16.1.4.1.3.5", "{i}1");
            agentProperties.setProperty("snmp4j.agent.cfg.index.1.3.6.1.6.3.16.1.4.1.4", "{o}7.'v3group'.0.3.1");
            agentProperties.setProperty("snmp4j.agent.cfg.value.1.3.6.1.6.3.16.1.4.1.4.0", "{i}1");
            agentProperties.setProperty("snmp4j.agent.cfg.value.1.3.6.1.6.3.16.1.4.1.4.1", "{s}unrestrictedReadView");
            agentProperties.setProperty("snmp4j.agent.cfg.value.1.3.6.1.6.3.16.1.4.1.4.2", "{s}");
            agentProperties.setProperty("snmp4j.agent.cfg.value.1.3.6.1.6.3.16.1.4.1.4.3", "{s}unrestrictedNotifyView");
            agentProperties.setProperty("snmp4j.agent.cfg.value.1.3.6.1.6.3.16.1.4.1.4.4", "{i}4");
            agentProperties.setProperty("snmp4j.agent.cfg.value.1.3.6.1.6.3.16.1.4.1.4.5", "{i}1");

            // view trees
            agentProperties.setProperty("snmp4j.agent.cfg.oid.1.3.6.1.6.3.16.1.5.2.1", "3:4");
            agentProperties
                    .setProperty("snmp4j.agent.cfg.index.1.3.6.1.6.3.16.1.5.2.1.0", "{o}20.'unrestrictedReadView'.3.1.3.6");
            agentProperties.setProperty("snmp4j.agent.cfg.value.1.3.6.1.6.3.16.1.5.2.1.0.0", "{s}");
            agentProperties.setProperty("snmp4j.agent.cfg.value.1.3.6.1.6.3.16.1.5.2.1.0.1", "{i}1");
            agentProperties.setProperty("snmp4j.agent.cfg.value.1.3.6.1.6.3.16.1.5.2.1.0.2", "{i}4");
            agentProperties.setProperty("snmp4j.agent.cfg.value.1.3.6.1.6.3.16.1.5.2.1.0.3", "{i}1");
            agentProperties
                    .setProperty("snmp4j.agent.cfg.index.1.3.6.1.6.3.16.1.5.2.1.1", "{o}21.'unrestrictedWriteView'.3.1.3.6");
            agentProperties.setProperty("snmp4j.agent.cfg.value.1.3.6.1.6.3.16.1.5.2.1.1.0", "{s}");
            agentProperties.setProperty("snmp4j.agent.cfg.value.1.3.6.1.6.3.16.1.5.2.1.1.1", "{i}1");
            agentProperties.setProperty("snmp4j.agent.cfg.value.1.3.6.1.6.3.16.1.5.2.1.1.2", "{i}4");
            agentProperties.setProperty("snmp4j.agent.cfg.value.1.3.6.1.6.3.16.1.5.2.1.1.3", "{i}1");
            agentProperties
                    .setProperty("snmp4j.agent.cfg.index.1.3.6.1.6.3.16.1.5.2.1.2", "{o}22.'unrestrictedNotifyView'.3.1.3.6");
            agentProperties.setProperty("snmp4j.agent.cfg.value.1.3.6.1.6.3.16.1.5.2.1.2.0", "{s}");
            agentProperties.setProperty("snmp4j.agent.cfg.value.1.3.6.1.6.3.16.1.5.2.1.2.1", "{i}1");
            agentProperties.setProperty("snmp4j.agent.cfg.value.1.3.6.1.6.3.16.1.5.2.1.2.2", "{i}4");
            agentProperties.setProperty("snmp4j.agent.cfg.value.1.3.6.1.6.3.16.1.5.2.1.2.3", "{i}1");

            // ------------------
            // SNMP community MIB
            // ------------------
            agentProperties.setProperty("snmp4j.agent.cfg.oid.1.3.6.1.6.3.18.1.1.1", "1:7");
            agentProperties.setProperty("snmp4j.agent.cfg.index.1.3.6.1.6.3.18.1.1.1.0", "{o}'public'");
            agentProperties.setProperty("snmp4j.agent.cfg.value.1.3.6.1.6.3.18.1.1.1.0.0", "{s}public");
            agentProperties.setProperty("snmp4j.agent.cfg.value.1.3.6.1.6.3.18.1.1.1.0.1", "{s}public");
            agentProperties.setProperty("snmp4j.agent.cfg.value.1.3.6.1.6.3.18.1.1.1.0.2", "{$1.3.6.1.6.3.10.2.1.1.0}");
            agentProperties.setProperty("snmp4j.agent.cfg.value.1.3.6.1.6.3.18.1.1.1.0.3", "{s}");
            agentProperties.setProperty("snmp4j.agent.cfg.value.1.3.6.1.6.3.18.1.1.1.0.4", "{s}");
            agentProperties.setProperty("snmp4j.agent.cfg.value.1.3.6.1.6.3.18.1.1.1.0.5", "{i}4");
            agentProperties.setProperty("snmp4j.agent.cfg.value.1.3.6.1.6.3.18.1.1.1.0.6", "{i}1");

            // -------
            // USM MIB
            // -------
            agentProperties.setProperty("snmp4j.agent.cfg.oid.1.3.6.1.6.3.15.1.2.2.1", "3:14");
            agentProperties
                    .setProperty("snmp4j.agent.cfg.index.1.3.6.1.6.3.15.1.2.2.1.0", "{o}$#{1.3.6.1.6.3.10.2.1.1.0}.6.'SHADES'");
            agentProperties.setProperty("snmp4j.agent.cfg.value.1.3.6.1.6.3.15.1.2.2.1.0.0", "{s}SHADES");
            agentProperties.setProperty("snmp4j.agent.cfg.value.1.3.6.1.6.3.15.1.2.2.1.0.1", "{o}");
            agentProperties.setProperty("snmp4j.agent.cfg.value.1.3.6.1.6.3.15.1.2.2.1.0.2", "{o}1.3.6.1.6.3.10.1.1.3");
            agentProperties.setProperty("snmp4j.agent.cfg.value.1.3.6.1.6.3.15.1.2.2.1.0.3", "{s}");
            agentProperties.setProperty("snmp4j.agent.cfg.value.1.3.6.1.6.3.15.1.2.2.1.0.4", "{s}");
            agentProperties.setProperty("snmp4j.agent.cfg.value.1.3.6.1.6.3.15.1.2.2.1.0.5", "{o}1.3.6.1.6.3.10.1.2.2");
            agentProperties.setProperty("snmp4j.agent.cfg.value.1.3.6.1.6.3.15.1.2.2.1.0.6", "{s}");
            agentProperties.setProperty("snmp4j.agent.cfg.value.1.3.6.1.6.3.15.1.2.2.1.0.7", "{s}");
            agentProperties.setProperty("snmp4j.agent.cfg.value.1.3.6.1.6.3.15.1.2.2.1.0.8", "{s}");
            agentProperties.setProperty("snmp4j.agent.cfg.value.1.3.6.1.6.3.15.1.2.2.1.0.9", "{i}4");
            agentProperties.setProperty("snmp4j.agent.cfg.value.1.3.6.1.6.3.15.1.2.2.1.0.10", "{i}1");
            agentProperties.setProperty("snmp4j.agent.cfg.value.1.3.6.1.6.3.15.1.2.2.1.0.11", "{s}SHADESAuthPassword");
            agentProperties.setProperty("snmp4j.agent.cfg.value.1.3.6.1.6.3.15.1.2.2.1.0.12", "{s}SHADESPrivPassword");
            agentProperties.setProperty("snmp4j.agent.cfg.value.1.3.6.1.6.3.15.1.2.2.1.0.13", "");
            agentProperties
                    .setProperty("snmp4j.agent.cfg.index.1.3.6.1.6.3.15.1.2.2.1.1", "{o}$#{1.3.6.1.6.3.10.2.1.1.0}.3.'SHA'");
            agentProperties.setProperty("snmp4j.agent.cfg.value.1.3.6.1.6.3.15.1.2.2.1.1.0", "{s}SHA");
            agentProperties.setProperty("snmp4j.agent.cfg.value.1.3.6.1.6.3.15.1.2.2.1.1.1", "{o}");
            agentProperties.setProperty("snmp4j.agent.cfg.value.1.3.6.1.6.3.15.1.2.2.1.1.2", "{o}1.3.6.1.6.3.10.1.1.3");
            agentProperties.setProperty("snmp4j.agent.cfg.value.1.3.6.1.6.3.15.1.2.2.1.1.3", "{s}");
            agentProperties.setProperty("snmp4j.agent.cfg.value.1.3.6.1.6.3.15.1.2.2.1.1.4", "{s}");
            agentProperties.setProperty("snmp4j.agent.cfg.value.1.3.6.1.6.3.15.1.2.2.1.1.5", "{o}1.3.6.1.6.3.10.1.2.1");
            agentProperties.setProperty("snmp4j.agent.cfg.value.1.3.6.1.6.3.15.1.2.2.1.1.6", "{s}");
            agentProperties.setProperty("snmp4j.agent.cfg.value.1.3.6.1.6.3.15.1.2.2.1.1.7", "{s}");
            agentProperties.setProperty("snmp4j.agent.cfg.value.1.3.6.1.6.3.15.1.2.2.1.1.8", "{s}");
            agentProperties.setProperty("snmp4j.agent.cfg.value.1.3.6.1.6.3.15.1.2.2.1.1.9", "{i}4");
            agentProperties.setProperty("snmp4j.agent.cfg.value.1.3.6.1.6.3.15.1.2.2.1.1.10", "{i}1");
            agentProperties.setProperty("snmp4j.agent.cfg.value.1.3.6.1.6.3.15.1.2.2.1.1.11", "{s}SHAAuthPassword");
            agentProperties.setProperty("snmp4j.agent.cfg.value.1.3.6.1.6.3.15.1.2.2.1.1.12", "");
            agentProperties.setProperty("snmp4j.agent.cfg.value.1.3.6.1.6.3.15.1.2.2.1.1.13", "");
            agentProperties
                    .setProperty("snmp4j.agent.cfg.index.1.3.6.1.6.3.15.1.2.2.1.2", "{o}$#{1.3.6.1.6.3.10.2.1.1.0}.5.'unsec'");
            agentProperties.setProperty("snmp4j.agent.cfg.value.1.3.6.1.6.3.15.1.2.2.1.2.0", "{s}unsec");
            agentProperties.setProperty("snmp4j.agent.cfg.value.1.3.6.1.6.3.15.1.2.2.1.2.1", "{o}");
            agentProperties.setProperty("snmp4j.agent.cfg.value.1.3.6.1.6.3.15.1.2.2.1.2.2", "{o}1.3.6.1.6.3.10.1.1.1");
            agentProperties.setProperty("snmp4j.agent.cfg.value.1.3.6.1.6.3.15.1.2.2.1.2.3", "{s}");
            agentProperties.setProperty("snmp4j.agent.cfg.value.1.3.6.1.6.3.15.1.2.2.1.2.4", "{s}");
            agentProperties.setProperty("snmp4j.agent.cfg.value.1.3.6.1.6.3.15.1.2.2.1.2.5", "{o}1.3.6.1.6.3.10.1.2.1");
            agentProperties.setProperty("snmp4j.agent.cfg.value.1.3.6.1.6.3.15.1.2.2.1.2.6", "{s}");
            agentProperties.setProperty("snmp4j.agent.cfg.value.1.3.6.1.6.3.15.1.2.2.1.2.7", "{s}");
            agentProperties.setProperty("snmp4j.agent.cfg.value.1.3.6.1.6.3.15.1.2.2.1.2.8", "{s}");
            agentProperties.setProperty("snmp4j.agent.cfg.value.1.3.6.1.6.3.15.1.2.2.1.2.9", "{i}4");
            agentProperties.setProperty("snmp4j.agent.cfg.value.1.3.6.1.6.3.15.1.2.2.1.2.10", "{i}1");
            agentProperties.setProperty("snmp4j.agent.cfg.value.1.3.6.1.6.3.15.1.2.2.1.2.11", "");
            agentProperties.setProperty("snmp4j.agent.cfg.value.1.3.6.1.6.3.15.1.2.2.1.2.12", "");
            agentProperties.setProperty("snmp4j.agent.cfg.value.1.3.6.1.6.3.15.1.2.2.1.2.13", "");

            // ----------
            // Target MIB
            // ----------

            // parameters
            agentProperties.setProperty("snmp4j.agent.cfg.oid.1.3.6.1.6.3.12.1.3.1", "1:6");
            agentProperties.setProperty("snmp4j.agent.cfg.index.1.3.6.1.6.3.12.1.3.1.0", "{o}'SNMPv2c'");
            agentProperties.setProperty("snmp4j.agent.cfg.index.1.3.6.1.6.3.12.1.3.1.0", "{o}'" + NOTIF_TARGET_PARAM_NAME + "'");
            agentProperties.setProperty("snmp4j.agent.cfg.value.1.3.6.1.6.3.12.1.3.1.0.0", "{i}1");
            agentProperties.setProperty("snmp4j.agent.cfg.value.1.3.6.1.6.3.12.1.3.1.0.1", "{i}2");
            agentProperties.setProperty("snmp4j.agent.cfg.value.1.3.6.1.6.3.12.1.3.1.0.2", "{s}public");
            agentProperties.setProperty("snmp4j.agent.cfg.value.1.3.6.1.6.3.12.1.3.1.0.3", "{i}1");
            agentProperties.setProperty("snmp4j.agent.cfg.value.1.3.6.1.6.3.12.1.3.1.0.4", "{i}4");
            agentProperties.setProperty("snmp4j.agent.cfg.value.1.3.6.1.6.3.12.1.3.1.0.5", "{i}1");

            // ----------
            // Notify MIB
            // ----------

            // Selection
            agentProperties.setProperty("snmp4j.agent.cfg.oid.1.3.6.1.6.3.13.1.1.1", "1:4");
            agentProperties.setProperty("snmp4j.agent.cfg.index.1.3.6.1.6.3.13.1.1.1.0", "{o}'unfiltered'");
            agentProperties.setProperty("snmp4j.agent.cfg.value.1.3.6.1.6.3.13.1.1.1.0.0", "{s}notify");
            agentProperties.setProperty("snmp4j.agent.cfg.value.1.3.6.1.6.3.13.1.1.1.0.1", "{i}1");
            agentProperties.setProperty("snmp4j.agent.cfg.value.1.3.6.1.6.3.13.1.1.1.0.2", "{i}4");
            agentProperties.setProperty("snmp4j.agent.cfg.value.1.3.6.1.6.3.13.1.1.1.0.3", "{i}1");

            // Filter Profiles
            agentProperties.setProperty("snmp4j.agent.cfg.oid.1.3.6.1.6.3.13.1.2.1", "1:3");
            agentProperties.setProperty("snmp4j.agent.cfg.index.1.3.6.1.6.3.13.1.2.1.0", "{o}'SNMPv2c'");
            agentProperties.setProperty("snmp4j.agent.cfg.index.1.3.6.1.6.3.13.1.2.1.0", "{o}'" + NOTIF_TARGET_PARAM_NAME + "'");
            agentProperties.setProperty("snmp4j.agent.cfg.value.1.3.6.1.6.3.13.1.2.1.0.0", "{s}kaazing");
            agentProperties.setProperty("snmp4j.agent.cfg.value.1.3.6.1.6.3.13.1.2.1.0.1", "{i}4");
            agentProperties.setProperty("snmp4j.agent.cfg.value.1.3.6.1.6.3.13.1.2.1.0.2", "{i}1");

            // Filters. Adjust the first number in the value as you add new traps
            agentProperties.setProperty("snmp4j.agent.cfg.oid.1.3.6.1.6.3.13.1.3.1", "19:4");

            //exclude authenticationFailure trap
            agentProperties.setProperty("snmp4j.agent.cfg.index.1.3.6.1.6.3.13.1.3.1.0", "{o}7.'kaazing'.1.3.6.1.6.3.1.1.5.5");
            agentProperties.setProperty("snmp4j.agent.cfg.value.1.3.6.1.6.3.13.1.3.1.0.0", "{s}");
            agentProperties.setProperty("snmp4j.agent.cfg.value.1.3.6.1.6.3.13.1.3.1.0.1", "{i}2");
            agentProperties.setProperty("snmp4j.agent.cfg.value.1.3.6.1.6.3.13.1.3.1.0.2", "{i}4");
            agentProperties.setProperty("snmp4j.agent.cfg.value.1.3.6.1.6.3.13.1.3.1.0.3", "{i}1");

            // include Kaazing notifs only (using the real Kaazing Enterprise ID)
            // gateway summary data
            agentProperties.setProperty("snmp4j.agent.cfg.index.1.3.6.1.6.3.13.1.3.1.1", "{o}7.'kaazing'.1.3.6.1.4.1.29197.2.4");
            agentProperties.setProperty("snmp4j.agent.cfg.value.1.3.6.1.6.3.13.1.3.1.1.0", "{s}");
            agentProperties.setProperty("snmp4j.agent.cfg.value.1.3.6.1.6.3.13.1.3.1.1.1", "{i}1");
            agentProperties.setProperty("snmp4j.agent.cfg.value.1.3.6.1.6.3.13.1.3.1.1.2", "{i}4");
            agentProperties.setProperty("snmp4j.agent.cfg.value.1.3.6.1.6.3.13.1.3.1.1.3", "{i}1");

            // cluster membership event
            agentProperties.setProperty("snmp4j.agent.cfg.index.1.3.6.1.6.3.13.1.3.1.2", "{o}7.'kaazing'.1.3.6.1.4.1.29197.2.5");
            agentProperties.setProperty("snmp4j.agent.cfg.value.1.3.6.1.6.3.13.1.3.1.2.0", "{s}");
            agentProperties.setProperty("snmp4j.agent.cfg.value.1.3.6.1.6.3.13.1.3.1.2.1", "{i}1");
            agentProperties.setProperty("snmp4j.agent.cfg.value.1.3.6.1.6.3.13.1.3.1.2.2", "{i}4");
            agentProperties.setProperty("snmp4j.agent.cfg.value.1.3.6.1.6.3.13.1.3.1.2.3", "{i}1");

            // management service event
            agentProperties.setProperty("snmp4j.agent.cfg.index.1.3.6.1.6.3.13.1.3.1.3", "{o}7.'kaazing'.1.3.6.1.4.1.29197.2.8");
            agentProperties.setProperty("snmp4j.agent.cfg.value.1.3.6.1.6.3.13.1.3.1.3.0", "{s}");
            agentProperties.setProperty("snmp4j.agent.cfg.value.1.3.6.1.6.3.13.1.3.1.3.1", "{i}1");
            agentProperties.setProperty("snmp4j.agent.cfg.value.1.3.6.1.6.3.13.1.3.1.3.2", "{i}4");
            agentProperties.setProperty("snmp4j.agent.cfg.value.1.3.6.1.6.3.13.1.3.1.3.3", "{i}1");

            // balancer map event
            agentProperties
                    .setProperty("snmp4j.agent.cfg.index.1.3.6.1.6.3.13.1.3.1.4", "{o}7.'kaazing'.1.3.6.1.4.1.29197.2.11");
            agentProperties.setProperty("snmp4j.agent.cfg.value.1.3.6.1.6.3.13.1.3.1.4.0", "{s}");
            agentProperties.setProperty("snmp4j.agent.cfg.value.1.3.6.1.6.3.13.1.3.1.4.1", "{i}1");
            agentProperties.setProperty("snmp4j.agent.cfg.value.1.3.6.1.6.3.13.1.3.1.4.2", "{i}4");
            agentProperties.setProperty("snmp4j.agent.cfg.value.1.3.6.1.6.3.13.1.3.1.4.3", "{i}1");

            // service current sessions
            agentProperties
                    .setProperty("snmp4j.agent.cfg.index.1.3.6.1.6.3.13.1.3.1.5", "{o}7.'kaazing'.1.3.6.1.4.1.29197.3.1.2");
            agentProperties.setProperty("snmp4j.agent.cfg.value.1.3.6.1.6.3.13.1.3.1.5.0", "{s}");
            agentProperties.setProperty("snmp4j.agent.cfg.value.1.3.6.1.6.3.13.1.3.1.5.1", "{i}1");
            agentProperties.setProperty("snmp4j.agent.cfg.value.1.3.6.1.6.3.13.1.3.1.5.2", "{i}4");
            agentProperties.setProperty("snmp4j.agent.cfg.value.1.3.6.1.6.3.13.1.3.1.5.3", "{i}1");

            // service total sessions
            agentProperties
                    .setProperty("snmp4j.agent.cfg.index.1.3.6.1.6.3.13.1.3.1.6", "{o}7.'kaazing'.1.3.6.1.4.1.29197.3.1.3");
            agentProperties.setProperty("snmp4j.agent.cfg.value.1.3.6.1.6.3.13.1.3.1.6.0", "{s}");
            agentProperties.setProperty("snmp4j.agent.cfg.value.1.3.6.1.6.3.13.1.3.1.6.1", "{i}1");
            agentProperties.setProperty("snmp4j.agent.cfg.value.1.3.6.1.6.3.13.1.3.1.6.2", "{i}4");
            agentProperties.setProperty("snmp4j.agent.cfg.value.1.3.6.1.6.3.13.1.3.1.6.3", "{i}1");

            // service bytes received
            agentProperties
                    .setProperty("snmp4j.agent.cfg.index.1.3.6.1.6.3.13.1.3.1.7", "{o}7.'kaazing'.1.3.6.1.4.1.29197.3.1.4");
            agentProperties.setProperty("snmp4j.agent.cfg.value.1.3.6.1.6.3.13.1.3.1.7.0", "{s}");
            agentProperties.setProperty("snmp4j.agent.cfg.value.1.3.6.1.6.3.13.1.3.1.7.1", "{i}1");
            agentProperties.setProperty("snmp4j.agent.cfg.value.1.3.6.1.6.3.13.1.3.1.7.2", "{i}4");
            agentProperties.setProperty("snmp4j.agent.cfg.value.1.3.6.1.6.3.13.1.3.1.7.3", "{i}1");

            // service bytes sent
            agentProperties
                    .setProperty("snmp4j.agent.cfg.index.1.3.6.1.6.3.13.1.3.1.8", "{o}7.'kaazing'.1.3.6.1.4.1.29197.3.1.5");
            agentProperties.setProperty("snmp4j.agent.cfg.value.1.3.6.1.6.3.13.1.3.1.8.0", "{s}");
            agentProperties.setProperty("snmp4j.agent.cfg.value.1.3.6.1.6.3.13.1.3.1.8.1", "{i}1");
            agentProperties.setProperty("snmp4j.agent.cfg.value.1.3.6.1.6.3.13.1.3.1.8.2", "{i}4");
            agentProperties.setProperty("snmp4j.agent.cfg.value.1.3.6.1.6.3.13.1.3.1.8.3", "{i}1");

            // service summary data
            agentProperties.setProperty("snmp4j.agent.cfg.index.1.3.6.1.6.3.13.1.3.1.9", "{o}7.'kaazing'.1.3.6.1.4.1.29197.3.4");
            agentProperties.setProperty("snmp4j.agent.cfg.value.1.3.6.1.6.3.13.1.3.1.9.0", "{s}");
            agentProperties.setProperty("snmp4j.agent.cfg.value.1.3.6.1.6.3.13.1.3.1.9.1", "{i}1");
            agentProperties.setProperty("snmp4j.agent.cfg.value.1.3.6.1.6.3.13.1.3.1.9.2", "{i}4");
            agentProperties.setProperty("snmp4j.agent.cfg.value.1.3.6.1.6.3.13.1.3.1.9.3", "{i}1");

            // service connection (sessionOpened)
            agentProperties
                    .setProperty("snmp4j.agent.cfg.index.1.3.6.1.6.3.13.1.3.1.10", "{o}7.'kaazing'.1.3.6.1.4.1.29197.3.5");
            agentProperties.setProperty("snmp4j.agent.cfg.value.1.3.6.1.6.3.13.1.3.1.10.0", "{s}");
            agentProperties.setProperty("snmp4j.agent.cfg.value.1.3.6.1.6.3.13.1.3.1.10.1", "{i}1");
            agentProperties.setProperty("snmp4j.agent.cfg.value.1.3.6.1.6.3.13.1.3.1.10.2", "{i}4");
            agentProperties.setProperty("snmp4j.agent.cfg.value.1.3.6.1.6.3.13.1.3.1.10.3", "{i}1");

            // service disconnection (sessionClosed)
            agentProperties
                    .setProperty("snmp4j.agent.cfg.index.1.3.6.1.6.3.13.1.3.1.11", "{o}7.'kaazing'.1.3.6.1.4.1.29197.3.6");
            agentProperties.setProperty("snmp4j.agent.cfg.value.1.3.6.1.6.3.13.1.3.1.11.0", "{s}");
            agentProperties.setProperty("snmp4j.agent.cfg.value.1.3.6.1.6.3.13.1.3.1.11.1", "{i}1");
            agentProperties.setProperty("snmp4j.agent.cfg.value.1.3.6.1.6.3.13.1.3.1.11.2", "{i}4");
            agentProperties.setProperty("snmp4j.agent.cfg.value.1.3.6.1.6.3.13.1.3.1.11.3", "{i}1");

            // session summary data
            agentProperties
                    .setProperty("snmp4j.agent.cfg.index.1.3.6.1.6.3.13.1.3.1.12", "{o}7.'kaazing'.1.3.6.1.4.1.29197.4.4");
            agentProperties.setProperty("snmp4j.agent.cfg.value.1.3.6.1.6.3.13.1.3.1.12.0", "{s}");
            agentProperties.setProperty("snmp4j.agent.cfg.value.1.3.6.1.6.3.13.1.3.1.12.1", "{i}1");
            agentProperties.setProperty("snmp4j.agent.cfg.value.1.3.6.1.6.3.13.1.3.1.12.2", "{i}4");
            agentProperties.setProperty("snmp4j.agent.cfg.value.1.3.6.1.6.3.13.1.3.1.12.3", "{i}1");

            // session message received notification
            agentProperties
                    .setProperty("snmp4j.agent.cfg.index.1.3.6.1.6.3.13.1.3.1.13", "{o}7.'kaazing'.1.3.6.1.4.1.29197.4.5");
            agentProperties.setProperty("snmp4j.agent.cfg.value.1.3.6.1.6.3.13.1.3.1.13.0", "{s}");
            agentProperties.setProperty("snmp4j.agent.cfg.value.1.3.6.1.6.3.13.1.3.1.13.1", "{i}1");
            agentProperties.setProperty("snmp4j.agent.cfg.value.1.3.6.1.6.3.13.1.3.1.13.2", "{i}4");
            agentProperties.setProperty("snmp4j.agent.cfg.value.1.3.6.1.6.3.13.1.3.1.13.3", "{i}1");

            // session message sent notification
            agentProperties
                    .setProperty("snmp4j.agent.cfg.index.1.3.6.1.6.3.13.1.3.1.14", "{o}7.'kaazing'.1.3.6.1.4.1.29197.4.6");
            agentProperties.setProperty("snmp4j.agent.cfg.value.1.3.6.1.6.3.13.1.3.1.14.0", "{s}");
            agentProperties.setProperty("snmp4j.agent.cfg.value.1.3.6.1.6.3.13.1.3.1.14.1", "{i}1");
            agentProperties.setProperty("snmp4j.agent.cfg.value.1.3.6.1.6.3.13.1.3.1.14.2", "{i}4");
            agentProperties.setProperty("snmp4j.agent.cfg.value.1.3.6.1.6.3.13.1.3.1.14.3", "{i}1");

            // system summary data (now split off from CPU & NIC data)
            agentProperties
                    .setProperty("snmp4j.agent.cfg.index.1.3.6.1.6.3.13.1.3.1.15", "{o}7.'kaazing'.1.3.6.1.4.1.29197.5.33");
            agentProperties.setProperty("snmp4j.agent.cfg.value.1.3.6.1.6.3.13.1.3.1.15.0", "{s}");
            agentProperties.setProperty("snmp4j.agent.cfg.value.1.3.6.1.6.3.13.1.3.1.15.1", "{i}1");
            agentProperties.setProperty("snmp4j.agent.cfg.value.1.3.6.1.6.3.13.1.3.1.15.2", "{i}4");
            agentProperties.setProperty("snmp4j.agent.cfg.value.1.3.6.1.6.3.13.1.3.1.15.3", "{i}1");

            // JVM summary data, but accessed in the "system management" area
            agentProperties
                    .setProperty("snmp4j.agent.cfg.index.1.3.6.1.6.3.13.1.3.1.16", "{o}7.'kaazing'.1.3.6.1.4.1.29197.5.43");
            agentProperties.setProperty("snmp4j.agent.cfg.value.1.3.6.1.6.3.13.1.3.1.16.0", "{s}");
            agentProperties.setProperty("snmp4j.agent.cfg.value.1.3.6.1.6.3.13.1.3.1.16.1", "{i}1");
            agentProperties.setProperty("snmp4j.agent.cfg.value.1.3.6.1.6.3.13.1.3.1.16.2", "{i}4");
            agentProperties.setProperty("snmp4j.agent.cfg.value.1.3.6.1.6.3.13.1.3.1.16.3", "{i}1");

            // CPU list summary data
            agentProperties
                    .setProperty("snmp4j.agent.cfg.index.1.3.6.1.6.3.13.1.3.1.17", "{o}7.'kaazing'.1.3.6.1.4.1.29197.6.6");
            agentProperties.setProperty("snmp4j.agent.cfg.value.1.3.6.1.6.3.13.1.3.1.17.0", "{s}");
            agentProperties.setProperty("snmp4j.agent.cfg.value.1.3.6.1.6.3.13.1.3.1.17.1", "{i}1");
            agentProperties.setProperty("snmp4j.agent.cfg.value.1.3.6.1.6.3.13.1.3.1.17.2", "{i}4");
            agentProperties.setProperty("snmp4j.agent.cfg.value.1.3.6.1.6.3.13.1.3.1.17.3", "{i}1");

            // NIC list summary data
            agentProperties
                    .setProperty("snmp4j.agent.cfg.index.1.3.6.1.6.3.13.1.3.1.18", "{o}7.'kaazing'.1.3.6.1.4.1.29197.7.6");
            agentProperties.setProperty("snmp4j.agent.cfg.value.1.3.6.1.6.3.13.1.3.1.18.0", "{s}");
            agentProperties.setProperty("snmp4j.agent.cfg.value.1.3.6.1.6.3.13.1.3.1.18.1", "{i}1");
            agentProperties.setProperty("snmp4j.agent.cfg.value.1.3.6.1.6.3.13.1.3.1.18.2", "{i}4");
            agentProperties.setProperty("snmp4j.agent.cfg.value.1.3.6.1.6.3.13.1.3.1.18.3", "{i}1");

            tableSizeLimits = new Properties();
//
//            // Generally all tables should have max 100 entries
//            tableSizeLimits.setProperty("snmp4j.tableSizeLimit.1.3.6.1", "1000");
//
//            // Allow only 16 USM users:
//            tableSizeLimits.setProperty("snmp4j.tableSizeLimit.1.3.6.1.6.3.15.1.2.2.1", "16");
//
//            // Allow only 10 targets:
//            tableSizeLimits.setProperty("snmp4j.tableSizeLimit.1.3.6.1.6.3.12.1.2.1", "10");
//
            // Limit notification log to 200
            tableSizeLimits.setProperty("snmp4j.tableSizeLimit.1.3.6.1.2.1.92", "200");
        }

        private static Properties getProperties() {
            return agentProperties;
        }

        private static Properties getTableSizeLimits() {
            return tableSizeLimits;
        }
    }
}

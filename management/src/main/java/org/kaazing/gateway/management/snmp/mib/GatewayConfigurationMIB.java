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

package org.kaazing.gateway.management.snmp.mib;

import java.util.List;
import org.kaazing.gateway.management.Utils;
import org.kaazing.gateway.management.config.ClusterConfigurationBean;
import org.kaazing.gateway.management.config.NetworkConfigurationBean;
import org.kaazing.gateway.management.config.RealmConfigurationBean;
import org.kaazing.gateway.management.config.SecurityConfigurationBean;
import org.kaazing.gateway.management.config.ServiceConfigurationBean;
import org.kaazing.gateway.management.config.ServiceDefaultsConfigurationBean;
import org.kaazing.gateway.management.gateway.GatewayManagementBean;
import org.snmp4j.agent.DuplicateRegistrationException;
import org.snmp4j.agent.MOGroup;
import org.snmp4j.agent.MOServer;
import org.snmp4j.agent.mo.DefaultMOMutableRow2PC;
import org.snmp4j.agent.mo.DefaultMOMutableTableModel;
import org.snmp4j.agent.mo.MOAccessImpl;
import org.snmp4j.agent.mo.MOColumn;
import org.snmp4j.agent.mo.MOFactory;
import org.snmp4j.agent.mo.MOMutableColumn;
import org.snmp4j.agent.mo.MOScalar;
import org.snmp4j.agent.mo.MOTable;
import org.snmp4j.agent.mo.MOTableIndex;
import org.snmp4j.agent.mo.MOTableSubIndex;
import org.snmp4j.smi.OID;
import org.snmp4j.smi.OctetString;
import org.snmp4j.smi.SMIConstants;
import org.snmp4j.smi.Variable;

/**
 * MIB support for Gateway-level configuration data.
 * <p/>
 * Kaazing's SNMP support is based on the SNMP4J open-source library under the Apache 2.0 license. To see the full text of the
 * license, please see the Kaazing third-party licenses file.
 */
public class GatewayConfigurationMIB implements MOGroup {


    private MOScalar clusterName;
    private MOScalar clusterAccepts;
    private MOScalar clusterConnects;
    private MOScalar clusterConnectOptions;

    private MOScalar networkAddressMapping;

    private MOScalar keyStoreType;
    private MOScalar keystoreCertificateInfo;
    private MOScalar trustStoreType;
    private MOScalar truststoreCertificateInfo;

    private MOTable realmTable;
    private MOTable serviceTable;

    private MOScalar serviceDefaultsAcceptOptions;
    private MOScalar serviceDefaultsMimeMappings;
    // TODO: add connect-options to service-defaults
//  private MOScalar serviceDefaultsConnectOptions;

    // extra fields that really aren't part of the config, but are fixed
    // per gateway, so should be reported here.
    private MOScalar versionInfoProductTitle;
    private MOScalar versionInfoProductBuild;
    private MOScalar versionInfoProductEdition;

    public GatewayConfigurationMIB(MOFactory factory) {
        createClusterConfig(factory);
        createNetworkConfig(factory);
        createSecurityConfig(factory);
        createServiceTable(factory);
        createServiceDefaults(factory);
        createVersionInfo(factory);
    }

    private void createClusterConfig(MOFactory moFactory) {
        clusterName = new MOScalar(MIBConstants.oidClusterName,
                moFactory.createAccess(MOAccessImpl.ACCESSIBLE_FOR_READ),
                new OctetString(""));
        clusterAccepts = new MOScalar(MIBConstants.oidClusterAccepts,
                moFactory.createAccess(MOAccessImpl.ACCESSIBLE_FOR_READ),
                new OctetString(""));
        clusterConnects = new MOScalar(MIBConstants.oidClusterConnects,
                moFactory.createAccess(MOAccessImpl.ACCESSIBLE_FOR_READ),
                new OctetString(""));
        clusterConnectOptions = new MOScalar(MIBConstants.oidClusterConnectOptions,
                moFactory.createAccess(MOAccessImpl.ACCESSIBLE_FOR_READ),
                new OctetString(""));
    }

    private void createNetworkConfig(MOFactory moFactory) {
        networkAddressMapping = new MOScalar(MIBConstants.oidNetworkConfigAddressMappings,
                moFactory.createAccess(MOAccessImpl.ACCESSIBLE_FOR_READ),
                new OctetString(""));
    }

    private void createSecurityConfig(MOFactory moFactory) {
        keyStoreType = new MOScalar(MIBConstants.oidSecurityKeystoreType,
                moFactory.createAccess(MOAccessImpl.ACCESSIBLE_FOR_READ),
                new OctetString(""));
        keystoreCertificateInfo = new MOScalar(MIBConstants.oidSecurityKeystoreCertificateInfo,
                moFactory.createAccess(MOAccessImpl.ACCESSIBLE_FOR_READ),
                new OctetString(""));
        trustStoreType = new MOScalar(MIBConstants.oidSecurityTruststoreType,
                moFactory.createAccess(MOAccessImpl.ACCESSIBLE_FOR_READ),
                new OctetString(""));
        truststoreCertificateInfo = new MOScalar(MIBConstants.oidSecurityTruststoreCertificateInfo,
                moFactory.createAccess(MOAccessImpl.ACCESSIBLE_FOR_READ),
                new OctetString(""));

        createRealmTable(moFactory);
    }

    private void createRealmTable(MOFactory moFactory) {
        // Index definition
        OID realmConfigEntryOID = ((OID) MIBConstants.oidRealmConfig.clone()).append(1);
        OID realmConfigEntryIndexOID = ((OID) realmConfigEntryOID.clone()).append(1);
        MOTableSubIndex[] realmConfigEntryIndexes =
                new MOTableSubIndex[]{
                        moFactory.createSubIndex(realmConfigEntryIndexOID,
                                SMIConstants.SYNTAX_INTEGER, 1, 1),
                };

        MOTableIndex realmConfigEntryIndex =
                moFactory.createIndex(realmConfigEntryIndexes, true);

        // Columns
        MOColumn[] realmConfigColumns = new MOColumn[MIBConstants.REALM_COLUMN_COUNT];

        // name
        realmConfigColumns[MIBConstants.realmConfigNameIndex] =
                new MOMutableColumn(MIBConstants.realmConfigName,
                        SMIConstants.SYNTAX_OCTET_STRING,
                        moFactory.createAccess(MOAccessImpl.ACCESSIBLE_FOR_READ));

        // description
        realmConfigColumns[MIBConstants.realmConfigDescriptionIndex] =
                new MOMutableColumn(MIBConstants.realmConfigDescription,
                        SMIConstants.SYNTAX_OCTET_STRING,
                        moFactory.createAccess(MOAccessImpl.ACCESSIBLE_FOR_READ));

        // user-principal-classes
        realmConfigColumns[MIBConstants.realmConfigUserPrincipalClassesIndex] =
                new MOMutableColumn(MIBConstants.realmConfigUserPrincipalClasses,
                        SMIConstants.SYNTAX_OCTET_STRING,
                        moFactory.createAccess(MOAccessImpl.ACCESSIBLE_FOR_READ));

        // HTTP challenge scheme
        realmConfigColumns[MIBConstants.realmConfigHttpChallengeSchemeIndex] =
                new MOMutableColumn(MIBConstants.realmConfigHttpChallengeScheme,
                        SMIConstants.SYNTAX_OCTET_STRING,
                        moFactory.createAccess(MOAccessImpl.ACCESSIBLE_FOR_READ));

        // HTTP headers
        realmConfigColumns[MIBConstants.realmConfigHttpHeadersIndex] =
                new MOMutableColumn(MIBConstants.realmConfigHttpHeaders,
                        SMIConstants.SYNTAX_OCTET_STRING,
                        moFactory.createAccess(MOAccessImpl.ACCESSIBLE_FOR_READ));

        // HTTP query params
        realmConfigColumns[MIBConstants.realmConfigQueryParamsIndex] =
                new MOMutableColumn(MIBConstants.realmConfigQueryParams,
                        SMIConstants.SYNTAX_OCTET_STRING,
                        moFactory.createAccess(MOAccessImpl.ACCESSIBLE_FOR_READ));

        // HTTP cookie names
        realmConfigColumns[MIBConstants.realmConfigCookieNamesIndex] =
                new MOMutableColumn(MIBConstants.realmConfigCookieNames,
                        SMIConstants.SYNTAX_OCTET_STRING,
                        moFactory.createAccess(MOAccessImpl.ACCESSIBLE_FOR_READ));

        // authorization mode
        realmConfigColumns[MIBConstants.realmConfigAuthorizationModeIndex] =
                new MOMutableColumn(MIBConstants.realmConfigAuthorizationMode,
                        SMIConstants.SYNTAX_OCTET_STRING,
                        moFactory.createAccess(MOAccessImpl.ACCESSIBLE_FOR_READ));

        // session timeout
        realmConfigColumns[MIBConstants.realmConfigSessionTimeoutIndex] =
                new MOMutableColumn(MIBConstants.realmConfigSessionTimeout,
                        SMIConstants.SYNTAX_OCTET_STRING,
                        moFactory.createAccess(MOAccessImpl.ACCESSIBLE_FOR_READ));

        // login modules
        realmConfigColumns[MIBConstants.realmConfigLoginModulesIndex] =
                new MOMutableColumn(MIBConstants.realmConfigLoginModules,
                        SMIConstants.SYNTAX_OCTET_STRING,
                        moFactory.createAccess(MOAccessImpl.ACCESSIBLE_FOR_READ));

        // Table model
        realmTable = moFactory.createTable(realmConfigEntryOID,
                realmConfigEntryIndex,
                realmConfigColumns,
                new RealmConfigurationTableModel());
    }

    private void createServiceTable(MOFactory moFactory) {
        // Index definition
        OID serviceConfigEntryOID = ((OID) MIBConstants.oidServiceConfig.clone()).append(1);
        OID serviceConfigEntryIndexOID = ((OID) serviceConfigEntryOID.clone()).append(1);
        MOTableSubIndex[] serviceConfigEntryIndexes =
                new MOTableSubIndex[]{
                        moFactory.createSubIndex(serviceConfigEntryIndexOID,
                                SMIConstants.SYNTAX_INTEGER, 1, 1),
                };

        MOTableIndex serviceConfigEntryIndex =
                moFactory.createIndex(serviceConfigEntryIndexes, true);

        // Columns
        MOColumn[] serviceConfigColumns = new MOColumn[MIBConstants.SERVICE_CONFIG_COLUMN_COUNT];

        // type
        serviceConfigColumns[MIBConstants.serviceConfigTypeIndex] =
                new MOMutableColumn(MIBConstants.serviceConfigType,
                        SMIConstants.SYNTAX_OCTET_STRING,
                        moFactory.createAccess(MOAccessImpl.ACCESSIBLE_FOR_READ));

        // name
        serviceConfigColumns[MIBConstants.serviceConfigNameIndex] =
                new MOMutableColumn(MIBConstants.serviceConfigName,
                        SMIConstants.SYNTAX_OCTET_STRING,
                        moFactory.createAccess(MOAccessImpl.ACCESSIBLE_FOR_READ));

        // description
        serviceConfigColumns[MIBConstants.serviceConfigDescriptionIndex] =
                new MOMutableColumn(MIBConstants.serviceConfigDescription,
                        SMIConstants.SYNTAX_OCTET_STRING,
                        moFactory.createAccess(MOAccessImpl.ACCESSIBLE_FOR_READ));

        // accepts
        serviceConfigColumns[MIBConstants.serviceConfigAcceptsIndex] =
                new MOMutableColumn(MIBConstants.serviceConfigAccepts,
                        SMIConstants.SYNTAX_OCTET_STRING,
                        moFactory.createAccess(MOAccessImpl.ACCESSIBLE_FOR_READ));

        // accept options
        serviceConfigColumns[MIBConstants.serviceConfigAcceptOptionsIndex] =
                new MOMutableColumn(MIBConstants.serviceConfigAcceptOptions,
                        SMIConstants.SYNTAX_OCTET_STRING,
                        moFactory.createAccess(MOAccessImpl.ACCESSIBLE_FOR_READ));

        // balances
        serviceConfigColumns[MIBConstants.serviceConfigBalancesIndex] =
                new MOMutableColumn(MIBConstants.serviceConfigBalances,
                        SMIConstants.SYNTAX_OCTET_STRING,
                        moFactory.createAccess(MOAccessImpl.ACCESSIBLE_FOR_READ));

        // connects
        serviceConfigColumns[MIBConstants.serviceConfigConnectsIndex] =
                new MOMutableColumn(MIBConstants.serviceConfigConnects,
                        SMIConstants.SYNTAX_OCTET_STRING,
                        moFactory.createAccess(MOAccessImpl.ACCESSIBLE_FOR_READ));

        // connect options
        serviceConfigColumns[MIBConstants.serviceConfigConnectOptionsIndex] =
                new MOMutableColumn(MIBConstants.serviceConfigConnectOptions,
                        SMIConstants.SYNTAX_OCTET_STRING,
                        moFactory.createAccess(MOAccessImpl.ACCESSIBLE_FOR_READ));

        // cross-site-constraints
        serviceConfigColumns[MIBConstants.serviceConfigCrossSiteConstraintsIndex] =
                new MOMutableColumn(MIBConstants.serviceConfigCrossSiteConstraints,
                        SMIConstants.SYNTAX_OCTET_STRING,
                        moFactory.createAccess(MOAccessImpl.ACCESSIBLE_FOR_READ));

        // properties
        serviceConfigColumns[MIBConstants.serviceConfigPropertiesIndex] =
                new MOMutableColumn(MIBConstants.serviceConfigProperties,
                        SMIConstants.SYNTAX_OCTET_STRING,
                        moFactory.createAccess(MOAccessImpl.ACCESSIBLE_FOR_READ));

        // required roles
        serviceConfigColumns[MIBConstants.serviceConfigRequiredRolesIndex] =
                new MOMutableColumn(MIBConstants.serviceConfigRequiredRoles,
                        SMIConstants.SYNTAX_OCTET_STRING,
                        moFactory.createAccess(MOAccessImpl.ACCESSIBLE_FOR_READ));

        // realm
        serviceConfigColumns[MIBConstants.serviceConfigRealmIndex] =
                new MOMutableColumn(MIBConstants.serviceConfigRealm,
                        SMIConstants.SYNTAX_OCTET_STRING,
                        moFactory.createAccess(MOAccessImpl.ACCESSIBLE_FOR_READ));

        // mime mappings
        serviceConfigColumns[MIBConstants.serviceConfigMimeMappingsIndex] =
                new MOMutableColumn(MIBConstants.serviceConfigMimeMappings,
                        SMIConstants.SYNTAX_OCTET_STRING,
                        moFactory.createAccess(MOAccessImpl.ACCESSIBLE_FOR_READ));

        // Table model
        serviceTable = moFactory.createTable(serviceConfigEntryOID,
                serviceConfigEntryIndex,
                serviceConfigColumns,
                new ServiceConfigurationTableModel());
    }

    private void createServiceDefaults(MOFactory moFactory) {
        serviceDefaultsAcceptOptions = new MOScalar(MIBConstants.oidServiceDefaultsAcceptOptions,
                moFactory.createAccess(MOAccessImpl.ACCESSIBLE_FOR_READ),
                new OctetString(""));
        serviceDefaultsMimeMappings = new MOScalar(MIBConstants.oidServiceDefaultsMimeMappings,
                moFactory.createAccess(MOAccessImpl.ACCESSIBLE_FOR_READ),
                new OctetString(""));
     // TODO: add connect-options to service-defaults
//        serviceDefaultsConnectOptions = new MOScalar(MIBConstants.oidServiceDefaultsConnectOptions,
//                moFactory.createAccess(MOAccessImpl.ACCESSIBLE_FOR_READ),
//                new OctetString(""));
    }

    private void createVersionInfo(MOFactory moFactory) {
        versionInfoProductTitle = new MOScalar(MIBConstants.oidVersionInfoProductTitle,
                moFactory.createAccess(MOAccessImpl.ACCESSIBLE_FOR_READ),
                new OctetString(""));
        versionInfoProductBuild = new MOScalar(MIBConstants.oidVersionInfoProductBuild,
                moFactory.createAccess(MOAccessImpl.ACCESSIBLE_FOR_READ),
                new OctetString(""));
        versionInfoProductEdition = new MOScalar(MIBConstants.oidVersionInfoProductEdition,
                moFactory.createAccess(MOAccessImpl.ACCESSIBLE_FOR_READ),
                new OctetString(""));
    }

    @Override
    public void registerMOs(MOServer server, OctetString context) throws DuplicateRegistrationException {
        server.register(clusterName, context);
        server.register(clusterAccepts, context);
        server.register(clusterConnects, context);
        server.register(clusterConnectOptions, context);
        server.register(networkAddressMapping, context);
        server.register(keyStoreType, context);
        server.register(keystoreCertificateInfo, context);
        server.register(trustStoreType, context);
        server.register(truststoreCertificateInfo, context);
        server.register(realmTable, context);
        server.register(serviceTable, context);
        server.register(serviceDefaultsAcceptOptions, context);
        server.register(serviceDefaultsMimeMappings, context);
        // TODO: add connect-options to service-defaults
//        server.register(serviceDefaultsConnectOptions, context);
        server.register(versionInfoProductTitle, context);
        server.register(versionInfoProductBuild, context);
        server.register(versionInfoProductEdition, context);
    }

    @Override
    public void unregisterMOs(MOServer server, OctetString context) {
        server.unregister(clusterName, context);
        server.unregister(clusterAccepts, context);
        server.unregister(clusterConnects, context);
        server.unregister(clusterConnectOptions, context);
        server.unregister(networkAddressMapping, context);
        server.unregister(keyStoreType, context);
        server.unregister(keystoreCertificateInfo, context);
        server.unregister(trustStoreType, context);
        server.unregister(truststoreCertificateInfo, context);
        server.unregister(realmTable, context);
        server.unregister(serviceTable, context);
        server.unregister(serviceDefaultsAcceptOptions, context);
        server.unregister(serviceDefaultsMimeMappings, context);
        // TODO: add connect-options to service-defaults
//        server.unregister(serviceDefaultsConnectOptions, context);
        server.unregister(versionInfoProductTitle, context);
        server.unregister(versionInfoProductBuild, context);
        server.unregister(versionInfoProductEdition, context);
    }

    public OID addClusterConfiguration(ClusterConfigurationBean bean) {
        clusterName.setValue(Utils.stringToVariable(bean.getName()));

        StringBuffer sb = new StringBuffer();
        List<String> accepts = bean.getAccepts();
        if (accepts != null) {
            for (String accept : accepts) {
                sb.append(accept + '\n');
            }
        }
        clusterAccepts.setValue(Utils.stringToVariable(sb.toString().trim()));

        sb = new StringBuffer();
        List<String> connects = bean.getConnects();
        if (connects != null) {
            for (String connect : connects) {
                sb.append(connect + '\n');
            }
        }
        clusterConnects.setValue(Utils.stringToVariable(sb.toString().trim()));

        clusterConnectOptions.setValue(Utils.stringToVariable(bean.getConnectOptions()));

        return MIBConstants.oidClusterConfig;
    }

//    public void removeClusterConfigurationBean(OID oid) {
//        clusterTable.removeRow(oid);
//    }

    public OID addNetworkConfiguration(NetworkConfigurationBean bean) {
        networkAddressMapping.setValue(Utils.stringToVariable(bean.getAddressMappings()));

        return MIBConstants.oidNetworkConfigAddressMappings;
    }

//    public void removeNetworkConfigurationBean(OID oid) {
//        networkTable.removeRow(oid);
//    }

    public OID addSecurityConfiguration(SecurityConfigurationBean securityBean) {
        keyStoreType.setValue(new OctetString(securityBean.getKeystoreType()));
        keystoreCertificateInfo.setValue(new OctetString(securityBean.getKeystoreCertificateInfo()));
        trustStoreType.setValue(new OctetString(securityBean.getTruststoreType()));
        truststoreCertificateInfo.setValue(new OctetString(securityBean.getTruststoreCertificateInfo()));

        return MIBConstants.oidSecurityConfig;
    }

    public OID addRealmConfiguration(RealmConfigurationBean bean) {
        // Note: if services are appearing and disappearing, we cannot just
        // have a service index as the current number of rows in the table,
        // because earlier service entries may be gone.
        GatewayManagementBean gatewayBean = bean.getGatewayManagementBean();
        OID realmConfigurationIndexOID = new OID(new int[]{gatewayBean.getId(), bean.getId()});
        realmTable.addRow(new RealmConfigurationEntryRow(realmConfigurationIndexOID, bean));

        return realmConfigurationIndexOID;
    }

    public void removeRealmConfigurationBean(OID oid) {
        realmTable.removeRow(oid);
    }

    public OID addServiceConfiguration(ServiceConfigurationBean bean) {
        // Note: if services are appearing and disappearing, we cannot just
        // have a service index as the current number of rows in the table,
        // because earlier service entries may be gone.
        GatewayManagementBean gatewayBean = bean.getGatewayManagementBean();
        OID serviceConfigurationIndexOID = new OID(new int[]{gatewayBean.getId(), bean.getId()});
        serviceTable.addRow(new ServiceConfigurationEntryRow(serviceConfigurationIndexOID, bean));

        return serviceConfigurationIndexOID;
    }

    public void removeServiceConfigurationBean(OID oid) {
        serviceTable.removeRow(oid);
    }

    public OID addServiceDefaultsConfiguration(ServiceDefaultsConfigurationBean bean) {
        serviceDefaultsAcceptOptions.setValue(Utils.stringToVariable(bean.getAcceptOptions()));
        serviceDefaultsMimeMappings.setValue(Utils.stringToVariable(bean.getMimeMappings()));
        // TODO: add connect-options to service-defaults
//        serviceDefaultsConnectOptions.setValue(Utils.stringToVariable(bean.getConnectOptions()));

        return MIBConstants.oidServiceDefaults;
    }

    public void addVersionInfo(GatewayManagementBean gatewayBean) {
        versionInfoProductTitle.setValue(Utils.stringToVariable(gatewayBean.getProductTitle()));
        versionInfoProductBuild.setValue(Utils.stringToVariable(gatewayBean.getProductBuild()));
        versionInfoProductEdition.setValue(Utils.stringToVariable(gatewayBean.getProductEdition()));
    }

    private class RealmConfigurationTableModel extends DefaultMOMutableTableModel {
    }

    private final class RealmConfigurationEntryRow extends DefaultMOMutableRow2PC {
        private RealmConfigurationBean bean;

        private RealmConfigurationEntryRow(OID index, RealmConfigurationBean bean) {
            super(index, null);
            this.bean = bean;
        }

        @Override
        public int size() {
            return MIBConstants.REALM_COLUMN_COUNT;
        }

        @Override
        public Variable getValue(int column) {
            switch (column) {
                case MIBConstants.realmConfigNameIndex: // name
                    return Utils.stringToVariable(bean.getName());
                case MIBConstants.realmConfigDescriptionIndex: // description
                    return Utils.stringToVariable(bean.getDescription());
                case MIBConstants.realmConfigUserPrincipalClassesIndex: // user-principal-classes
                    return Utils.stringToVariable(bean.getUserPrincipalClasses());
                case MIBConstants.realmConfigHttpChallengeSchemeIndex: // HTTP challenge scheme
                    return Utils.stringToVariable(bean.getHTTPChallengeScheme());
                case MIBConstants.realmConfigHttpHeadersIndex: // HTTP headers
                    return Utils.stringToVariable(bean.getHTTPHeaders());
                case MIBConstants.realmConfigQueryParamsIndex: // HTTP query params
                    return Utils.stringToVariable(bean.getHTTPQueryParameters());
                case MIBConstants.realmConfigCookieNamesIndex: // HTTP cookie names
                    return Utils.stringToVariable(bean.getHTTPCookieNames());
                case MIBConstants.realmConfigAuthorizationModeIndex: // authorization mode
                    return Utils.stringToVariable(bean.getAuthorizationMode());
                case MIBConstants.realmConfigSessionTimeoutIndex: // session timeout
                    return Utils.stringToVariable(bean.getSessionTimeout());
                case MIBConstants.realmConfigLoginModulesIndex: // configuration entries
                    return Utils.stringToVariable(bean.getLoginModules());
                default:
                    throw new RuntimeException("Unknown realm table column: " + column);
            }
        }
    }

    private class ServiceConfigurationTableModel extends DefaultMOMutableTableModel {
    }

    private final class ServiceConfigurationEntryRow extends DefaultMOMutableRow2PC {
        private ServiceConfigurationBean bean;

        private ServiceConfigurationEntryRow(OID index, ServiceConfigurationBean bean) {
            super(index, null);
            this.bean = bean;
        }

        @Override
        public int size() {
            return MIBConstants.SERVICE_CONFIG_COLUMN_COUNT;
        }

        @Override
        public Variable getValue(int column) {
            switch (column) {
                case MIBConstants.serviceConfigTypeIndex:
                    return Utils.stringToVariable(bean.getType());
                case MIBConstants.serviceConfigNameIndex:
                    return Utils.stringToVariable(bean.getServiceName());
                case MIBConstants.serviceConfigDescriptionIndex:
                    return Utils.stringToVariable(bean.getServiceDescription());
                case MIBConstants.serviceConfigAcceptsIndex:
                    return Utils.stringToVariable(bean.getAccepts());
                case MIBConstants.serviceConfigAcceptOptionsIndex:
                    return Utils.stringToVariable(bean.getAcceptOptions());
                case MIBConstants.serviceConfigBalancesIndex:
                    return Utils.stringToVariable(bean.getBalances());
                case MIBConstants.serviceConfigConnectsIndex:
                    return Utils.stringToVariable(bean.getConnects());
                case MIBConstants.serviceConfigConnectOptionsIndex:
                    return Utils.stringToVariable(bean.getConnectOptions());
                case MIBConstants.serviceConfigCrossSiteConstraintsIndex:
                    return Utils.stringToVariable(bean.getCrossSiteConstraints());
                case MIBConstants.serviceConfigPropertiesIndex:
                    return Utils.stringToVariable(bean.getProperties());
                case MIBConstants.serviceConfigRequiredRolesIndex:
                    return Utils.stringToVariable(bean.getRequiredRoles());
                case MIBConstants.serviceConfigRealmIndex:
                    return Utils.stringToVariable(bean.getServiceRealm());
                case MIBConstants.serviceConfigMimeMappingsIndex:
                    String mimeMappings = bean.getMimeMappings();
                    return Utils.stringToVariable(mimeMappings);
                default:
                    throw new RuntimeException("Unknown service table column: " + column);
            }
        }
    }
}

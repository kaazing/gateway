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
package org.kaazing.gateway.transport.ssl.cert;

import static org.kaazing.gateway.resource.address.Comparators.compareResourceOriginPathAlternatesAndProtocolStack;
import static org.kaazing.gateway.resource.address.Comparators.compareResourceOriginAndProtocolStack;
import static org.kaazing.gateway.resource.address.ssl.SslResourceAddress.CIPHERS;
import static org.kaazing.gateway.resource.address.ssl.SslResourceAddress.PROTOCOLS;
import static org.kaazing.gateway.resource.address.ssl.SslResourceAddress.NEED_CLIENT_AUTH;
import static org.kaazing.gateway.resource.address.ssl.SslResourceAddress.WANT_CLIENT_AUTH;
import static org.kaazing.gateway.transport.BridgeSession.LOCAL_ADDRESS;
import static org.kaazing.gateway.transport.BridgeSession.REMOTE_ADDRESS;

import static java.util.Arrays.asList;

import java.net.URI;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.PublicKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateNotYetValidException;
import java.security.cert.CertificateParsingException;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.security.auth.x500.X500Principal;

import org.apache.mina.core.session.IoSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.kaazing.gateway.resource.address.ResourceAddress;
import org.kaazing.gateway.transport.ssl.bridge.filter.SslCertificateSelectionFilter;

public class VirtualHostKeySelector
    extends AbstractKeySelector {

    private static final Logger LOGGER = LoggerFactory.getLogger(VirtualHostKeySelector.class);

    // Set of certificate aliases that can be delivered for SSL connections
    // for a given host.
    private Map<String, Collection<String>> hostnameToCertAliases = new HashMap<>();

    // Set of certificate aliases that can be delivered for SSL connections
    // for a given ssl://[hostname]:[port] address.
    private Map<ResourceAddress, Collection<String>> transportAddressToCertAliases = new TreeMap<>(compareResourceOriginPathAlternatesAndProtocolStack());

    // Set of ResourceAddresses bound to a given physical host/port.
    private Map<ResourceAddress, List<ResourceAddress>> transportAddressToResourceAddresses = new TreeMap<>(compareResourceOriginAndProtocolStack());

    // Certificate Common Names (CNs) for a given hostname
    private Map<String, String> aliasToCertCN = new HashMap<>();

    @Override
    public ResourceAddress getAvailableCertAliasesKey(boolean clientMode) {
        IoSession session = SslCertificateSelectionFilter.getCurrentSession(clientMode);
        if (session == null) {
            return null;
        }

        if (clientMode) {
            return REMOTE_ADDRESS.get(session);
        }
        else {
            return LOCAL_ADDRESS.get(session);
        }
    }

    @Override
    public Collection<String> getAvailableCertAliases(boolean clientMode) {
        return transportAddressToCertAliases.get(getAvailableCertAliasesKey(clientMode));
    }

    // Read the CN out of the cert
    private String getCertCN(X509Certificate x509)
        throws CertificateParsingException {

        X500Principal principal = x509.getSubjectX500Principal();
        String subjectName = principal.getName();

        String[] fields = subjectName.split(",");
        for (String field : fields) {
            if (field.startsWith("CN=")) {
                String serverName = field.substring(3);
                return serverName.toLowerCase();
            }
        }

        throw new CertificateParsingException("Certificate CN not found");
    }

    // Build up the list of server names represented by a certificate
    private Collection<String> getCertServerNames(X509Certificate x509)
        throws CertificateParsingException {

        Collection<String> serverNames = new LinkedHashSet<>();

        /* Add CN for the certificate */
        String certCN = getCertCN(x509);
        serverNames.add(certCN);

        /* Add alternative names for each certificate */
        try {
            Collection<List<?>> altNames = x509.getSubjectAlternativeNames();
            if (altNames != null) {
                for (List<?> entry : altNames) {
                    if (entry.size() >= 2) {
                        Object entryType = entry.get(0);
                        if (entryType instanceof Integer &&
                            (Integer) entryType == 2) {

                            Object field = entry.get(1);
                            if (field instanceof String) {
                                serverNames.add(((String) field).toLowerCase());
                            }
                        }
                    }
                }
            }

        } catch (CertificateParsingException cpe) {
            LOGGER.warn("Certificate alternative names ignored for certificate " + (certCN != null ? " " + certCN : ""), cpe);
        }

        return serverNames;
    }

    public VirtualHostKeySelector() {
        super();
    }

    @Override
    public void init(KeyStore keyStore, char[] keyStorePassword)
        throws KeyStoreException {

        if (keyStore == null) {
            return;
        }

        /* Get all aliases available for each possible server hostname for all
         * certificates in KeyStore.
         */
        Enumeration<String> aliases = keyStore.aliases();
        while (aliases.hasMoreElements()) {
            String alias = aliases.nextElement();

            if (keyStore.entryInstanceOf(alias, KeyStore.PrivateKeyEntry.class)) {
                Certificate cert = keyStore.getCertificate(alias);
                if (cert instanceof X509Certificate) {
                    try {
                        X509Certificate x509 = (X509Certificate) cert;
                        String certCN = getCertCN(x509);
                        aliasToCertCN.put(alias, certCN);

                        // Check if the certificate is currently valid
                        try {
                            x509.checkValidity();

                        } catch (CertificateExpiredException cee) {
                            LOGGER.warn("The certificate associated with alias " + alias + " in the keystore is expired.");

                        } catch (CertificateNotYetValidException cnyve) {
                            LOGGER.warn("The certificate associated with alias " + alias + " in the keystore is not yet valid.");
                        }

                        // Check for DSA certificates.  If found, warn
                        // about them (they require the use of Diffie-Hellman
                        // key exchanges, which are vulnerable to MITM attacks
                        // since they do not provide authentication).  See
                        // KG-6189 for details.
                        PublicKey pubKey = x509.getPublicKey();
                        if (pubKey.getAlgorithm().equals("DSA")) {
                            LOGGER.warn(String.format("The certificate associated with alias %s is a DSA certificate.  DSA certificates require Diffie-Hellman ciphersuites, which do not provide authentication.  Search 'ssl.ciphers' in the documentation for details.", alias));
                        }

                        // Add host -> certificate alias mapping
                        Collection<String> serverNames = getCertServerNames(x509);
                        for (String serverName : serverNames) {
                            // Add server name (CN and alternative names) as is,
                            // including wildcards (e.g. "*.kaazing.com")
                            Collection<String> serverCertAliases = hostnameToCertAliases.get(serverName);
                            if (serverCertAliases == null) {
                                serverCertAliases = new HashSet<>();
                                hostnameToCertAliases.put(serverName, serverCertAliases);
                            }

                            if (serverCertAliases.add(alias)) {
                                String msg = "Found certificate for " + serverName + " (alias: " + alias + ")";
                                LOGGER.debug(msg);
                            }
                        }

                    } catch (CertificateParsingException cpe) {
                        LOGGER.warn("The certificate associated with alias " + alias + " was ignored due to a parsing exceptions", cpe);
                    }
                }
            }
        }
    }

    @Override
    public void bind(ResourceAddress resourceAddress) throws Exception {

        URI resourceURI = resourceAddress.getResource();

        // Get the server name from the original resource address/URI
        String serverName = resourceURI.getHost();

        // JRF: this looks like we're breaking the transport abstraction by assuming that SSL is always over TCP
        ResourceAddress transport = resourceAddress.getTransport();
        assert (transport != null);
        URI transportURI = transport.getResource();
        
        if (serverName == null) {
            throw new CertificateNotFoundException("Unable to determine server name or address");
        }

        // Check that all available certificates are still valid for this
        // address.
        Collection<String> hostnameCertAliases = hostnameToCertAliases.get(serverName);
        if (hostnameCertAliases == null) {
            hostnameCertAliases = new HashSet<>();
        }

        if (serverName.contains(".")) {
            String wildcard = serverName.replaceFirst("[^.]+", "*");
            Collection<String> wildcardCertAliases = hostnameToCertAliases.get(wildcard);
            if (wildcardCertAliases != null) {
                hostnameCertAliases.addAll(wildcardCertAliases);
            }
        }

        if (hostnameCertAliases.isEmpty()) {
            String msg = "Keystore does not have a certificate entry for " + serverName;
            LOGGER.error(msg);
            throw new CertificateNotFoundException(msg);
        }

        // Verify and add certificate aliases (and other SSL settings) bound
        // to IP address/port.

        // First, verify/check certificate aliases
        Collection<String> transportAddressCertAliases = transportAddressToCertAliases.get(transport);
        if (transportAddressCertAliases == null) {
            // First case
            transportAddressCertAliases = (new HashSet<>(hostnameCertAliases));
            transportAddressToCertAliases.put(transport, transportAddressCertAliases);

        } else {
            // Subsequent cases: Find intersection of possible aliases for
            //  currently bound certificate aliases.
            if (!hostnameCertAliases.containsAll(transportAddressCertAliases)) {
                Collection<String> conflictingAliases = new HashSet<>(hostnameCertAliases);
                conflictingAliases.removeAll(transportAddressCertAliases);
                for (String conflictingAlias : conflictingAliases) {
                    String certCN = aliasToCertCN.get(conflictingAlias);

                    String msg = String.format("A certificate for %s (alias %s) cannot be used on transport %s, because it does not match all possible hostnames bound to that port", certCN, conflictingAlias, transportURI);
                    LOGGER.warn(msg);
                }
            }

            if (!transportAddressCertAliases.containsAll(hostnameCertAliases)) {
                Collection<String> conflictingAliases = new HashSet<>(transportAddressCertAliases);
                conflictingAliases.removeAll(hostnameCertAliases);
                for (String conflictingAlias : conflictingAliases) {
                    String certCN = aliasToCertCN.get(conflictingAlias);

                    String msg = String.format("A certificate for %s (alias %s) cannot be used on transport %s, because it does not match all possible hostnames bound to that port", certCN, conflictingAlias, transportURI);
                    LOGGER.warn(msg);
                }

                // Keep the intersection of the available certificates only
                transportAddressCertAliases.retainAll(hostnameCertAliases);
                if (transportAddressCertAliases.isEmpty()) {
                    String msg = String.format("keystore does not have any certificate entries matching all possible hostnames bound to %s", transportURI);
                    LOGGER.error(msg);
                    throw new CertificateNotAvailableException(msg);
                }
            }
        }

        // Next, check SSL settings.  Specifically, the <ssl.ciphers> and
        // <ssl.verify-client> configurations.
        List<ResourceAddress> transportAddressResourceAddresses = transportAddressToResourceAddresses.get(transport);
        if (transportAddressResourceAddresses == null) {
            // First case
            transportAddressResourceAddresses = new LinkedList<>();
            transportAddressResourceAddresses.add(resourceAddress);
            transportAddressToResourceAddresses.put(transport, transportAddressResourceAddresses);

        } else {
            // Subsequent cases: All ResourceAddresses MUST have the same
            // values for <ssl.verify-client> and <ssl.ciphers>, otherwise
            // it is a configuration error.

            ResourceAddress boundAddress = transportAddressResourceAddresses.get(0);

            boolean boundWantClientAuth = boundAddress.getOption(WANT_CLIENT_AUTH);
            boolean boundNeedClientAuth = boundAddress.getOption(NEED_CLIENT_AUTH);

            boolean bindWantClientAuth = resourceAddress.getOption(WANT_CLIENT_AUTH);
            boolean bindNeedClientAuth = resourceAddress.getOption(NEED_CLIENT_AUTH);

            if ((boundWantClientAuth != bindWantClientAuth) ||
                (boundNeedClientAuth != bindNeedClientAuth)) {
                // <ssl.verify-client> values mismatch

                //TODO: Add an SSL transport option to capture the original URI for the purposes of keeping this error message clear.
                URI boundURI = boundAddress.getResource();
                URI bindURI = resourceAddress.getResource();

                String msg = String.format("<ssl.verify-client> value for <accept>%s</accept> does not match <ssl.verify-client> value for <accept>%s</accept> on same transport %s", bindURI, boundURI, transportURI);
                LOGGER.error(msg);
                throw new CertificateException(msg);
            }

            String[] boundCiphers = boundAddress.getOption(CIPHERS);
            String[] bindCiphers = resourceAddress.getOption(CIPHERS);

            Arrays.sort(bindCiphers);
            Arrays.sort(boundCiphers);

            if (!Arrays.equals(boundCiphers, bindCiphers)) {
                // <ssl.ciphers> values mismatch


                //TODO: Add an SSL transport option to capture the original URI for the purposes of keeping this error message clear.
                URI boundURI = ( boundAddress.getResource() );
                URI bindURI = ( resourceAddress.getResource() );

                String msg = String.format("<ssl.ciphers>%s</ssl.ciphers> value for %s does not match <ssl.ciphers>%s</ssl.ciphers> also configured for %s on same transport %s", asList(bindCiphers), bindURI, asList(boundCiphers), boundURI, transportURI);
                LOGGER.error(msg);
                throw new CertificateException(msg);
            }

            String[] boundProtocols = boundAddress.getOption(PROTOCOLS);
            String[] bindProtocols = resourceAddress.getOption(PROTOCOLS);

            if (bindProtocols != null) {
                Arrays.sort(bindProtocols);
            }
            if (boundProtocols != null) {
                Arrays.sort(boundProtocols);
            }

            if (!Arrays.equals(boundProtocols, bindProtocols)) {
                // <ssl.protocols> values mismatch

                URI boundURI = ( boundAddress.getResource() );
                URI bindURI = ( resourceAddress.getResource() );

                String msg = String.format("<ssl.protocols>%s</ssl.protocols> value for %s does not match <ssl.protocols>%s</ssl.protocols> also configured for %s on same transport %s",
                        bindProtocols == null ? null : asList(bindProtocols), bindURI,
                        boundProtocols == null ? null : asList(boundProtocols), boundURI,
                        transportURI);
                LOGGER.error(msg);
                throw new CertificateException(msg);
            }

            transportAddressResourceAddresses.add(resourceAddress);
        }
    }

    @Override
    public void unbind(ResourceAddress resourceAddress) {
        // Next, check SSL settings.  Specifically, the <ssl.ciphers> and
        // <ssl.verify-client> configurations.
        List<ResourceAddress> inetAddressResourceAddresses = transportAddressToResourceAddresses.get(resourceAddress.getTransport());
        if (inetAddressResourceAddresses != null) {
            transportAddressToResourceAddresses.remove(resourceAddress.getTransport());
        }
    }
}

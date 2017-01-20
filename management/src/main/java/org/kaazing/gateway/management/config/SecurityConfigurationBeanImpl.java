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
package org.kaazing.gateway.management.config;

import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.Collection;
import java.util.Date;
import java.util.Enumeration;
import java.util.List;

import javax.security.auth.x500.X500Principal;

import org.json.JSONArray;
import org.json.JSONObject;
import org.kaazing.gateway.management.gateway.GatewayManagementBean;
import org.kaazing.gateway.security.SecurityContext;

public class SecurityConfigurationBeanImpl implements SecurityConfigurationBean {

    private final GatewayManagementBean gatewayBean;
    private final KeyStore keyStore;
    private final String keyStoreType;
    private final String keyStoreCertificateInfo;  // JSON

    private final String trustStoreType;
    private final KeyStore trustStore;
    private final String trustStoreCertificateInfo; // JSON

    public SecurityConfigurationBeanImpl(SecurityContext context, GatewayManagementBean gatewayBean) {
        this.gatewayBean = gatewayBean;

        // Add the keystore certs to one set of cert. info
        try {
            keyStore = context.getKeyStore();
            keyStoreType = keyStore == null ? null : keyStore.getType();
            keyStoreCertificateInfo = keyStore == null ? null : computeStoreCertificateInfo(keyStore);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }

        try {
            trustStore = context.getTrustStore();
            trustStoreType = trustStore == null ? null : trustStore.getType();
            trustStoreCertificateInfo = trustStore == null ? null : computeStoreCertificateInfo(trustStore);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    @Override
    public GatewayManagementBean getGatewayManagementBean() {
        return gatewayBean;
    }

    @Override
    public String getKeystoreType() {
        return nonNullString(keyStoreType);
    }


    @Override
    public String getKeystoreCertificateInfo() {
        return nonNullString(keyStoreCertificateInfo);
    }

    @Override
    public String getTruststoreType() {
        return nonNullString(trustStoreType);
    }

    @Override
    public String getTruststoreCertificateInfo() {
        return nonNullString(trustStoreCertificateInfo);
    }

    private String nonNullString(String value) {
        return value == null ? "" : value;
    }

    private String computeStoreCertificateInfo(KeyStore store) {
        JSONArray jsonArray = new JSONArray();
        JSONObject jsonObj;

        try {
            // Compute the array of certificates on the fly
            Enumeration<String> aliases = store.aliases();
            while (aliases.hasMoreElements()) {
                String alias = aliases.nextElement();

                // we only care about certificate entries, not key entries
                if (!store.isCertificateEntry(alias)) {
                    continue;
                }
                Certificate cert = store.getCertificate(alias);
                String type = null;
                if (cert != null) {
                    type = cert.getType();
                }

                jsonObj = new JSONObject();
                jsonObj.put("alias", alias);

                if (type != null) {
                    jsonObj.put("certificate-type", type);
                }

                // Do stuff here depending on certificate type
                if (cert instanceof X509Certificate) {
                    // add a ton of X509-specific attributes
                    X509Certificate x509Cert = (X509Certificate) cert;
                    jsonObj.put("version", x509Cert.getVersion());

                    try {
                        x509Cert.checkValidity(new Date());  // throws if not valid
                        jsonObj.put("valid", true);
                    } catch (Exception ex) {
                        // certificate is expired or not valid yet
                        jsonObj.put("valid", false);
                    }

                    // For certificate authority determination, need the path constraint
                    // length from basic constraints. Only >= 0 if this is a CA.
                    jsonObj.put("pathConstraint", x509Cert.getBasicConstraints());

                    Date d = x509Cert.getNotBefore();
                    jsonObj.put("notValidBefore", d == null ? d : d.getTime());
                    d = x509Cert.getNotAfter();
                    jsonObj.put("notValidAfter", d == null ? d : d.getTime());
                    jsonObj.put("serialNumber", x509Cert.getSerialNumber());
                    X500Principal issuer = x509Cert.getIssuerX500Principal();
                    jsonObj.put("issuer", issuer.getName());
                    X500Principal subject = x509Cert.getSubjectX500Principal();
                    jsonObj.put("subject", subject.getName());

                    JSONArray subjectAlternativeNames = getAlternativeNames(x509Cert.getSubjectAlternativeNames());
                    if (subjectAlternativeNames != null) {
                        jsonObj.put("subjectAlternativeNames", subjectAlternativeNames);
                    }

                    JSONArray issuerAlternativeNames = getAlternativeNames(x509Cert.getIssuerAlternativeNames());
                    if (issuerAlternativeNames != null) {
                        jsonObj.put("issuerAlternativeNames", issuerAlternativeNames);
                    }

                    jsonObj.put("signatureAlgorithm", x509Cert.getSigAlgName());
                }

                jsonArray.put(jsonObj);
            }
        } catch (Exception ex) {
            // This is only for JSON exceptions, but there should be no way to
            // hit this.
        }

        return jsonArray.toString();
    }

    /**
     * Given an AlternativeNames structure, return a JSONArray with the name strings.
     *
     * @param alternativeNames
     * @return
     */
    private JSONArray getAlternativeNames(Collection<List<?>> alternativeNames) {

        if (alternativeNames == null || alternativeNames.size() == 0) {
            return null;
        }

        JSONArray altNames = new JSONArray();
        for (List<?> altName : alternativeNames) {
            //int altNameType = (Integer) altName.get(0);  // XXX For now we're going to ignore the type
            //                                             // We might want to do something special later.
            String altNameValue = altName.get(1).toString();
            altNames.put(altNameValue);
        }

        return altNames;
    }
}

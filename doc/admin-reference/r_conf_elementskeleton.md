-   [Home](../../index.md)
-   [Documentation](../index.md)
-   Administration with KAAZING Gateway

Configuration Skeleton
============================================

You can view and link to all Gateway configuration elements and properties using these lists:

-   [Configuration Element Index](r_conf_elementindex.md) for an alphabetical listing
-   **Configuration Skeleton** (this topic) for a bare bones Gateway configuration

-   [gateway-config](r_conf_gwconfig.md)
    -   [service](r_conf_service.md)
        -   [name](r_conf_service.md#service)
        -   [description](r_conf_service.md#service)
        -   [accept](r_conf_service.md#accept)
        -   [connect](r_conf_service.md#connect)
        -   [balance](r_conf_service.md#balance)
        -   [notify](r_conf_service.md#notify) ![This feature is available in KAAZING Gateway - Enterprise Edition](../images/enterprise-feature.png) 
        -   [type](r_conf_service.md#type)
            -   [balancer](r_conf_service.md#balancer)
            -   [broadcast](r_conf_service.md#broadcast)

                Property:

                -   [accept](r_conf_service.md#broadcast)
            -   [directory](r_conf_service.md#directory)

                Properties:

                -   [directory](r_conf_service.md#directory)
                -   [options](r_conf_service.md#directory)
                -   [welcome-file](r_conf_service.md#directory)
                -   [error-pages-directory](r_conf_service.md#directory)
            -   [echo](r_conf_service.md#echo)
            -   [management.jmx](r_conf_service.md#managementjmx)
            -   [management.snmp](r_conf_service.md#managementsnmp)
            -   [kerberos5.proxy](r_conf_service.md#kerberos5proxy) ![This feature is available in KAAZING Gateway - Enterprise Edition](../images/enterprise-feature.png)
            -   [proxy](r_conf_service.md#proxy-amqpproxy-and-jmsproxy)

                Properties:

                -   [maximum.pending.bytes](r_conf_service.md#maximumpendingbytes)
                -   [maximum.recovery.interval](r_conf_service.md#maximumrecoveryinterval)
                -   [prepared.connection.count](r_conf_service.md#preparedconnectioncount)
            -   [amqp.proxy](r_conf_service.md#proxy-amqpproxy-and-jmsproxy)

                Properties:

                -   [maximum.pending.bytes](r_conf_service.md#maximumpendingbytes)
                -   [maximum.recovery.interval](r_conf_service.md#maximumrecoveryinterval)
                -   [prepared.connection.count](r_conf_service.md#preparedconnectioncount)
                -   [virtual.host](r_conf_service.md#virtualhost)
            -   [jms](r_conf_jms.md#jms) ![This feature is available in KAAZING Gateway - Enterprise Edition](../images/enterprise-feature.png) 
            -   [jms.proxy](r_conf_jms.md#jmsproxy)  ![This feature is available in KAAZING Gateway - Enterprise Edition](../images/enterprise-feature.png)

                Properties:

                -   [maximum.pending.bytes](r_conf_service.md#maximumpendingbytes)
                -   [maximum.recovery.interval](r_conf_service.md#maximumrecoveryinterval)
                -   [prepared.connection.count](r_conf_service.md#preparedconnectioncount)
            -   [session](r_conf_service.md#session_svc)

        -   [properties](r_conf_service.md#properties)
        -   [accept-options](r_conf_service.md#accept-options-and-connect-options)
            -   [*protocol*.bind](r_conf_service.md#protocolbind), where *protocol* can be ws, wss, http, https, ssl, socks, tcp, or udp
            -   [*protocol*.transport](r_conf_service.md#protocoltransport), where *protocol* can be pipe, tcp, ssl, or http
            -   [ws.maximum.message.size](r_conf_service.md#wsmaximummessagesize)
            -   [http.keepalive.timeout](r_conf_service.md#httpkeepalivetimeout)
            -   [ssl.ciphers](r_conf_service.md#sslciphers)
            -   [ssl.protocols](r_conf_service.md#sslprotocols-and-sockssslprotocols)
            -   [ssl.encryption](r_conf_service.md#sslencryption)
            -   [ssl.verify-client](r_conf_service.md#sslverify-client)
            -   [socks.mode](r_conf_service.md#socksmode) ![This feature is available in KAAZING Gateway - Enterprise Edition](../images/enterprise-feature.png)
            -   [socks.ssl.ciphers](r_conf_service.md#sockssslciphers) ![This feature is available in KAAZING Gateway - Enterprise Edition](../images/enterprise-feature.png)
            -   [socks.ssl.protocols](r_conf_service.md#sslprotocols-and-sockssslprotocols) ![This feature is available in KAAZING Gateway - Enterprise Edition](../images/enterprise-feature.png)
            -   [socks.ssl.verify-client](r_conf_service.md#sslverify-client) ![This feature is available in KAAZING Gateway - Enterprise Edition](../images/enterprise-feature.png)
            -   [socks.retry.maximum.interval](r_conf_service.md#socksretrymaximuminterval) ![This feature is available in KAAZING Gateway - Enterprise Edition](../images/enterprise-feature.png)
            -   [tcp.maximum.outbound.rate](r_conf_service.md#tcpmaximumoutboundrate) ![This feature is available in KAAZING Gateway - Enterprise Edition](../images/enterprise-feature.png)
            -   [ws.inactivity.timeout](r_conf_service.md#wsinactivitytimeout)
            -   [http.server.header](r_conf_service.md#httpserverheader)
        -   [connect-options](r_conf_service.md#accept-options-and-connect-options)
            -   [*protocol*.transport](r_conf_service.md#protocoltransport), where *protocol* can be pipe, tcp, ssl, or http
            -   [ssl.ciphers](r_conf_service.md#sslciphers)
            -   [ssl.protocols](r_conf_service.md#sslprotocols-and-sockssslprotocols)
            -   [ssl.encryption](r_conf_service.md#sslencryption)
            -   [socks.mode](r_conf_service.md#socksmode) ![This feature is available in KAAZING Gateway - Enterprise Edition](../images/enterprise-feature.png)
            -   [socks.timeout](r_conf_service.md#conn_sockstimeout) ![This feature is available in KAAZING Gateway - Enterprise Edition](../images/enterprise-feature.png)
            -   [socks.ssl.ciphers](r_conf_service.md#sockssslciphers) ![This feature is available in KAAZING Gateway - Enterprise Edition](../images/enterprise-feature.png)
            -   [socks.ssl.protocols](r_conf_service.md#sslprotocols-and-sockssslprotocols) ![This feature is available in KAAZING Gateway - Enterprise Edition](../images/enterprise-feature.png)
            -   [socks.ssl.verify-client](r_conf_service.md#sockssslverify-client) ![This feature is available in KAAZING Gateway - Enterprise Edition](../images/enterprise-feature.png)
            -   [ws.inactivity.timeout](r_conf_service.md#wsinactivitytimeout)
            -   [ws.version](r_conf_service.md#wsversion-deprecated) (deprecated)
        -   [notify-options](r_conf_service.md#notify-options) ![This feature is available in KAAZING Gateway - Enterprise Edition](../images/enterprise-feature.png)
            -   [apns.feedback.transport](r_conf_service.md#notify-options)
            -   [apns.feedback.transport](r_conf_service.md#notify-options)
            -   [ssl.ciphers](r_conf_service.md#notify-options)
            -   [tcp.transport](r_conf_service.md#notify-options)
        -   [realm-name](r_conf_service.md#realm-name)
        -   [authorization-constraint](r_conf_service.md#authorization-constraint)
            -   [require-role](r_conf_service.md#authorization-constraint)
            -   [require-valid-user](r_conf_service.md#authorization-constraint)
        -   [mime-mapping](r_conf_service.md#mime-mapping)
            -   [extension](r_conf_serv_defs.md#mime-mapping)
            -   [mime-type](r_conf_serv_defs.md#mime-mapping)
        -   [cross-site-constraint](r_conf_service.md#cross-site-constraint)
            -   [allow-origin](r_conf_service.md#cross-site-constraint)
            -   [allow-methods](r_conf_service.md#cross-site-constraint)
            -   [allow-headers](r_conf_service.md#cross-site-constraint)
    -   [service-defaults](r_conf_serv_defs.md)
        -   [accept-options](r_conf_serv_defs.md#accept-options-service-defaults)
            -   [*protocol*.bind](r_conf_service.md#protocolbind), where *protocol* can be ws, wss, http, https, ssl, socks, tcp, or udp
            -   [*protocol*.transport](r_conf_service.md#protocoltransport), where *protocol* can be pipe, tcp, ssl, or http
            -   [ws.maximum.message.size](r_conf_service.md#wsmaximummessagesize)
            -   [http.keepalive.timeout](r_conf_service.md#httpkeepalivetimeout)
            -   [ssl.ciphers](r_conf_service.md#sslciphers)
            -   [ssl.protocols](r_conf_service.md#sslprotocols-and-sockssslprotocols)
            -   [ssl.encryption](r_conf_service.md#sslencryption)
            -   [ssl.verify-client](r_conf_service.md#sslverify-client)
            -   [socks.mode](r_conf_service.md#socksmode) ![This feature is available in KAAZING Gateway - Enterprise Edition](../images/enterprise-feature.png)
            -   [socks.ssl.ciphers](r_conf_service.md#sockssslciphers) ![This feature is available in KAAZING Gateway - Enterprise Edition](../images/enterprise-feature.png)
            -   [socks.ssl.protocols](r_conf_service.md#sslprotocols-and-sockssslprotocols) ![This feature is available in KAAZING Gateway - Enterprise Edition](../images/enterprise-feature.png)
            -   [socks.ssl.verify-client](r_conf_service.md#sockssslverify-client) ![This feature is available in KAAZING Gateway - Enterprise Edition](../images/enterprise-feature.png)
            -   [socks.retry.maximum.interval](r_conf_service.md#socksretrymaximuminterval) ![This feature is available in KAAZING Gateway - Enterprise Edition](../images/enterprise-feature.png)
            -   [tcp.maximum.outbound.rate](r_conf_service.md#tcpmaximumoutboundrate) ![This feature is available in KAAZING Gateway - Enterprise Edition](../images/enterprise-feature.png)
            -   [ws.inactivity.timeout](r_conf_service.md#wsinactivitytimeout)
            -   [http.server.header](r_conf_service.md#httpserverheader)
        -   [mime-mapping](r_conf_serv_defs.md#mime-mapping)
            -   [extension](r_conf_serv_defs.md#mime-mapping)
            -   [mime-type](r_conf_serv_defs.md#mime-mapping)
    -   [security](r_conf_security.md)
        -   [keystore](r_conf_security.md#keystore)
            -   [type](r_conf_security.md#keystore)
            -   [file](r_conf_security.md#keystore)
            -   [password-file](r_conf_security.md#keystore)
        -   [truststore](r_conf_security.md#truststore)
            -   [type](r_conf_security.md#truststore)
            -   [file](r_conf_security.md#truststore)
            -   [password-file](r_conf_security.md#truststore)
        -   [realm](r_conf_security.md#realm)
            -   [name](r_conf_security.md#realm)
            -   [description](r_conf_security.md#realm)
            -   [authentication](r_conf_security.md#authentication)
                -   [http-challenge-scheme](r_conf_security.md#authentication)
                -   [http-header](r_conf_security.md#authentication)
                -   [http-query-parameter](r_conf_security.md#authentication)
                -   [http-cookie](r_conf_security.md#authentication)
                -   [authorization-mode](r_conf_security.md#authentication)
                -   [authorization-timeout](r_conf_security.md#authentication) ![This feature is available in KAAZING Gateway - Enterprise Edition](../images/enterprise-feature.png)
                -   [session-timeout](r_conf_security.md#authentication) ![This feature is available in KAAZING Gateway - Enterprise Edition](../images/enterprise-feature.png)
                -   [login-modules](r_conf_security.md#authentication)
                    -   [login-module](r_conf_security.md#login-module)
                        -   [type](r_conf_security.md#login-module)
                        -   [success](r_conf_security.md#login-module)
                        -   [options](r_conf_security.md#options-login-module)
                            -   [debug](r_conf_security.md#options-login-module)
                            -   [tryFirstToken](r_conf_security.md#options-login-module)
            -   [user-principal-class](r_conf_security.md#realm)
    -   [cluster](r_conf_cluster.md)
        -   [name](r_conf_cluster.md#cluster)
        -   [accept](r_conf_cluster.md#cluster)
        -   [connect](r_conf_cluster.md#cluster)


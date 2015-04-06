-   [Home](../../index.md)
-   [Documentation](../index.md)
-   Administration with ${gateway.name.short}

Configuration Skeleton${enterprise.logo.jms}
============================================

You can view and link to all ${gateway.cap} configuration elements and properties using these lists:

-   [Configuration Element Index](r_conf_elementindex.md) for an alphabetical listing
-   **Configuration Skeleton** (this topic) for a bare bones ${gateway.cap} configuration

-   [gateway-config](r_conf_gwconfig.md)
    -   [service](r_conf_service.md)
        -   [name](r_conf_service.md#servicename)
        -   [description](r_conf_service.md#servicedescription)
        -   [accept](r_conf_service.md#acceptele)
        -   [connect](r_conf_service.md#connectele)
        -   [balance](r_conf_service.md#balanceele)
        -   [notify](r_conf_service.md#notifyele)(JMS only) ${enterprise.logo.jms}
        -   [type](r_conf_service.md#typeele)
            -   [balancer](r_conf_service.md#balancer)
            -   [broadcast](r_conf_service.md#broadcast)

                Property:

                -   [accept](r_conf_service.md#broadcast-accept)
            -   [directory](r_conf_service.md#directory)

                Properties:

                -   [directory](r_conf_service.md#directory-directory)
                -   [options](r_conf_service.md#directory-options)
                -   [welcome-file](r_conf_service.md#directory-welcomefile)
                -   [error-pages-directory](r_conf_service.md#directory-errorpagesdirectory)
            -   [echo](r_conf_service.md#echo)
            -   [management.jmx](r_conf_service.md#mgmtjmx)
            -   [management.snmp](r_conf_service.md#mgmtsnmp)
            -   [kerberos5.proxy](r_conf_service.md#kerberos5) ${enterprise.logo}
            -   [proxy](r_conf_service.md#proxy)

                Properties:

                -   [maximum.pending.bytes](r_conf_service.md#proxy-maximumpendingbytes)
                -   [maximum.recovery.interval](r_conf_service.md#proxy-maximumrecoveryinterval)
                -   [prepared.connection.count](r_conf_service.md#proxy-preparedconnectioncount)
            -   [amqp.proxy](r_conf_service.md#proxy)

                Properties:

                -   [maximum.pending.bytes](r_conf_service.md#proxy-maximumpendingbytes)
                -   [maximum.recovery.interval](r_conf_service.md#proxy-maximumrecoveryinterval)
                -   [prepared.connection.count](r_conf_service.md#proxy-preparedconnectioncount)
                -   [virtual.host](r_conf_service.md#proxy-virtualhost)
            -   [jms](r_stomp_service.md#stompjms) (JMS only) ${enterprise.logo.jms}
            -   <a href="r_stomp_service.md#stompinterceptor">jms.proxy
            -   [xmpp.proxy](r_conf_service.md#proxy)

                Properties:

                -   [maximum.pending.bytes](r_conf_service.md#proxy-maximumpendingbytes)
                -   [maximum.recovery.interval](r_conf_service.md#proxy-maximumrecoveryinterval)
                -   [prepared.connection.count](r_conf_service.md#proxy-preparedconnectioncount)
            -   [session](r_conf_service.md#session_svc)

        -   [properties](r_conf_service.md#propertiesele)
        -   [accept-options](r_conf_service.md#svcacceptopts)
            -   [*protocol*.bind](r_conf_service.md#protocolbind), where *protocol* can be ws, wss, http, https, ssl, socks, tcp, or udp
            -   [*protocol*.transport](r_conf_service.md#protocoltransport), where *protocol* can be pipe, tcp, ssl, or http
            -   [ws.maximum.message.size](r_conf_service.md#wsmaxmsg)
            -   [http.keepalive.timeout](r_conf_service.md#keepalive)
            -   [ssl.ciphers](r_conf_service.md#sslciphers)
            -   [ssl.protocols](r_conf_service.md#sslprotocols)
            -   [ssl.encryption](r_conf_service.md#sslencrypt)
            -   [ssl.verify-client](r_conf_service.md#sslverifyclient)
            -   [socks.mode](r_conf_service.md#socksmode)${enterprise.logo}
            -   [socks.ssl.ciphers](r_conf_service.md#sockssslciphers)${enterprise.logo}
            -   [socks.ssl.protocols](r_conf_service.md#sslprotocols)${enterprise.logo}
            -   [socks.ssl.verify-client](r_conf_service.md#sockssslverifyclient)${enterprise.logo}
            -   [socks.retry.maximum.interval](r_conf_service.md#socksretrymaxint)${enterprise.logo}
            -   [tcp.maximum.outbound.rate](r_conf_service.md#tcpmaxoutbndrate)${enterprise.logo}
            -   [ws.inactivity.timeout](r_conf_service.md#wsinactivitytimeout)
        -   [connect-options](r_conf_service.md#svcconnectopts)
            -   [*protocol*.transport](r_conf_service.md#protocoltransport), where *protocol* can be pipe, tcp, ssl, or http
            -   [ssl.ciphers](r_conf_service.md#sslciphers)
            -   [ssl.protocols](r_conf_service.md#sslprotocols)
            -   [ssl.encryption](r_conf_service.md#sslencrypt)
            -   [socks.mode](r_conf_service.md#socksmode)${enterprise.logo}
            -   [socks.timeout](r_conf_service.md#conn_sockstimeout)${enterprise.logo}
            -   [socks.ssl.ciphers](r_conf_service.md#sockssslciphers)${enterprise.logo}
            -   [socks.ssl.protocols](r_conf_service.md#sslprotocols)${enterprise.logo}
            -   [socks.ssl.verify-client](r_conf_service.md#sockssslverifyclient)${enterprise.logo}
            -   [ws.inactivity.timeout](r_conf_service.md#wsinactivitytimeout)
            -   [ws.version](r_conf_service.md#wsversionopt) (deprecated)
        -   [notify-options](r_conf_service.md#notifyopts) (JMS only) ${enterprise.logo.jms}
            -   [apns.notify.transport](r_conf_service.md#notify_apnstrnsp)
            -   [apns.feedback.transport](r_conf_service.md#notify_apnsfeedback)
            -   [ssl.ciphers](r_conf_service.md#notify_sslciphers)
            -   [tcp.transport](r_conf_service.md#notify_tcptransport)
        -   [realm-name](r_conf_service.md#realm-name)
        -   [authorization-constraint](r_conf_service.md#svcauthconst)
            -   [require-role](r_conf_service.md#requireroleopt)
            -   [require-valid-user](r_conf_service.md#requirevaliduser)
        -   [mime-mapping](r_conf_service.md#svcmimemapping)
            -   [extension](r_conf_serv_defs.md#mimemapextension)
            -   [mime-type](r_conf_serv_defs.md#mimemapextension)
        -   [cross-site-constraint](r_conf_service.md#xsiteconst)
            -   [allow-origin](r_conf_service.md#alloworigin)
            -   [allow-methods](r_conf_service.md#allowmethods)
            -   [allow-headers](r_conf_service.md#alllowheaders)
    -   [service-defaults](r_conf_serv_defs.md)
        -   [accept-options](r_conf_serv_defs.md#svcdftacceptoptions)
            -   [*protocol*.bind](r_conf_service.md#protocolbind), where *protocol* can be ws, wss, http, https, ssl, socks, tcp, or udp
            -   [*protocol*.transport](r_conf_service.md#protocoltransport), where *protocol* can be pipe, tcp, ssl, or http
            -   [ws.maximum.message.size](r_conf_service.md#wsmaxmsg)
            -   [http.keepalive.timeout](r_conf_service.md#keepalive)
            -   [ssl.ciphers](r_conf_service.md#sslciphers)
            -   [ssl.protocols](r_conf_service.md#sslprotocols)
            -   [ssl.encryption](r_conf_service.md#sslencrypt)
            -   [ssl.verify-client](r_conf_service.md#sslverifyclient)
            -   [socks.mode](r_conf_service.md#socksmode)${enterprise.logo}
            -   [socks.ssl.ciphers](r_conf_service.md#sockssslciphers)${enterprise.logo}
            -   [socks.ssl.protocols](r_conf_service.md#sslprotocols)${enterprise.logo}
            -   [socks.ssl.verify-client](r_conf_service.md#sockssslverifyclient)${enterprise.logo}
            -   [socks.retry.maximum.interval](r_conf_service.md#socksretrymaxint)${enterprise.logo}
            -   [tcp.maximum.outbound.rate](r_conf_service.md#tcpmaxoutbndrate)${enterprise.logo}
            -   [ws.inactivity.timeout](r_conf_service.md#wsinactivitytimeout)
        -   [mime-mapping](r_conf_serv_defs.md#svcdftmimemapping)
            -   [extension](r_conf_serv_defs.md#mimemapextension)
            -   [mime-type](r_conf_serv_defs.md#mimemapextension)
    -   [security](r_conf_security.md)
        -   [keystore](r_conf_security.md#keystore)
            -   [type](r_conf_security.md#keystore_type)
            -   [file](r_conf_security.md#keystore_file)
            -   [password-file](r_conf_security.md#keystore_passwordfile)
        -   [truststore](r_conf_security.md#truststore)
            -   [type](r_conf_security.md#truststore_type)
            -   [file](r_conf_security.md#truststore_file)
            -   [password-file](r_conf_security.md#truststore_passwordfile)
        -   [realm](r_conf_security.md#realm_element)
            -   [name](r_conf_security.md#realm_name)
            -   [description](r_conf_security.md#realm_description)
            -   [authentication](r_conf_security.md#sec_auth)
                -   [http-challenge-scheme](r_conf_security.md#challenge_scheme)
                -   [http-header](r_conf_security.md#http-header)
                -   [http-query-parameter](r_conf_security.md#httpqueryparameter)
                -   [http-cookie](r_conf_security.md#http-cookie)
                -   [authorization-mode](r_conf_security.md#auth_mode)
                -   [authorization-timeout](r_conf_security.md#auth_timeout)${enterprise.logo}
                -   [session-timeout](r_conf_security.md#sessiontimeout)${enterprise.logo}
                -   [login-modules](r_conf_security.md#realm_loginmodules)
                    -   [login-module](r_conf_security.md#loginmodule)
                        -   [type](r_conf_security.md#loginmoduletype)
                        -   [success](r_conf_security.md#success)
                        -   [options](r_conf_security.md#loginmoduleoptions)
                            -   [debug](r_conf_security.md#debug_opt)
                            -   [tryFirstToken](r_conf_security.md#tryFirstToken_option)
            -   [user-principal-class](r_conf_security.md#userprincipalclass)
    -   [cluster](r_conf_cluster.md)
        -   [name](r_conf_cluster.md#clstrnameprop)
        -   [accept](r_conf_cluster.md#clstracceptopt)
        -   [connect](r_conf_cluster.md#clstrconnectopt)

</div>


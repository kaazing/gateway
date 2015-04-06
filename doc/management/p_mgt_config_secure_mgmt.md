-   [Home](../../index.md)
-   [Documentation](../index.md)
-   Secure ${gateway.name.short} Monitoring

<a name="securing"></a>Secure ${gateway.name.short} Monitoring${enterprise.logo.jms}
====================================================================================

${gateway.name.long} monitoring is secured by an explicitly specified security realm name. The `realm-name` element refers to one of the named realms inside the `service` element.

Before You Begin
----------------

This procedure is part of [Monitoring ${gateway.name.short}](o_admin_monitor.md):

1.  [Introduction to Monitoring ${gateway.name.long}](o_admin_monitor.md#intromonitor)
2.  **Secure ${gateway.name.long} Monitoring**
3.  Monitor a ${gateway.cap} or ${gateway.cap} cluster
    -   [Monitor with ${console.name}](p_monitor_cc.md) (Recommended)
    -   [Monitor with JMX](p_monitor_jmx.md)

4.  [Troubleshoot ${the.gateway}](../troubleshooting/o_ts.md)

To Secure Management for ${the.gateway}
---------------------------------------

1.  Open ${the.gateway} configuration file (for example, `GATEWAY_HOME/conf/gateway-config.xml)` in a text editor.
2.  Add a realm and a security authorization constraint as shown in the following example for the SNMP Management service:

    ``` auto-links:
        <service>
          <name>SNMP Management</name>
          <description>SNMP Management Service</description>
          <accept>ws://${gateway.hostname}:${gateway.base.port}/snmp</accept>
          
          <type>management.snmp</type>

          <!-- secure monitoring using a security realm -->
          <realm-name>commandcenter</realm-name>

          <!-- configure the authorized user roles -->
          <authorization-constraint>
            <require-role>ADMINISTRATOR</require-role>
          </authorization-constraint>
          
          <cross-site-constraint>
            <allow-origin>*</allow-origin>
          </cross-site-constraint>
        </service>
    ```

**Note:** Password authentication over the Secure Sockets Layer (SSL) and Transport Layer Security (TLS) is enabled by default in JMX. Consequently, you must have a digital certificate for the hostname of ${the.gateway} in the keystore.db file used by ${the.gateway}. In addition, access to port 2020 must be enabled in your network for the remote monitoring agent to connect to ${the.gateway}. For information on how to create a certificate for the hostname of ${the.gateway}, see [Secure Network Traffic with ${the.gateway}](../security/o_tls.md).
<a name="seealso"></a>See Also
------------------------------

-   The `management.snmp` and `management.jmx` service types in the [Service Reference](../admin-reference/r_conf_service.md#service) documentation
-   [About Security with ${gateway.name.short}](../security/c_sec_security.md)



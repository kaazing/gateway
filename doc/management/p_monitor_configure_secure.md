Secure KAAZING Gateway Monitoring
====================================================================================

KAAZING Gateway monitoring is secured by an explicitly specified security realm name. The `realm-name` element refers to one of the named realms inside the `service` element.

Before You Begin
----------------

This procedure is part of [Monitor the Gateway](o_monitor.md):

1.  [Introduction to Monitoring KAAZING Gateway](o_monitor.md#introduction-to-monitoring-kaazing-gateway)
2.  **Secure KAAZING Gateway Monitoring**
3.  Monitor a Gateway or Gateway cluster
    -   [Monitor with Command Center](p_monitor_cc.md) (Recommended)
    -   [Monitor with JMX](p_monitor_jmx.md)

4.  [Troubleshoot the Gateway](../troubleshooting/o_troubleshoot.md)

To Secure Management for the Gateway
---------------------------------------

1.  Open the Gateway configuration file (for example, `GATEWAY_HOME/conf/gateway-config.xml)` in a text editor.
2.  Add a realm and a security authorization constraint as shown in the following example for the SNMP Management service:

    ``` xml
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

**Note:** Password authentication over the Secure Sockets Layer (SSL) and Transport Layer Security (TLS) is enabled by default in JMX. Consequently, you must have a digital certificate for the hostname of the Gateway in the keystore.db file used by the Gateway. In addition, access to port 2020 must be enabled in your network for the remote monitoring agent to connect to the Gateway. For information on how to create a certificate for the hostname of the Gateway, see [Secure Network Traffic with the Gateway](../security/o_tls.md).

See Also
------------------------------

-   The `management.snmp` and `management.jmx` service types in the [Service Reference](../admin-reference/r_configure_gateway_service.md) documentation
-   [About Security with KAAZING Gateway](../security/c_security_about.md)

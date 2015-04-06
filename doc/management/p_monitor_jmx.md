-   [Home](../../index.md)
-   [Documentation](../index.md)
-   Monitoring ${gateway.name.short}

<a name="monitorjmx"></a>Monitor with JMX${enterprise.logo.jms}
===============================================================

${gateway.name.short} supports Java Management Extension (JMX) access through any JMX-compliant console, such as Java's built-in Java Management and Monitoring Console ([JConsole](http://docs.oracle.com/javase/7/docs/technotes/guides/management/jconsole.html "Using JConsole - Java SE Monitoring and Management Guide")) or MC4J, or through any program that supports communication using the JMX protocol. ${the.gateway.cap} supports a `management.jmx` service to allow JMX-compliant applications to monitor current ${gateway.cap} service and session state and operate on individual sessions. See the [Service Reference](../admin-reference/r_conf_service.md#service) for information about the JMX management service (`management.jmx`).

Before You Begin
----------------

This procedure is part of [Monitoring ${gateway.name.short}](o_admin_monitor.md):

<ol>
<li>
[Introduction to Monitoring ${gateway.name.long}](o_admin_monitor.md#intromonitor)
</li>
<li>
[Secure ${gateway.name.long} Monitoring](p_mgt_config_secure_mgmt.md)
</li>
<li>
Monitor a ${gateway.cap} or ${gateway.cap} cluster
</li>
-   [Monitor with ${console.name}](p_monitor_cc.md) (**Recommended**)
-   **Monitor with JMX**

</li>
<li>
[Troubleshoot ${the.gateway}](../troubleshooting/o_ts.md)
</li>
</ol>
To Monitor with JMX
-------------------

1.  Install and start ${the.gateway} as described in ${setting.up.inline}. **Note:** To connect to a JMX service on a ${gateway.cap} running on an EC2 instance, configure the `GATEWAY_OPTS` environment variable with the `-Djava.rmi.server.hostname` property set to the IP address or hostname being used in the RMI registry. For example, `GATEWAY_OPTS="-Xmx512m -Djava.rmi.server.hostname=ec2-54-205-184-88.example-1.amazonaws.com`. See the topic [Configure ${gateway.name.long} Using the GATEWAY\_OPTS Environment Variable](../admin-reference/p_conf_gw_opts.md) for more information.
2.  Ensure secure monitoring by verifying that your configuration specifies a security realm name and an authorization constraint. This is set up automatically if you use the default ${gateway.cap} configuration. See [Secure ${gateway.name.long} Monitoring](p_mgt_config_secure_mgmt.md) for more information.
3.  Start your favorite Java monitoring console or application. This documentation uses JConsole for its examples.
4.  Select the local process, for example `org.kaazing.gateway.server.Main` (as shown in the following screenshot), then click **Connect**. (For Windows, the local process name is `org.kaazing.gateway.server.WindowsMain`.)

    <figure>
    ![JConsole](../images/jconsole-mgt-bean.png)

    <figcaption>
    
**Figure: Monitoring the Local Process with JConsole**


    </figcaption>
    </figure>
    <span class="note">**Notes:** 

    -   To connect to a *remote* process you must specify an address that uniquely represents the remote instance of ${the.gateway} and provide the administrator's user name and password (by default, `admin`/`admin`). If there are multiple instances of ${the.gateway} on a remote server then JMX management will be hosted on different ports.
    -   Use the following syntax to access your local ${gateway.cap} as a remote process (where `hostname` is the remote hostname): `service:jmx:rmi://hostname/jndi/rmi://hostname:2020/jmxrmi`
    -   Password authentication over the Secure Sockets Layer (SSL) and Transport Layer Security (TLS) is enabled by default in JMX. Consequently, you must have a digital certificate for the hostname of ${the.gateway} in the keystore.db file used by ${the.gateway}. In addition, access to port 2020 must be enabled in your network for the remote monitoring agent to connect to ${the.gateway}. For information on how to create a certificate for the hostname of ${the.gateway}, see [Secure Network Traffic with the ${gateway.cap}](../security/o_tls.md).

    </span>

5.  JConsole displays information about the particular JVM process you just clicked on or entered.
6.  Click the **MBeans** tab.
7.  Expand the service that you want to monitor.
    <p>
    For example, the following JConsole screenshot shows an expanded `org.kaazing.gateway.server.management` \> `service` and expanded the `echo` node.
    <figure>
    ![JConsole session](../images/jconsole-session.png)
    <figcaption>
    
**Figure: Viewing the Echo Service in JConsole**

    </figcaption>
    </figure>
8.  Click one of the session IDs. The session data exposed by the MBeans displays. Here, you can examine the user sessions on ${the.gateway}.

<a name="examine_jconsole"></a>Get Started with JMX Monitoring
--------------------------------------------------------------

The steps in this section use the JConsole example from the previous section to demonstrate the procedure.

### To Examine Available Sessions:

1.  Click the **** service that you want to examine.

    For example, in our running JConsole example, you would click the **Echo** service, as this is the service being used by the demo you started.

2.  Expand the connection.
3.  Expand the sessions node to view the session IDs.
4.  Select the session ID you want to manage. The following screenshot shows that you can manually manage the session, such as using the `close()` or the `closeImmediately` operation, to explicitly close the specific session:

    <figure>
    ![This screenshot shows the JConsole GUI displying the service \> echo \> ws://localhost:8001/echo \> sessions \> Operations path. Two operations are available under Operations: close and closeImmediately](../images/jconsole-session-id.png)

    <figcaption>
    
**Figure: Subscribing to a Session in JConsole**


    </figcaption>
    </figure>
5.  To subscribe to notifications in the console, expand the **summary** section, click **Notifications**, then click **javax.management.Notification**, as shown in the following screenshot:

    <figure>
    ![This screenshot shows the JConsole GUI displying the service \> echo \> ws://localhost:8001/echo \> summary \> Notifications path. Two operations are available under Notifications](../images/jconsole-session-subscribe.png)

    <figcaption>
    
**Figure: Subscribing to Notifications in JConsole**


    </figcaption>
    </figure>
6.  Now that you've taken a look at some of the actions you can perform on a session, you can use this information with your own management console to manage user sessions. ${the.gateway.cap} also tracks the information you see here, such as the opening and closing of sessions, and sends this information by way of JMX notifications. You can then set up your JMX clients to subscribe to these notifications or query the sessions directly from the JMX server bean. To learn how to do this, see the next section, [To Configure Notifications](#managing_sessions_notif).

<span id="managing_sessions_notif"></span></a>To Configure Notifications
------------------------------------------------------------------------

${the.gateway.cap} tracks sessions that are opened and closed in the management interface and sends out JMX notifications. You can configure a JMX client to subscribe to these notifications or configure ${the.gateway} to query the connected sessions. The JMX notification includes the user's data, which consists of a key and a value.

You can also configure ${the.gateway} to extract user information from the authentication information within the session. For example, in the security section of the `gateway-config.xml` file, you can use the `user-principal-class` property, as shown in the following example.

``` auto-links:
<security>
    <realm>
        <name>demo</name>
        <user-principal-class>
            org.kaazing.gateway.server.auth.config.parse.DefaultUserConfig
        </user-principal-class>
        <user-principal-class>
            org.kaazing.gateway.server.auth.gss.GssSourcePrincipal
        </user-principal-class>
        <user-principal-class>
            org.kaazing.gateway.server.auth.gss.GssTargetPrincipal
        </user-principal-class>
    </realm>
</security>
```

After you add the `userPrincipalClass` property to the [realm](../admin-reference/r_conf_security.md#realm_element), save the `gateway-config.xml` file and start ${the.gateway}. You should then see notifications in JConsole of one of two types: `session.created` or `session.closed`.

Next Step
---------

You have configured management with JMX for ${gateway.name.short}.

See Also
--------

-   The [realm](../admin-reference/r_conf_security.md#realm_element) element for reference information about the `user-principal-class` property.
-   ${gateway.name.long} [documentation](../index.md) for more information about. ${gateway.cap} administration.



IP Filtering with Kaazing Gateway ![This feature is available in KAAZING Gateway - Enterprise Edition](../images/enterprise-feature.png)
=================================

You can configure a Gateway service to only accept connections from remote hosts based on their IP addresses. The list of allowed IP addresses is called a [whitelist](https://en.wikipedia.org/wiki/Whitelist). Basically, an IP whitelist puts a Gateway service in a default posture of denying remote connections, permitting only a specific list of remote hosts from connecting to the service.

To use IP filtering, you have two options:


  0.  [Filter Remote IP Addresses Using the ip-filter Login Module Type](#filter-remote-ip-addresses-using-the-ip-filter-login-module-type) - On the Gateway, configure a Gateway [service](../../admin-reference/r_conf_service.html) with a login module of type ip-filter, set the login module’s success element’s value to required, and populate the whitelist element with at least one IP address or IP address range.
  0.  [Filter Remote IP Addresses Using a Custom Login Module](#filter-remote-ip-addresses-using-a-custom-login-module) - Create and use your own custom login module (LoginModule) and apply it to a Gateway service. From within the login module, remote IP addresses can be inspected and the login module can determine whether or not the connection should be allowed.

For most use cases, option \#1 is the easiest solution. Option \#2 is available for more advanced filtering. For example, if you want to look up IP addresses by country and accept or reject the IP addresses according to different countries or regions.

**Important:** Never rely entirely on IP filtering to determine whether a request for a remote connection is safe. IP filtering should only be used as part of a comprehensive security configuration for your network resources. Other security tools, such as auditing, firewalls, DMZs (see [Common Kaazing Gateway Production Topologies](../../admin-reference/c_topologies.html)), encryption (see [Secure Network Traffic with the Gateway](../../security/o_tls.html)), Kerberos (see [Configure Kerberos V5 Network Authentication](../../security/o_krb.html)), [Enterprise Shield](../../reverse-connectivity/o_rc_checklist.html), used along with IP filtering will help to ensure safe remote connections. For more information, see [About Security with Kaazing Gateway](../../security/c_sec_security.html).


Components and Tools
--------------------

Before you get started, review the components and tools used to enable geolocation security in the Gateway.

| Component or Tool              | Description                                                                                                                                                                                                                                                                                                                                                                                |
|:-------------------------------|:-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Kaazing Gateway service        | Any [service](../../admin-reference/r_conf_service.html) hosted on the Gateway can use IP filtering.                                                                                                                                                                                                                                                                                       |
| Login module type, `ip-filter` | A [login module](../../admin-reference/r_conf_security.html#loginmodule) communicates with a user database to authenticate user credentials and to determine a set of authorized roles. The ip-filter type is used to configure a whitelist within the Gateway configuration. For more information, see [Configure a Chain of Login Modules](../../security/p_aaa_config_lm.html).         |
| Custom login module            | A custom [LoginModule](http://docs.oracle.com/javase/8/docs/api/javax/security/auth/spi/LoginModule.html) can be created and applied to security settings of any service hosted by the Gateway. The custom LoginModule manages the IP whitelist outside of the Gateway configuration. For more information, see [Create Custom Login Modules](../../security/p_aaa_config_custom_lm.html). |
| Whitelist                      | The list and/or range of IP addresses that are accepted by the login module. For information on IP address notation, see [CIDR notation](https://en.wikipedia.org/wiki/Classless_Inter-Domain_Routing#CIDR_notation). There are many online CIDR calculation tools that can assist you in determining an IP address range.                                                                 |
| Remote host                    | A remote host is any network host connecting to the Gateway.                                                                                                                                                                                                                                                                                                                               |
| Remote host IP                 | The IP address identified by the HTTP Forwarded header ([RFC 7239](http://tools.ietf.org/html/rfc7239)) or IP header in the TCP datagram.                                                                                                                                                                                                                                                  |


How the Gateway Performs IP Filtering
-------------------------------------

When the Gateway receives a remote connection it attempts to use the HTTP `Forwarded` header ([RFC 7239](http://tools.ietf.org/html/rfc7239)) from the connection to retrieve the remote host’s IP address and pass it into the login module chain. If the `Forwarded` header is not set (or is set to an invalid IP address), then the Gateway will obtain the remote IP address from the IP header in the TCP datagram, and send that IP address to the login module chain.

Any `service` on the Gateway that is configured with a login module of type `ip-filter` and set to `required` compares the IP address obtained by the Gateway to those addresses or address ranges in the login module’s `whitelist`. If a match is found, the connection to the service is permitted. If no match is found, the Gateway refuses the connection and sends an authentication challenge to the remote host. The Gateway does not throw an exception.

**Notes:**

  -  Intermediaries such as load balancers must propagate the remote host IP address to the Gateway using the HTTP `Forwarded` header.
  -  The [login modules](../../admin-reference/r_conf_security.html#loginmodule) on a Gateway service are organized as a chain, with the Gateway using each login module as the Gateway proceeds down the chain. You can place the IP filtering login module anywhere in the chain.

Filter Remote IP Addresses Using the ip-filter Login Module Type
----------------------------------------------------------------

The following procedure demonstrates the how to configure the [login module](../../admin-reference/r_conf_security.html#loginmodule) element for a Gateway service and define the IP address whitelist for that service.

  1. If the Gateway is running, stop the Gateway. For information on starting and stopping the Gateway, see [Setting Up the Gateway](../../about/setup-guide.html).
  2. Open the Gateway configuration file (`GATEWAY_HOME/conf/gateway-config.xml`).
  3. Locate the `service` on which you want to apply IP filtering. For this example, the default **echo** service is used.

  ``` xml
  <service>
     <name>echo</name>
     <description>Simple echo service</description>
     <accept>ws://${gateway.hostname}:${gateway.base.port}/echo</accept>

     <type>echo</type>
     <realm-name>demo</realm-name>

     <authorization-constraint>
       <require-role>AUTHORIZED</require-role>
     </authorization-constraint>

     <cross-site-constraint>
       <allow-origin>*</allow-origin>
     </cross-site-constraint>
   </service>        
  ```

  4. Ensure that the `authorization-constraint` is enabled and `require-role` is set to `AUTHORIZED`.
  5. Locate the `realm-name` element for the service. Note the value for `realm-name`, such as **demo**. If you do not configure `realm-name`, then authentication and authorization are not enforced for the service.
  6. Navigate to the `Security configuration` section of the configuration file, where the security element is configured.
  7. Locate the `realm` with the same name as the `realm-name` element for the `service`.
  8. Under `login-modules`, enter a new login module, for example:

  ``` xml
    <realm>
        <name>demo</name>
        <description>Demo Realm</description>
        <authentication>
          <http-challenge-scheme>Application Basic</http-challenge-scheme>
          <login-modules>
            <login-module>
            ...
            <login-module>
              <type>file</type>
              <success>required</success>
              <options>
                <file>jaas-config.xml</file>
              </options>
            </login-module>
          </login-modules>
        </authentication>
      </realm>

  ```

  9. In the new login-module, enter the following:
      -  A `type` element with the value `ip-filter`
      - A `success` element with the value `required`
      - An `options` element with a child element `whitelist`
      - In the `whitelist` element, enter the IP addresses and/or range of IP addresses to allow. Be sure to verify that the IP addresses or ranges are entered correctly.

    Here is what a completed login-module looks like:

    ``` xml
    <realm>
          <name>demo</name>
          <description>Demo Realm</description>

          <authentication>
            <http-challenge-scheme>Application Basic</http-challenge-scheme>

            <login-modules>
              <login-module>
                <type>ip-filter</type>
                <success>required</success>
                <options>
                  ....
                  <whitelist>
                    192.168.10.0/24
                    192.172.10.0/24
                    2001:db8::/40
                    2001:db9::/40
                    192.168.10.0
                    192.168.10.8
                    192.168.10.16
                    2001:db8:0:1:1:1:1:1
                    2001:db8:0:1:1:1:1:2
                  </whitelist>
                </options>
              </login-module>
              <login-module>
                <type>file</type>
                <success>required</success>
                <options>
                  <file>jaas-config.xml</file>
                </options>
              </login-module>
            </login-modules>
          </authentication>
        </realm>
      <security>
    ```

    Note that IP addresses and ranges may be separated by whitespace or on separate lines. IP addresses and ranges must follow [CIDR notation](https://en.wikipedia.org/wiki/Classless_Inter-Domain_Routing#CIDR_notation).

  10. Save the configuration file and start the Gateway.
  11. Test the IP filter by connecting to the Gateway service using a remote host with an allowed IP address and a remote host with a forbidden IP address.


Filter Remote IP Addresses Using a Custom Login Module
------------------------------------------------------

The following [LoginModule](http://docs.oracle.com/javase/8/docs/api/javax/security/auth/spi/LoginModule.html) methods demonstrate how to obtain the remote host IP address which you can then use to accept or reject remote host connection attempts.

  1. Create the method to obtain the remote host IP address:

    ``` java
    private InetAddress getInetAddressFromCallback() {
        InetAddressCallback inetAddrCallback = new InetAddressCallback();

        try {
            handler.handle(new Callback[] { inetAddrCallback });
        }
        catch (IOException ex) {
            if (LOG.isTraceEnabled()) {
                LOG.trace("Encountered IOxception handling InetAddressCallback", ex);
            }
        }
        catch (UnsupportedCallbackException e) {
            if (LOG.isTraceEnabled()) {
                LOG.trace("Encountered UnsupportedCallbackException handling InetAddressCallback", e);
            }
        }

        return inetAddrCallback.getInetAddress();
    }
    ```

  For more information on `getInetAddressFromCallback()`, see the documentation under the **Server API Documentation** heading on the [table of contents](../../index.html).

  2. Use a `login()` method to invoke the `getInetAddressFromCallback()` method above:

  ``` java
  private InetAddress remoteAddress = getInetAddressFromCallback();
  if (remoteAddress == null) {
      throw new LoginException("Remote IP address is not available");
  }
  LOG.debug("Remote IP address: " + remoteAddress.getHostAddress());
  ```

  3. Integrate your custom login module into the Gateway. For more information, see [Create Custom Login Modules](../../security/p_aaa_config_custom_lm.html) and [Integrate an Existing Custom Login Module into the Gateway](../../security/p_aaa_integ_custom_lm.html).


See Also
--------

  - [Common Kaazing Gateway Production Topologies](../../admin-reference/c_topologies.html)
  - [Secure Network Traffic with the Gateway](../../security/o_tls.html)
  - [Configure Kerberos V5 Network Authentication](../../security/o_krb.html)
  - [About Security with Kaazing Gateway](../../security/c_sec_security.html)

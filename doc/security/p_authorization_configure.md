Configure Authorization
=========================================================================

After the client is authenticated, then authorization works to verify and grant access to sensitive resources. Access to services provided by KAAZING Gateway can be limited to only authorized users by setting authorization constraints. You configure the `authorization-constraint` element in the Gateway configuration to specify the user roles that are authorized to perform management operations for Gateway services. The `authorization-constraint` element contains the `require-role` element.

Before You Begin
----------------

This procedure is part of [Configure Authentication and Authorization](o_auth_configure.md):

1.  [Configure the HTTP Challenge Scheme](p_authentication_config_http_challenge_scheme.md)
2.  [Configure a Chain of Login Modules](p_auth_configure_login_module.md)
3.  [Configure a Challenge Handler on the Client](p_auth_configure_challenge_handler.md)
4.  **Configure Authorization**

To Configure Authorization
--------------------------

1.  On the server, update the Gateway configuration (for example, by editing `GATEWAY_HOME/conf/gateway-config.xml` in a text editor).
2.  Determine the type of service you want to define in the Gateway configuration.

    Define the `service` element to configure a service to run on the Gateway. The Gateway supports several types of services:

    -   balancer
    -   broadcast
    -   directory
    -   echo
    -   kerberos5.proxy
    -   session

    See the [Service Reference](../admin-reference/r_configure_gateway_service.md) documentation for more configuration information and examples of these services.

3.  Specify a service and define an authorization constraint.

    To specify a service and include constraints, you must set at least two properties on the service. Define an `accept` or a `connect` element that specifies the URL on which the service accepts connections, and define an `authorization-constraint` to configure the user roles that are authorized to access the service.

    For example, suppose that a client requests content from an URL for which the directory service for the URL is configured to have a security (authorization) constraint.

    ``` xml
    <service>
      <accept>http://${gateway.hostname}:${gateway.extras.port}/</accept>

      <type>directory</type>
      <properties>
        <directory>/</directory>
        <welcome-file>index.md</welcome-file>
      </properties>

      <authorization-constraint>
        <require-role>AUTHORIZED</require-role>
      </authorization-constraint>
    </service>
    ```

Notes
-----

-   Access rights are typically stored in the policy store that is associated with the application.
-   To configure `authorization-constraint` for highly available systems, you can set authorization constraints on either the balancer or the cluster members, or on both but the best practice is to set authorization constraints on both the balancer service and the cluster members. The following table describes the behavior you can expect when setting the `authorization-constraint` for a highly available Gateway environment.

    | If you set `authorization-constraint` on ... | Then ...                                                                                                                                                                                                                                                                                                                                                                                                                                                                |
    |----------------------------------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
    | balancer service only                        | Authentication occurs for all connections processed through the balancer service. Thus, the client will already have a valid token when the balancer redirects the client to a cluster member. The client can then use the token in its response to the `HTTP 401 Authorization Required` challenge from the Gateway. **Note:** This option does not protect direct connections to cluster members.                                                                  |
    | cluster-members only                         | Authentication occurs for all direct connections to Gateway services on cluster members. **Note:** This option does not protect connections made through the balancer service.                                                                                                                                                                                                                                                                                          |
    | balancer service and cluster members         | Authentication occurs whether the connection is made through the balancer service or directly to a cluster member. This is the best practice recommendation if you want to ensure that authentication occurs. With this option, consider configuring the challenge handler on the client to remember the credentials. Doing so provides the advantage of accessing the cluster member based on the credentials established through the balancer service authentication. |

    For more information about configuring high availability, see [Using the Gateway to Support High Availability](../high-availability/u_high_availability.md) and the [balancer](../admin-reference/r_configure_gateway_service.md#balancer) service in the [Service Reference](../admin-reference/r_configure_gateway_service.md) documentation.

Next Steps
----------

Ensure your clients are also configured for secure networking using Transport Layer Security (TLS) and its predecessor Secure Sockets Layer (SSL). Please see [Secure Network Traffic with the Gateway](o_tls.md) that provides the steps necessary to secure network traffic.

See Also
------------------------------

-   [Configure the Gateway](../admin-reference/o_configure_gateway_checklist.md)
-   [About Authentication and Authorization](c_auth_about.md)
-   [What Happens During Authentication](u_authentication_gateway_client_interactions.md)
-   [How Authentication and Authorization Work with the Gateway](u_auth_how_it_works_with_the_gateway.md)

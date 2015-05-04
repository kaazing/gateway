-   [Home](../../index.md)
-   [Documentation](../index.md)
-   About Protocol Injection

About Protocol Injection ![This feature is available in KAAZING Gateway - Enterprise Edition](images/enterprise-feature.png)
====================================================================

In many architectures, the back-end server may require specific information about the client, such as the user identity associated with a connection or session. Protocol injection enables you to securely propagate this information to your back-end server.

**Note:** While you can use protocol injection to inject information of your choosing, the most common usage is where your back-end server requires identity information about the client, and is the example for this topic.

Because the Gateway performs authentication, it is aware of the identity information associated with each client connection. After the the Gateway authenticates the client, the Gateway can inject a series of bytes conforming with the back-end server’s protocol, which can contain the identity information. By injecting this information, the back-end server can receive the information it expects in the format it expects.

You may choose to use protocol injection when you want to ensure that the client’s identity information comes from a validated and trusted source like the Gateway at the point of authentication. After you implement protocol injection, when the Gateway connects to the back-end server through the proxy service, the Gateway then sends the injected bytes. When the back-end server receives the expected bytes, the Gateway then sends data from the client, and traffic flows normally.

The following figure shows a high-level overview of how protocol injection works with the Gateway and your back-end server.

![Protocol Injection](../images/f-protocol-injection-web.jpg)
**Figure: Injecting Bytes into a Custom Protocol**

See Also
------------------------------

-   [Inject Bytes into a Custom Protocol](p_aaa_inject.md)
-   [Configure Authentication and Authorization](o_aaa_config_authentication.md)
-   [What Happens During Authentication](u_aaa_gw_client_interactions.md)
-   [How Authentication and Authorization Work with the Gateway](u_aaa_implement.md)

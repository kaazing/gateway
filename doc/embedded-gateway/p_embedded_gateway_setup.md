Set Up Your Development Environment
===================================

The process of embedding the Gateway uses a standard Java model for packaging external code into your Java application, and then uses the built-in methods of the API to allow you to control the embedded Gateway programmatically.

Before You Begin
----------------

This procedure is part of [Embed KAAZING Gateway in Your Java Application](../embedded-gateway/o_embedded_gateway.md):

1.  **Set Up Your Development Environment**
2.  [Create the Embedded Gateway Object](../embedded-gateway/p_embedded_gateway_object.md)
3.  [Use the Embedded Gateway Methods](../embedded-gateway/p_embedded_gateway_methods.md)
4.  [Configure Logging for an Embedded Gateway](../embedded-gateway/p_embedded_gateway_logging.md)
5.  [Monitor and Manage an Embedded Gateway](../embedded-gateway/p_embedded_gateway_monitor.md)
6.  [Troubleshoot Your Embedded Gateway](../embedded-gateway/p_embedded_gateway_troubleshoot.md)

**Note:** Before you can embed the Gateway, you need to install the Gateway as described in [Setting Up KAAZING Gateway](../about/setup-guide.md).

To Set Up Your Development Environment
--------------------------------------

An embedded Gateway uses the same Java Archive (JAR) file libraries as when the Gateway is running as a standalone application or a system service. You must add these libraries to your Java application to embed and run the Gateway.

To set up your Java application to use the Gateway libraries, include the Gateway library folder in the system or application classpath. The library folder is located in the directory `GATEWAY_HOME/lib/`. For more information about the `GATEWAY_HOME` directory, see [Setting Up KAAZING Gateway](../about/setup-guide.md).

Next Steps
----------

[Create the Embedded Gateway Object](../embedded-gateway/p_embedded_gateway_object.md)

Notes
-----

-   Refer to the [Java SE documentation](http://docs.oracle.com/javase/7/docs/technotes/guides/jar/index.html "Java SE 7 Java Archive (JAR)-related APIs & Developer Guides") for more information on how to package external code in your Java application.
-   The default environment is to have `GATEWAY_HOME` set to the parent directory of the location of the JAR file containing the Gateway class file. Any configuration files, content files, and the license file are searched for under this directory.

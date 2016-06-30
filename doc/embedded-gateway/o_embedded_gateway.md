Embed KAAZING Gateway in Your Java Application
==============================================

The following checklist provides the steps necessary to embed KAAZING Gateway into your Java application, and how to configure logging, monitoring, and management for the embedded Gateway:

| \#  | Step                                                                                                               | Topic or Reference                                                                   |
|-----|--------------------------------------------------------------------------------------------------------------------|--------------------------------------------------------------------------------------|
| 1   | Configure your development environment to use the necessary Java Archive (JAR) files for embedding the Gateway. | [Set Up Your Development Environment](../embedded-gateway/p_embedded_gateway_setup.md)                |
| 2   | Embed the Gateway class into your application and create a Gateway object.                                      | [Create the Embedded Gateway Object](../embedded-gateway/p_embedded_gateway_object.md)         |
| 3   | Learn how to use the publicly available methods to configure, start, and stop the Gateway.                      | [Use the Embedded Gateway Methods](../embedded-gateway/p_embedded_gateway_methods.md)          |
| 4   | Learn how to override the default Log4j settings used by the Gateway.                                           | [Configure Logging for an Embedded Gateway](../embedded-gateway/p_embedded_gateway_logging.md) |
| 5   | Learn how to override the default management configuration using an MBean server on an embedded Gateway.        | [Monitor and Manage an Embedded Gateway](../embedded-gateway/p_embedded_gateway_monitor.md)    |
| 6   | Troubleshoot the most common issues that occur when using an embedded Gateway.                                  | [Troubleshoot Your Embedded Gateway](../embedded-gateway/p_embedded_gateway_troubleshoot.md)         |


**Important:** 

-   Before you can embed the Gateway, you need to install the Gateway as described in [Setting Up KAAZING Gateway](../about/setup-guide.md).
-   Review the [API documentation](http://developer.kaazing.com/documentation/5.0/apidoc/server/gateway/server/api/index.html) for embedding the Gateway. The API documentation provides syntax and descriptions for all the methods associated with an embedded Gateway instance.

Introduction
------------

You can embed an instance of the Gateway into your Java application to be started, stopped, and configured programmatically from your application.

The Gateway includes the following features for embedding the Gateway and administering it once it is running:
-   An API for developers to use when embedding the Gateway into their Java applications.
-   Monitoring and management using MBeans that conform to the JMX specification and Java's built-in Java Management and Monitoring Console ([JConsole](http://docs.oracle.com/javase/7/docs/technotes/guides/management/jconsole.html)).

Why Embed the Gateway?
-------------------------

By embedding the Gateway within your Java application, you can start, stop, and configure the Gateway according to the logic in your software application. This configuration is very useful for integrating the Gateway into a separate software application or platform that relies on WebSocket for real-time communication.

Embedded Gateway Example
-------------------------------

Here is an example of how to create, configure, and launch the embedded Gateway:

``` java
Properties props = new Properties();
// specify configuration properties
props.setProperty("GATEWAY_HOME_PROPERTY", "/kaazing/");
props.setProperty("GATEWAY_CONFIG_PROPERTY", "/kaazing/config/");

// create the Gateway
Gateway gateway = GatewayFactory.createGateway();

// implement configuration properties
gateway.setProperties(props);

// launch the Gateway
gateway.launch();
```

Please see the remaining steps in this [checklist](#embed-kaazing-gateway-in-your-java-application) to learn how to embed the Gateway into your Java program.

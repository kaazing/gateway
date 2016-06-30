Configure Logging for an Embedded Gateway
=======================================================================

The default logging configuration for embedded Gateway uses the runtime logging tool log4j, and log4j logging is configured using the XML file log4j-config.xml in `GATEWAY_HOME/conf/`. You can make the following modifications to this configuration:

-   You can change where the embedded Gateway looks for the XML file in cases where you want to store your log4j XML file in a location outside of the default directory of the Gateway.
-   You can change the file the embedded Gateway uses when you want the logging data configured using a different XML file.
-   You can also override the default logging setting for the embedded Gateway to use a different logging system.

Before You Begin
----------------

This procedure is part of [Embed KAAZING Gateway in Your Java Application](../embedded-gateway/o_embedded_gateway.md):

1.  [Set Up Your Development Environment](../embedded-gateway/p_embedded_gateway_setup.md)
2.  [Create the Embedded Gateway Object](../embedded-gateway/p_embedded_gateway_object.md)
3.  [Use the Embedded Gateway Methods](../embedded-gateway/p_embedded_gateway_methods.md)
4.  **Configure Logging for an Embedded Gateway**
5.  [Monitor and Manage an Embedded Gateway](../embedded-gateway/p_embedded_gateway_monitor.md)
6.  [Troubleshoot Your Embedded Gateway](../embedded-gateway/p_embedded_gateway_troubleshoot.md)

To Override the Default Log4j Logging Settings
----------------------------------------------

Log4j settings are configured using an XML file named log4j-config.xml and stored by default in the directory `GATEWAY_HOME/conf/`. You can configure the embedded Gateway to use a different directory and file by modifying the fully qualified path to a log4J configuration file. To configure a different log4j file for the embedded Gateway, use the LOG4J\_CONFIG\_PROPERTY property as follows:

``` java
Properties props = new Properties();

props.setProperty("LOG4J_CONFIG_PROPERTY", "/home/kaazing/log4j.xml");

/* Windows Example
 * props.setProperty("LOG4J_CONFIG_PROPERTY", "C:\kaazing\conf\log4j.xml");
 */

Gateway gateway = GatewayFactory.createGateway();

gateway.setProperties(props);
```

**Note:** See [Apache log4j™](http://logging.apache.org/log4j/) for more information about configuring log4j.

To Use A Different Logging System
---------------------------------

If you want to use a logging system other than log4j (the default), you can override the default logging configuration for the embedded Gateway. You can use any logging system that supports the [Simple Logging Facade for Java (SLF4J)](http://www.slf4j.org/).

To override the default logging configuration of the embedded Gateway:

1.  Set the OVERRIDE\_LOGGING property to true. For example:

    ``` java
    Properties props = new Properties();

    props.setProperty("OVERRIDE_LOGGING", "true");

    Gateway gateway = GatewayFactory.createGateway();

    gateway.setProperties(props);
    ```

2.  Replace the log4j SLF4J bindings on your classpath with the new SLF4J framework. For example, to switch from log4j to java.util.logging, replace slf4j-log4j12-1.5.2.jar (located in `GATEWAY_HOME/lib/`) with slf4j-jdk14-1.6.4.jar.

**Note:** For more information, see [Binding with a logging framework at deployment time](http://www.slf4j.org/manual.html#binding) in the [SLF4J user manual](http://www.slf4j.org/manual.html).

Next Steps
----------

[Monitor and Manage an Embedded Gateway](../embedded-gateway/p_embedded_gateway_monitor.md)

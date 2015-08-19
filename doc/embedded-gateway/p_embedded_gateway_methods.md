Use the Embedded Gateway Methods
================================

This procedure describes how to use the methods available to the Gateway objects in your Java program.

Before You Begin
----------------

This procedure is part of [Embed KAAZING Gateway in Your Java Application](../embedded-gateway/o_embedded_gateway.md):

1.  [Set Up Your Development Environment](../embedded-gateway/p_embedded_gateway_setup.md)
2.  [Create the Embedded Gateway Object](../embedded-gateway/p_embedded_gateway_object.md)
3.  **Use the Embedded Gateway Methods**
4.  [Configure Logging for an Embedded Gateway](../embedded-gateway/p_embedded_gateway_logging.md)
5.  [Monitor and Manage an Embedded Gateway](../embedded-gateway/p_embedded_gateway_monitor.md)
6.  [Troubleshoot Your Embedded Gateway](../embedded-gateway/p_embedded_gateway_troubleshoot.md)

Embedded Gateway Methods
-------------------------------

An embedded Gateway interface exposes the following methods:

| Method                            | Description                                                      | Returns                                                                                                               |
|-----------------------------------|------------------------------------------------------------------|-----------------------------------------------------------------------------------------------------------------------|
| [setProperties()](#setproperties-method) | Configures the properties of an embedded Gateway.             | Nothing.                                                                                                              |
| [getProperties()](#getproperties-method) | Obtains the current set of properties on an embedded Gateway. | Name/value pairs for properties configured on the embedded Gateway.                                                |
| [launch()](#launch-method)               | Starts an embedded Gateway.                                   | Nothing. Any exceptions are thrown when the Gateway starts.                                                        |
| [destroy()](#destroy-method)             | Stops an embedded Gateway.                                    | Nothing. Any exceptions are thrown when the Gateway stops.                                                         |
| setMBeanServer()                  | Set an MBeanServer on an embedded Gateway.                    | Nothing. For more information, see [Monitor and Manage an Embedded Gateway](../embedded-gateway/p_embedded_gateway_monitor.md). |

### Example

Here is an example of how to create, configure, and launch the embedded Gateway:

``` java
Properties props = new Properties();
// specify configuration properties
props.setProperty(Gateway.GATEWAY_HOME_PROPERTY, "/kaazing/");
props.setProperty(Gateway.GATEWAY_CONFIG_PROPERTY, "/kaazing/config/");

// or as hard-coded keys:
// props.setProperty("GATEWAY_HOME", "/kaazing/");
// props.setProperty("GATEWAY_CONFIG", "/kaazing/config/");

// create the Gateway
Gateway gateway = GatewayFactory.createGateway();

// implement configuration properties
gateway.setProperties(props);

// launch the Gateway
gateway.launch();
```

**Important:** You must restart the Gateway to apply configuration changes. A good practice is to configure the Gateway settings using the setProperties() method before using the launch() method to run the Gateway.

setProperties() Method
----------------------

By default, the embedded Gateway uses the properties set in the `GATEWAY_HOME/conf/gateway-config.xml` configuration file. The `setProperties()` method configures property names and values for the embedded Gateway that override the settings in the default configuration file. The method is optional and you do not need to change any default settings to use the embedded Gateway.

**Note:** For information on the default properties of the Gateway configuration file, see [About KAAZING Gateway Configuration File Elements and Properties](../admin-reference/c_configure_gateway_concepts.md#about-kaazing-gateway-configuration-file-elements-and-properties).
### Syntax and Example

`void setProperties(java.util.Properties properties);`

-   **properties:** The properties argument contains the name/value pairs used to configure the embedded Gateway. For example, you can set the location of GATEWAY\_HOME, and GATEWAY\_CONFIG.

For a list of the properties that you can modify, see the [API Documentation](http://developer.kaazing.com/documentation/5.0/apidoc/server/gateway/server/api/index.html).

Here is an example of how to configure the GATEWAY\_HOME and GATEWAY\_CONFIG properties before running the Gateway:

``` java
Properties props = new Properties();

props.setProperty("GATEWAY_HOME_PROPERTY", "/kaazing/");
props.setProperty("GATEWAY_CONFIG_PROPERTY", "/kaazing/configs/3.3");

Gateway gateway = GatewayFactory.createGateway();

gateway.setProperties(props);
```

getProperties() Method
----------------------

The `getProperties()` method returns the current set of properties (name/value pairs) used to configure the embedded Gateway, including the names of properties in the `GATEWAY_HOME/conf/gateway-config.xml` and their values.

### Syntax and Example

`java.util.Properties getProperties()`

The following example code writes out the Gateway configuration properties:

``` java
Properties props = new Properties();

props = gateway.getProperties();
System.out.println(props);
```

launch() Method
---------------

The `launch()` method starts the embedded Gateway and initializes and starts up all the services configured in `GATEWAY_HOME/conf/gateway-config.xml` (such as JMX services), including binding to the hosts and ports specified in the `<accept>` child of a `<service>` element. If the Gateway is running and `launch()` is called, an exception is thrown. After calling `destroy()`, the Gateway can be launched again.

This method throws any exceptions when the embedded Gateway starts. For example, if the embedded Gateway is unable to read the configuration file, unable to bind to a network port specified in a `<service>` configuration, or if the value of a `<directory>` child of a \<property\> element does not contain a valid directory.

### Syntax and Example

`void launch() throws java.lang.Exception`

Here is an example of how to launch the embedded Gateway:

``` java
Gateway gateway = GatewayFactory.createGateway();
gateway.launch();
```

destroy() Method
----------------

The `destroy()` method stops the embedded Gateway object that was launched using launch(). If the embedded Gateway has been stopped, this method has no effect. The `destroy()` does not [finalize](http://docs.oracle.com/javase/7/docs/api/java/lang/Object.html#finalize()) the Gateway object and it does not modify configuration files.

This method throws exceptions when the embedded Gateway service stops (also called “destroy time”). An example of an exception would be if the embedded Gateway is unable to clean up management information from the MBeanServer, which might happen if the MBeanServer is stopped unexpectedly.

### Syntax and Example

`void destroy(); throws Exception`

Here is an example of how to stop the embedded Gateway:

``` java
Gateway gateway = GatewayFactory.createGateway();
gateway.launch();
gateway.destroy();
```

Next Steps
----------

[Configure Logging for an Embedded Gateway](../embedded-gateway/p_embedded_gateway_logging.md)

Monitor and Manage an Embedded Gateway
====================================================================

By default, an embedded Gateway uses the \<management\> element in the Gateway configuration file for its management configuration. By using this element, you can track and manage user sessions using Java's built-in Java Management and Monitoring Console ([JConsole](http://docs.oracle.com/javase/7/docs/technotes/guides/management/jconsole.html)) and the management service configured on the Gateway. This is the same configuration used by the Gateway when it is run as a standalone application, system service, or as an embedded service.

You can override the default management configuration using an MBean server on an embedded Gateway. The embedded Gateway will populate the MBean server you specify with information about configured services and current sessions connected to the Gateway.

For detailed information on monitoring the Gateway, see [Monitor the Gateway](../management/o_monitor.md).

Before You Begin
----------------

This procedure is part of [Embed KAAZING Gateway in Your Java Application](../embedded-gateway/o_embedded_gateway.md):

1.  [Set Up Your Development Environment](../embedded-gateway/p_embedded_gateway_setup.md)
2.  [Create the Embedded Gateway Object](../embedded-gateway/p_embedded_gateway_object.md)
3.  [Use the Embedded Gateway Methods](../embedded-gateway/p_embedded_gateway_methods.md)
4.  [Configure Logging for an Embedded Gateway](../embedded-gateway/p_embedded_gateway_logging.md)
5.  **Monitor and Manage an Embedded Gateway**
6.  [Troubleshoot Your Embedded Gateway](../embedded-gateway/p_embedded_gateway_troubleshoot.md)

To Configure the Embedded Gateway to Use a Different MBean server
-----------------------------------------------------------------

1.  Remove the `<management>` element from the Gateway configuration file (`GATEWAY_HOME/conf/gateway-config.xml`).
2.  Use the `setMBeanServer()` method in your Java application to specify an MBean server for the embedded Gateway.

### Syntax and Example

`void setMBeanServer(javax.management.MBeanServer server)`

The `server` argument is the MBean server the embedded Gateway will populate with its management information.

Here is an example of how to create a registry, an MBean server, and a connector and then pass the MBean server to the embedded Gateway:

``` java
import javax.management.*;
import java.lang.management.*;
import javax.management.remote.*;

import org.kaazing.gateway.server.GatewayFactory;
import org.kaazing.gateway.server.Gateway;

public class SimpleAgent {
private MBeanServer mbs = null;

    public init() {
      try {
         // Create the RMI Registry
         LocateRegistry.createRegistry(9999);

         // Get the platform MBeanServer
         mbs = ManagementFactory.getPlatformMBeanServer();

         // Create an RMI connector and start it
         JMXServiceURL url =
             new JMXServiceURL("service:jmx:rmi:///jndi/rmi://localhost:9999/server");
         JMXConnectorServer cs = JMXConnectorServerFactory.newJMXConnectorServer(url, null, mbs);
         cs.start();

         // Unique identification of MBeans
         Hello helloBean = new Hello();
         ObjectName helloName = null;

         // Uniquely identify the MBeans and register them with the MBeanServer
         helloName = new ObjectName("SimpleAgent:name=hellothere");
         mbs.registerMBean(helloBean, helloName);

         // Create the Gateway, set the MBeanServer, and launch the Gateway
         Gateway gw = GatewayFactory.createGateway();
         gw.setMBeanServer(mbs);
         gw.launch();
      } catch(Exception e) {
         e.printStackTrace();
      }
    }

    public static void main(String argv[]) {
      SimpleAgent agent = new SimpleAgent();
      agent.init();
      System.out.println("SimpleAgent is running...");
    }
}
```

Next Steps
----------

[Troubleshoot Your Embedded Gateway](../embedded-gateway/p_embedded_gateway_troubleshoot.md)

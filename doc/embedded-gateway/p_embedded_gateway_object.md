Create the Embedded Gateway Object
==================================

This procedure describes how to create Gateway objects within your Java program.

Before You Begin
----------------

This procedure is part of [Embed KAAZING Gateway in Your Java Application](../embedded-gateway/o_embedded_gateway.md):

1.  [Set Up Your Development Environment](../embedded-gateway/p_embedded_gateway_setup.md)
2.  **Create the Embedded Gateway Object**
3.  [Use the Embedded Gateway Methods](../embedded-gateway/p_embedded_gateway_methods.md)
4.  [Configure Logging for an Embedded Gateway](../embedded-gateway/p_embedded_gateway_logging.md)
5.  [Monitor and Manage an Embedded Gateway](../embedded-gateway/p_embedded_gateway_monitor.md)
6.  [Troubleshoot Your Embedded Gateway](../embedded-gateway/p_embedded_gateway_troubleshoot.md)

To Create an Embedded Gateway Object
-------------------------------------------

1.  Import the embedded Gateway class into your Java application:

    ``` java
    import org.kaazing.gateway.server.GatewayFactory;
    import org.kaazing.gateway.server.Gateway;
    ```

2.  Create the embedded Gateway using the following publicly available static method:

    ``` java
    GatewayFactory.createGateway();
    ```

### Example

The following example demonstrates how you can embed the Gateway:

``` java
import org.kaazing.gateway.server.GatewayFactory;
import org.kaazing.gateway.server.Gateway;

public class GatewayTest {
  private Gateway gw;

  public GatewayTest() {
    gw = GatewayFactory.createGateway();
  }
}
```

Next Steps
----------

[Use the Embedded Gateway Methods](../embedded-gateway/p_embedded_gateway_methods.md)

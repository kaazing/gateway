Troubleshoot Your Embedded Gateway
==================================

This procedure provides troubleshooting information for the most common issues that occur when using an embedded Gateway.

Before You Begin
----------------

This procedure is part of [Embed KAAZING Gateway in Your Java Application](../embedded-gateway/o_embedded_gateway.md):

1.  [Set Up Your Development Environment](../embedded-gateway/p_embedded_gateway_setup.md)
2.  [Create the Embedded Gateway Object](../embedded-gateway/p_embedded_gateway_object.md)
3.  [Use the Embedded Gateway Methods](../embedded-gateway/p_embedded_gateway_methods.md)
4.  [Configure Logging for an Embedded Gateway](../embedded-gateway/p_embedded_gateway_logging.md)
5.  [Monitor and Manage an Embedded Gateway](../embedded-gateway/p_embedded_gateway_monitor.md)
6.  **Troubleshoot Your Embedded Gateway**

To Troubleshoot Your Embedded Gateway
--------------------------------------------

The following issue can go wrong when using an embedded Gateway:

### Error: Failed to load GatewayFactory implementation class

This error occurs if the embedded Gateway could not be created. Review the code implementing the embedded Gateway class to confirm whether the error is caused by a programming issue. The error could also appear if the configuration file or a library file is missing or inaccessible.

See Also
--------

See [Troubleshoot the Gateway](../troubleshooting/o_troubleshoot.md) for errors that might occur with the Gateway.

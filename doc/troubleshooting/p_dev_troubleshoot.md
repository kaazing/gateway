Troubleshoot Your Clients
=========================

### Troubleshoot All Clients

#### "403 Forbidden Error" When a Client Attempts to Connect to the Gateway

**Cause:** A "403 Forbidden" error displays in the log after launching the Gateway and a client attempts to connect to it. This error can happen if the `realm-name` element for a service is set, but the `authorization-constraint` element for the same service is not properly configured in the Gateway configuration.

**Solution:** A service protected by user authentication and authorization must have both the realm and authorization-constraint configured. See the documentation for `realm-name` and `authorization-constraint`. Alternatively, if you do not want to configure security on the service, you can omit or comment out both the `realm-name` and `authorization-constraint` elements from the service.

### Troubleshoot Specific Clients

The following topics describe how to troubleshoot clients built using the KAAZING Gateway Client APIs:

| Client Platform      | Troubleshooting and Logging WebSocket Clients                                                                                                                                                       | Troubleshooting and Logging JMS Clients                                                                                                                             |
| -------------------- | --------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| JavaScript           | [Display Logs for the JavaScript Client](https://github.com/kaazing/javascript.client/blob/develop/gateway/doc/p_clientlogging_js.md)                                                                                                                           | [Troubleshoot Your JavaScript JMS Client](https://github.com/kaazing/enterprise.javascript.client/blob/develop/jms/doc/p_dev_js_tshoot_jms.md) and [Display Logs for the JavaScript JMS Client](https://github.com/kaazing/enterprise.javascript.client/blob/develop/jms/doc/p_clientlogging_js_jms.md)           |
| Objective-C          | [Troubleshoot Your Objective-C Client](../ios/p_dev_objc_tshoot.md),  [Display Logs for the Objective-C Client](../ios/p_dev_objc_log.md)                                                 | [Troubleshoot Your Objective-C JMS Client](../ios/p_dev_objc_tshoot.md) and [Display Logs for the Objective-C JMS Client](../ios/p_dev_objc_log.md)       |
| Android              | [Display Logs for the Android Client](../java/p_dev_android_log.md)                                                                                                                          | [Display Logs for the Android JMS Client](../java/p_dev_android_log.md)                                                                                      |
| Flash                | [Troubleshoot Your Flash Client](../flash/p_dev_flash_tshoot.md), [Display Logs for the Flash Client](../flash/p_clientlogging_flash.md)                                                    | [Troubleshoot Flash JMS Clients](../flash/p_dev_flash_tshoot.md) and [Display Logs for the Flash JMS Client](../flash/p_clientlogging_flash.md)             |
| .NET and Silverlight | [Troubleshoot Your Microsoft .NET and Silverlight Clients](../windows/p_dev_dotnet_tshoot.md) and  [Display Logs for .NET and Silverlight Clients](../windows/p_clientlogging_dotnet.md)      | [Display Logs for .NET and Silverlight JMS Clients](../windows/p_clientlogging_dotnet.md)                                                                        |
| Java                 | [Troubleshoot Your Java Client](../java/p_dev_java_tshoot.md), [Display Logs for the Java Client](../java/p_dev_java_logging.md)                                                            | [Troubleshoot Java JMS Clients](../java/p_dev_java_tshoot.md) and  [Display Logs for the Java JMS Client](../java/p_dev_java_logging.md)                    |


Also, see [Troubleshoot the Gateway](o_troubleshoot.md) for help troubleshooting your configuration, clusters, and security.

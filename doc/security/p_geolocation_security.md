IP Filtering with Kaazing Gateway
=================================

You can configure a Gateway service to only accept connections from remote hosts based on their IP addresses. The list of allowed IP addresses is called a [whitelist](https://en.wikipedia.org/wiki/Whitelist). Basically, an IP whitelist puts a Gateway service in a default posture of denying remote connections, permitting only a specific list of remote hosts from connecting to the service.

In order to use IP filtering, you have two options:


  -  Filter Remote IP Addresses Using the ip-filter Login Module Type - On the Gateway, configure a Gateway [service](../../admin-reference/r_conf_service.html) with a login module of type ip-filter, set the login module’s success element’s value to required, and populate the whitelist element with at least one IP address or IP address range.
  -  Filter Remote IP Addresses Using a Custom Login Module - Create and use your own custom login module (LoginModule) and apply it to a Gateway service. From within the login module, remote IP addresses can be inspected and the login module can determine whether or not the connection should be allowed.

Components and Tools
--------------------


Filter Remote IP Addresses Using the ip-filter Login Module Type
----------------------------------------------------------------



Filter Remote IP Addresses Using a Custom Login Module
------------------------------------------------------





See Also
--------

-   []()

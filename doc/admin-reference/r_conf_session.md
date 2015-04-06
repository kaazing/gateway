-   [Home](../../index.md)
-   [Documentation](../index.md)
-   Administration with ${gateway.name.short}

Session Reference (Deprecated) ${enterprise.logo.jms}
=====================================================

This document describes the elements and properties for the deprecated `session` element that was supported in previous releases for ${gateway.name.short} configuration.

<a name="configuring"></a>Overview
----------------------------------

This element has been deprecated. The `session` element was used to associate a session with one or more services by matching the domain of each service's accept URL.

<a name="descelements"></a>Structure
------------------------------------

${the.gateway.cap} configuration file (`gateway-config.xml` or `gateway-config-minimal.xml`) defines the following configuration elements contained in the top-level `gateway-config` element:

-   [gateway-config](r_conf_gwconfig.md)
    -   [session](#session) (deprecated)

<a name="session"></a>session
-----------------------------

The `session` element has been deprecated. If you are using an existing configuration that includes the `session` element, you can continue to use it. However, the following table describes the new properties that have been added to ${the.gateway} configuration to replace the `session` element.

| Deprecated `session` Element Property | Description                                                                                                                 |
|---------------------------------------|-----------------------------------------------------------------------------------------------------------------------------|
| service-domain                        | ${the.gateway.cap} manages this functionality automatically.                                                                |
| authentication-scheme                 | Use [`http-challenge-scheme`](r_conf_security.md#challenge_scheme)in the `security` element instead.                      |
| authentication-connect                | Use the Kerberos challenge handler instead. For more information, see [Client API Documentation](../index.md#api_topics). |
| realm-name                            | Use [`realm-name`](r_conf_service.md#realm-name) in the [`service`](r_conf_service.md) element instead.                 |
| encryption-key-alias                  | ${the.gateway.cap} manages this functionality automatically.                                                                |
| inactivity-timeout                    | Use [`authorization-timeout`](r_conf_security.md#auth_timeout) in the `security` element instead.                         |

Summary
-------

In this document, you learned about ${the.gateway} `session` element and how to specify it in your Gateway configuration file. For more information about the location of the configuration files and starting ${the.gateway}, see ${setting.up.inline}. For more information about ${gateway.name.short} administration, see the [documentation](../index.md).

</div>


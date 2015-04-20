-   [Home](../../index.md)
-   [Documentation](../index.md)
-   Upgrade KAAZING Gateway

Upgrade the Gateway
============================================

The upgrade procedure migrates the existing KAAZING Gateway (including any associated applications) into KAAZING Gateway release 5.0. The following checklist provides the steps necessary to upgrade KAAZING Gateway to the next release.

<table class="checklist">
<tr>
<th scope="col" width="408">
Step
</th>
<th width="333" scope="col">
Topic or Reference
</th>
</tr>

<tr>
<td>
Understand Gateway release number strategy and the upgrade process.
</td>
<td>
[About KAAZING Gateway Releases and Upgrades](c_upgrade_gw.md)
</td>
</tr>

<tr>
<td>
Upgrade to the new Gateway release  using one of the following:
-   Using the Windows or Linux installer to install the Gateway into conventional (default) operating system locations.
-   Unpacking the .zip for Windows or .tar.gz for Linux, UNIX, or Mac) into any directory location.


</td>
<td>
[Setting Up KAAZING Gateway](../about/setup-guide.md) See [Upgrade Options](c_upgrade_gw.md#upgradeoptions) for help choosing an upgrade methodology.
</td>
</tr>
<tr>
<td>
Migrate your existing Gateway configuration file and other files to the new Gateway installation.
</td>
<td>
[Migrate the Gateway Configuration](p_migrate_gwconfig.md)
</td>
</tr>
<tr>
<td>
If you are using a Gateway in a cluster, then you need to perform additional steps to migrate the cluster members.
</td>
<td>
[Clustering and Load Balancing Migration](../high-availability/u_ha.md#migrate)**Note:** Upgrade all members of the Gateway cluster before you migrate any clients.
</td>
</tr>
<tr>
<td>
Verify the Gateway is upgraded.
</td>
<td>
View the release number via Command Center, the Gateway start-up output, or the log file.
</td>
</tr>
<tr>
<td>
If you are using JMX MBeans to monitor the Gateway, update the MBean paths.
</td>
<td>
-   In 4.*x* and 5.*x*, the MBeans path is `org.kaazing.gateway.server.management > gateways > process_id@hostname > services`.
-   In 3.x, the MBeans path is `org.kaazing.gateway.server.management > services`.

See [Monitor with JMX](../management/p_monitor_jmx.md) for more information. Note that some service names may have changed or been deprecated.
</td>
</tr>
<tr>
<td>
Learn how clients built using KAAZING Gateway can take advantage of the KAAZING Gateway client libraries.
</td>
<td>
[General Client Information](../dev-general/c_general_client_information.md)
</td>
</tr>
<tr>
<td>
(Recommended) Migrate Kaazing JavaScript and Java clients that use the ByteSocket API on releases 3.3 (and later releases) to the WebSocket API in release 4.*x*.
</td>
<td>
-   [Migrate JavaScript Applications to KAAZING Gateway 4.*x*.](../dev-js/p_dev_js_migrate.md)
-   [Migrate WebSocket and ByteSocket Applications to KAAZING Gateway 5.0](../dev-java/p_dev_java_migrate.md)

</td>
</tr>
<tr>
<td>
(Recommended) For AMQP API changes, migrate Kaazing JavaScript and Java clients running KAAZING Gateway from release 3.3 (and later releases) to release 4.*x* and use the new AMQP client libraries.
</td>
<td>
-   [Migrate JavaScript Applications to KAAZING Gateway 4.*x*.](../dev-js/p_dev_js_client_amqp.md#migrate)
-   [Migrate Java AMQP Applications to KAAZING Gateway.](../dev-java/p_dev_java_client_amqp.md#migrate)

</td>
</tr>

<tr>
<td>
(Recommended) For JMS API changes, migrate Kaazing JavaScript and Java clients running KAAZING Gateway from release 3.3 (and later releases) to release 4.*x* and use the new JMS Client library.
</td>
<td>
-   [Migrate iOS Applications to KAAZING Gateway.](../dev-objc/p_dev_objc_client.md#migrate)
-   [Migrate JavaScript Applications to KAAZING Gateway.](../dev-js/p_dev_js_client_amqp.md#migrate)
-   [Migrate Java AMQP Applications to KAAZING Gateway.](../dev-java/p_dev_java_client_amqp.md#migrate)

</td>
</tr>

</table>

<a name="seealso"></a>See Also
------------------------------

-   KAAZING Gateway [documentation](../index.md).





-   [Home](../../index.md)
-   [Documentation](../index.md)
-   Upgrade ${gateway.name.long}

Upgrade ${the.gateway}${enterprise.logo.jms}
============================================

The upgrade procedure migrates the existing ${gateway.name.long} (including any associated applications) into ${gateway.name.long} release ${gateway.version}. The following checklist provides the steps necessary to upgrade ${gateway.name.short} to the next release.

<table class="checklist">
<tr>
<th scope="col" width="408">
Step
</th>
<th width="333" scope="col">
Topic or Reference
</th>
</tr>
${begin.comment}
<tr>
<td>
Understand ${gateway.cap} release number strategy and the upgrade process.
</td>
<td>
[About ${gateway.name.long} Releases and Upgrades](c_upgrade_gw.md)
</td>
</tr>
${end.comment}
<tr>
<td>
Upgrade to the new ${gateway.cap} release ${begin.comment} using one of the following:
-   Using the Windows or Linux installer to install ${the.gateway} into conventional (default) operating system locations.
-   Unpacking the .zip for Windows or .tar.gz for Linux, UNIX, or Mac) into any directory location.

${end.comment}
</td>
<td>
${setting.up.inline} ${begin.comment}See [Upgrade Options](c_upgrade_gw.md#upgradeoptions) for help choosing an upgrade methodology.${end.comment}
</td>
</tr>
<tr>
<td>
Migrate your existing ${gateway.cap} configuration file and other files to the new ${gateway.cap} installation.
</td>
<td>
[Migrate ${the.gateway} Configuration](p_migrate_gwconfig.md)
</td>
</tr>
<tr>
<td>
If you are using a ${gateway.cap} in a cluster, then you need to perform additional steps to migrate the cluster members.
</td>
<td>
[Clustering and Load Balancing Migration](../high-availability/u_ha.md#migrate)**Note:** Upgrade all members of ${the.gateway} cluster before you migrate any clients.
</td>
</tr>
<tr>
<td>
Verify ${the.gateway} is upgraded.
</td>
<td>
View the release number via ${console.name}, ${the.gateway} start-up output, or the log file.
</td>
</tr>
<tr>
<td>
If you are using JMX MBeans to monitor ${the.gateway}, update the MBean paths.
</td>
<td>
-   In 4.*x* and 5.*x*, the MBeans path is `org.kaazing.gateway.server.management > gateways > process_id@hostname > services`.
-   In 3.x, the MBeans path is `org.kaazing.gateway.server.management > services`.

See [Monitor with JMX](../management/p_monitor_jmx.md) for more information. Note that some service names may have changed or been deprecated.
</td>
</tr>
<tr>
<td>
Learn how clients built using ${gateway.name.long} can take advantage of the ${gateway.name.long} client libraries.
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
-   [Migrate JavaScript Applications to ${gateway.name.long} 4.*x*.](../dev-js/p_dev_js_migrate.md)
-   [Migrate WebSocket and ByteSocket Applications to ${gateway.name.long} ${gateway.version}](../dev-java/p_dev_java_migrate.md)

</td>
</tr>
<tr>
<td>
(Recommended) For AMQP API changes, migrate Kaazing JavaScript and Java clients running ${gateway.name.short} from release 3.3 (and later releases) to release 4.*x* and use the new AMQP client libraries.
</td>
<td>
-   [Migrate JavaScript Applications to ${gateway.name.long} 4.*x*.](../dev-js/p_dev_js_client_amqp.md#migrate)
-   [Migrate Java AMQP Applications to ${gateway.name.long}.](../dev-java/p_dev_java_client_amqp.md#migrate)

</td>
</tr>
${upgrade.begin.jms.only}
<tr>
<td>
(Recommended) For JMS API changes, migrate Kaazing JavaScript and Java clients running ${gateway.name.short} from release 3.3 (and later releases) to release 4.*x* and use the new JMS Client library.
</td>
<td>
-   [Migrate iOS Applications to ${gateway.name.long}.](../dev-objc/p_dev_objc_client.md#migrate)
-   [Migrate JavaScript Applications to ${gateway.name.long}.](../dev-js/p_dev_js_client_amqp.md#migrate)
-   [Migrate Java AMQP Applications to ${gateway.name.long}.](../dev-java/p_dev_java_client_amqp.md#migrate)

</td>
</tr>
${upgrade.end.jms.only}
</table>
${begin.comment}
<a name="seealso"></a>See Also
------------------------------

-   ${gateway.name.long} [documentation](../index.md).

${end.comment}



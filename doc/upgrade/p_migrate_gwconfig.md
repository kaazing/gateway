-   [Home](../../index.md)
-   [Documentation](../index.md)
-   Upgrade ${gateway.name.long}

Migrate ${the.gateway} Configuration${enterprise.logo.jms}
==========================================================

This topic explains how to migrate your existing configuration to a new ${gateway.cap} release. Before you migrate your ${gateway.cap} configuration. review the procedures in [Upgrade ${the.gateway}](o_upgrade.md).

<table>
<colgroup>
<col width="33%" />
<col width="33%" />
<col width="33%" />
</colgroup>
<thead>
<tr class="header">
<th align="left">#</th>
<th align="left">Step</th>
<th align="left">Topic or Reference</th>
</tr>
</thead>
<tbody>
<tr class="odd">
<td align="left">1</td>
<td align="left">Stop ${the.gateway} or ${gateway.cap} cluster if currently running.</td>
<td align="left">${setting.up.inline} for instructions on how to stop ${the.gateway}.</td>
</tr>
<tr class="even">
<td align="left">2</td>
<td align="left">Back up any existing user-modified files (such as ${the.gateway} configuration file, the <code>log4j-config.xml</code>, the keystore file, license file, and so on).</td>
<td align="left">${upgrade.dirs}</a></td>
</tr>
<tr class="odd">
<td align="left">3</td>
<td align="left">Copy your current (old) ${gateway.cap} configuration file to the new ${gateway.cap} installation.</td>
<td align="left">Copy ${the.gateway} configuration file from your old configuration (for example. <code>gateway-config.xml</code>) to the <code>GATEWAY_HOME/conf</code> directory in the new ${gateway.cap} installation. <strong>Note:</strong> If this is a ${gateway.cap} cluster, then perform this step on all cluster members before proceeding to the next step. See <a href="../high-availability/u_ha.md#migrate">Clustering and Load Balancing Migration.</a></td>
</tr>
<tr class="even">
<td align="left">4</td>
<td align="left">If you modified any files (other than ${the.gateway} configuration file) in the old ${gateway.cap} configuration, then edit those same files in the new ${gateway.cap} installation to apply your modifications. Note: Do not edit ${the.gateway} configuration file now because it will be updated automatically when you start ${the.gateway} in the next step.</td>
<td align="left">Except for ${the.gateway} configuration file, you must manually edit files in the new configuration to make the same customizations you made in the old configuration. For example, modified files might include the <code>log4j-config.xml</code>, <code>jaas-config.xml</code>, the keystore file, the license file, and so on.
<ul>
<li>For help with the <code>log4j-config.xml</code> file, see <a href="../embedded/p_embed_logging.md">Configure Logging for an Embedded Gateway</a>.</li>
<li>For information about keystores and truststores, see <a href="../security/p_tls_trusted.md">Secure ${the.gateway} Using Trusted Certificates</a>.</li>
<li>For licensing information and the license file, see <a href="http://kaazing.com/legal/license/">licenses</a> on <code>kaazing.com</code>.</li>
</ul>
<strong>Note:</strong> If this is a ${gateway.cap} cluster, then perform this step on all cluster members before proceeding to the next step.</td>
</tr>
<tr class="odd">
<td align="left">5</td>
<td align="left">Start the new ${gateway.cap}.</td>
<td align="left">When ${the.gateway} starts it uses the old configuration file that you copied to the new <em>GATEWAY_HOME</em> installation. If ${the.gateway} configuration file contains out-of-date configuration elements or namespace declaration, then ${the.gateway} automatically updates the configuration information in memory and writes a modified configuration file to disk and appends a <code>.new</code> extension (for example, <code>gateway-config.xml.new</code>). See <a href="../admin-reference/c_conf_concepts.md">About ${gateway.cap} Configuration</a> to learn more about the <code>.new</code> configuration file.</td>
</tr>
<tr class="even">
<td align="left">6</td>
<td align="left">Stop the new ${gateway.cap} and replace the ${gateway.cap} configuration file, if necessary.</td>
<td align="left">If a <code>.new</code> configuration file was generated when you started ${the.gateway}, then delete the current (old) configuration file and rename the <code>.new</code> configuration file to <code>gateway-config.xml</code>) so that ${the.gateway} uses the up-to-date configuration the next time it starts. This is also a good time to make other modifications, if any, to the configuration file. For example, if you are running a ${gateway.cap} cluster you should update the cluster configuration with the changes suggested in <a href="../high-availability/u_ha.md#migrate">Clustering and Load Balancing Migration</a>. Also, see:
<ul>
<li><a href="../admin-reference/c_conf_concepts.md">About ${gateway.cap} Configuration</a> to learn how to ensure the elements used in your configuration match what is supported by the namespace.</li>
</ul></td>
</tr>
<tr class="odd">
<td align="left">7</td>
<td align="left">Start ${the.gateway} or ${gateway.cap} cluster and check the version number on the start log or use ${console.name} to check that you are using the new version.</td>
<td align="left">See:
<ul>
<li>${setting.up.inline} for starting instructions.</li>
<li><a href="../management/o_admin_monitor.md">Monitor ${the.gateway}</a> to learn about monitoring with ${console.name}.</li>
</ul>
<strong>Note:</strong> Upgrade all members of ${the.gateway} cluster before you migrate any clients. See <a href="../high-availability/u_ha.md#migrate">Clustering and Load Balancing Migration</a>.</td>
</tr>
<tr class="even">
<td align="left">8</td>
<td align="left">Complete the upgrade instructions in <a href="o_upgrade.md">Upgrade ${the.gateway}</a>.</td>
<td align="left">Return to <a href="o_upgrade.md">Upgrade ${the.gateway}</a> and follow the steps to to verify the configuration, migrate clients, and any other remaining migration work.</td>
</tr>
</tbody>
</table>

${begin.comment}
<a name="seealso"></a>See Also
------------------------------

-   ${gateway.name.long} documentation that is available online at [kaazing.org](http://kaazing.org/)

${end.comment}



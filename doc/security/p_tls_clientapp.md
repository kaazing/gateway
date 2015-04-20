-   [Home](../../index.md)
-   [Documentation](../index.md)
-   Secure Clients and Web Browsers with a Self-Signed Certificate

<a name="tls_config_clients"></a>Secure Clients and Web Browsers with a Self-Signed Certificate
===============================================================================================

<span class="alert">**Warning:** Using self-signed certificates can result in unpredictable behavior because various browsers, plug-ins, operating systems, and related run-time systems handle self-signed certificates differently. Resulting issues may include connectivity failures and other security issues which can be difficult to diagnose. Instead, use [trusted certificates](p_tls_trusted.md) issued from a trusted certificate authority (CA) for real-world development, test, and production environments.</span>
In these procedures, you will learn how to configure secure networking between KAAZING Gateway and clients and web browsers using self-signed certificates. Typically, a self-signed certificate is intended for limited development and testing environments.

In this procedure, you will configure applications for TLS/SSL connections with the Gateway in the following ways:

-   [To Use Self-Signed Certificates With iOS Devices and iOS Simulator](#tls_ios)
-   [To Use Self-Signed Certificates With Android Devices](#tls_android)
-   [To Use Self-Signed Certificates With .Net Clients](#tls_dotnet_clients)
-   [To Use Self-Signed Certificates with Java Clients](#tls_java_clients)
-   [To Import Self-Signed Certificates into a Web Browser](#tls_import_browser)

**Note:** Using self-signed certificates with clients involves importing and exporting certificates from the truststore and keystore storage locations. For more information, see the [truststore](../admin-reference/r_conf_security.md#truststore) and [keystore](../admin-reference/r_conf_security.md#keystore) elements in the [Security Reference](../admin-reference/r_conf_security.md) documentation.
Before You Begin
----------------

This procedure is part of [Secure Network Traffic with the Gateway](o_tls.md):

-   [Secure the Gateway Using Trusted Certificates](p_tls_trusted.md)
-   [Secure the Gateway Using Self-Signed Certificates](p_tls_selfsigned.md)
-   **Secure Clients and Web Browsers with a Self-Signed Certificate**
-   [Require Clients to Provide Certificates to the Gateway](../security/p_tls_mutualauth.md)

<a name="tls_ios"></a>To Use Self-Signed Certificates With iOS Devices and iOS Simulator
----------------------------------------------------------------------------------------

The following steps describe how to import a self-signed certificate into an iOS device and the iOS Simulator included with Xcode.

### iOS Devices

To import the self-signed certificate into the iOS device, do the following:

1.  On the Gateway, enter the following command to export the certificate from the default keystore on the Gateway:

    keytool -exportcert -alias *hostname* -keystore *GATEWAY\_HOME*\\conf\\keystore.db -storetype JCEKS -file *GATEWAY\_HOME*\\web\\base\\*hostname*.cer -storepass *password*

    The exported certificate is located in `GATEWAY_HOME\web\base`.

2.  Email the certificate as an email attachment to an account that can be reached using the iOS device.
3.  On the iOS device, open the email message, select the certificate attachment, and then select **Install**. Now the certificate can be used without warnings in Safari or other iOS apps that use the iOS device’s keychain. You can access the certificate in **Settings \> General \> Profiles** and remove it if required.

### iOS Simulator

Importing self-signed certificates is not directly supported in the iOS Simulator. To add a self-signed certificate to the iOS Simulator, do the following:

1.  Download the management script [ADVTrustStore](https://github.com/ADVTOOLS/ADVTrustStore) from GitHub. ADVTrustStore is a simple management script to import, list, and remove self-signed certificates in the iOS Simulator. You might find it convenient to download the ADVTrustStore script into the `GATEWAY_HOME/conf/` folder, as that is where the keystore.db file is located.
2.  On the Gateway, enter the following command to export the certificate from the default keystore on the Gateway:

    keytool -exportcert -alias *hostname* -keystore *GATEWAY\_HOME*\\conf\\keystore.db -storetype JCEKS -file *GATEWAY\_HOME*\\conf\\*hostname*.cer -storepass *password* -rfc

    The exported certificate is located in `GATEWAY_HOME\conf\`. Note the use of the `-rfc` parameter. The `-rfc` parameter exports the certificate in PEM format.

3.  Use the ADVTrustStore script to add the *hostname*.cer certificate into the iOS Simulator. The ADVTrustStore script will prompt you to install the self-signed certificate for each available iOS Simulator version.

    For Windows: `iosCertTrustManager.py -a example.cer`

    For Mac/Linux: `./iosCertTrustManager.py -a example.cer`

    For each iOS Simulator version, the output will look similar to this:

    `Import certificate to iPhone/iPad simulator v5.1 [y/N] y Importing to /Users/johnsmith/Library/Application Support/iPhone Simulator/5.1/Library/Keychains/TrustStore.sqlite3 Existing certificate replaced`

The self-signed certificate is added to the iOS Simulator.

<a name="tls_android"></a>To Use Self-Signed Certificates With Android Devices
------------------------------------------------------------------------------

There are many different Android devices, but the following instructions for Android 4.3 and the Nexus 10 device should apply to most Android devices.

To import the self-signed certificate into the Android device, do the following:

1.  On the Gateway, enter the following command to export the certificate from the default keystore on the Gateway:

    keytool -exportcert -alias *hostname* -keystore *GATEWAY\_HOME*\\conf\\keystore.db -storetype JCEKS -file *GATEWAY\_HOME*\\conf\\*hostname*.cer -storepass *password*

    The exported certificate is located in `GATEWAY_HOME\conf\`.

2.  Follow the steps in [Work with certificates](https://support.google.com/nexus/10/answer/2844832?hl=en) from Google to import the certificate into your Android 4.3 device. For other Android versions and devices, see [Manufacturer support](https://support.google.com/android/answer/3094742).

<a name="tls_dotnet_clients"></a>To Use Self-Signed Certificates with .NET Clients
----------------------------------------------------------------------------------

Place self-signed certificates in the Windows Trusted Root Certification Authorities store. You can do this with Certificate Creation Tool (MakeCert.exe), as described on Microsoft's support site: <http://msdn.microsoft.com/en-us/library/ms733813.aspx>.

<a name="tls_java_clients"></a>To Use Self-Signed Certificates with Java Clients
--------------------------------------------------------------------------------

After you have created or imported the self-signed certificate into the keystore.db file for the Gateway, you can enable your Java clients to use that self-signed certificate in one of two ways:

-   [Import the self-signed certificate into the Java client's JVM truststore](#tls_java_clients_import), or
-   [Add the self-signed certificate to a truststore and reference it when Launching the Java Client](#tls_java_clients_launch).

### <a name="tls_java_clients_import"></a>Import the Self-Signed Certificate into the Java Client's JVM Truststore

To import the certificate into the Java client's truststore, export the self-signed certificate located on the Gateway in a format that can be imported into the Java client.

1.  On the Gateway, enter the following command to exports the certificate from the default keystore on the Gateway:

    keytool -exportcert -alias *hostname* -keystore *GATEWAY\_HOME*\\conf\\keystore.db -storetype JCEKS -file *GATEWAY\_HOME*\\conf\\*hostname*.cer -storepass *password*

2.  Copy the `hostname.cer` file to the client.
3.  Ensure that `JAVA_HOME/jre/lib/security/cacert` exists on the client and import the `hostname.cer` file into the JVM-wide truststore using keytool. The following example command uses the default Java Certificate Authority certificates password `changeit` and assumes the `hostname.cer` file is in the current directory. Be sure to change `hostname` to your hostname.

    keytool -importcert -keystore *JAVA\_HOME*\\jre\\lib\\security\\cacerts -storepass changeit -alias *hostname* -file *hostname*.cer

    The Java client should now be able to find your certificate in the JVM's truststore and implicitly trust it.

### <a name="tls_java_clients_launch"></a>Add the self-signed certificate to a truststore and reference it when Launching the Java Client

To add the self-signed certificate to the default truststore used by the JVM, import the certificate into a truststore and then reference it when launching the Java client.

1.  On the Gateway, enter the following command to exports the certificate from the default keystore on the Gateway:

    keytool -exportcert -alias *hostname* -keystore *GATEWAY\_HOME*\\conf\\keystore.db -storetype JCEKS -file *GATEWAY\_HOME*\\conf\\*hostname*.cer -storepass *password*

2.  Create a new truststore on the client that has as its sole content the certificate you exported on the server. The following example command uses `testing.db` as the new truststore, assumes the file is in the current directory, and uses the default password `changeit`:

    keytool -importcert -keystore testing.db -storepass changeit -storetype JCEKS -alias *hostname* -file *hostname*.cer

3.  When you launch the Java client, use the `"java.net.ssl.trustStore" -D` parameter to provide the location of trusted certificates to the client application. The following example references `testing.db` as the location of the truststore and `EnabledJavaClient` as the name of your client application:

    java -cp . -Djavax.net.ssl.trustStore=testing.db EnabledJavaClient

    You can also use a system property to set the location of the truststore by adding the following code to your Java client, including the location of the certificate.

    System.setProperty("javax.net.ssl.trustStore", "*path to truststore*");

    If you are running the client on the same computer as the Gateway, you can use the following code in your Java client to set the location of the truststore and certificate:

    ``` brush:
        final String trustStore = new File(new File(GatewayLauncher.getGatewayHomeDir(), "conf/"),
            "truststore.db").getAbsoluteFile().toString();
        
        System.setProperty("javax.net.ssl.trustStore", trustStore);

        final String trustStorePassword = new String(loadKeyStorePassword(new File
            (new File(GatewayLauncher.getGatewayHomeDir(), "conf/"), "keystore.pw")));
        
        System.setProperty("javax.net.ssl.trustStorePassword", trustStorePassword);
        
        System.setProperty("javax.net.ssl.trustStoreType", "JCEKS");
    ```

<a name="tls_import_browser"></a>To Import Self-Signed Certificates into a Web Browser
--------------------------------------------------------------------------------------

When using self-signed certificates with web browsers, you can import the self-signed certificate into the web browser using the browser's built-in functionality. The following table includes general steps for importing self-signed certificates into web browsers. For information specific to web browsers and operating systems, see their documentation on importing certificates.

**Note:** In general, you will only be using self-signed certificates for testing purposes. When using a WebSocket Secure (WSS) connection, you are not prompted to import a certificate. The prompt displays if you navigate to an HTTPS page. Follow the steps in [Secure the Gateway Using Self-Signed Certificates](p_tls_selfsigned.md) to accept the self-signed certificate in a web browser. When you surf to the secure directory service on the Gateway (`https://`), the browser will display a warning stating that the certificate is not trusted. Accept the untrusted certificate and proceed to the web page.
<table>
<colgroup>
<col width="50%" />
<col width="50%" />
</colgroup>
<thead>
<tr class="header">
<th align="left">Browser</th>
<th align="left">Resources for Importing Self-Signed Certificates</th>
</tr>
</thead>
<tbody>
<tr class="odd">
<td align="left">Apple Safari</td>
<td align="left">You might want to export the certificate from the keystore of the Gateway and import it into your Mac Keychain. Mac Keychain only recognizes the PKCS12 and PKCS7 certificate types. When you export the certificate from the keystore of the Gateway using <a href="http://docs.oracle.com/javase/7/docs/technotes/tools/windows/keytool.html">keytool</a> and the <code>-exportcert</code> command, use <code>-storetype PKCS7</code>. You can also use the <a href="http://www.lazgosoftware.com/kse/">KeyStore Explorer</a> tool. For more information, see <a href="http://support.apple.com/kb/PH10968">OS X Mountain Lion: If your certificate isn’t being accepted</a> from Apple.</td>
</tr>
<tr class="even">
<td align="left">Google Chrome</td>
<td align="left">See <a href="https://support.google.com/chrome/answer/95572?hl=en&amp;ref_topic=14666">Advanced security settings</a> from Google.</td>
</tr>
<tr class="odd">
<td align="left">Microsoft Internet Explorer</td>
<td align="left">See <a href="http://windows.microsoft.com/en-US/windows-vista/Import-or-export-certificates-and-private-keys">Import or export certificates and private keys</a> and <a href="http://windows.microsoft.com/en-us/internet-explorer/certificate-errors-faq#ie=ie-10">Certificate errors: FAQ</a> from Microsoft. <strong>For Internet Explorer 8 and 9</strong>
<ol>
<li>Navigate to the Gateway home using an HTTPS URL.</li>
<li>At the prompt <strong>There is a problem with this website's security certificate</strong>, click <strong>Continue to this website (not recommended)</strong>.</li>
<li>Click <strong>Internet Options</strong> on the <strong>Tools</strong> menu.</li>
<li>On the <strong>Security</strong> tab, click <strong>Trusted sites</strong> and then click <strong>Sites</strong>.</li>
<li>Confirm that the URL matches the URL you entered and click <strong>Add</strong>, and then click <strong>Close</strong>.</li>
<li>Close <strong>Internet Options</strong>.</li>
<li>Refresh the web page.</li>
<li>At the prompt <strong>There is a problem with this website's security certificate</strong>, choose <strong>Continue to this website (not recommended)</strong>.</li>
<li>Click <strong>Certificate Error</strong> in the address bar and click <strong>View certificates</strong>.</li>
<li>Click <strong>Install Certificate</strong>, and then click <strong>Next</strong> in the <strong>Certificate Import Wizard</strong>.</li>
<li>Select <strong>Place all certificates in the following store</strong>.</li>
<li>Click <strong>Browse</strong>, click <strong>Trusted Root Certification Authorities</strong>, and click <strong>OK</strong>.</li>
<li>Click <strong>Next</strong> in the wizard until you reach the last screen, and then click <strong>Finish</strong>. If you get a <strong>Security Warning</strong> message box, click <strong>Yes</strong>.</li>
<li>Click <strong>OK</strong>.</li>
<li>On the <strong>Tools</strong> menu, click <strong>Internet Options</strong>.</li>
<li>On the <strong>Security</strong> tab, click <strong>Trusted sites</strong> and then click <strong>Sites</strong>.</li>
<li>Select the URL you added and click <strong>Remove</strong>, and then click <strong>Close</strong>.</li>
<li>Restart Internet Explorer. The web site's certificate as well as any WebSocket URL will now work in Internet Explorer.</li>
</ol></td>
</tr>
<tr class="even">
<td align="left">Mozilla Firefox</td>
<td align="left">See <a href="https://support.mozilla.org/en-US/kb/connection-untrusted-error-message?esab=a&amp;s=self+signed+certificate&amp;r=3&amp;as=s">&quot;This Connection is Untrusted&quot; error message appears - What to do</a> from Mozilla.</td>
</tr>
<tr class="odd">
<td align="left">Opera</td>
<td align="left">See <a href="http://help.opera.com/Mac/12.10/en/certificates.html">Security certificates</a> from Opera.</td>
</tr>
</tbody>
</table>

Next Steps
----------

To troubleshoot TLS/SSL errors and exceptions, see [Troubleshooting KAAZING Gateway Security](../troubleshooting/ts_security.md).

<a name="see_also"></a>See Also
-------------------------------

-   [Transport Layer Security (TLS/SSL) Concepts](c_tls.md)
-   [How TLS/SSL Works with the Gateway](u_tls_works.md)



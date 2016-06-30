Configure KAAZING Gateway Using the GATEWAY\_OPTS Environment Variable
===================================================================================================================================

On occasion you might want the flexibility to change the configuration that is set up in your Gateway configuration file (see [About KAAZING Gateway Configuration Files](c_configure_gateway_concepts.md#about-kaazing-gateway-configuration-files)) or change the Java process temporarily without modifying the configuration file itself. For example, you might want to define your own startup scripts that specify different log4j configurations or different log directories. You can do that by setting the `GATEWAY_OPTS` environment variable (or any other Java option or command-line setting) to include any of the properties in the following table:

| Variable                     | Description                                                   |
|------------------------------|---------------------------------------------------------------|
| `GATEWAY_HOME`               | The directory path for the Gateway home                    |
| `GATEWAY_CONFIG`             | The name of the Gateway configuration file                 |
| `GATEWAY_CONFIG_DIRECTORY`   | The path for the Gateway configuration directory           |
| `GATEWAY_WEB_DIRECTORY`      | The path for the Gateway web directory                     |
| `GATEWAY_TEMP_DIRECTORY`     | The path for the Gateway temp files directory              |
| `GATEWAY_LOG_DIRECTORY`      | The path for the Gateway logfiles directory                |
| `GATEWAY_USER_LIB_DIRECTORY` | The path for the Gateway user library files                |
| `LOG4J_CONFIG`               | The name and path for the Gateway log4j configuration file |

For `GATEWAY_OPTS` environment variable examples, see [`GATEWAY_OPTS` Environment Variable Examples](#gateway_opts-environment-variable-examples) below.

Before You Begin
----------------

This procedure is part of [Configure the Gateway](o_configure_gateway_checklist.md):

-   [Configure KAAZING Gateway](p_configure_gateway_files.md)
-   **Configure KAAZING Gateway Using the `GATEWAY_OPTS` Environment Variables**
-   Verify the Gateway configuration following the instructions for your deployment in [Setting Up the Gateway](../about/setup-guide.md)

To Configure the `GATEWAY_OPTS` Environment Variable
----------------------------------------------------

1.  Ensure you have followed the steps in [Setting Up the Gateway](../about/setup-guide.md) to download and install KAAZING Gateway.
2.  Ensure the Gateway is stopped.
    1.  To Stop the Gateway on Windows: press **CTRL + C** in the command prompt that was used to start the Gateway, or close the command prompt.
    2.  To Stop the Gateway on Linux, UNIX, and Mac: use the following commands (where *process ID* is the process ID of the Gateway process):

        ```
        ps –ef | grep *process name*
         kill *process ID*
        ```
3.  Specify one or more system property values using `-Dproperty=value` with the `GATEWAY_OPTS` environment variable.

    The environment variable `GATEWAY_OPTS` can contain a space-separated list of command-line arguments. For example, to specify the name of the Gateway configuration and the Gateway log directory at the Linux command line, specify the `GATEWAY_OPTS` environment variable as follows:

    ```
    GATEWAY_OPTS="-DGATEWAY_CONFIG=gateway-config.xml
    -DGATEWAY_LOG_DIRECTORY=/home/myname/gateway/logs"
    ```

    To learn more about using the `-Dproperty=value` switch on the Java command line, see the [Java Application Launcher documentation](http://docs.oracle.com/javase/7/docs/technotes/tools/windows/java.html).

    **Notes:**

    -   If you define multiple `-Dproperty=value` variables then there must be a space between each variable, and you must enclose the entire value in quotes. For example:

        ```
        GATEWAY_OPTS="-DGATEWAY_CONFIG=gateway-config.xml
        -DGATEWAY_LOG_DIRECTORY=/home/my-company/gateway/logs"
        ```
    -   If the value of a `-Dproperty=value` variable contains spaces, then use the **`\"`** notation around the value. In the following example, the value for `-DGATEWAY_LOG_DIRECTORY` is wrapped in the **`\"`** notation because it contains spaces:

        ```
        GATEWAY_OPTS="-DGATEWAY_CONFIG=gateway-config.xml
        -DGATEWAY_LOG_DIRECTORY=\"/home/my company/gateway/logs\""
        ```

4.  Start the Gateway.

    To Start the Gateway on Windows:

    1.  In Windows Explorer, navigate to the `GATEWAY_HOME/bin` directory where you installed the Gateway.
    2.  Double-click the `gateway.start.bat` script.

    To Start the Gateway on Linux, UNIX, or Mac:

    1.  Navigate to the `KAAZING_HOME/bin` directory where you installed the Gateway.
    2.  Run the `gateway.start` script: `./gateway.start`

    When you successfully start the Gateway, messages display in your shell or command prompt indicating the services that are hosted by the Gateway. The startup message may differ depending on your Gateway configuration. If the server does not start successfully, contact your administrator or see [Troubleshoot the Gateway](../troubleshooting/o_troubleshoot.md) for help finding the problem.

Next Step
-------------------------

Verify the Gateway configuration following the instructions for your deployment in [Setting Up the Gateway](../about/setup-guide.md)

Notes
-----

-   Any system property values that you define with the `GATEWAY_OPTS` environment variable before starting the Gateway are used and override the values specified in the default Gateway configuration.

GATEWAY_OPTS Environment Variable Examples
-------------------------------------------------------------------

### GATEWAY_HOME

Windows Example:

`> SET GATEWAY_OPTS=-DGATEWAY_HOME=C:\Gateway\`

`> C:\Gateway\bin\gateway.start.bat`

Mac Example:

`$ export GATEWAY_OPTS="-DGATEWAY_HOME=/Users/johnsmith/Desktop/Gateway/"`
`$ /Users/johnsmith/Desktop/Gateway/bin/gateway.start`

### GATEWAY_CONFIG

Windows Example:

`> SET GATEWAY_OPTS=-DGATEWAY_CONFIG=C:\Gateway\conf\gateway-config-test.xml`
`> C:\Gateway\bin\gateway.start.bat`

Here is an alternative way to specify the configuration file:

`> gateway.start.bat --config C:\Gateway\conf\gateway-config-test.xml`

Mac Example:

`$ export GATEWAY_OPTS="-DGATEWAY_CONFIG=/Users/johnsmith/Desktop/Gateway/conf/gateway-config-test.xml"`

`$ /Users/johnsmith/Desktop/Gateway/bin/gateway.start`

Here is an alternative way to specify the configuration file:

`./gateway.start --config /Users/johnsmith/Desktop/Gateway/conf/gateway-config-test.xml`

### GATEWAY_CONFIG_DIRECTORY

Windows Example:

`> SET GATEWAY_OPTS=-DGATEWAY_CONFIG=C:\Gateway\conf\gateway-config-test.xml -DGATEWAY_CONFIG_DIRECTORY=C:\Gateway\conf`

 `> C:\Gateway\bin\gateway.start.bat`

Mac Example:

`$ export GATEWAY_OPTS="-DGATEWAY_CONFIG=gateway-config-test.xml -DGATEWAY_CONFIG_DIRECTORY= \ /Users/johnsmith/Desktop/Gateway/conf"`

`$ /Users/johnsmith/Desktop/Gateway/bin/gateway.start`

### GATEWAY_WEB_DIRECTORY

Windows Example:

`> SET GATEWAY_OPTS=-DGATEWAY_CONFIG=C:\Gateway\conf\gateway-config.xml -DGATEWAY_WEB_DIRECTORY=C:\Gateway\web-test`

`> C:\Gateway\bin\gateway.start.bat`

Mac Example:

`$ export GATEWAY_OPTS="-DGATEWAY_CONFIG=gateway-config.xml -DGATEWAY\_WEB\_DIRECTORY=/Users/johnsmith/Desktop/Gateway/web-test"`

`$ /Users/johnsmith/Desktop/Gateway/bin/gateway.start`

### GATEWAY_TEMP_DIRECTORY

Windows Example:

`> SET GATEWAY_OPTS=-DGATEWAY_CONFIG=C:\Gateway\conf\gateway-config.xml -DGATEWAY_TEMP_DIRECTORY=C:\Gateway\temp-test`

`> C:\Gateway\bin\gateway.start.bat`

Mac Example:

`$ export GATEWAY_OPTS="-DGATEWAY_CONFIG=gateway-config.xml -DGATEWAY_TEMP_DIRECTORY=/Users/johnsmith/Desktop/Gateway/temp-test"`

`$ /Users/johnsmith/Desktop/Gateway/bin/gateway.start`

### GATEWAY_LOG_DIRECTORY

Windows Example:

`> SET GATEWAY_OPTS=-DGATEWAY_CONFIG=C:\Gateway\conf\gateway-config.xml -DGATEWAY_LOG_DIRECTORY=C:\Gateway\log`

`> C:\Gateway\bin\gateway.start.bat`

Mac Example:

`$ export GATEWAY_OPTS="-DGATEWAY_CONFIG=gateway-config.xml -DGATEWAY_LOG_DIRECTORY=/Users/johnsmith/Desktop/Gateway/log"`

`$ /Users/johnsmith/Desktop/Gateway/bin/gateway.start`

### GATEWAY_USER_LIB_DIRECTORY

Windows Example:

`> SET GATEWAY_OPTS=-DGATEWAY_CONFIG=C:\Gateway\conf\gateway-config.xml -DGATEWAY_USER_LIB_DIRECTORY=C:\Gateway\lib`

`> C:\Gateway\bin\gateway.start.bat`

Mac Example:

`$ export GATEWAY_OPTS="-DGATEWAY_CONFIG=gateway-config.xml -DGATEWAY\_USER\_LIB\_DIRECTORY=/Users/johnsmith/Desktop/Gateway/lib"`

`$ /Users/johnsmith/Desktop/Gateway/bin/gateway.start`

### LOG4J_CONFIG

Windows Example:

`> SET GATEWAY_OPTS=-DGATEWAY_CONFIG=C:\Gateway\conf\gateway-config.xml -DLOG4J_CONFIG=C:\Gateway\log\log4j.xml`

`> C:\Gateway\bin\gateway.start.bat`

Mac Example:

`$ export GATEWAY_OPTS="-DGATEWAY_CONFIG=gateway-config.xml -DLOG4J_CONFIG=/Users/johnsmith/Desktop/Gateway/log/log4j.xml"`

`$ /Users/johnsmith/Desktop/Gateway/bin/gateway.start`

See Also
--------

-   [About Gateway Configuration](c_configure_gateway_concepts.md)
-   [Configure the Gateway](o_configure_gateway_checklist.md)
-   [Documentation Conventions](../about/about.md)

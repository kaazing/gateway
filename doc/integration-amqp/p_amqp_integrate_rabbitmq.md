Integrate RabbitMQ Messaging
============================

In this procedure, you will learn how to integrate KAAZING Gateway and [RabbitMQ](https://www.rabbitmq.com/), a highly reliable enterprise messaging system based on the AMQP standard.

**Note:** The instructions in this topic use RabbitMQ 3.3.0.

To Integrate RabbitMQ Messaging
-------------------------------

1.  Set up RabbitMQ.
    1.  Download and install RabbitMQ by following the instructions on the RabbitMQ [website](http://www.rabbitmq.com/download.html). The root folder for the RabbitMQ installation will be referred to as `RABBITMQ_HOME` in this procedure. Note that RabbitMQ requires that [Erlang](http://www.erlang.org/download.html "Erlang Programming Language OTP 17.0") is installed and running on the server running RabbitMQ.
    2.  Open a shell or command prompt on the location `RABBITMQ_HOME/sbin`.
    3.  Start RabbitMQ by running the following command:

        For Windows:
         `rabbitmq-server.bat`

        For Mac and Linux:
         `./rabbitmq-server`

        The output will contain the following:
         `Starting broker... completed with 1 plugins`

        The RabbitMQ broker is started along with the installed plugin.

2.  Download and install the Gateway as described in [Setting Up KAAZING Gateway](../about/setup-guide.md).
3.  Open the configuration file for the Gateway, located at `GATEWAY_HOME/conf/gateway-config.xml`.
4.  Locate the [amqp.proxy](../admin-reference/r_configure_gateway_service.md#proxy-amqpproxy-and-jmsproxy) service:


    ``` xml
      <service>
        <accept>ws://${gateway.hostname}:${gateway.extras.port}/amqp</accept>
        <connect>tcp://${broker.hostname}:5672</connect>
        <type>amqp.proxy</type>
        <properties>
          <service.domain>${gateway.hostname}</service.domain>
          <encryption.key.alias>session</encryption.key.alias>
        </properties>

        <realm-name>demo</realm-name>

        <!--
        <authorization-constraint>
        <require-role>AUTHORIZED</require-role>
        </authorization-constraint>
        -->

        <cross-site-constraint>
        <allow-origin>http://${gateway.hostname}:${gateway.extras.port}</allow-origin>
      </cross-site-constraint>
    </service>
    ```

    Note that the connect element uses the default AMQP port 5672. For information on other properties that may be configured for the `amqp.proxy` service, see [amqp.proxy](../admin-reference/r_configure_gateway_service.md#proxy-amqpproxy-and-jmsproxy).

5.  Start the Gateway as described in [Setting Up the Gateway](../about/setup-guide.md).
6.  Test the Gateway RabbitMQ integration.

    1.  In a browser, navigate to the out of the box demos at `http://localhost:8001/demo/`.
    2.  Click the **JavaScript** demo.
    3.  Click **Connect**.
         The status message `CONNECTING: ws://localhost:8001/amqp guest` appears followed by `CONNECTED`.
    4.  Using the demo, publish messages to an exchange, control flow, commit and rollback messages.

See Also
--------

-   For general troubleshooting information, see [Troubleshoot the Gateway](../troubleshooting/o_troubleshoot.md).
-   For information on AMQP and RabbitMQ, see [AMQP 0-9-1 Model Explained](https://www.rabbitmq.com/tutorials/amqp-concepts.html), [RabbitMQ Documentation](https://www.rabbitmq.com/documentation.html), [Downloading and Installing RabbitMQ](https://www.rabbitmq.com/download.html).
-   [Running Erlang on Mac OS X](http://rudamoura.com/erlang-on-mac.html "Running Erlang/OTP on Mac OS X").

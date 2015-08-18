General Client Information
================================================

This topic contains information that applies to clients built using the KAAZING Gateway JavaScript, Java, and Objective-C WebSocket APIs. In addition, native WebSocket clients, meaning clients that implement the [WebSocket API standard](http://www.w3.org/TR/websockets/) but are built without the KAAZING Gateway APIs, can take advantage of the features of the Gateway described in this topic.

**Note:** The Objective-C client information in this topic applies to the Objective-C client libraries in the KAAZING Gateway JMS 4.0 and Gateway 4.0 Editions only.

This topic contains the following sections:

-   [Behavior and Configuration Options in the Gateway](#behavior-and-configuration-options-in-the-gateway)
    -   [Proxy Binary Messages](#proxy-binary-messages)
    -   [Proxy Text Messages On TCP, UDP, MDP](#proxy-text-messages-on-tcp-udp-mdp)
    -   [Proxy All WebSocket Negotiations](#proxy-all-websocket-negotiations)
    -   [Proxy Ping and Pong Frames](#proxy-ping-and-pong-frames)
    -   [Proxy Close Frames](#proxy-close-frames)
-   [Reading WebSocket Connection State in the JavaScript and Objective-C APIs](#reading-websocket-connection-state-in-the-javascript-and-objective-c-websocket-apis)
-   [Best Practices](#best-practices)

Behavior and Configuration Options in the Gateway
---------------------------------------------------------------------------

The following Gateway configuration details are of interest to client developers. In the following descriptions, the term *proxying service* is used to mean any service configured on the Gateway that proxies client messages to and from a back-end service, such the [proxy](../admin-reference/r_configure_gateway_service.md) service, or a protocol-specific service.

### Proxy Binary Messages

Your client can send and receive binary messages with a proxy service configured on the Gateway. All TCP traffic sent from the back-end server to the Gateway is transferred to your client from the Gateway via the proxy service as binary frames.

### Proxy Text Messages On TCP, UDP, MDP

WebSocket messages received from the Gateway are binary by default, however your client can connect to a proxy service on the Gateway and receive text messages on all transports (Transport Layer protocols). The Echo service hosted by the Gateway will respond with both text or binary depending on what it receives.

### Proxy All WebSocket Negotiations

In a network topology that uses two Gateways (an outer Gateway in a DMZ subnet connecting to an Internal Gateway in the internal, trusted network), any transport handshake and extension (for example, WebSocket) that is negotiated between the client and the DMZ Gateway is propagated to the Internal Gateway.

### Proxy Ping and Pong Frames

Your client can send and receive Ping and Pong frames (control frames that are used to determine if a connection is still open) with the back-end server via the proxy services on the Gateway. For more information on Ping and Pong frames in WebSocket, see [WebSocket RFC 6455](http://tools.ietf.org/html/rfc6455#section-5.5.2).

### Proxy Close Frames

The Gateway propagates a WebSocket close handshake (a control frame terminating the WebSocket connection, and underlying TCP connection) via any proxying service. For more information on Close frames in WebSocket, see [WebSocket RFC 6455](http://tools.ietf.org/html/rfc6455#section-5.5.1). The close handshake may contain the optional body portion, wherein a reason for the close in the form of a [status code](http://tools.ietf.org/html/rfc6455#section-7.4) is included.

For general information about configuring the Gateway, see [For Administrators](../index.md#for-administrators).

Reading WebSocket Connection State in the JavaScript and Objective-C WebSocket APIs
-----------------------------------------------------------------------------------

The [`readyState`](http://www.w3.org/TR/websockets/#dom-websocket-readystate) attribute of the WebSocket interface represents the state of the WebSocket connection. A number is associated with each connection state and you can read the `readyState` to determine the state of the WebSocket connection between your client and the Gateway.

In version 3.x of KAAZING Gateway, numeric values for `readyState` are different than those in the finalized WebSocket API standard. The JavaScript, Java, and Objective-C WebSocket APIs in version 4.x of the Gateway use the `readyState` values of the [WebSocket API standard](http://dev.w3.org/html5/websockets/#dom-websocket-readystate "The WebSocket API").

The following table includes both the previous numeric values and the current numeric values.

| State      | Numeric Value | Previous Value | Description                                                            |
|------------|---------------|----------------|------------------------------------------------------------------------|
| Connecting | 0             | 0              | The connection is not yet established.                                 |
| Open       | 1             | 1              | The WebSocket connection is established and communication is possible. |
| Closing    | 2             | Not included.  | The connection is going through the closing handshake.                 |
| Closed     | 3             | 2              | The connection has been closed or could not be opened.                 |

Here is a simple example of how to read the readyState in JavaScript.

``` js
w = new WebSocket("ws://localhost:8001/echo");
w.onopen = function() {
   alert(w.readyState);
}
```

In the Objective-C WebSocket API, readyState is read using `(KGReadyState)readyState`.

**Note:** Developers migrating applications to the latest version of the Gateway will need to update any code that uses readyState values to use the latest values.

Best Practices
---------------------------------

KAAZING Gateway client libraries dispatch messages on the message-dispatch thread to the client application. The application receives messages on the message-dispatch thread via an event-listener method or a callback function. Message receiving and message sending should be performed on separate threads. Do not try to send a message or perform other long-running operations in the message-dispatch thread. If a message needs to be sent in response to a received message, then the message should be sent on a separate thread. On single-threaded client platforms such as Javascript and Flash, the message delivery can be scheduled for a later time using the `setTimeout()` function.

See Also
--------

KAAZING Gateway [Client API documentation](../index.md).

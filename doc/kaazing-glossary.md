KAAZING Glossary
================

This document describes the terminology used in KAAZING's products and product [documentation](index.md).

-   [A-L](#a)
-   [M-Z](#m)

A
-----------------

##### Ajax

*A*synchronous *J*avaScript *a*nd *X*ML. A set of technologies based on HTML and CSS that enables web applications to retrieve information from a server asynchronously without refreshing the entire web page.

##### AMQP

Advanced Message Queuing Protocol. An open standard for messaging middleware that was originally designed by the financial services industry to provide an interoperable protocol for managing the flow of enterprise messages. To guarantee messaging interoperability, AMQP 0-9-1 defines both a wire-level protocol and a model — the AMQP Model — of messaging capabilities.

##### Android

Android is an SDK that is used for the development and deployment of Java-based Android applications (applications that run on the Android platform). The Android SDK includes a set of user interface components that you can use to build a rich Internet application (RIA), such as lists, grids, and text boxes.

##### authentication

The mechanism by which a system identifies a user and verifies whether or not the user really is who he represents himself to be. To start the authentication process, the Gateway issues a standard challenge using the `HTTP 401 Authorization Required` code. The browser or client then responds by providing the requested authentication information.

##### authorization

The mechanism by which a system determines what level of access a particular user has. Even after a user is successfully authenticated for general access, the user is not necessarily entitled to perform any operation. Access rights are typically stored in the policy store that is associated with the application.

B
-

##### bidirectional communication

Communication between a browser and server where messages can be sent and received by both the browser and server at the same time, as enabled by [KAAZING Gateway](#kaazing-gateway). See also [*full-duplex*](#fullduplex).

C
-

##### Certification Authority (CA)

An entity that issues certificates. In practice, a CA is a server running a certificate service that processes certificate requests and performs certificate validation and revocation. In a [PKI](#pki), there is a single root CA and one or more subordinate CAs.

##### client ID

Client ID (sometimes referred to as front-end client ID) is the identifier set by the client application on its JMS Connection. The client ID is set by calling one of the `StompConnectionFactory.createConnection` methods that takes a `clientID` parameter, or you can configure [KAAZING Gateway](#kwsg) to set the client ID automatically.

##### cluster

High availability for services is achieved by configuring multiple Gateways to be part of a cluster. To configure multiple Gateways (cluster members) to communicate with each other and to take advantage of clustering and load balancing, you add the cluster configuration element to the Gateway configuration (`gateway-config.xml`) file.

##### cluster pair or pairing

Clustering for Enterprise Shield™ requires *pairing* (sometimes referred to as coupling) of Gateways. For example, in an Enterprise Shield™ topology you might configure each internal and DMZ cluster member to use pairing to match up each cluster member in the DMZ with a cluster member in the trusted internal network.

##### Comet

Web-based communication where a server pushes data to a browser or client without an explicit request from the browser. Comet is not an official standard, but rather an umbrella term for [push technology](#push) that is used in [Ajax](#ajax) web applications. It is also known as "Reverse Ajax."

##### Cross Origin Resource Sharing (CORS)

The [HTML5](#html5) standard includes Cross Origin Resource Sharing (CORS) that makes it possible to safely share and move data across domains. Prior to CORS, web browsers abided by the same-origin policy that restricted user access to documents within the same origin. With CORS, you can build web applications to access documents and services in different domains.

D
-

##### DMZ Gateway

A Gateway instance that receives connections from clients that are outside of the firewall. You can optionally set up KAAZING Gateway in a DMZ (perimeter network) for an additional layer of security. See also [internal Gateway](#internalgw) and [Enteprise Shield™](#enterpriseshield).

##### durable name

To create a durable subscriber for a topic, the client application calls the `createDurableSubscriber()` method of a `session` object and specifies a durable name and a destination. The durable names that the Gateway will use depend on whether or not a client ID is set.

E
-

##### Enterprise Shield™

A Gateway implementation of reverse connectivity that protects your enterprise network by letting you close all inbound ports of your internal firewall, eliminating penetration into your trusted enterprise network. Enterprise Shield™ works by initiating the connection from the internal trusted network towards the DMZ. This provides maximum security and minimizes attack vectors for malicious users seeking to exploit the DMZ or ports in your firewall, while still allowing clients to initiate connections.

F
-

##### full-duplex

Web-based communication between a browser and server where messages are sent and received by both the browser and server simultaneously, as enabled by [KAAZING Gateway](#kwsg). See also [*bidirectional.*](#bidirectional) Compare with [half-duplex](#halfduplex).

G
-

##### GATEWAY_HOME![This feature is available in KAAZING Gateway - Enterprise Edition.](images/enterprise-feature.png)

This is the directory that contains KAAZING Gateway and its components. The default Gateway home is represented in the documentation as `GATEWAY_HOME` because the actual directory destination depends on your operating system and the method you use to install the Gateway:

-   If you download and unpack the Gateway using the standalone method, then you can unpack the download into a directory of your choice (for example, `C:\kaazing` or `/home/username/kaazing`).
-   If you install the Gateway using the Windows or Linux Installer, then the installation creates the destination directory location as described in the following table, where *edition* refers to the product edition (for example, JMS, XMPP, or AMQP) and *version* refers to the version number (for example, 4.0):

| Operating System                                        | *GATEWAY_HOME*                                          |
|---------------------------------------------------------|----------------------------------------------------------|
| Windows: 32-bit                                         | `C:\Program Files\Kaazing\edition\version\Gateway`       |
| Windows: 64-bit                                         | `C:\Program Files\Kaazing\edition\version\Gateway`       |
| Windows: 32-bit installation on a 64-bit Windows system | `C:\Program Files (x86)\Kaazing\edition\version\Gateway` |
| Linux: Debian-based system                              | `/usr/share/kaazing/edition/version/`                  |

You can find more information about `GATEWAY_HOME` and the directory structure that is set up during installation in *Setting Up KAAZING Gateway*. To read this document, go to the [Kaazing Documentation home page](index.md), choose the edition (for example, HTML5 or JMS) of the Gateway you are running, and open *Setting Up KAAZING Gateway*. See also [*KAAZING_HOME*](#kaazing_home).

H
-

##### half-duplex

Web-based communication between a browser and server where messages are sent or received by the browser or the server, one direction at a time (not simultaneously). A server can send a message to the browser; if the browser has a message to send back to the server, it must wait until it has received the message from the server before sending its message. Contrast with [*full-duplex*](#fullduplex) and [bidirectional.](#bidirectional)

##### HTML5

The next major revision of HTML (Hypertext Markup Language) for the Web. It introduces new elements and attributes that reflect typical usage in modern web sites and web applications. See the [W3C specification](http://dev.w3.org/html5/spec/Overview.html).

##### HTTPS

The name used to denote the URI scheme `https://`. The URI indicates that HTTP network traffic is protected using [TLS](#tls) and [SSL](#ssl).

I
-

##### internal Gateway

A Gateway instance that is inside the firewall on the trusted network. The internal Gateway acts as a gatekeeper between the DMZ Gateway and the back-end server, which can include external services that exist either in a cloud vendor or another company's infrastructure. See also [DMZ Gateway](#dmzgw) and [Enteprise Shield™](#enterpriseshield).

##### iOS client

A client application that runs on the iOS operating system. See [Objective-C client](#objcClient).

J
-

##### Java Message Service (JMS)

A public API that enables the publishing and subscribing of messages between one or more clients and a message broker, and consists of a [JMS provider](#jmsprovider), a [JMS client](#jmsclient), a [JMS producer/publisher](#jmsproducer), a [JMS consumer/subscriber](#jmsconsumer), a [JMS message](#jmsmessage), a [JMS topic](#jmstopic), and a [JMS queue](#jmsqueue).

JMS messages can be transferred between a producer/publisher and a consumer/subscriber in two ways. A JMS producer/publisher sends a message to a destination in the JMS provider. This destination can be either a JMS queue or a JMS topic. If the destination is a queue, only the consumer/subscriber at that destination can receive the message. If the destination is a topic, any subscriber actively subscribing to the topic receives the message. See the [JMS Specification](http://www.oracle.com/technetwork/java/docs-136352.html).

##### JMS client

An application or process that creates and sends and/or receives [JMS](#jms) messages.

##### JMS consumer (or subscriber)

A [JMS](#jms) client that receives messages from a [JMS provider](#jmsprovider).

##### JMS message

An object that contains the data being delivered between a [JMS producer/publisher](#jmsproducer) and a [JMS consumer/subscriber](#jmsconsumer).

##### JMS producer (or publisher)

A [JMS client](#jmsclient) that creates and sends messages to a [JMS provider](#jmsprovider).

##### JMS provider

A proprietary Java [JMS](#jms) implementation or an adapter to a non-Java Message Oriented Middleware (MOM), such as 29West, Apache ActiveMQ, and TIBCO EMS.

##### JMS queue

A staging area that contains messages that have been sent but not yet received by the [JMS consumer/subscriber](#jmsconsumer).

##### JMS topic

A mechanism for distributing and publishing messages to multiple [JMS consumers/subscribers](#jmsconsumer).

K
-

##### KAAZING_HOME![This feature is available in KAAZING Gateway - Enterprise Edition.](images/enterprise-feature.png)

By default, when you install or upgrade KAAZING Gateway, the *KAAZING_HOME* directory is created. This top-level directory contains the KAAZING Gateway directory (referred to as *GATEWAY_HOME*), a message broker home (depending on the Gateway edition you are running), and Gateway components. The value of *GATEWAY_HOME* depends on the operating system. See [*GATEWAY_HOME*](#gateway_home) to learn more about Gateway directory destinations.

This documentation assumes you are running the Gateway from the default location. You may override the default and install KAAZING Gateway into a directory of your choice.

##### KAAZING Gateway

KAAZING provides a high-performance Web platform that enables [full-duplex](#fullduplex) communication over the Web. Also known as *Kaazing WebSocket Gateway*.

##### Kerberos

A network authentication protocol that enables security on a web client with a web server. For information about Kerberos, see the [Kerberos web site](http://web.mit.edu/Kerberos/).

L
-

##### login module

A login module handles the challenge/response authentication sequence of events during authentication, and evaluates the encoded login credentials that the Gateway passes to it. You implement login modules using the Java Authentication and Authorization Service (JAAS) framework.

##### long-polling

Web-based communication where the browser or client requests information from the server. If the server does not have any information to return at the time of the request, it waits for information to become available, then sends a complete response back to the client. See *[Comet](#comet)*. Compare with [*polling*](#polling).

M
-----------------

##### message broker

A program that translates a message from the messaging protocol of the sender to the messaging protocol of the receiver, in a network where clients and servers communicate by exchanging messages.

##### mutual verification or mutually verified connection

For added security, you can implement a mutual verification pattern where, in addition to the Gateway presenting a certificate to the client, the client also presents a certificate to the Gateway so that the Gateway can validate the client's authenticity. This configuration is implemented using the `ssl.verify-client `and `socks.ssl.verify-client` options on the accept and connect elements to ensure that both the DMZ Gateway and internal Gateway are verified via TLS/SSL before transmitting data. This is also referred to as a mutually verified connection.

##### mx.messaging

An API produced by Adobe that enables you to build Flash/Flex applications that can transfer messages with a back-end message broker. For more information about the API, see Adobe's [mx.messaging documentation](http://livedocs.adobe.com/flex/3/langref/mx/messaging/package-detail.html).

N
-----------------

##### network interface card (NIC)

A network interface card (NIC) is a computer circuit board or card that is installed in a computer so that it can be connected to a network and to the Internet. Each NIC has an IP address (or multiple addresses with sub-interfaces). Network security is improved by using separate NICs for the frontplane NIC and backplane NIC.

For security purposes, networking routing rules may control how a NIC can be accessed. The *frontplane* contains NICs driving traffic to and from the clients, while the *backplane* contains NICs driving traffic to and from the back-end systems. For example, in an Enterprise Shield™ topology, the DMZ Gateway’s frontplane NIC is addressable from the Web, but the backplane is not. In contrast, the internal Gateway frontplane is not publicly addressable. See also [Enterprise Shield™](#enterpriseshield).

##### network transport

A basic mechanism or protocol that carries information between endpoints in a topology, such as between clients and a back-end service. KAAZING Gateway has a sophisticated and flexible network transport framework that facilitates communication for a wide variety of use cases, including reverse connections in Enterprise Shield™ and VPA configurations. Systems administrators and network administrators can configure transport protocols such as TCP/IP, TLS, and SOCKS for communication across a KAAZING Gateway topology.

P
-

##### Objective-C client

A client application built using the iOS programming language. An Objective-C client is a mobile client application for devices running the iOS operating system, such as Apple iPhone and Apple iPad. See [iOS client](#iosClient).

##### pipe transport

An in-memory, in-process transfer of bytes from one end of a “pipe” connection to the other. Pipes can be named, and are specified by URLs of the form `pipe://pipe-name`. The pipe transport is essential to the [reverse connectivity](#reverseconn) topology.

##### polling

Web-based communication where the browser sends HTTP requests to a server at regular intervals and immediately receives a response.

##### prepared connection

A connection between KAAZING Gateway and a back-end service. In a KAAZING Gateway architecture, a client connects to the Gateway, which accepts the connection. The Gateway then connects to a back-end service, and establishes [full-duplex](#fullduplex) communication between the client and the back-end service. A *prepared connection* is one that only exists between the Gateway and the back-end service, in preparation for use by a client.

##### protocol client libraries

Libraries built and packaged with [KAAZING Gateway](#kwsg) to enable certain client-based WebSocket applications to interact with the Gateway. Examples of client libraries include [Stomp](#stomp), [AMQP](#amqp), and [XMPP](#xmpp).

##### Public Key Infrastructure (PKI)

A hierarchical network security model where all trusted certificates are created, verified, and revoked from a trusted root [Certificate Authority (CA)](#certification_authority). Adding Subordinate CAs to the hierarchy distribute the infrastructure across sites.

##### push technology

Also known as *server push*. Web-based communication where the request for a transaction is initiated by the server. HTTP server push is an example of push technology, where data is sent from a web server to a web browser over an HTTP connection. See also [*Comet*](#comet).

R
-

##### reverse connectivity

A feature of KAAZING Gateway that allows logical connections to be initiated from the internal Gateway to the DMZ Gateway, without requiring any ports to be open inbound towards the internal trusted network. With reverse connectivity, messages flow in reverse for all connections, from the back-end server on the trusted network through the internal Gateway to the clients outside of the DMZ Gateway. In other words, there are no inbound connections. See also <a href="#enterpriseshield">Enterprise Shield™</a.>

##### RIA

See [*Rich Internet Application*](#ria).

##### Rich Internet Application (RIA)

A web application that is typically delivered through a web browser and contains features similar to those in a desktop application. RIAs are most commonly built using Java, Adobe Flash, and Microsoft Silverlight.

S
-

##### Secure Session

The goal of a [TLS](#tls) connection is to negotiate a secure session. A successful TLS connection will result is a secure session that contains a 48-byte secret shared between the client and server, known as the master secret (RFC 2246). A secure session also contains algorithm information and a session identifier used to resume the session.

##### Secure Sockets Layer (SSL)

A pioneering network security protocol based on public key cryptography and described in historical RFC 6101. SSL was never standardized by an IETF RFC. See [*Transport Layer Security (TLS)*](#tls).

##### Server-Sent Events (SSE)

An HTML5 standard that describes how servers can initiate data transmission towards clients once an initial client connection has been established. These events are commonly used to send message updates or continuous data streams to a client. As such, SSE effectively standardizes [server-side push](#push) ([Comet](#comet)) technology. SSE was designed to enhance native, cross-browser streaming through the new EventSource JavaScript API, through which a client requests a particular URL in order to receive an event stream.

##### single sign-on (SSO)

The mechanism where users log in only once to access many different services, removing the need to distribute usernames, passwords, and ACLs throughout the enterprise.

##### SPNEGO

Simple and Protected GSSAPI Negotiation Mechanism. A pseudo-mechanism that uses a protocol to determine the common GSSAPI mechanisms that are available on a client and a remote server, selects one, then bases the security operations on it. Used when a client application needs to authenticate with a server, but it is not clear which authentication protocols each supports.

##### SSE

See [*Server-Sent Events*](#serversentevents).

##### SSL

Secure Sockets Layer. See [*Transport Layer Security (TLS)*](#tls).

##### SSO

See [single sign-on (SSO)](#sso)

##### Stomp

Streaming Text Oriented Messaging Protocol. A simple, yet effective protocol that provides an interoperable wire format, allowing Stomp clients to communicate with almost every available message broker. An example of a message broker that provides built-in support for Stomp is Apache ActiveMQ.

T
-

##### TLS

See [*Transport Layer Security*](#tls).

##### Transport Layer Security (TLS)

A network security protocol based on public key cryptography. The latest version of TLS is standardized in RFC 5246. TLS succeeded [SSL](#ssl) as development in SSL ended in 1996. TLS provides secure transport for application level protocols, such as HTTP, WS, FTP, SMTP, and LDAP. TLS includes important improvements to SSL, such as new alert messages, an improved authentication algorithm, and the ability to verify certificates using subordinate [CA](#certification_authority)s. TLS is used to establish [WSS](#wss).

W
-

##### WebSocket

An HTML5 API that enables [full-duplex](#fullduplex) communication between web pages or web applications and a remote host over the Web. The two official WebSocket schemes are "ws" and "wss." See the [W3C specification](http://dev.w3.org/html5/websockets/).

##### WebSocket Acceleration

The technology behind [KAAZING Gateway](#kwsg) that handles the transmission of massive quantities of messages a second between clients and servers. WebSocket Acceleration extends any TCP-based messaging format out to the web client, which turns any browser into a full-featured enterprise platform: a first-class citizen of any enterprise messaging system that is both fast and fully manageable.

##### WebSocket Secure (WSS)

The name used to denote the URI scheme `wss://`. The URI indicates that [WebSocket](#ws) traffic is protected using [TLS](#tls) and [SSL](#ssl).

X
-

##### XMPP

Extensible Messaging and Presence Protocol (XMPP) is an open XML technology for presence and real-time communication, developed by the Jabber open-source community in 1999. XMPP was formalized by the Internet Engineering Task Force (IETF) between 2002 and 2004 and the protocol continues to be extended through the XMPP Standards Foundation.

XMPP consists of XML streams that enable any two entities on the Internet to exchange messages, presence, and other structured information. Chat and presence are obvious candidates for real-time communication. Examples of chat solutions that use XMPP today are Google Talk and Apple iChat.

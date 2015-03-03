# gateway.resource.address

[![Build Status][build-status-image]][build-status]

[build-status-image]: https://travis-ci.org/kaazing/gateway.resource.address.svg?branch=develop
[build-status]: https://travis-ci.org/kaazing/gateway.resource.address

# About this Project

The gateway.resource.address is a core abstraction to represent an endpoint. The endpoint can be a server enpoint or a client endpoint.  It is hierarchical in nature and may have a transport resource address representing an another endpoint used to reach it.

The resource address endpoints are defined using an URI. For example, a HTTP endpoint uses http URI scheme and a WebSocket endpoint uses ws URI scheme. This project serves the core abstraction for all those endpoint implementations and the specific endpoint implementations are hosted in other projects.

# Building this Project

## Minimum requirements for building the project
* Java SE Development Kit (JDK) 7 or higher

## Steps for building this project
0. Clone the repo
0. mvn clean install

# Running this Project

0. Integrate this component in gateway.distribution by updating the version in gateway.distribution's pom
0. Build the corresponding gateway.distribution and use it for application development

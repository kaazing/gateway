# Gateway

[![Join the chat at https://gitter.im/kaazing/gateway](https://badges.gitter.im/Join%20Chat.svg)](https://gitter.im/kaazing/gateway?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)

[![Gitter](https://badges.gitter.im/Join%20Chat.svg)](https://gitter.im/kaazing/gateway?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)
[![Build Status][build-status-image]][build-status]

[build-status-image]: https://travis-ci.org/kaazing/gateway.svg?branch=develop
[build-status]: https://travis-ci.org/kaazing/gateway

# About this Project

The gateway.distribution packages together all the dependencies that are incorporated in a
KAAZING Gateway and generates an executable.

# Building this Project

## Minimum requirements for building the project
* Java Developer Kit (JDK) or Java Runtime Environment (JRE) Java 7 (version 1.7.0_21) or higher
* Apache Maven 3.0.5 or higher

## Steps for building this project
0. mvn clean install

# Running this Project

0. cd base/target
1. Unpack the appropriate distribution (Mac/Linux tar -xvf kaazing-gateway-community-5.0.0-unix.tar.gz,
Windows unzip kaazing-gateway-community-5.0.0-windows.zip)
2. Start the Gateway (on Mac/Linux ./kaazing-gateway-community-5.0.0/bin/gateway.start, on Windows ./kaazing-gateway-community-5.0.0/bin/gateway.start.bat)

# Running a Prebuilt Project

You can also download and run this project by downloading the full distribution from kaazing.org

# Learning How to Develop Client Applications

To learn how to develop client applications using the distribution, see the documentation on http://kaazing.org.

# View a Running Demo

To view a demo, see http://kaazing.org.

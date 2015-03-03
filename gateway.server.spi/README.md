# gateway.server.spi

[![Build Status][build-status-image]][build-status]

[build-status-image]: https://travis-ci.org/kaazing/gateway.server.spi.svg?branch=develop
[build-status]: https://travis-ci.org/kaazing/gateway.server.spi

# About this Project

The gateway.server.spi hosts SPI that applications can use to implement some custom functionality. For example, applications can build custom login modules that do authentication and autorization.

# Building this Project

## Minimum requirements for building the project
* Java SE Development Kit (JDK) 7 or higher

## Steps for building this project
0. Clone the repo
0. mvn clean install

# Running this Project

0. Integrate this component in gateway.distribution by updating the version in gateway.distribution's pom
0. Build the corresponding gateway.distribution and use it for application development

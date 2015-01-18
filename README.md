# gateway.service.http.balancer

[![Build Status][build-status-image]][build-status]

[build-status-image]: https://travis-ci.org/kaazing/gateway.service.http.balancer.svg?branch=develop
[build-status]: https://travis-ci.org/kaazing/gateway.service.http.balancer

# About this Project

The gateway.service.http.balancer is an implemenation of balancer service that balances requests for any other gateway service.

# Building this Project

## Minimum requirements for building the project
* Java SE Development Kit (JDK) 7 or higher

## Steps for building this project
0. Clone the repo
0. mvn clean install

# Running this Project

0. Integrate this component in gateway.distribution by updating the version in gateway.distribution's pom
0. Build the corresponding gateway.distribution and use it for application development

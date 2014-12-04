# Gateway Distribution

# About this Project

The gateway.distribution packages together all the dependencies that are incorporated in a 
Kaazing Gateway and generates an executable.

# Building this Project

## Minimum requirements for building the project
* Java Developer Kit (JDK) or Java Runtime Environment (JRE) Java 7 (version 1.7.0_21) or higher
* MVN 3.0.5 or higher
* Node package manager (npm) already installed, necessary to include Kaazing Command Center files. On Linux systems
  this is available by installing nodejs, e.g. 'sudo apt-get install nodejs' on Debian-based systems. On Windows
  system, npm is available in the nodejs install. See http://nodejs.org/download/ for the Windows download.

## Steps for building this project
0. mvn clean install

# Running this Project

0. cd base/target
1. Unpack the appropriate distribution (Mac/Linux tar -xvf kaazing-community-edition-gateway-5.0.0-unix-base.tar.gz, 
Windows unzip kaazing-community-edition-gateway-5.0.0-windows-base.zip)
2. Start the gateway (on Mac/Linux ./kaazing-community-edition-gateway-5.0.0/bin/gateway.start, on Windows ./kaazing-community-edition-gateway-5.0.0/bin/gateway.start.bat)

# Running a Prebuilt Project

You can also download and run this project by downloading the full distribution from kaazing.org

# Learning How to Develop Client Applications

Learn how to develop client applications using the distribution (download from kaazing.org). For example, for JavaScript, point to http://kaazing.org/documentation/5.0/dev-js/o_dev_js.html.

# View a Running Demo

View a demo (see kaazing.org)

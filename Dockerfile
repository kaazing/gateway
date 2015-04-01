#########################################################################################
#
#            .:::
#  ..        .:::        ...    Kaazing Gateway Build Dockerfile
#   ::::     .:::     ,:::
#    ::::    .:::    ,:::       NOTE: Not recommended for production instead find   
#      ::::  .:::  ,:::         official dockerfiles and repos at: 
#       :::: .::: ,:::          https://registry.hub.docker.com/u/kaazing/gateway/
#       :::: .::: ::::          and https://github.com/kaazing/gateway.docker
#      ::::  .:::  ::::
#    ::::    .:::    ::::       This is the Kaazing Gateway Build Dockerfile, it 
#   ::::     .:::     ::::      builds the build and produces a debuggable distribution
#  ,,,       .:::       .,,,
#            .:::
#
#########################################################################################

# Pull base image
FROM maven:3.2.3-jdk-8

# Copy build directories to "/build" dir
# Wish there was an easyway to do this
ADD CONTRIBUTING.md build/CONTRIBUTING.md
ADD README.md build/README.md
ADD management build/management
ADD resource.address build/resource.address
ADD server.demo build/server.demo
ADD test.util build/test.util
ADD Dockerfile build/Dockerfile
ADD bom build/bom
ADD mina.core build/mina.core
ADD security build/security
ADD server.spi build/server.spi
ADD transport build/transport
ADD LICENSE.txt build/LICENSE.txt
ADD bridge build/bridge
ADD mina.netty build/mina.netty
ADD server build/server
ADD service build/service
ADD truststore build/truststore
ADD NOTICE.txt build/NOTICE.txt
ADD distribution build/distribution
ADD pom.xml build/pom.xml
ADD server.api build/server.api
ADD util build/util

# Set "/build" dir as working dir
WORKDIR /build 

# Run the build
RUN mvn -B clean install

# Install the gateway in "/built-gateway"
RUN tar -xvf distribution/target/kaazing-gateway-community-*.tar.gz && \
    mv kaazing-gateway-community-* /built-gateway

# Set "/built-gateway" as workingdir
WORKDIR /built-gateway

# Create volume of key folders 
VOLUME ["build", "kaazing-gateway"]

# Setup container to run gateway
expose 8000
CMD ["bin/gateway.start"]

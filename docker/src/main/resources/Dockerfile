FROM java:openjdk-8u45-jdk

# Get the gateway
COPY target/gateway/kaazing* kaazing-gateway

# Add Log4J settings to redirect to STDOUT
COPY src/main/resources/log4j-config.xml /kaazing-gateway/conf/

# Set Working Dir
WORKDIR kaazing-gateway

# Define default command
CMD ["bin/gateway.start"]

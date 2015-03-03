#
#
#   Copyright (c) 2007-2013, Kaazing Corporation. All rights reserved.
#
#
# Note: Requires Java 7 Keytool

###########################################
# Add democa.crt to cacerts (JDK 1.6,1.7) #
###########################################

export CACERTS=${JAVA_HOME}/jre/lib/security/cacerts
#export CACERTS=/System/Library/Java/JavaVirtualMachines/1.6.0.jdk/Contents/Home/lib/security/cacerts

# Deleting kaazing-democa alias
sudo keytool -delete -alias kaazing-democa -keystore ${CACERTS} -storepass changeit -noprompt

# Adding Demo CA as kaazing-democa alias
sudo keytool -importcert -trustcacerts -alias kaazing-democa -file democa.crt -keystore ${CACERTS} -storepass changeit -noprompt

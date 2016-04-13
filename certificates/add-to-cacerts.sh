# Note: Requires Java 8 Keytool

###########################################
# Add democa.crt to cacerts (JDK 1.8) #
###########################################

export exeDir=$(dirname ${BASH_SOURCE})
export CACERTS=${JAVA_HOME}/jre/lib/security/cacerts

# Deleting kaazing-democa alias
# sudo keytool -delete -alias kaazing-democa -keystore ${CACERTS} -storepass changeit -noprompt

# Adding Demo CA as kaazing-democa alias
sudo keytool -importcert -trustcacerts -alias kaazing-democa -file ${exeDir}/democa.crt -keystore ${CACERTS} -storepass changeit -noprompt

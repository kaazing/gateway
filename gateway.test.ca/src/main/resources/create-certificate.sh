#
#
#   Copyright (c) 2007-2013, Kaazing Corporation. All rights reserved.
#
#
# Note: Requires Java 7 Keytool
# See: http://docs.oracle.com/javase/6/docs/technotes/tools/windows/keytool.html
# See: http://docs.oracle.com/javase/7/docs/technotes/tools/windows/keytool.html

export CERTNAME=$1
export ALIAS=$2
export DNAME=$3
export SAN=$4
export KEYALG=$5

if [ "$KEYALG" == "" ]; then
  export KEYALG="RSA";
fi

echo 'CERTNAME: '$1
echo 'ALIAS: '$2
echo 'DNAME: '$3
echo 'SAN: '$4
echo 'KEYALG: '$KEYALG

rm -rf target
mkdir target
pushd target

######################
# Sign Certificate   #
######################

export DEMOCA_KEYSTORE=../democa.jks
export DEMOCA_CERT=../democa.crt
export DEMOCA_ALIAS=democa

echo 'Download the keytool jar from github, this is a tool that fixes a bug in Java 7 Keytool with wildcard certs'
#git clone https://github.com/dpwspoon/keytool-dnsname -b feature/community
#pushd keytool-dnsname
#mvn clean install
#cp target/keytool.dnsname*.jar ../keytool.dnsname.jar
#popd
#curl -o keytool.dnsname.jar http://artifactory.kaazing.wan/artifactory/archiva-internal/com/kaazing/keytool/com.kaazing.keytool.dnsname/0.0.0.2/com.kaazing.keytool.dnsname-0.0.0.2.jar
cp ../keytool.dnsname.jar keytool.dnsname.jar

echo 'Generate certificate'
keytool -genkeypair -keystore ${CERTNAME}.jks -keypass storepass -storepass storepass -alias ${ALIAS} -dname "${DNAME}" -keyalg ${KEYALG} -validity 1500

echo 'Generate certificate signing request'
keytool -keystore ${CERTNAME}.jks -keypass storepass -storepass storepass -certreq -alias ${ALIAS} > ${CERTNAME}.csr

echo 'Sign certificate'
keytool -J-javaagent:keytool.dnsname.jar -keystore ${DEMOCA_KEYSTORE} -storepass capass -keypass capass -gencert -alias ${DEMOCA_ALIAS} -ext ku:c=dig,keyenc -ext SAN="${SAN}" -rfc < ${CERTNAME}.csr > ${CERTNAME}.crt -validity 1800

echo 'Import signed certificate'
cat ${DEMOCA_CERT} ${CERTNAME}.crt > ca-and-${CERTNAME}.crt
keytool -keystore ${CERTNAME}.jks -storepass storepass -importcert -file ca-and-${CERTNAME}.crt -alias ${ALIAS} -noprompt
# ... is not trusted. Install reply anyway? [no]: yes

cp ${CERTNAME}.crt ..
cp ${CERTNAME}.jks ..

popd

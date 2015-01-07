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
export OCSP_URI=$6

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

echo 'Generate certificate'
keytool -genkeypair -keystore ${CERTNAME}.jks -keypass storepass -storepass storepass -alias ${ALIAS} -dname "${DNAME}" -keyalg RSA -validity 1500

ls
echo "==========="

echo 'Generate certificate signing request'
keytool -keystore ${CERTNAME}.jks -keypass storepass -storepass storepass -certreq -alias ${ALIAS} > ${CERTNAME}.csr

ls
echo "==========="

echo 'Sign certificate'
keytool -keystore ${DEMOCA_KEYSTORE} -storepass capass -keypass capass -gencert -alias ${DEMOCA_ALIAS} -ext ku:c=dig,keyenc -ext SAN="${SAN}" -ext aia=ocsp:uri:http://localhost:8192 -rfc < ${CERTNAME}.csr > ${CERTNAME}.crt -validity 1800

echo 'Import signed certificate'
cat ${DEMOCA_CERT} ${CERTNAME}.crt > ca-and-${CERTNAME}.crt
keytool -keystore ${CERTNAME}.jks -storepass storepass -importcert -file ca-and-${CERTNAME}.crt -alias ${ALIAS} -noprompt
# ... is not trusted. Install reply anyway? [no]: yes

cp ${CERTNAME}.crt ..
cp ${CERTNAME}.jks ..

popd

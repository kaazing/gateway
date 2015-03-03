#
#
#   Copyright (c) 2007-2013, Kaazing Corporation. All rights reserved.
#
#
# Note: Requires Java 7 Keytool
# See: http://docs.oracle.com/javase/6/docs/technotes/tools/windows/keytool.html
# See: http://docs.oracle.com/javase/7/docs/technotes/tools/windows/keytool.html

rm -rf target
mkdir target
pushd target

###################
# Create Demo CA #
###################

echo 'Generate demo CA'
keytool -genkeypair -keystore democa.jks -keypass capass -storepass capass -alias democa -dname "C=US, ST=California, O=Kaazing Corporation, OU=Development, CN=Kaazing Development/emailAddress=support@kaazing.com" -ext bc:c -validity 10000 -keyalg RSA

echo 'Export demo CA certificate in pem format'
keytool -keystore democa.jks -storepass capass -alias democa -exportcert -rfc > democa.crt

echo 'Note: import the democa.crt certificate into the Trusted CAs of each browser once before testing'

cp democa.crt ..
cp democa.jks ..

popd

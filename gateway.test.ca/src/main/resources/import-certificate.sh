#
#
#   Copyright (c) 2007-2013, Kaazing Corporation. All rights reserved.
#
#
# Note: Requires Java 7 Keytool
# See: http://docs.oracle.com/javase/6/docs/technotes/tools/windows/keytool.html
# See: http://docs.oracle.com/javase/7/docs/technotes/tools/windows/keytool.html

######################
# Import Certificate #
######################

export CERTNAME=localhost
export ALIAS=localhost

echo Import into keystore
keytool -importkeystore -srckeystore localhost.jks -srcstoretype JKS -srcalias localhost -srcstorepass storepass -destkeystore keystore.db -deststoretype JCEKS -deststorepass ab987c -destalias localhost -destkeypass ab987c -noprompt
keytool -importkeystore -srckeystore kaazing-test.jks -srcstoretype JKS -srcalias *.kaazing.test -srcstorepass storepass -destkeystore keystore.db -deststoretype JCEKS -deststorepass ab987c -destalias .kaazing.test -destkeypass ab987c -noprompt
keytool -importkeystore -srckeystore one.kaazing.test.jks -srcstoretype JKS -srcalias one.kaazing.test -srcstorepass storepass -destkeystore keystore.db -deststoretype JCEKS -deststorepass ab987c -destalias one.kaazing.test -destkeypass ab987c -noprompt
keytool -importkeystore -srckeystore two.kaazing.test.jks -srcstoretype JKS -srcalias two.kaazing.test -srcstorepass storepass -destkeystore keystore.db -deststoretype JCEKS -deststorepass ab987c -destalias two.kaazing.test -destkeypass ab987c -noprompt
keytool -importkeystore -srckeystore one.example.test.jks -srcstoretype JKS -srcalias one.example.test -srcstorepass storepass -destkeystore keystore.db -deststoretype JCEKS -deststorepass ab987c -destalias one.example.test -destkeypass ab987c -noprompt
keytool -importkeystore -srckeystore two.example.test.jks -srcstoretype JKS -srcalias two.example.test -srcstorepass storepass -destkeystore keystore.db -deststoretype JCEKS -deststorepass ab987c -destalias two.example.test -destkeypass ab987c -noprompt

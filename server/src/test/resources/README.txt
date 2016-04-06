====
    Copyright 2007-2016, Kaazing Corporation. All rights reserved.

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
====

How to replace the localhost certificate: 

* Remove the existing localhost keypair
keytool -delete -alias localhost -keystore keystore.db -storetype JCEKS -storepass `cat keystore.pw` 

* Generate a new keypair into the keystore
keytool -genkeypair \
-alias localhost -keyalg RSA -dname "CN=localhost, OU=Engineering, O=Kaazing, L=Mountain View, ST=California, C=US" \
-keystore keystore.db -storetype JCEKS -storepass `cat keystore.pw` -validity 3600

* Extract the keypair certificate
keytool -exportcert -alias localhost -file localhost.cer \
-keystore keystore.db -storetype JCEKS -storepass `cat keystore.pw` 

* Import the keypair certificate into a truststore as a trusted CA
keytool -importcert -alias localhost -file localhost.cer
-keystore truststore-JCEKS.db -storepass changeit -storetype JCEKS

----------------------------------------------------------------

How to (re)generate the keystore-vhost-*.db files:

  # keytool -genkey -keyalg RSA -alias 'jira.kaazing.wan' -keystore src/test/resources/keystore-vhost-nowildcard.db -storetype JCEKS -storepass ab987c -validity 3600 -keysize 2048

In the prompts, make sure that the "What is your first and last name?" question
is answered using the same name as that of the alias, i.e.:

  What is your first and last name?
    [Unknown]:  jira.kaazing.wan

The answer to this prompt becomes part of the "server name" in the generated
cert, via the Subject Alternative Name (SAN) attribute.

  # keytool -genkey -keyalg RSA -alias 'vpn.kaazing.wan' -keystore src/test/resources/keystore-vhost-nowildcard.db -storetype JCEKS -storepass ab987c -validity 3600 -keysize 2048

Then, to create the keystore which also contains a wilcard cert, do:

  # cp src/test/resources/keystore-vhost-nowildcard.db src/test/resources/keystore-vhost-wildcard.db
  # keytool -genkey -keyalg RSA -alias '*.kaazing.wan' -keystore src/test/resources/keystore-vhost-wildcard.db -storetype JCEKS -storepass ab987c -validity 3600 -keysize 2048


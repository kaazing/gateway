#./create-certificate.sh CERTNAwan ALIAS DNAwan SAN

./create-certificate.sh localhost localhost 'CN=localhost, OU=Engineering, O=Kaazing Corporation, L=Mountain View, S=California, C=US' 'DNS:localhost,DNS:www.localhost'
./create-certificate.sh kaazing-test *.kaazing.test 'CN=*.kaazing.test, OU=Domain Control Validated, O=*.kaazing.test' 'DNS:*.kaazing.test,DNS:kaazing.test'
./create-certificate.sh one.kaazing.test one.kaazing.test 'CN=one.kaazing.test, OU=Engineering, O=Kaazing Corporation, L=Mountain View, S=California, C=US' 'DNS:one.kaazing.test,DNS:www.one.kaazing.test'
./create-certificate.sh two.kaazing.test two.kaazing.test 'CN=two.kaazing.test, OU=Engineering, O=Kaazing Corporation, L=Mountain View, S=California, C=US' 'DNS:two.kaazing.test,DNS:www.two.kaazing.test'
./create-certificate.sh one.example.test one.example.test 'CN=one.example.test, OU=Engineering, O=Kaazing Corporation, L=Mountain View, S=California, C=US' 'DNS:one.example.test,DNS:www.one.example.test'
./create-certificate.sh two.example.test two.example.test 'CN=two.example.test, OU=Engineering, O=Kaazing Corporation, L=Mountain View, S=California, C=US' 'DNS:two.example.test,DNS:www.two.example.test'

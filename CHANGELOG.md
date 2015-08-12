# Change Log

## [Unreleased](https://github.com/kaazing/gateway/tree/HEAD)

[Full Changelog](https://github.com/kaazing/gateway/compare/5.0.1.28...HEAD)

**Fixed bugs:**

- Build errors on travis  [\#264](https://github.com/kaazing/gateway/issues/264)

- KGS-976: UP: sporadic error connecting Command Center to JMS 4.0.6 Dev / JMS 4.0.7 Prod [\#226](https://github.com/kaazing/gateway/issues/226)

- \(doc\) Admin guide is missing maximum-age [\#145](https://github.com/kaazing/gateway/issues/145)

**Closed issues:**

- Remove protocol injection topics again [\#228](https://github.com/kaazing/gateway/issues/228)

- Update http.keepalive.timeout example to use time unit [\#224](https://github.com/kaazing/gateway/issues/224)

- Remove the Session \<type\> from gateway documentation  [\#214](https://github.com/kaazing/gateway/issues/214)

- AgronaMonitoringEntityFactoryTest fails the build \(on Mac OS x\) [\#171](https://github.com/kaazing/gateway/issues/171)

- \(doc\) Add links to admin-reference/p\_config\_multicast.md [\#134](https://github.com/kaazing/gateway/issues/134)

## [5.0.1.28](https://github.com/kaazing/gateway/tree/5.0.1.28) (2015-06-30)

[Full Changelog](https://github.com/kaazing/gateway/compare/5.0.1.27...5.0.1.28)

**Closed issues:**

-  Create AmqpProxyServiceExtensionSpi in gateway [\#159](https://github.com/kaazing/gateway/issues/159)

## [5.0.1.27](https://github.com/kaazing/gateway/tree/5.0.1.27) (2015-06-15)

[Full Changelog](https://github.com/kaazing/gateway/compare/5.0.1.26...5.0.1.27)

**Fixed bugs:**

- Update to latest community to consume fixes for animal sniffer plugin [\#176](https://github.com/kaazing/gateway/issues/176)

**Closed issues:**

- Long start time on systems with low entropy \(containers\) due to calls to getSecureBytes. [\#167](https://github.com/kaazing/gateway/issues/167)

## [5.0.1.26](https://github.com/kaazing/gateway/tree/5.0.1.26) (2015-05-26)

[Full Changelog](https://github.com/kaazing/gateway/compare/5.0.1.25...5.0.1.26)

**Closed issues:**

- Allow ProxyService extensions [\#130](https://github.com/kaazing/gateway/issues/130)

- Allow NIOSocketConnector to use a proxy transport if configured [\#122](https://github.com/kaazing/gateway/issues/122)

## [5.0.1.25](https://github.com/kaazing/gateway/tree/5.0.1.25) (2015-05-21)

[Full Changelog](https://github.com/kaazing/gateway/compare/5.0.1.24...5.0.1.25)

## [5.0.1.24](https://github.com/kaazing/gateway/tree/5.0.1.24) (2015-05-20)

[Full Changelog](https://github.com/kaazing/gateway/compare/5.0.1.23...5.0.1.24)

**Fixed bugs:**

- Test is using ephemeral port to bind [\#114](https://github.com/kaazing/gateway/issues/114)

- JmxManagementServiceHandlerTest\#testNoJMXBindingNameConflictsOnMultiServicesUsingSameAccept failing and has been ignored [\#32](https://github.com/kaazing/gateway/issues/32)

**Closed issues:**

- \(doc\) Typos [\#142](https://github.com/kaazing/gateway/issues/142)

- SseSameOriginIT sseIe8HttpxeConnectAndGetData fails intermittently [\#141](https://github.com/kaazing/gateway/issues/141)

- accept-options / connect options should be "sequence" to allows extenstions  [\#69](https://github.com/kaazing/gateway/issues/69)

## [5.0.1.23](https://github.com/kaazing/gateway/tree/5.0.1.23) (2015-04-17)

[Full Changelog](https://github.com/kaazing/gateway/compare/5.0.1.22...5.0.1.23)

**Closed issues:**

- Enterprise Shield +1 connection fails when using different transport URIs over SOCKS [\#96](https://github.com/kaazing/gateway/issues/96)

## [5.0.1.22](https://github.com/kaazing/gateway/tree/5.0.1.22) (2015-04-09)

[Full Changelog](https://github.com/kaazing/gateway/compare/5.0.1.21...5.0.1.22)

**Implemented enhancements:**

- Configuration doesn't allow \<connect-options\> in \<service-defaults\> [\#64](https://github.com/kaazing/gateway/issues/64)

**Fixed bugs:**

- SseSameOriginIT.sseIe8HttpxeConnectAndGetData sometimes fails [\#60](https://github.com/kaazing/gateway/issues/60)

- WsebTransportIT.testEchoAlignedDownstream sporadically failing [\#41](https://github.com/kaazing/gateway/issues/41)

- WsnConnectorTest.shouldNotHangOnToHttpConnectSessionsWhenEstablishingAndTearingDownWsnConnectorSessions sporadically hanging  [\#37](https://github.com/kaazing/gateway/issues/37)

**Closed issues:**

- Use sequence numbers to detect out of order requests in wseb [\#73](https://github.com/kaazing/gateway/issues/73)

- Add Visitor to reorder elements in accept/connect options [\#70](https://github.com/kaazing/gateway/issues/70)

## [5.0.1.21](https://github.com/kaazing/gateway/tree/5.0.1.21) (2015-04-03)

[Full Changelog](https://github.com/kaazing/gateway/compare/gateway.distribution-5.0.1.20...5.0.1.21)

**Closed issues:**

- Create maven BOM of gateway [\#75](https://github.com/kaazing/gateway/issues/75)

## [gateway.distribution-5.0.1.20](https://github.com/kaazing/gateway/tree/gateway.distribution-5.0.1.20) (2015-03-26)

[Full Changelog](https://github.com/kaazing/gateway/compare/gateway.distribution-5.0.1.19...gateway.distribution-5.0.1.20)

**Fixed bugs:**

- Small tweaks for readme [\#18](https://github.com/kaazing/gateway/issues/18)

- Incorrect port number in README.md [\#15](https://github.com/kaazing/gateway/issues/15)

**Closed issues:**

- transport.wsn.UnresolvableHostnameIT [\#59](https://github.com/kaazing/gateway/issues/59)

- Ensure Subject gets correctly propagated up the transport layers using the new get/setSubject methods [\#55](https://github.com/kaazing/gateway/issues/55)

- Add IoSessionEx methods to store and get Subject and listen for subject changes [\#52](https://github.com/kaazing/gateway/issues/52)

- Gateway will not start up using Azul zing JVM [\#49](https://github.com/kaazing/gateway/issues/49)

- Integration tests failing in HTTP directory service [\#47](https://github.com/kaazing/gateway/issues/47)

- Importing all Gateway modules into eclipse results in many errors "Plugin execution not covered by lifecycle configuration..." [\#44](https://github.com/kaazing/gateway/issues/44)

- gateway.client.javascript.bridge only builds with clean do to unpack-bower plugin [\#35](https://github.com/kaazing/gateway/issues/35)

- Sslv3Test failing in latest JDK and have been ignored [\#31](https://github.com/kaazing/gateway/issues/31)

- git also required for cloning [\#10](https://github.com/kaazing/gateway/issues/10)

## [gateway.distribution-5.0.1.19](https://github.com/kaazing/gateway/tree/gateway.distribution-5.0.1.19) (2015-01-09)

[Full Changelog](https://github.com/kaazing/gateway/compare/gateway.distribution-5.0.1.18...gateway.distribution-5.0.1.19)

## [gateway.distribution-5.0.1.18](https://github.com/kaazing/gateway/tree/gateway.distribution-5.0.1.18) (2014-12-20)

[Full Changelog](https://github.com/kaazing/gateway/compare/gateway.distribution-5.0.1.17...gateway.distribution-5.0.1.18)

## [gateway.distribution-5.0.1.17](https://github.com/kaazing/gateway/tree/gateway.distribution-5.0.1.17) (2014-12-18)

[Full Changelog](https://github.com/kaazing/gateway/compare/gateway.distribution-5.0.1.16...gateway.distribution-5.0.1.17)

**Closed issues:**

- building gateway on MacOSX 10.7+ can yield " group id '2119906183' is too big \( \> 2097151 \)" assembly error [\#23](https://github.com/kaazing/gateway/issues/23)

## [gateway.distribution-5.0.1.16](https://github.com/kaazing/gateway/tree/gateway.distribution-5.0.1.16) (2014-12-17)

[Full Changelog](https://github.com/kaazing/gateway/compare/gateway.distribution-5.0.1.15...gateway.distribution-5.0.1.16)

## [gateway.distribution-5.0.1.15](https://github.com/kaazing/gateway/tree/gateway.distribution-5.0.1.15) (2014-12-17)

[Full Changelog](https://github.com/kaazing/gateway/compare/gateway.distribution-5.0.1.14...gateway.distribution-5.0.1.15)

## [gateway.distribution-5.0.1.14](https://github.com/kaazing/gateway/tree/gateway.distribution-5.0.1.14) (2014-12-16)

[Full Changelog](https://github.com/kaazing/gateway/compare/gateway.distribution-5.0.0.16...gateway.distribution-5.0.1.14)

## [gateway.distribution-5.0.0.16](https://github.com/kaazing/gateway/tree/gateway.distribution-5.0.0.16) (2014-12-16)

[Full Changelog](https://github.com/kaazing/gateway/compare/gateway.distribution-5.0.0.15...gateway.distribution-5.0.0.16)

## [gateway.distribution-5.0.0.15](https://github.com/kaazing/gateway/tree/gateway.distribution-5.0.0.15) (2014-12-12)

[Full Changelog](https://github.com/kaazing/gateway/compare/gateway.distribution-5.0.0.14...gateway.distribution-5.0.0.15)

## [gateway.distribution-5.0.0.14](https://github.com/kaazing/gateway/tree/gateway.distribution-5.0.0.14) (2014-12-11)

[Full Changelog](https://github.com/kaazing/gateway/compare/gateway.distribution-5.0.0.13...gateway.distribution-5.0.0.14)

## [gateway.distribution-5.0.0.13](https://github.com/kaazing/gateway/tree/gateway.distribution-5.0.0.13) (2014-12-10)

[Full Changelog](https://github.com/kaazing/gateway/compare/gateway.distribution-5.0.0.12...gateway.distribution-5.0.0.13)

## [gateway.distribution-5.0.0.12](https://github.com/kaazing/gateway/tree/gateway.distribution-5.0.0.12) (2014-12-10)

[Full Changelog](https://github.com/kaazing/gateway/compare/gateway.distribution-5.0.0.11...gateway.distribution-5.0.0.12)

## [gateway.distribution-5.0.0.11](https://github.com/kaazing/gateway/tree/gateway.distribution-5.0.0.11) (2014-12-04)

[Full Changelog](https://github.com/kaazing/gateway/compare/gateway.distribution-5.0.0.10...gateway.distribution-5.0.0.11)

## [gateway.distribution-5.0.0.10](https://github.com/kaazing/gateway/tree/gateway.distribution-5.0.0.10) (2014-12-04)

[Full Changelog](https://github.com/kaazing/gateway/compare/gateway.distribution-5.0.0.9...gateway.distribution-5.0.0.10)

## [gateway.distribution-5.0.0.9](https://github.com/kaazing/gateway/tree/gateway.distribution-5.0.0.9) (2014-12-04)



\* *This Change Log was automatically generated by [github_changelog_generator](https://github.com/skywinder/Github-Changelog-Generator)*
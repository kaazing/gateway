# Change Log

## [5.5.0-RC001](https://github.com/kaazing/gateway/tree/5.5.0-RC001) (2017-02-16)
[Full Changelog](https://github.com/kaazing/gateway/compare/5.4.1...5.5.0-RC001)

**Merged pull requests:**

- NPEs in DEBUG and ERROR log if IP address is incorrect  [\#868](https://github.com/kaazing/gateway/pull/868) ([StCostea](https://github.com/StCostea))
- Add.hazelcast.topics [\#862](https://github.com/kaazing/gateway/pull/862) ([Anisotrop](https://github.com/Anisotrop))

## [5.4.1](https://github.com/kaazing/gateway/tree/5.4.1) (2017-02-10)
[Full Changelog](https://github.com/kaazing/gateway/compare/5.4.0...5.4.1)

**Merged pull requests:**

- Revert "\(\#850\) Sync'd AbstractIoAcceptor from mina 2.0.16, fixed bind… [\#867](https://github.com/kaazing/gateway/pull/867) ([sbadugu](https://github.com/sbadugu))
- Clean up system property. [\#865](https://github.com/kaazing/gateway/pull/865) ([vmaraloiu](https://github.com/vmaraloiu))
- Reverted shade plugin to generate the reduced pom. [\#864](https://github.com/kaazing/gateway/pull/864) ([Anisotrop](https://github.com/Anisotrop))
- Remove System Properties which should be undocumented from Javadoc [\#860](https://github.com/kaazing/gateway/pull/860) ([vmaraloiu](https://github.com/vmaraloiu))
- Fix serverShouldSend501ToUnknownTransferEncoding\(\) test. [\#859](https://github.com/kaazing/gateway/pull/859) ([vmaraloiu](https://github.com/vmaraloiu))
- Removed tests which are implemented in k3po [\#854](https://github.com/kaazing/gateway/pull/854) ([vstratan](https://github.com/vstratan))
- Added Log warning first time we see Http 1.0. [\#851](https://github.com/kaazing/gateway/pull/851) ([vmaraloiu](https://github.com/vmaraloiu))
- Added the `aggregate-add-third-party-mojo` plugin [\#812](https://github.com/kaazing/gateway/pull/812) ([mgherghe](https://github.com/mgherghe))

## [5.4.0](https://github.com/kaazing/gateway/tree/5.4.0) (2017-01-20)
[Full Changelog](https://github.com/kaazing/gateway/compare/5.3.2...5.4.0)

**Fixed bugs:**

- UdpAcceptorIT.concurrentConnections is failing intermittently [\#796](https://github.com/kaazing/gateway/issues/796)
- JMX management service: closeSessions \(by principal\) service bean method causes NPEs [\#783](https://github.com/kaazing/gateway/issues/783)
- Exceptions reported by HttpAcceptor...doExceptionCaught do not include session id [\#777](https://github.com/kaazing/gateway/issues/777)
- management.jmx service:  user principals are not visible in jconsole in our mxbeans unless concrete class is named in gateway config [\#773](https://github.com/kaazing/gateway/issues/773)
- ClassCastException in ServiceManagementBean$DefaultServiceManagementBean.doSessionCreated [\#733](https://github.com/kaazing/gateway/issues/733)
- OOTB log4j-config.xml and gateway-config.xml improvements are needed \(does not document MaxBackupIndex, etc\) [\#709](https://github.com/kaazing/gateway/issues/709)
- java.io.IOException: Broken pipe causes incorrect event reporting in TCP layer [\#688](https://github.com/kaazing/gateway/issues/688)
- Diagnostic message for unexpected exception in a filter should include session details [\#685](https://github.com/kaazing/gateway/issues/685)
- Build failure in Http Directory Service: 3 tests systematically failing in HttpDirectoryServiceIT on my Windows laptop [\#682](https://github.com/kaazing/gateway/issues/682)
- failure in WsnConnectorTest [\#655](https://github.com/kaazing/gateway/issues/655)
- Need better diagnostics when a ProtocolDecoderException occurs or other unexpected transport exception [\#384](https://github.com/kaazing/gateway/issues/384)

**Closed issues:**

- HTTP request decoder exception during method decoding \(sporadic\) [\#806](https://github.com/kaazing/gateway/issues/806)
- Remove sigar dependency [\#781](https://github.com/kaazing/gateway/issues/781)
- test.util dependency should always be test scope [\#676](https://github.com/kaazing/gateway/issues/676)
- net.sourceforge.cobertura.javancss.parser.ParseException [\#208](https://github.com/kaazing/gateway/issues/208)
- Gateway 4.0.4.224: Text received as BLOB when connecting to an Echo service through a Proxy [\#154](https://github.com/kaazing/gateway/issues/154)

**Merged pull requests:**

- Remove unnecessary code from product [\#856](https://github.com/kaazing/gateway/pull/856) ([mgherghe](https://github.com/mgherghe))
- .ksn parameter is converted into X-Sequence-No header for [\#855](https://github.com/kaazing/gateway/pull/855) ([jitsni](https://github.com/jitsni))
- Fixing a race in UdpConnectorIT.clientSentData [\#853](https://github.com/kaazing/gateway/pull/853) ([jitsni](https://github.com/jitsni))
- Syncing AbstractIoAcceptor from mina [\#850](https://github.com/kaazing/gateway/pull/850) ([jitsni](https://github.com/jitsni))
- Fixed Basic and Digest authentication scripts on http.proxy [\#849](https://github.com/kaazing/gateway/pull/849) ([apirvu](https://github.com/apirvu))
- Not consuming extra CRLF after HTTP response [\#847](https://github.com/kaazing/gateway/pull/847) ([jitsni](https://github.com/jitsni))
- Set early access feature http proxy test to be disabled [\#845](https://github.com/kaazing/gateway/pull/845) ([dpwspoon](https://github.com/dpwspoon))
- Added keystore and truststore to LoginModule options [\#844](https://github.com/kaazing/gateway/pull/844) ([dpwspoon](https://github.com/dpwspoon))
- Enabling http.proxy feature by default [\#842](https://github.com/kaazing/gateway/pull/842) ([jitsni](https://github.com/jitsni))
- \(issue\#912, management\) Fixed a memory leak by altering CollectOnlyMa… [\#841](https://github.com/kaazing/gateway/pull/841) ([cmebarrow](https://github.com/cmebarrow))
- Fix for issue 926. [\#840](https://github.com/kaazing/gateway/pull/840) ([vmaraloiu](https://github.com/vmaraloiu))
- Ordering open and message events of UDP child channels [\#839](https://github.com/kaazing/gateway/pull/839) ([jitsni](https://github.com/jitsni))
- Add logging filter constructor [\#838](https://github.com/kaazing/gateway/pull/838) ([cmebarrow](https://github.com/cmebarrow))
- Fix the message in log warning [\#837](https://github.com/kaazing/gateway/pull/837) ([stanculescu](https://github.com/stanculescu))
- Fix the diagnostic message for unexpected exception to include session details [\#836](https://github.com/kaazing/gateway/pull/836) ([stanculescu](https://github.com/stanculescu))
- Fix the message in connect and accept options. [\#835](https://github.com/kaazing/gateway/pull/835) ([stanculescu](https://github.com/stanculescu))
- Improving the http.proxy service accept/connect uri validation message [\#834](https://github.com/kaazing/gateway/pull/834) ([jitsni](https://github.com/jitsni))
- Http proxy redirect \(not following\) occasional test failure fix [\#833](https://github.com/kaazing/gateway/pull/833) ([dpwspoon](https://github.com/dpwspoon))
- Upgrading agrona version \(package names are different\) [\#832](https://github.com/kaazing/gateway/pull/832) ([jitsni](https://github.com/jitsni))
- Fixing k3po scripts for http methods [\#831](https://github.com/kaazing/gateway/pull/831) ([jitsni](https://github.com/jitsni))
- Fix ws.inactivity.timeout and http.keepalive.timeout logging order in connect and accept options. [\#828](https://github.com/kaazing/gateway/pull/828) ([stanculescu](https://github.com/stanculescu))
- http.proxy service is changed to accept only with URIs ending with / [\#827](https://github.com/kaazing/gateway/pull/827) ([jitsni](https://github.com/jitsni))
- Issue 676 test.util scope [\#826](https://github.com/kaazing/gateway/pull/826) ([cmebarrow](https://github.com/cmebarrow))
- \(\#688\) Fixed NioWorker to ensure we don't incorrectly report a data received event after an exception... [\#823](https://github.com/kaazing/gateway/pull/823) ([cmebarrow](https://github.com/cmebarrow))
- Reusing of downstream message event issue for udp [\#820](https://github.com/kaazing/gateway/pull/820) ([jitsni](https://github.com/jitsni))
- .ksn parameter is converted into header only when httpxe is detected. [\#819](https://github.com/kaazing/gateway/pull/819) ([jitsni](https://github.com/jitsni))
- Added license.txt to distribution [\#817](https://github.com/kaazing/gateway/pull/817) ([dpwspoon](https://github.com/dpwspoon))
- Validating Host header when client sends a request in absolute-form [\#816](https://github.com/kaazing/gateway/pull/816) ([jitsni](https://github.com/jitsni))
- \[DO NOT MERGE\] Kroadmap 1005 login module [\#815](https://github.com/kaazing/gateway/pull/815) ([DoruM](https://github.com/DoruM))
- Memory Cleanup [\#814](https://github.com/kaazing/gateway/pull/814) ([dpwspoon](https://github.com/dpwspoon))
- Fixed instance were SslConnector was limiting the TLS session cache o… [\#813](https://github.com/kaazing/gateway/pull/813) ([dpwspoon](https://github.com/dpwspoon))
- Making usascii CharsetDecoder an instance variable [\#808](https://github.com/kaazing/gateway/pull/808) ([jitsni](https://github.com/jitsni))
- HTTP Connector tests [\#807](https://github.com/kaazing/gateway/pull/807) ([jitsni](https://github.com/jitsni))
- Hazelcast upgrade [\#804](https://github.com/kaazing/gateway/pull/804) ([danibusu](https://github.com/danibusu))
- Forward port: Converting transport options that contain tls values  [\#803](https://github.com/kaazing/gateway/pull/803) ([sbadugu](https://github.com/sbadugu))
- Http Acceptor tests [\#802](https://github.com/kaazing/gateway/pull/802) ([jitsni](https://github.com/jitsni))
- Turn rest TTL service clarification [\#801](https://github.com/kaazing/gateway/pull/801) ([dpwspoon](https://github.com/dpwspoon))
- Converting transport options that contain tls values [\#799](https://github.com/kaazing/gateway/pull/799) ([jitsni](https://github.com/jitsni))
- Case sensative bug [\#797](https://github.com/kaazing/gateway/pull/797) ([dpwspoon](https://github.com/dpwspoon))
- Simplify WsnAcceptorRule by removing the unused log4j related logic and using TransportFactory.injectResources [\#795](https://github.com/kaazing/gateway/pull/795) ([cmebarrow](https://github.com/cmebarrow))
- Removed some remaining vestiges of sigar from NOTICE.txt and ... [\#794](https://github.com/kaazing/gateway/pull/794) ([cmebarrow](https://github.com/cmebarrow))
- Fixed a trivial error in JmxSessionPrincipalsIT which was causing occasional test failures \(in that test or subclass JmxSessionPrincipalsSupertypeIT\) [\#793](https://github.com/kaazing/gateway/pull/793) ([cmebarrow](https://github.com/cmebarrow))
- Remove sigar dependency, remove system MBeans [\#792](https://github.com/kaazing/gateway/pull/792) ([cmebarrow](https://github.com/cmebarrow))
- \(management\) Change junit and \(especially\) jmock-junit dependencies … [\#791](https://github.com/kaazing/gateway/pull/791) ([cmebarrow](https://github.com/cmebarrow))
- Issue 783: NullPointerExceptions after calling closeSessions on ServiceMXBean [\#790](https://github.com/kaazing/gateway/pull/790) ([cmebarrow](https://github.com/cmebarrow))
- Added udp.tranport option to xsd [\#789](https://github.com/kaazing/gateway/pull/789) ([dpwspoon](https://github.com/dpwspoon))
- tls scheme [\#788](https://github.com/kaazing/gateway/pull/788) ([jitsni](https://github.com/jitsni))
- UDP child channel write future is fired as success always [\#785](https://github.com/kaazing/gateway/pull/785) ([jitsni](https://github.com/jitsni))
- Management fixes [\#784](https://github.com/kaazing/gateway/pull/784) ([cmebarrow](https://github.com/cmebarrow))
- Using a queue instead of ring buffer to pass messages to udp child channels [\#782](https://github.com/kaazing/gateway/pull/782) ([jitsni](https://github.com/jitsni))
- Add README [\#768](https://github.com/kaazing/gateway/pull/768) ([robinzimmermann](https://github.com/robinzimmermann))
- Added new k3po tests for HTTP proxy story [\#622](https://github.com/kaazing/gateway/pull/622) ([vstratan](https://github.com/vstratan))

## [5.3.2](https://github.com/kaazing/gateway/tree/5.3.2) (2016-11-14)
[Full Changelog](https://github.com/kaazing/gateway/compare/5.3.1...5.3.2)

**Merged pull requests:**

- Http proxy [\#787](https://github.com/kaazing/gateway/pull/787) ([claudiaop](https://github.com/claudiaop))
- Adding new websocket specification tests for case-insensiitve headers [\#786](https://github.com/kaazing/gateway/pull/786) ([jitsni](https://github.com/jitsni))
- If there is transport for udp, udp's host address is not resolved. [\#780](https://github.com/kaazing/gateway/pull/780) ([jitsni](https://github.com/jitsni))
- Logging improvements and metrics fix [\#779](https://github.com/kaazing/gateway/pull/779) ([cmebarrow](https://github.com/cmebarrow))
- Ticket\#434 fix frame masking check [\#775](https://github.com/kaazing/gateway/pull/775) ([danibusu](https://github.com/danibusu))
- Udp benchmarks [\#772](https://github.com/kaazing/gateway/pull/772) ([jitsni](https://github.com/jitsni))
- Configuring mina.netty logging factory [\#762](https://github.com/kaazing/gateway/pull/762) ([jitsni](https://github.com/jitsni))

## [5.3.1](https://github.com/kaazing/gateway/tree/5.3.1) (2016-11-03)
[Full Changelog](https://github.com/kaazing/gateway/compare/5.2.5...5.3.1)

**Fixed bugs:**

- Issue with finer to coarser granularity conversion of time unit in config file  [\#595](https://github.com/kaazing/gateway/issues/595)

**Closed issues:**

- UDP processing may be slowed by select, we should call selector.wakeup when udp events come in [\#752](https://github.com/kaazing/gateway/issues/752)

**Merged pull requests:**

- Catchup merge from release branch [\#776](https://github.com/kaazing/gateway/pull/776) ([dpwspoon](https://github.com/dpwspoon))
- \(service/proxy\) Make AbstractProxyHandler.setMaximumPendingBytes public [\#770](https://github.com/kaazing/gateway/pull/770) ([cmebarrow](https://github.com/cmebarrow))
- Support for configuring http.realm [\#765](https://github.com/kaazing/gateway/pull/765) ([dpwspoon](https://github.com/dpwspoon))
- shutdown causes selector loop not terminate [\#763](https://github.com/kaazing/gateway/pull/763) ([jitsni](https://github.com/jitsni))
- Fixing udp server boss thread names [\#761](https://github.com/kaazing/gateway/pull/761) ([jitsni](https://github.com/jitsni))
- 5.3.1 nioworker perf fix selector wakeup [\#760](https://github.com/kaazing/gateway/pull/760) ([cmebarrow](https://github.com/cmebarrow))
- Catchup develop from release [\#758](https://github.com/kaazing/gateway/pull/758) ([dpwspoon](https://github.com/dpwspoon))
- Sync with netty 3.5.10 \(apply to release/5.3.1\) [\#757](https://github.com/kaazing/gateway/pull/757) ([cmebarrow](https://github.com/cmebarrow))
- Enhance ITUtil.toTestRule\(...\) to support @Mock and @Auto annotations… [\#756](https://github.com/kaazing/gateway/pull/756) ([jfallows](https://github.com/jfallows))
- Skip transport factories for disabled transports [\#754](https://github.com/kaazing/gateway/pull/754) ([jfallows](https://github.com/jfallows))
- Http acceptor multi factor acceptor tests [\#753](https://github.com/kaazing/gateway/pull/753) ([dpwspoon](https://github.com/dpwspoon))
- Removed Agrona from the mina.netty jar [\#751](https://github.com/kaazing/gateway/pull/751) ([mgherghe](https://github.com/mgherghe))
- Make mock handler tolerant of call to sessionClosed [\#750](https://github.com/kaazing/gateway/pull/750) ([jfallows](https://github.com/jfallows))
- Relax count on number of IDLE events [\#749](https://github.com/kaazing/gateway/pull/749) ([jfallows](https://github.com/jfallows))
- Reduced number of follows in ConnectorMultiFactorAuthIT such that tes… [\#748](https://github.com/kaazing/gateway/pull/748) ([dpwspoon](https://github.com/dpwspoon))

## [5.2.5](https://github.com/kaazing/gateway/tree/5.2.5) (2016-10-19)
[Full Changelog](https://github.com/kaazing/gateway/compare/5.2.4...5.2.5)

**Merged pull requests:**

- catchup develop from release [\#747](https://github.com/kaazing/gateway/pull/747) ([dpwspoon](https://github.com/dpwspoon))
- Altered NioWorker.select and AbstractNioSelector.select to use 10ms select timeout [\#746](https://github.com/kaazing/gateway/pull/746) ([cmebarrow](https://github.com/cmebarrow))
- Add "connect.strategy" property to proxy service\(s\) [\#745](https://github.com/kaazing/gateway/pull/745) ([jfallows](https://github.com/jfallows))
- add check for mux scheme when adding trailing slash [\#744](https://github.com/kaazing/gateway/pull/744) ([danibusu](https://github.com/danibusu))
- Changed turn.rest response to use json library instead of string formatting [\#743](https://github.com/kaazing/gateway/pull/743) ([mgherghe](https://github.com/mgherghe))
- Adjusting IdleStrategy's parking times [\#741](https://github.com/kaazing/gateway/pull/741) ([jitsni](https://github.com/jitsni))
- Created GrantLoginModule. [\#739](https://github.com/kaazing/gateway/pull/739) ([nemigaservices](https://github.com/nemigaservices))
- add early access feature for tcp security extensions [\#736](https://github.com/kaazing/gateway/pull/736) ([danibusu](https://github.com/danibusu))
- Add support for proxy service connect.strategy … [\#735](https://github.com/kaazing/gateway/pull/735) ([jfallows](https://github.com/jfallows))
- Http.multi.auth [\#734](https://github.com/kaazing/gateway/pull/734) ([dpwspoon](https://github.com/dpwspoon))
- Add convenience methods and tidy POM [\#732](https://github.com/kaazing/gateway/pull/732) ([jfallows](https://github.com/jfallows))
- Modified DefaultExpiringState.putIfAbsent to accept ttl [\#731](https://github.com/kaazing/gateway/pull/731) ([apirvu](https://github.com/apirvu))
- Selector.selectNow is triggered by passing timeout=0L [\#728](https://github.com/kaazing/gateway/pull/728) ([jitsni](https://github.com/jitsni))
- Tcp.ip.whitelist [\#727](https://github.com/kaazing/gateway/pull/727) ([danibusu](https://github.com/danibusu))
- Network interface syntax enhancements [\#626](https://github.com/kaazing/gateway/pull/626) ([irina-mitrea-luxoft](https://github.com/irina-mitrea-luxoft))

## [5.2.4](https://github.com/kaazing/gateway/tree/5.2.4) (2016-10-03)
[Full Changelog](https://github.com/kaazing/gateway/compare/5.2.3...5.2.4)

**Fixed bugs:**

- RC releases: Gateway fails to start up due to error in Update service concerning version format [\#715](https://github.com/kaazing/gateway/issues/715)

**Merged pull requests:**

- Configure Agrona's buffer size for udp child channels [\#726](https://github.com/kaazing/gateway/pull/726) ([jitsni](https://github.com/jitsni))
- Release 5.2.4 branch merge [\#725](https://github.com/kaazing/gateway/pull/725) ([jitsni](https://github.com/jitsni))
- Syncing netty changes w.r.t interestops [\#724](https://github.com/kaazing/gateway/pull/724) ([jitsni](https://github.com/jitsni))
- Set UDP receive buffer size to 2048 [\#723](https://github.com/kaazing/gateway/pull/723) ([dpwspoon](https://github.com/dpwspoon))
- Revert "Changed turn.rest json response to include stun URLs" [\#722](https://github.com/kaazing/gateway/pull/722) ([dpwspoon](https://github.com/dpwspoon))
- Revert "Changed turn.rest json response to include stun URLs" [\#721](https://github.com/kaazing/gateway/pull/721) ([dpwspoon](https://github.com/dpwspoon))
- Fixing IdleStrategy compilation issue [\#720](https://github.com/kaazing/gateway/pull/720) ([jitsni](https://github.com/jitsni))
- Parking the thread for 10mills instead of 10micros [\#719](https://github.com/kaazing/gateway/pull/719) ([jitsni](https://github.com/jitsni))
- Bugfix/870 update parent [\#717](https://github.com/kaazing/gateway/pull/717) ([ahousing](https://github.com/ahousing))
- Add support for release candidates. [\#716](https://github.com/kaazing/gateway/pull/716) ([Anisotrop](https://github.com/Anisotrop))
- Added annotation for the turn.rest service in xsd file [\#714](https://github.com/kaazing/gateway/pull/714) ([mgherghe](https://github.com/mgherghe))
- Turn Proxy - Unify Logging Output [\#713](https://github.com/kaazing/gateway/pull/713) ([mgherghe](https://github.com/mgherghe))
- Added maximum.redirects to accept/connect options [\#712](https://github.com/kaazing/gateway/pull/712) ([dpwspoon](https://github.com/dpwspoon))
- Worker threads are going to PARK state unnecessarily [\#711](https://github.com/kaazing/gateway/pull/711) ([jitsni](https://github.com/jitsni))

## [5.2.3](https://github.com/kaazing/gateway/tree/5.2.3) (2016-09-20)
[Full Changelog](https://github.com/kaazing/gateway/compare/5.2.2...5.2.3)

**Fixed bugs:**

- WSN Connector: Connection should fail when the negotiated extension is not in the list of supported extensions [\#314](https://github.com/kaazing/gateway/issues/314)
- WSN Connector: Missing Sec-WebSocket-Extensions header in the handshake request [\#309](https://github.com/kaazing/gateway/issues/309)

**Merged pull requests:**

- add getter for GatewayConfigurationDocument: [\#708](https://github.com/kaazing/gateway/pull/708) ([dpwspoon](https://github.com/dpwspoon))
- Update test.Gateway to allow fetching of generated xml configuration [\#707](https://github.com/kaazing/gateway/pull/707) ([danibusu](https://github.com/danibusu))
- WebSocket extensions decide whether to be part of handshake or not [\#705](https://github.com/kaazing/gateway/pull/705) ([jitsni](https://github.com/jitsni))
- Fixed typo in early access feature description [\#704](https://github.com/kaazing/gateway/pull/704) ([dpwspoon](https://github.com/dpwspoon))
- Changed turn.rest json response to include stun URLs [\#701](https://github.com/kaazing/gateway/pull/701) ([mgherghe](https://github.com/mgherghe))
- TLS support added [\#653](https://github.com/kaazing/gateway/pull/653) ([justinma246](https://github.com/justinma246))

## [5.2.2](https://github.com/kaazing/gateway/tree/5.2.2) (2016-09-09)
[Full Changelog](https://github.com/kaazing/gateway/compare/5.2.1...5.2.2)

**Fixed bugs:**

- wsn transport is reacting incorrectly to case where maximum ws message size is exceeded [\#463](https://github.com/kaazing/gateway/issues/463)
- WSN Connector: Connection should fail when the value of the `Connection` header in handshake response is not `Upgrade` [\#310](https://github.com/kaazing/gateway/issues/310)

**Merged pull requests:**

- Removed turn.proxy module [\#703](https://github.com/kaazing/gateway/pull/703) ([mgherghe](https://github.com/mgherghe))
- Apirvu param prot [\#702](https://github.com/kaazing/gateway/pull/702) ([dpwspoon](https://github.com/dpwspoon))
- Turn proxy support for turn data message [\#699](https://github.com/kaazing/gateway/pull/699) ([dpwspoon](https://github.com/dpwspoon))
- Tweak comment in configuration [\#698](https://github.com/kaazing/gateway/pull/698) ([robinzimmermann](https://github.com/robinzimmermann))
- Update schema to latest. Add update.check service [\#697](https://github.com/kaazing/gateway/pull/697) ([robinzimmermann](https://github.com/robinzimmermann))
- Http Connector authenticating to 401 and Login Module expiring state [\#696](https://github.com/kaazing/gateway/pull/696) ([dpwspoon](https://github.com/dpwspoon))
- Using tcp acceptor's worker pool for udp child sessions [\#694](https://github.com/kaazing/gateway/pull/694) ([jitsni](https://github.com/jitsni))
- Parameterized test classes to run with TCP and UDP [\#693](https://github.com/kaazing/gateway/pull/693) ([apirvu](https://github.com/apirvu))
- Changes for MESSAGE-INTEGRITY generation [\#692](https://github.com/kaazing/gateway/pull/692) ([Anisotrop](https://github.com/Anisotrop))
- Added tests for webRTC turn rest service [\#691](https://github.com/kaazing/gateway/pull/691) ([vstratan](https://github.com/vstratan))
- Adding proxy service tests [\#689](https://github.com/kaazing/gateway/pull/689) ([jitsni](https://github.com/jitsni))
- Ignored symlink tests if the user has no OS access to create symlinks [\#686](https://github.com/kaazing/gateway/pull/686) ([mgherghe](https://github.com/mgherghe))
- Refactor turn.rest API to match feedback [\#684](https://github.com/kaazing/gateway/pull/684) ([mgherghe](https://github.com/mgherghe))
- Mask XOR-RELAY-ADDRESS and unmask XOR-PEER-ADDRESS [\#681](https://github.com/kaazing/gateway/pull/681) ([Anisotrop](https://github.com/Anisotrop))
- Removed echoing of java path and version on startup [\#680](https://github.com/kaazing/gateway/pull/680) ([mgherghe](https://github.com/mgherghe))
- add ws extensions functionality in WsnConnector [\#657](https://github.com/kaazing/gateway/pull/657) ([danibusu](https://github.com/danibusu))
- Changes for time unit conversion. [\#649](https://github.com/kaazing/gateway/pull/649) ([vmaraloiu](https://github.com/vmaraloiu))
- Add the port of the remote client to HEADER\_X\_FORWARDED\_FOR and FORWARDED\_FOR headers. Perform changes in k3po scripts for the new changes. [\#630](https://github.com/kaazing/gateway/pull/630) ([vmaraloiu](https://github.com/vmaraloiu))

## [5.2.1](https://github.com/kaazing/gateway/tree/5.2.1) (2016-08-19)
[Full Changelog](https://github.com/kaazing/gateway/compare/5.2.0...5.2.1)

**Merged pull requests:**

- Turn.proxy implementation [\#679](https://github.com/kaazing/gateway/pull/679) ([Anisotrop](https://github.com/Anisotrop))
- Implementation of turn.rest authentication service [\#674](https://github.com/kaazing/gateway/pull/674) ([DoruM](https://github.com/DoruM))
- HTTP Proxy Issue 648 [\#672](https://github.com/kaazing/gateway/pull/672) ([a-zuckut](https://github.com/a-zuckut))
- Remove copyright info from log4j xml/properties files [\#668](https://github.com/kaazing/gateway/pull/668) ([ahousing](https://github.com/ahousing))
- Fix for issue 310: WSN Connector: Connection should fail when the value of the  header in handshake response is not  \#310 [\#665](https://github.com/kaazing/gateway/pull/665) ([vmaraloiu](https://github.com/vmaraloiu))
- Help written from file [\#656](https://github.com/kaazing/gateway/pull/656) ([DoruM](https://github.com/DoruM))
- Http acceptor tests pulled from K3po [\#644](https://github.com/kaazing/gateway/pull/644) ([a-zuckut](https://github.com/a-zuckut))

## [5.2.0](https://github.com/kaazing/gateway/tree/5.2.0) (2016-08-08)
[Full Changelog](https://github.com/kaazing/gateway/compare/5.1.2...5.2.0)

**Fixed bugs:**

- Gateway not specification compliant for invalid headers [\#637](https://github.com/kaazing/gateway/issues/637)
- RFC 7232: Directory Service responds with 200 OK when If-Modified-Since matches Last-Modified from previous response [\#383](https://github.com/kaazing/gateway/issues/383)
- WSN Connector: Missing `Connection` header in handshake response should not result in a successful connection [\#311](https://github.com/kaazing/gateway/issues/311)
- WsnConnector does not reject a masked text or binary frame from the server [\#308](https://github.com/kaazing/gateway/issues/308)
- WSN Connector: Gateway sends back invalid UTF-8 reason instead of just the CLOSE code 1002 [\#307](https://github.com/kaazing/gateway/issues/307)
- WSN Connector, WSE connector: IllegalArgumentException is thrown when sending a frame with empty payload [\#306](https://github.com/kaazing/gateway/issues/306)
- WSN Transport Bug : shouldEchoBinaryFrameWithPayloadLength0 and shouldEchoTextFrameWithPayloadLength0 [\#254](https://github.com/kaazing/gateway/issues/254)

**Merged pull requests:**

- reset all changes made by the revert commit\(https://github.com/kaazin… [\#671](https://github.com/kaazing/gateway/pull/671) ([AdrianCozma](https://github.com/AdrianCozma))
- reset version to develop-SNAPSHOT, it was changed during a revert on … [\#670](https://github.com/kaazing/gateway/pull/670) ([AdrianCozma](https://github.com/AdrianCozma))
- Implemented the new \<symbolic-link\> property for the Directory Service [\#669](https://github.com/kaazing/gateway/pull/669) ([msalavastru](https://github.com/msalavastru))
- Update copyright stored in server.?pi/pom.xml [\#667](https://github.com/kaazing/gateway/pull/667) ([ahousing](https://github.com/ahousing))
- Removing http transport dependency in proxy service [\#666](https://github.com/kaazing/gateway/pull/666) ([jitsni](https://github.com/jitsni))
- Corrected namespace for management.xsd [\#664](https://github.com/kaazing/gateway/pull/664) ([DoruM](https://github.com/DoruM))
- Revert "Corrected namespace for management.xsd" [\#663](https://github.com/kaazing/gateway/pull/663) ([AdrianCozma](https://github.com/AdrianCozma))
- Corrected namespace for management.xsd [\#662](https://github.com/kaazing/gateway/pull/662) ([DoruM](https://github.com/DoruM))
- Corrected namespace for management.xsd [\#659](https://github.com/kaazing/gateway/pull/659) ([DoruM](https://github.com/DoruM))
- Disable TcpConnectorIT.shouldHandleServerClose [\#654](https://github.com/kaazing/gateway/pull/654) ([ahousing](https://github.com/ahousing))
- Added validation for empty space after HTTP header field name [\#652](https://github.com/kaazing/gateway/pull/652) ([DoruM](https://github.com/DoruM))
- Updated license for log4j-config [\#650](https://github.com/kaazing/gateway/pull/650) ([vishalsatish](https://github.com/vishalsatish))
- reject masked frames sent by the server in a websocket connection [\#647](https://github.com/kaazing/gateway/pull/647) ([danibusu](https://github.com/danibusu))
- Udp impl using netty [\#646](https://github.com/kaazing/gateway/pull/646) ([jitsni](https://github.com/jitsni))
- Add checks for invalid UTF-8 reason on Close frame [\#641](https://github.com/kaazing/gateway/pull/641) ([DoruM](https://github.com/DoruM))
- \[Work in progress\] add support for sending empty payload frame \(gateway issue \#254\) [\#640](https://github.com/kaazing/gateway/pull/640) ([danibusu](https://github.com/danibusu))

## [5.1.2](https://github.com/kaazing/gateway/tree/5.1.2) (2016-07-20)
[Full Changelog](https://github.com/kaazing/gateway/compare/5.1.1...5.1.2)

**Fixed bugs:**

- AcceptUriComparedToBalanceUriVisitor is comparing URIs prior to parameters being replaced, causing NPE that throws Parse Exception [\#612](https://github.com/kaazing/gateway/issues/612)
- WSN Connector: Invalid value for the `Upgrade` header in handshake response must not result in a successful connection [\#312](https://github.com/kaazing/gateway/issues/312)
- WsebAcceptorTest.shouldBindAWsAddress timeouts [\#287](https://github.com/kaazing/gateway/issues/287)

**Merged pull requests:**

- Revert "Pokemon exceptions" [\#645](https://github.com/kaazing/gateway/pull/645) ([dpwspoon](https://github.com/dpwspoon))
- Un-suspended successful test case [\#642](https://github.com/kaazing/gateway/pull/642) ([DoruM](https://github.com/DoruM))
- Checking if 'Connection' header is present when Gateway connects as a client [\#635](https://github.com/kaazing/gateway/pull/635) ([DoruM](https://github.com/DoruM))
- Remove realm with no authorization constraint / login module passes [\#634](https://github.com/kaazing/gateway/pull/634) ([justinma246](https://github.com/justinma246))
- RFC 7232: Directory Service responds with 200 OK when If-Modified-Since [\#633](https://github.com/kaazing/gateway/pull/633) ([DoruM](https://github.com/DoruM))
- Revert "Login Module fix" [\#632](https://github.com/kaazing/gateway/pull/632) ([dpwspoon](https://github.com/dpwspoon))
- Pokemon exceptions [\#631](https://github.com/kaazing/gateway/pull/631) ([DoruM](https://github.com/DoruM))
- Remove overriding values in child poms for failsafe plugin [\#629](https://github.com/kaazing/gateway/pull/629) ([justinma246](https://github.com/justinma246))
- Login Module fix [\#628](https://github.com/kaazing/gateway/pull/628) ([justinma246](https://github.com/justinma246))
- Revert "Login Module passes when there is a realm and login module" [\#627](https://github.com/kaazing/gateway/pull/627) ([jitsni](https://github.com/jitsni))
- PR for issue \#615: Remove all but hostname must match gateway-config.xml balancer check [\#623](https://github.com/kaazing/gateway/pull/623) ([msalavastru](https://github.com/msalavastru))
- Adding get/setInternalInterestOps\(\) methods to AbstractNioChannel [\#621](https://github.com/kaazing/gateway/pull/621) ([jitsni](https://github.com/jitsni))
- Checking for correct java version in scripts [\#620](https://github.com/kaazing/gateway/pull/620) ([justinma246](https://github.com/justinma246))
- SNMP and sessionServiceType no longer supported [\#618](https://github.com/kaazing/gateway/pull/618) ([justinma246](https://github.com/justinma246))
- Http connector follow redirect for maximum.redirects, also http acceptor send 302 for wsn and wsx [\#611](https://github.com/kaazing/gateway/pull/611) ([dpwspoon](https://github.com/dpwspoon))
- Login Module passes when there is a realm and login module [\#604](https://github.com/kaazing/gateway/pull/604) ([justinma246](https://github.com/justinma246))

## [5.1.1](https://github.com/kaazing/gateway/tree/5.1.1) (2016-06-27)
[Full Changelog](https://github.com/kaazing/gateway/compare/5.1.0...5.1.1)

**Fixed bugs:**

- HttpDirectoryServiceIT.testUriWithParams fails with max-age mismatch [\#468](https://github.com/kaazing/gateway/issues/468)
- JMX Session count is not correct on Echo Service [\#331](https://github.com/kaazing/gateway/issues/331)
- Sporadic failure: Hangs on "transport.wsn.WsnAcceptorTest" [\#197](https://github.com/kaazing/gateway/issues/197)
- Multiple Log4J in distribution  [\#80](https://github.com/kaazing/gateway/issues/80)
- Sslv3Test\#connectSucceedsWithoutSslv3 fails intermittently [\#71](https://github.com/kaazing/gateway/issues/71)
- transport.wsn DuplicateBindTest: test timed out [\#58](https://github.com/kaazing/gateway/issues/58)

**Closed issues:**

- Connect directly to ws service [\#590](https://github.com/kaazing/gateway/issues/590)
- Java 7 throws unfriendly error [\#351](https://github.com/kaazing/gateway/issues/351)
- Command center page throws an error when you open Configuration/Overview in Command center [\#320](https://github.com/kaazing/gateway/issues/320)
- change "KAAZING" to "Kaazing" in all 5.0 docs [\#298](https://github.com/kaazing/gateway/issues/298)
- log4j clean up [\#277](https://github.com/kaazing/gateway/issues/277)
- WsebConnector doesn't send next protocol [\#266](https://github.com/kaazing/gateway/issues/266)
- EE label in doc should have hint describing what it is [\#215](https://github.com/kaazing/gateway/issues/215)
- Document using /dev/random vs /dev/urandom in production environments [\#170](https://github.com/kaazing/gateway/issues/170)

**Merged pull requests:**

- updated sonar user and password [\#609](https://github.com/kaazing/gateway/pull/609) ([AdrianCozma](https://github.com/AdrianCozma))
- Fix for issue \#537: WsnAcceptorUserLoggingIT sporadic failures on missing close exception [\#608](https://github.com/kaazing/gateway/pull/608) ([msalavastru](https://github.com/msalavastru))
- Added support for deeply nested properties to GatewayConfiguration for testing purposes [\#607](https://github.com/kaazing/gateway/pull/607) ([sanjay-saxena](https://github.com/sanjay-saxena))
- Fix for issue \#518: Http proxy is adding its own unauthorized content to http response that are unauthorized [\#606](https://github.com/kaazing/gateway/pull/606) ([msalavastru](https://github.com/msalavastru))
- Updated mina.netty version to resolve security scan \(note: we don't a… [\#603](https://github.com/kaazing/gateway/pull/603) ([dpwspoon](https://github.com/dpwspoon))
- Early access features in Gateway [\#600](https://github.com/kaazing/gateway/pull/600) ([cmebarrow](https://github.com/cmebarrow))
- Fix for issue 558 Directory service property directory must start with a / \#558 [\#599](https://github.com/kaazing/gateway/pull/599) ([msalavastru](https://github.com/msalavastru))
- Fix for issue 80:  Multiple Log4J in distribution [\#598](https://github.com/kaazing/gateway/pull/598) ([msalavastru](https://github.com/msalavastru))
- ws.inactivity.timeout populates http.keealive.timeout [\#597](https://github.com/kaazing/gateway/pull/597) ([jitsni](https://github.com/jitsni))
- Disable WsnAcceptorUserLoggingIT.verifyPrincipalNameLoggedInLayersAbo… [\#596](https://github.com/kaazing/gateway/pull/596) ([ahousing](https://github.com/ahousing))
- Updating slf4j to 1.7.21 version [\#593](https://github.com/kaazing/gateway/pull/593) ([jitsni](https://github.com/jitsni))
- No need to override hashCode\(\) in OtherSslCipher [\#592](https://github.com/kaazing/gateway/pull/592) ([jitsni](https://github.com/jitsni))
- Disable TcpConnectorIT.shouldEstablishConnection [\#591](https://github.com/kaazing/gateway/pull/591) ([ahousing](https://github.com/ahousing))
- fixed travis file bug that wasn't building all branches [\#588](https://github.com/kaazing/gateway/pull/588) ([AdrianCozma](https://github.com/AdrianCozma))
- Adding TLSv1.2 ciphers that are enabled by java 8 sun provider  [\#587](https://github.com/kaazing/gateway/pull/587) ([jitsni](https://github.com/jitsni))
- changed the position of the sonar command to catch failures [\#584](https://github.com/kaazing/gateway/pull/584) ([AdrianCozma](https://github.com/AdrianCozma))
- Cleanup [\#583](https://github.com/kaazing/gateway/pull/583) ([jitsni](https://github.com/jitsni))
- Update pom.xml [\#582](https://github.com/kaazing/gateway/pull/582) ([dpwspoon](https://github.com/dpwspoon))
- Cleanup [\#581](https://github.com/kaazing/gateway/pull/581) ([jitsni](https://github.com/jitsni))
- Cleanup [\#579](https://github.com/kaazing/gateway/pull/579) ([jitsni](https://github.com/jitsni))
- Cleanup [\#578](https://github.com/kaazing/gateway/pull/578) ([jitsni](https://github.com/jitsni))
- WsPingMessage's empty buffer needs to be shared buffer [\#577](https://github.com/kaazing/gateway/pull/577) ([jitsni](https://github.com/jitsni))
- Made authType.getSessionTimeout check for instance of != null as I wa… [\#569](https://github.com/kaazing/gateway/pull/569) ([dpwspoon](https://github.com/dpwspoon))
- Added test file that checks the server's pom.xml has consistent manif… [\#564](https://github.com/kaazing/gateway/pull/564) ([Anisotrop](https://github.com/Anisotrop))

## [5.1.0](https://github.com/kaazing/gateway/tree/5.1.0) (2016-05-17)
[Full Changelog](https://github.com/kaazing/gateway/compare/5.0.1.55...5.1.0)

**Closed issues:**

- Extension filter is not removed when WSEB's TransportSession is closed [\#555](https://github.com/kaazing/gateway/issues/555)

**Merged pull requests:**

- Catchup release [\#576](https://github.com/kaazing/gateway/pull/576) ([dpwspoon](https://github.com/dpwspoon))
- Adding a better log message when WsCheckAliveFilter closes ws session [\#573](https://github.com/kaazing/gateway/pull/573) ([jitsni](https://github.com/jitsni))
- Fix for issue \#385 [\#571](https://github.com/kaazing/gateway/pull/571) ([msalavastru](https://github.com/msalavastru))
- Removing IdleTimeoutFilter in wseb  [\#570](https://github.com/kaazing/gateway/pull/570) ([jitsni](https://github.com/jitsni))
- Synchronizing the state during acceptor/connector initialization [\#567](https://github.com/kaazing/gateway/pull/567) ([jitsni](https://github.com/jitsni))
- Resolved cipher suites if not already a String\[\] in SslResourceAddres… [\#566](https://github.com/kaazing/gateway/pull/566) ([dpwspoon](https://github.com/dpwspoon))
- Update Stack Overflow link to use the Kaazing tag [\#565](https://github.com/kaazing/gateway/pull/565) ([robinzimmermann](https://github.com/robinzimmermann))
- ignored sse test in general\(not just Jenkins\) [\#563](https://github.com/kaazing/gateway/pull/563) ([AdrianCozma](https://github.com/AdrianCozma))
- ignored sseIe8HttpxeConnectAndGetData test for Jenkins builds [\#562](https://github.com/kaazing/gateway/pull/562) ([AdrianCozma](https://github.com/AdrianCozma))
- Ignored DatagramPortUnreachableTest [\#561](https://github.com/kaazing/gateway/pull/561) ([AdrianCozma](https://github.com/AdrianCozma))
- Catch up merge from release/5.1.0 branch [\#560](https://github.com/kaazing/gateway/pull/560) ([jitsni](https://github.com/jitsni))
- Fix webservice url for community/enterprise. [\#559](https://github.com/kaazing/gateway/pull/559) ([Anisotrop](https://github.com/Anisotrop))
- redirected failsafe reports to surefire directory as a workaround to … [\#557](https://github.com/kaazing/gateway/pull/557) ([AdrianCozma](https://github.com/AdrianCozma))
- WSEB Extension filter's IoFilter\#onPostRemove\(\) is not invoked [\#556](https://github.com/kaazing/gateway/pull/556) ([jitsni](https://github.com/jitsni))
- Making sure that next protocol is not null \(let the binding deal with… [\#554](https://github.com/kaazing/gateway/pull/554) ([jitsni](https://github.com/jitsni))
- Fix for sporadic failure of HttpDirectoryServiceIT.testPostLargeData [\#553](https://github.com/kaazing/gateway/pull/553) ([mgherghe](https://github.com/mgherghe))
- Removing the logic to force ciphers for applets [\#552](https://github.com/kaazing/gateway/pull/552) ([jitsni](https://github.com/jitsni))
- Fixed HttpBalancerService to add bindings properly [\#551](https://github.com/kaazing/gateway/pull/551) ([sanjay-saxena](https://github.com/sanjay-saxena))
- Thread unsafe processing in ProtocolCodecFilter during wseb realignment [\#548](https://github.com/kaazing/gateway/pull/548) ([jitsni](https://github.com/jitsni))
- Ignored test JmxSessionPrincipalIT. [\#546](https://github.com/kaazing/gateway/pull/546) ([NicoletaOita](https://github.com/NicoletaOita))
- Fix OpeningHandshakeIT [\#545](https://github.com/kaazing/gateway/pull/545) ([Anisotrop](https://github.com/Anisotrop))
- Fix OpeningHandshakeIT [\#544](https://github.com/kaazing/gateway/pull/544) ([Anisotrop](https://github.com/Anisotrop))
- Ignored test as suggested in ticket 434 [\#543](https://github.com/kaazing/gateway/pull/543) ([vstratan](https://github.com/vstratan))
- Develop.catchup [\#541](https://github.com/kaazing/gateway/pull/541) ([dpwspoon](https://github.com/dpwspoon))
- Updated k3po version [\#540](https://github.com/kaazing/gateway/pull/540) ([dpwspoon](https://github.com/dpwspoon))
- Release/5.1.0 [\#539](https://github.com/kaazing/gateway/pull/539) ([robinzimmermann](https://github.com/robinzimmermann))
- Applied filtering to windows edition [\#538](https://github.com/kaazing/gateway/pull/538) ([dpwspoon](https://github.com/dpwspoon))
- Add filtered version to index.html [\#537](https://github.com/kaazing/gateway/pull/537) ([dpwspoon](https://github.com/dpwspoon))
- Updated k3po version [\#536](https://github.com/kaazing/gateway/pull/536) ([dpwspoon](https://github.com/dpwspoon))
- Ping/Pongs are not encoded in wsx [\#535](https://github.com/kaazing/gateway/pull/535) ([jitsni](https://github.com/jitsni))
- Investigate.sporadic.failures.3 [\#534](https://github.com/kaazing/gateway/pull/534) ([dpwspoon](https://github.com/dpwspoon))
- Selectively ignore certain tests on Linux [\#533](https://github.com/kaazing/gateway/pull/533) ([sanjay-saxena](https://github.com/sanjay-saxena))
- updated travis.yml to make sonar analyses on master and release [\#532](https://github.com/kaazing/gateway/pull/532) ([AdrianCozma](https://github.com/AdrianCozma))
- Add location paramter to websocket.org link [\#531](https://github.com/kaazing/gateway/pull/531) ([robinzimmermann](https://github.com/robinzimmermann))
- Replace support link with Github issues link. Add panel headers [\#530](https://github.com/kaazing/gateway/pull/530) ([robinzimmermann](https://github.com/robinzimmermann))
- Changed part of script to be http and not tcp [\#529](https://github.com/kaazing/gateway/pull/529) ([dpwspoon](https://github.com/dpwspoon))
- Updated to K3po 3.0.0.-alpha-29. Fixed two scripts to address fragmen… [\#528](https://github.com/kaazing/gateway/pull/528) ([sanjay-saxena](https://github.com/sanjay-saxena))
- Removed dynamically generated classes from sonar qube results.  This … [\#527](https://github.com/kaazing/gateway/pull/527) ([dpwspoon](https://github.com/dpwspoon))
- Removed static from urlCacheControlMap and made it thread safe [\#526](https://github.com/kaazing/gateway/pull/526) ([mgherghe](https://github.com/mgherghe))
- Changed gateway-config namespace and added default Cache-control: max-age=0 header [\#525](https://github.com/kaazing/gateway/pull/525) ([mgherghe](https://github.com/mgherghe))
- Adding a test for legacy wsn draft handshake [\#524](https://github.com/kaazing/gateway/pull/524) ([jitsni](https://github.com/jitsni))
- Investigate.sporadic.failures [\#523](https://github.com/kaazing/gateway/pull/523) ([dpwspoon](https://github.com/dpwspoon))
- Revert "Removing draft resource address components from distribution" [\#522](https://github.com/kaazing/gateway/pull/522) ([dpwspoon](https://github.com/dpwspoon))
- moved jacoco\(used for sonar test coverage\) to separate profile and up… [\#519](https://github.com/kaazing/gateway/pull/519) ([AdrianCozma](https://github.com/AdrianCozma))
- added certificate to travis and enabled management tests for travis [\#518](https://github.com/kaazing/gateway/pull/518) ([AdrianCozma](https://github.com/AdrianCozma))
- OpeningHandshakeIT.shouldEstablishConnectionWithRequestHeaderSecWebSo… [\#517](https://github.com/kaazing/gateway/pull/517) ([sanjay-saxena](https://github.com/sanjay-saxena))
- Fixed issue that could cause sporadic failures [\#516](https://github.com/kaazing/gateway/pull/516) ([dpwspoon](https://github.com/dpwspoon))
- Fixed condition where the write "something" was failing in the script… [\#515](https://github.com/kaazing/gateway/pull/515) ([dpwspoon](https://github.com/dpwspoon))
- Fixed issue where fragmenentation on wire or k3po pipeline could caus… [\#514](https://github.com/kaazing/gateway/pull/514) ([dpwspoon](https://github.com/dpwspoon))
- Add Mailchimp signup [\#513](https://github.com/kaazing/gateway/pull/513) ([robinzimmermann](https://github.com/robinzimmermann))
- Improving out of order log message [\#512](https://github.com/kaazing/gateway/pull/512) ([jitsni](https://github.com/jitsni))
- Http caching - k3po tests  [\#511](https://github.com/kaazing/gateway/pull/511) ([vstratan](https://github.com/vstratan))
- Revert of Sec-WebSocket-Protocol enforcement in connector [\#510](https://github.com/kaazing/gateway/pull/510) ([dpwspoon](https://github.com/dpwspoon))
- Revert "Validation of Sec-WebSocket-Protocol on the connector side" [\#509](https://github.com/kaazing/gateway/pull/509) ([dpwspoon](https://github.com/dpwspoon))
- Updated the timeout to 30seconds to make the ClusterBalancerServiceIT… [\#507](https://github.com/kaazing/gateway/pull/507) ([sanjay-saxena](https://github.com/sanjay-saxena))
- Removed.pointless.tests [\#506](https://github.com/kaazing/gateway/pull/506) ([dpwspoon](https://github.com/dpwspoon))
- "Added Test cases for logging identity" [\#505](https://github.com/kaazing/gateway/pull/505) ([vstratan](https://github.com/vstratan))
- ignore failures in travis until travis.yml is setup properly [\#504](https://github.com/kaazing/gateway/pull/504) ([dpwspoon](https://github.com/dpwspoon))
- Reverting the change where ssl tests are run in forked mode. [\#503](https://github.com/kaazing/gateway/pull/503) ([jitsni](https://github.com/jitsni))
- Adding a log message when binding is not found and 404 is sent [\#502](https://github.com/kaazing/gateway/pull/502) ([jitsni](https://github.com/jitsni))
- Added on.client.message property to broadcast service, which when set… [\#501](https://github.com/kaazing/gateway/pull/501) ([dpwspoon](https://github.com/dpwspoon))
- Changed headers last year to 2016 [\#499](https://github.com/kaazing/gateway/pull/499) ([vstratan](https://github.com/vstratan))
- Adding a log message when binding is not found and 404 is sent [\#498](https://github.com/kaazing/gateway/pull/498) ([jitsni](https://github.com/jitsni))
- Don't use IoBuffer as it uses AbstractIoBufferEx's IoBufferAllocator [\#497](https://github.com/kaazing/gateway/pull/497) ([jitsni](https://github.com/jitsni))
- Update copyright to 2016, part 2 [\#496](https://github.com/kaazing/gateway/pull/496) ([ahousing](https://github.com/ahousing))
- Be explicit about groupID in pom to workaround plugin bug [\#495](https://github.com/kaazing/gateway/pull/495) ([ahousing](https://github.com/ahousing))
- Update copyright to 2016, update parent in pom.xml [\#494](https://github.com/kaazing/gateway/pull/494) ([ahousing](https://github.com/ahousing))
- Added Test cases for logging identity  [\#450](https://github.com/kaazing/gateway/pull/450) ([vstratan](https://github.com/vstratan))

## [5.0.1.55](https://github.com/kaazing/gateway/tree/5.0.1.55) (2016-04-05)
[Full Changelog](https://github.com/kaazing/gateway/compare/5.0.1.54...5.0.1.55)

**Fixed bugs:**

- WSN Connector: Connection should fail when the negotiated protocol is not in the list of supported protocols [\#315](https://github.com/kaazing/gateway/issues/315)

**Merged pull requests:**

- Add management tests [\#493](https://github.com/kaazing/gateway/pull/493) ([dpwspoon](https://github.com/dpwspoon))
- http.transport accept and connect option [\#492](https://github.com/kaazing/gateway/pull/492) ([jitsni](https://github.com/jitsni))
- Adding a log message for next protocol before finding the bridge filters [\#491](https://github.com/kaazing/gateway/pull/491) ([jitsni](https://github.com/jitsni))
- Validation of Sec-WebSocket-Protocol on the connector side [\#479](https://github.com/kaazing/gateway/pull/479) ([jitsni](https://github.com/jitsni))
- added jacoco\(code coverage\) plugin to all modules and updated travis … [\#464](https://github.com/kaazing/gateway/pull/464) ([AdrianCozma](https://github.com/AdrianCozma))

## [5.0.1.54](https://github.com/kaazing/gateway/tree/5.0.1.54) (2016-04-04)
[Full Changelog](https://github.com/kaazing/gateway/compare/5.0.1.53...5.0.1.54)

**Merged pull requests:**

- Ilya amqp open handshake clean [\#488](https://github.com/kaazing/gateway/pull/488) ([cmebarrow](https://github.com/cmebarrow))

## [5.0.1.53](https://github.com/kaazing/gateway/tree/5.0.1.53) (2016-04-04)
[Full Changelog](https://github.com/kaazing/gateway/compare/5.0.1.52...5.0.1.53)

**Fixed bugs:**

- WSN Connector: Writing a BINARY\(0x02\) frame results in a TEXT\(0x01\) frame to be written out [\#316](https://github.com/kaazing/gateway/issues/316)
- WSN Connector: Missing `Upgrade` header in handshake response must not result in a successful connection [\#313](https://github.com/kaazing/gateway/issues/313)

**Merged pull requests:**

- Fix for issue \#363 Trailing slash mismatch in \<accept\> & \<connect\> exception message [\#490](https://github.com/kaazing/gateway/pull/490) ([msalavastru](https://github.com/msalavastru))
- Update shipping config and welcome page [\#489](https://github.com/kaazing/gateway/pull/489) ([robinzimmermann](https://github.com/robinzimmermann))
- Removing draft resource address components from distribution [\#487](https://github.com/kaazing/gateway/pull/487) ([jitsni](https://github.com/jitsni))
- Added connect options to broadcast service [\#486](https://github.com/kaazing/gateway/pull/486) ([dpwspoon](https://github.com/dpwspoon))
- Improved regexp to require brackets for network interface containing spaces [\#485](https://github.com/kaazing/gateway/pull/485) ([Anisotrop](https://github.com/Anisotrop))
- Removed behavior to always send "Cache-control: max-age=0" header  [\#484](https://github.com/kaazing/gateway/pull/484) ([mgherghe](https://github.com/mgherghe))
- Fix for HttpDirectoryServiceIT failure [\#483](https://github.com/kaazing/gateway/pull/483) ([mgherghe](https://github.com/mgherghe))
- Failing the connect future when Upgrade: websocket header is not found [\#481](https://github.com/kaazing/gateway/pull/481) ([jitsni](https://github.com/jitsni))
- WsnConnector doesn't send text frame by default. [\#480](https://github.com/kaazing/gateway/pull/480) ([jitsni](https://github.com/jitsni))
- Adding a log message when a session is closed due to Connection: close [\#478](https://github.com/kaazing/gateway/pull/478) ([jitsni](https://github.com/jitsni))
- Transport http methods clean [\#475](https://github.com/kaazing/gateway/pull/475) ([ilyaanisimov-kaazing](https://github.com/ilyaanisimov-kaazing))
-  Trace messages in ServiceConnectManager [\#471](https://github.com/kaazing/gateway/pull/471) ([jitsni](https://github.com/jitsni))

## [5.0.1.52](https://github.com/kaazing/gateway/tree/5.0.1.52) (2016-03-28)
[Full Changelog](https://github.com/kaazing/gateway/compare/5.0.1.51...5.0.1.52)

**Closed issues:**

- Gateway build failing in ssl transport: VirtualHostKeySelectorTest when etc/hosts has the entries required by the test [\#457](https://github.com/kaazing/gateway/issues/457)
- Sporadic NPE from WsnAcceptor while closing the session [\#347](https://github.com/kaazing/gateway/issues/347)

**Merged pull requests:**

- Avoid possible NPE in WsnAcceptor.ioBridgeHandler.sessionClosed [\#470](https://github.com/kaazing/gateway/pull/470) ([cmebarrow](https://github.com/cmebarrow))
- Issue \#463: in transport/wsn, added suspendRead in WsCloseFilter.filt… [\#466](https://github.com/kaazing/gateway/pull/466) ([cmebarrow](https://github.com/cmebarrow))
- Added changes for having the network interface syntax regexp exposed … [\#465](https://github.com/kaazing/gateway/pull/465) ([irina-mitrea-luxoft](https://github.com/irina-mitrea-luxoft))
- Anisotrop fix ipv6 accept options [\#455](https://github.com/kaazing/gateway/pull/455) ([Anisotrop](https://github.com/Anisotrop))
- Forward feature \#560 from 4.x to 5.x: Implement the directives for Cache-Control header in Directory Service [\#451](https://github.com/kaazing/gateway/pull/451) ([mgherghe](https://github.com/mgherghe))

## [5.0.1.51](https://github.com/kaazing/gateway/tree/5.0.1.51) (2016-03-22)
[Full Changelog](https://github.com/kaazing/gateway/compare/5.0.1.50...5.0.1.51)

**Merged pull requests:**

- Removed double creation of TcpExtensionFactory [\#462](https://github.com/kaazing/gateway/pull/462) ([dpwspoon](https://github.com/dpwspoon))
- Use project version as double layer of indirection would cause update… [\#454](https://github.com/kaazing/gateway/pull/454) ([dpwspoon](https://github.com/dpwspoon))
- Added test for web socket compliance \(maximum message size and maxmimum lifetime\). [\#447](https://github.com/kaazing/gateway/pull/447) ([NicoletaOita](https://github.com/NicoletaOita))

## [5.0.1.50](https://github.com/kaazing/gateway/tree/5.0.1.50) (2016-03-17)
[Full Changelog](https://github.com/kaazing/gateway/compare/5.0.1.49...5.0.1.50)

**Merged pull requests:**

- nit fixes + Added initingGateway method in GatewayObserverApi [\#460](https://github.com/kaazing/gateway/pull/460) ([dpwspoon](https://github.com/dpwspoon))
- VirtualHostKeySelectorTest: Accomodate change of transport option from type URI to String [\#458](https://github.com/kaazing/gateway/pull/458) ([cmebarrow](https://github.com/cmebarrow))
- Added initingGateway method in GatewayObserverApi [\#456](https://github.com/kaazing/gateway/pull/456) ([mgherghe](https://github.com/mgherghe))
- Enable the ignored test in WsxAcceptorLoggingIT. [\#453](https://github.com/kaazing/gateway/pull/453) ([cmebarrow](https://github.com/cmebarrow))
- Add transport.tcp logger to the OOTB log4j config.xml file [\#449](https://github.com/kaazing/gateway/pull/449) ([cmebarrow](https://github.com/cmebarrow))
- Gateway returns incorrect error code when payload is too long \(1002 instead of 1009\) [\#445](https://github.com/kaazing/gateway/pull/445) ([Anisotrop](https://github.com/Anisotrop))

## [5.0.1.49](https://github.com/kaazing/gateway/tree/5.0.1.49) (2016-03-16)
[Full Changelog](https://github.com/kaazing/gateway/compare/5.0.1.48...5.0.1.49)

**Closed issues:**

- Build failure in HttpPipeliningIT.twoRequestsBeforeReponseOK [\#430](https://github.com/kaazing/gateway/issues/430)

**Merged pull requests:**

- Fixing NPE when an exception is caught but wsn session is not yet created [\#452](https://github.com/kaazing/gateway/pull/452) ([jitsni](https://github.com/jitsni))
- Fixed pipe error message. [\#448](https://github.com/kaazing/gateway/pull/448) ([vmaraloiu](https://github.com/vmaraloiu))
- fixes issue where k3po regex might match on tcp packet fragmentation [\#446](https://github.com/kaazing/gateway/pull/446) ([dpwspoon](https://github.com/dpwspoon))
- Connection closed when http.proxy loops encountered [\#443](https://github.com/kaazing/gateway/pull/443) ([irina-mitrea-luxoft](https://github.com/irina-mitrea-luxoft))
- Support proxying to application servers that generate dynamic resource addresses [\#415](https://github.com/kaazing/gateway/pull/415) ([msalavastru](https://github.com/msalavastru))

## [5.0.1.48](https://github.com/kaazing/gateway/tree/5.0.1.48) (2016-03-11)
[Full Changelog](https://github.com/kaazing/gateway/compare/5.0.1.47...5.0.1.48)

**Fixed bugs:**

- Sporadic Travis build failure in random tests due to hang during Gateway shutdown [\#292](https://github.com/kaazing/gateway/issues/292)

**Merged pull requests:**

- Fix ipv6 accept options [\#442](https://github.com/kaazing/gateway/pull/442) ([Anisotrop](https://github.com/Anisotrop))
- Delete accidentally added file transport/wseb/dumpfile.pcap. [\#441](https://github.com/kaazing/gateway/pull/441) ([cmebarrow](https://github.com/cmebarrow))
- Added gatewayContext to startingGateway and stoppingGateway methods from GatewayObserverApi [\#440](https://github.com/kaazing/gateway/pull/440) ([mgherghe](https://github.com/mgherghe))
- Pull request holding URI to String and network interface support changes [\#437](https://github.com/kaazing/gateway/pull/437) ([irina-mitrea-luxoft](https://github.com/irina-mitrea-luxoft))
- Wseb session thread migration [\#392](https://github.com/kaazing/gateway/pull/392) ([jitsni](https://github.com/jitsni))

## [5.0.1.47](https://github.com/kaazing/gateway/tree/5.0.1.47) (2016-03-08)
[Full Changelog](https://github.com/kaazing/gateway/compare/5.0.1.46...5.0.1.47)

**Closed issues:**

- Exception sometimes occurs using emulated WebSocket: RuntimeException expected current thread ... to match ... [\#427](https://github.com/kaazing/gateway/issues/427)
- Gateway Travis build failed in wseb transport, 3 test failures [\#424](https://github.com/kaazing/gateway/issues/424)
- Recent gateway builds are failing on Travis in wsn transport, "Killed" [\#423](https://github.com/kaazing/gateway/issues/423)

**Merged pull requests:**

- Proxy service fails the connect future when the corresponding accept is closed [\#439](https://github.com/kaazing/gateway/pull/439) ([jitsni](https://github.com/jitsni))
- Fix line endings on DuplicateBindTest.java [\#436](https://github.com/kaazing/gateway/pull/436) ([Anisotrop](https://github.com/Anisotrop))
- Minor correction of the IllegalArgumentException message throwen from checkForTralingSlashes\(\) [\#435](https://github.com/kaazing/gateway/pull/435) ([ilyaanisimov-kaazing](https://github.com/ilyaanisimov-kaazing))
- Duplicate bind test fix [\#434](https://github.com/kaazing/gateway/pull/434) ([Anisotrop](https://github.com/Anisotrop))
- Adding code to rewrite cookie domain, cookie path, location headers [\#432](https://github.com/kaazing/gateway/pull/432) ([jitsni](https://github.com/jitsni))
- Throw error on specific circumstances when PreferedIPv4 flag is true and the host IP is IPV6 [\#431](https://github.com/kaazing/gateway/pull/431) ([vmaraloiu](https://github.com/vmaraloiu))
- Issue 427: thread alignment violation in wse due to race with client close [\#428](https://github.com/kaazing/gateway/pull/428) ([cmebarrow](https://github.com/cmebarrow))
- Changes to resolve build failures in wseb transport [\#426](https://github.com/kaazing/gateway/pull/426) ([cmebarrow](https://github.com/cmebarrow))
- Added hazelcast-client and hazelcast-cloud as dependencies [\#422](https://github.com/kaazing/gateway/pull/422) ([irina-mitrea-luxoft](https://github.com/irina-mitrea-luxoft))
- HTTPX: Tests for Extended Handshake  [\#417](https://github.com/kaazing/gateway/pull/417) ([sanjay-saxena](https://github.com/sanjay-saxena))
- Httpxe.spec.tests [\#413](https://github.com/kaazing/gateway/pull/413) ([jitsni](https://github.com/jitsni))

## [5.0.1.46](https://github.com/kaazing/gateway/tree/5.0.1.46) (2016-02-13)
[Full Changelog](https://github.com/kaazing/gateway/compare/5.0.1.45...5.0.1.46)

**Fixed bugs:**

- wse transport behavior when it receives an invalid WSE control frame on the input stream is incorrect [\#390](https://github.com/kaazing/gateway/issues/390)
- WSE connector \(WsebConnector\) goes into infinite loop if a protocol error is detected on the reader [\#387](https://github.com/kaazing/gateway/issues/387)
- Remove Build Warnings [\#375](https://github.com/kaazing/gateway/issues/375)
- Wse Acceptor: should not connect if method is not Post [\#327](https://github.com/kaazing/gateway/issues/327)
- Wse Acceptor: should not connect if "X-WebSocket\_Version" is missing or has wrong values [\#326](https://github.com/kaazing/gateway/issues/326)
- Wse Acceptor: Missing HTTP header: X-WebSocket-Protocol in response [\#325](https://github.com/kaazing/gateway/issues/325)
- Wse Acceptor: Missing HTTP header: X-WebSocket-Extensions in respose [\#324](https://github.com/kaazing/gateway/issues/324)
- Wse Acceptor: should not connect if "X-Sequence-Number" is missing or has wrong values [\#323](https://github.com/kaazing/gateway/issues/323)
- Wse Acceptor: should not connect if "X-Accept-Commands" has wrong values [\#322](https://github.com/kaazing/gateway/issues/322)

**Closed issues:**

- wse transport sometimes fails with "Attempt to read into suspended session that already has a current read request" [\#421](https://github.com/kaazing/gateway/issues/421)
- http transport is not parsing certain comma separated http headers correctly \(ones used for wseb\) [\#410](https://github.com/kaazing/gateway/issues/410)
- WSE connector \(WsebConnector\) does not complete the upstream HTTP request when closing  [\#408](https://github.com/kaazing/gateway/issues/408)
- WSE connector \(WsebConnector\) does not do close handshake [\#345](https://github.com/kaazing/gateway/issues/345)

**Merged pull requests:**

- Fixed org.kaazing.gateway.server.test.Gateway testware class not to swallow exceptions [\#420](https://github.com/kaazing/gateway/pull/420) ([cmebarrow](https://github.com/cmebarrow))
- Wse spec tests and changes for wse spec compliance [\#418](https://github.com/kaazing/gateway/pull/418) ([cmebarrow](https://github.com/cmebarrow))
- Fixed issue regarding IndexOutOfBounds when no addresses available [\#414](https://github.com/kaazing/gateway/pull/414) ([vmaraloiu](https://github.com/vmaraloiu))
- Issue.183 added 2 new scenarios for validating accept and connect trailing slashes [\#412](https://github.com/kaazing/gateway/pull/412) ([msalavastru](https://github.com/msalavastru))
- Forward Port issue266  from 4.0 to  CE/EE [\#407](https://github.com/kaazing/gateway/pull/407) ([mjolie](https://github.com/mjolie))

## [5.0.1.45](https://github.com/kaazing/gateway/tree/5.0.1.45) (2016-01-28)
[Full Changelog](https://github.com/kaazing/gateway/compare/5.0.1.44...5.0.1.45)

**Merged pull requests:**

- \(\#410\) Fixed HttpHeaderDecodingState so it will treat http request he… [\#411](https://github.com/kaazing/gateway/pull/411) ([cmebarrow](https://github.com/cmebarrow))
- A minor fix for issue 183 [\#405](https://github.com/kaazing/gateway/pull/405) ([msalavastru](https://github.com/msalavastru))
- Not firing session idle after the session is closed [\#403](https://github.com/kaazing/gateway/pull/403) ([jitsni](https://github.com/jitsni))

## [5.0.1.44](https://github.com/kaazing/gateway/tree/5.0.1.44) (2016-01-25)
[Full Changelog](https://github.com/kaazing/gateway/compare/5.0.1.43...5.0.1.44)

**Closed issues:**

- WsebConnector should set Content-Type header on the upstream request [\#136](https://github.com/kaazing/gateway/issues/136)

**Merged pull requests:**

- Added input area for choosing which URL to connect to [\#406](https://github.com/kaazing/gateway/pull/406) ([mgherghe](https://github.com/mgherghe))
- Modified xsd file in order to support exactly one \<connect\> for http.proxy [\#404](https://github.com/kaazing/gateway/pull/404) ([mgherghe](https://github.com/mgherghe))
- Adding timeout to WsnAcceptorTest [\#402](https://github.com/kaazing/gateway/pull/402) ([jitsni](https://github.com/jitsni))
- Decoding the “Set-cookie” response header bug fix [\#400](https://github.com/kaazing/gateway/pull/400) ([msalavastru](https://github.com/msalavastru))
- Added NioSystemProperty that will kill session if idle at tcp level  [\#397](https://github.com/kaazing/gateway/pull/397) ([dpwspoon](https://github.com/dpwspoon))

## [5.0.1.43](https://github.com/kaazing/gateway/tree/5.0.1.43) (2016-01-18)
[Full Changelog](https://github.com/kaazing/gateway/compare/5.0.1.42...5.0.1.43)

**Fixed bugs:**

- gateway.bridge.jar does not contain both JS and Java bridge [\#337](https://github.com/kaazing/gateway/issues/337)

**Closed issues:**

- HTTP response without Content-Length or chunked encoding causes parsing loop [\#146](https://github.com/kaazing/gateway/issues/146)

**Merged pull requests:**

- Modified xsd file in order to support only one \<connect\> for http.proxy [\#401](https://github.com/kaazing/gateway/pull/401) ([mgherghe](https://github.com/mgherghe))
- clarified logging [\#398](https://github.com/kaazing/gateway/pull/398) ([dpwspoon](https://github.com/dpwspoon))
- Issue 384: provide INFO level diagnostics for exceptions like ProtocolDecoderException [\#396](https://github.com/kaazing/gateway/pull/396) ([cmebarrow](https://github.com/cmebarrow))
- Allow 0 or more spaces in parsing of wse accept content-type header [\#393](https://github.com/kaazing/gateway/pull/393) ([dpwspoon](https://github.com/dpwspoon))
- Fixed bridge build to include javascript and java bridge [\#391](https://github.com/kaazing/gateway/pull/391) ([dpwspoon](https://github.com/dpwspoon))
- Http.proxy does not append path in connect \#183 [\#382](https://github.com/kaazing/gateway/pull/382) ([msalavastru](https://github.com/msalavastru))

## [5.0.1.42](https://github.com/kaazing/gateway/tree/5.0.1.42) (2016-01-06)
[Full Changelog](https://github.com/kaazing/gateway/compare/5.0.1.41...5.0.1.42)

**Merged pull requests:**

- Forward port to 5.x: Minor test fix for ITest failing intermittently \#103 [\#380](https://github.com/kaazing/gateway/pull/380) ([msalavastru](https://github.com/msalavastru))
- Removed redundant itests and added a new spec test. [\#359](https://github.com/kaazing/gateway/pull/359) ([NicoletaOita](https://github.com/NicoletaOita))

## [5.0.1.41](https://github.com/kaazing/gateway/tree/5.0.1.41) (2016-01-05)
[Full Changelog](https://github.com/kaazing/gateway/compare/5.0.1.40...5.0.1.41)

**Merged pull requests:**

- Fix InvalidateCastException, this function is used by Kaazing Revalidate Extension [\#389](https://github.com/kaazing/gateway/pull/389) ([chao-sun-kaazing](https://github.com/chao-sun-kaazing))
- Fixing the BroadcasttestSlowConsumer by increasing the target bytes [\#388](https://github.com/kaazing/gateway/pull/388) ([jitsni](https://github.com/jitsni))

## [5.0.1.40](https://github.com/kaazing/gateway/tree/5.0.1.40) (2016-01-05)
[Full Changelog](https://github.com/kaazing/gateway/compare/5.0.1.39...5.0.1.40)

**Merged pull requests:**

- Wseb over socks [\#386](https://github.com/kaazing/gateway/pull/386) ([jitsni](https://github.com/jitsni))
- Uptake latest k3po version \(3.0.0-alpha-6\) [\#385](https://github.com/kaazing/gateway/pull/385) ([cmebarrow](https://github.com/cmebarrow))
- If a session is migrated, need to fire idle event on the migrated session [\#378](https://github.com/kaazing/gateway/pull/378) ([jitsni](https://github.com/jitsni))
- Removed Warnings at end of build by coming into compliance with maven… [\#374](https://github.com/kaazing/gateway/pull/374) ([dpwspoon](https://github.com/dpwspoon))
- Acceptor is not BridgeConnector [\#373](https://github.com/kaazing/gateway/pull/373) ([jitsni](https://github.com/jitsni))
- Forward Port to 5.x: Log when users disconnect [\#370](https://github.com/kaazing/gateway/pull/370) ([msalavastru](https://github.com/msalavastru))

## [5.0.1.39](https://github.com/kaazing/gateway/tree/5.0.1.39) (2015-12-15)
[Full Changelog](https://github.com/kaazing/gateway/compare/5.0.1.38...5.0.1.39)

**Closed issues:**

- Server API Documentation section needs updating [\#219](https://github.com/kaazing/gateway/issues/219)

**Merged pull requests:**

- Forward Port Class Cast Exception fix for Hazel Cast from 4.0 [\#371](https://github.com/kaazing/gateway/pull/371) ([mjolie](https://github.com/mjolie))
- Fixing local and remote addresses of some transports [\#369](https://github.com/kaazing/gateway/pull/369) ([jitsni](https://github.com/jitsni))
- Adding round trip latency for websocket sessions\(it gives meaningful values on… [\#368](https://github.com/kaazing/gateway/pull/368) ([jitsni](https://github.com/jitsni))
- Override host header in http.proxy with hostname from \<connect\> [\#360](https://github.com/kaazing/gateway/pull/360) ([msalavastru](https://github.com/msalavastru))

## [5.0.1.38](https://github.com/kaazing/gateway/tree/5.0.1.38) (2015-12-04)
[Full Changelog](https://github.com/kaazing/gateway/compare/5.0.1.37...5.0.1.38)

**Merged pull requests:**

- Streaming http response body [\#362](https://github.com/kaazing/gateway/pull/362) ([jitsni](https://github.com/jitsni))

## [5.0.1.37](https://github.com/kaazing/gateway/tree/5.0.1.37) (2015-12-03)
[Full Changelog](https://github.com/kaazing/gateway/compare/5.0.1.36...5.0.1.37)

**Merged pull requests:**

- Formalized support for extension services without requiring specific type… [\#367](https://github.com/kaazing/gateway/pull/367) ([sanjay-saxena](https://github.com/sanjay-saxena))
- Provisional changes for setting the parent session for SseSession [\#366](https://github.com/kaazing/gateway/pull/366) ([sanjay-saxena](https://github.com/sanjay-saxena))
- Provisional changes for specifying the parent session of SseSession. [\#365](https://github.com/kaazing/gateway/pull/365) ([sanjay-saxena](https://github.com/sanjay-saxena))

## [5.0.1.36](https://github.com/kaazing/gateway/tree/5.0.1.36) (2015-12-01)
[Full Changelog](https://github.com/kaazing/gateway/compare/5.0.1.35...5.0.1.36)

**Merged pull requests:**

- Added IT tests for RFC 7232 for Directory Service [\#358](https://github.com/kaazing/gateway/pull/358) ([sanjay-saxena](https://github.com/sanjay-saxena))
- RFC 7235 specification tests [\#356](https://github.com/kaazing/gateway/pull/356) ([jitsni](https://github.com/jitsni))
- If the parent session is already closed, ssl filter would be null. [\#355](https://github.com/kaazing/gateway/pull/355) ([jitsni](https://github.com/jitsni))
- Fail-fast HTTP responses while decoding HTTP version [\#354](https://github.com/kaazing/gateway/pull/354) ([jitsni](https://github.com/jitsni))
- wsn session may be null when there is an exception [\#353](https://github.com/kaazing/gateway/pull/353) ([jitsni](https://github.com/jitsni))
- Throwing an exception at gateway startup when 'pipe://' URL is used with path [\#352](https://github.com/kaazing/gateway/pull/352) ([msalavastru](https://github.com/msalavastru))
- Adding tests for http proxy sending 504 status code [\#349](https://github.com/kaazing/gateway/pull/349) ([jitsni](https://github.com/jitsni))
- Added x-kaazing-ping-pong WSN robot tests [\#338](https://github.com/kaazing/gateway/pull/338) ([mgherghe](https://github.com/mgherghe))
- Remove/block use of accept in properties section of broadcast service  [\#335](https://github.com/kaazing/gateway/pull/335) ([mjolie](https://github.com/mjolie))

## [5.0.1.35](https://github.com/kaazing/gateway/tree/5.0.1.35) (2015-11-05)
[Full Changelog](https://github.com/kaazing/gateway/compare/5.0.1.34...5.0.1.35)

**Merged pull requests:**

- Cluster bug [\#350](https://github.com/kaazing/gateway/pull/350) ([mjolie](https://github.com/mjolie))
- HTTP HEAD method resources may have Content-Length without content [\#348](https://github.com/kaazing/gateway/pull/348) ([jitsni](https://github.com/jitsni))
- Fix license headers in two recently added test files in transport/wseb. [\#344](https://github.com/kaazing/gateway/pull/344) ([cmebarrow](https://github.com/cmebarrow))
- Really fix the license header in HttpPathMatchingFilter.java [\#343](https://github.com/kaazing/gateway/pull/343) ([cmebarrow](https://github.com/cmebarrow))
- Fix year in license header in HttpPathMatchingFilter.java [\#342](https://github.com/kaazing/gateway/pull/342) ([cmebarrow](https://github.com/cmebarrow))
- Added WSN x-kaazing-idle-timeout k3po test scenarios [\#340](https://github.com/kaazing/gateway/pull/340) ([irina-mitrea-luxoft](https://github.com/irina-mitrea-luxoft))
- HttpBindings comparator didn't check transport's origin [\#319](https://github.com/kaazing/gateway/pull/319) ([jitsni](https://github.com/jitsni))
- upstream/downstream requests should get 404 \(instead of 401\)  [\#257](https://github.com/kaazing/gateway/pull/257) ([jitsni](https://github.com/jitsni))

## [5.0.1.34](https://github.com/kaazing/gateway/tree/5.0.1.34) (2015-10-09)
[Full Changelog](https://github.com/kaazing/gateway/compare/5.0.1.33...5.0.1.34)

**Merged pull requests:**

- Upgrade k3po version from 2.0.1 to latest version 2.1.0. [\#341](https://github.com/kaazing/gateway/pull/341) ([cmebarrow](https://github.com/cmebarrow))

## [5.0.1.33](https://github.com/kaazing/gateway/tree/5.0.1.33) (2015-10-08)
[Full Changelog](https://github.com/kaazing/gateway/compare/5.0.1.32...5.0.1.33)

**Merged pull requests:**

- Update configuration of license-maven-plugin, add/update license as needed [\#339](https://github.com/kaazing/gateway/pull/339) ([ahousing](https://github.com/ahousing))
- Issue \#508, Issue \#151: Forward port memory leak fixes and the abilit… [\#336](https://github.com/kaazing/gateway/pull/336) ([sanjay-saxena](https://github.com/sanjay-saxena))
- Refactored tests rules in order for time out to give K3po diffs [\#334](https://github.com/kaazing/gateway/pull/334) ([dpwspoon](https://github.com/dpwspoon))
- Forward port: report "Early termination of IO session" and "Network collectivity has been lost [\#330](https://github.com/kaazing/gateway/pull/330) ([cmebarrow](https://github.com/cmebarrow))

## [5.0.1.32](https://github.com/kaazing/gateway/tree/5.0.1.32) (2015-09-22)
[Full Changelog](https://github.com/kaazing/gateway/compare/5.0.1.31...5.0.1.32)

**Fixed bugs:**

- sporadic failure of wsn build: WsnConnectorTest.shouldNotHangOnToHttpConnectSessionsWhenEstablishingAndTearingDownWsnConnectorSessions [\#162](https://github.com/kaazing/gateway/issues/162)

**Merged pull requests:**

- upgrade handler is not found during ws connect [\#328](https://github.com/kaazing/gateway/pull/328) ([jitsni](https://github.com/jitsni))
- Gateway enhancements for improved extensibility [\#318](https://github.com/kaazing/gateway/pull/318) ([dpwspoon](https://github.com/dpwspoon))
- change minimum required jdk version from 1.7.0\_21 to 1.8.0 [\#317](https://github.com/kaazing/gateway/pull/317) ([chao-sun-kaazing](https://github.com/chao-sun-kaazing))
- Cleanup: remove unnecessary boxing, using multi-catch [\#304](https://github.com/kaazing/gateway/pull/304) ([jitsni](https://github.com/jitsni))
- deprecating Application Negotiate [\#299](https://github.com/kaazing/gateway/pull/299) ([michaelcretzman](https://github.com/michaelcretzman))

## [5.0.1.31](https://github.com/kaazing/gateway/tree/5.0.1.31) (2015-09-10)
[Full Changelog](https://github.com/kaazing/gateway/compare/5.0.1.30...5.0.1.31)

**Closed issues:**

- Rename http.keepalive.max.connections to http.keepalive.connections in Gateway configuration [\#209](https://github.com/kaazing/gateway/issues/209)

**Merged pull requests:**

- IP filtering doc [\#305](https://github.com/kaazing/gateway/pull/305) ([michaelcretzman](https://github.com/michaelcretzman))
- Add Kerberos callback registrar function  [\#302](https://github.com/kaazing/gateway/pull/302) ([chao-sun-kaazing](https://github.com/chao-sun-kaazing))
- Issue\# 472: Added specification tests for WSN Connector. [\#301](https://github.com/kaazing/gateway/pull/301) ([sanjay-saxena](https://github.com/sanjay-saxena))
- Remove wsr from gateway [\#297](https://github.com/kaazing/gateway/pull/297) ([mjolie](https://github.com/mjolie))
- Added support for the new MMF format [\#275](https://github.com/kaazing/gateway/pull/275) ([irina-mitrea-luxoft](https://github.com/irina-mitrea-luxoft))
- wse doesn't write RECONNECT and CLOSE frames when the channel is not connected [\#268](https://github.com/kaazing/gateway/pull/268) ([jitsni](https://github.com/jitsni))

## [5.0.1.30](https://github.com/kaazing/gateway/tree/5.0.1.30) (2015-08-28)
[Full Changelog](https://github.com/kaazing/gateway/compare/5.0.1.29...5.0.1.30)

**Fixed bugs:**

- TravisCI build tests hanging [\#282](https://github.com/kaazing/gateway/issues/282)

**Merged pull requests:**

- Fixed issue in duplicate jar detection where list was not cleared on … [\#296](https://github.com/kaazing/gateway/pull/296) ([dpwspoon](https://github.com/dpwspoon))
- added link to Release Notes [\#295](https://github.com/kaazing/gateway/pull/295) ([michaelcretzman](https://github.com/michaelcretzman))
- Feature/config translator ii [\#294](https://github.com/kaazing/gateway/pull/294) ([mjolie](https://github.com/mjolie))
- Remove code for KSESSIONID and "recycle" authorization mode [\#293](https://github.com/kaazing/gateway/pull/293) ([sanjay-saxena](https://github.com/sanjay-saxena))
- Trying to address travis gateway build failures. [\#291](https://github.com/kaazing/gateway/pull/291) ([cmebarrow](https://github.com/cmebarrow))
- \(\#267\) Ignore OcspIT.testGoodCertificate since we have not yet found … [\#290](https://github.com/kaazing/gateway/pull/290) ([cmebarrow](https://github.com/cmebarrow))
- Adding Method execution trace, and timeout rules [\#289](https://github.com/kaazing/gateway/pull/289) ([jitsni](https://github.com/jitsni))
- Travis ci failures [\#286](https://github.com/kaazing/gateway/pull/286) ([dpwspoon](https://github.com/dpwspoon))
- Wseb connector sending X-WebSocket-Protocol header [\#284](https://github.com/kaazing/gateway/pull/284) ([jitsni](https://github.com/jitsni))
- Binding explicitly for .../;api endpoints for wsn and wseb cases [\#280](https://github.com/kaazing/gateway/pull/280) ([jitsni](https://github.com/jitsni))
- Change http.keepalive.max.connections to http.keepalive.connections [\#278](https://github.com/kaazing/gateway/pull/278) ([jitsni](https://github.com/jitsni))
- Adds jar version to manifest entry such that it can be reported when duplicate jars are [\#276](https://github.com/kaazing/gateway/pull/276) ([dpwspoon](https://github.com/dpwspoon))

## [5.0.1.29](https://github.com/kaazing/gateway/tree/5.0.1.29) (2015-08-19)
[Full Changelog](https://github.com/kaazing/gateway/compare/5.0.1.28...5.0.1.29)

**Fixed bugs:**

- Build errors on travis  [\#264](https://github.com/kaazing/gateway/issues/264)
- remove HTML from Glossary [\#250](https://github.com/kaazing/gateway/issues/250)
- KGS-976: UP: sporadic error connecting Command Center to JMS 4.0.6 Dev / JMS 4.0.7 Prod [\#226](https://github.com/kaazing/gateway/issues/226)
- \(doc\) Admin guide is missing maximum-age [\#145](https://github.com/kaazing/gateway/issues/145)

**Closed issues:**

- Remove protocol injection topics again [\#228](https://github.com/kaazing/gateway/issues/228)
- Update http.keepalive.timeout example to use time unit [\#224](https://github.com/kaazing/gateway/issues/224)
- \(doc\) Remove reference to GWT [\#223](https://github.com/kaazing/gateway/issues/223)
- Remove the Session \<type\> from gateway documentation  [\#214](https://github.com/kaazing/gateway/issues/214)
- AgronaMonitoringEntityFactoryTest fails the build \(on Mac OS x\) [\#171](https://github.com/kaazing/gateway/issues/171)
- \(doc\) Add connect options to service defaults description [\#143](https://github.com/kaazing/gateway/issues/143)
- \(doc\) Add links to admin-reference/p\_config\_multicast.md [\#134](https://github.com/kaazing/gateway/issues/134)
- Create a single distribution file for all environments [\#20](https://github.com/kaazing/gateway/issues/20)

**Merged pull requests:**

- Adding info message when wsn is falling back to wse [\#283](https://github.com/kaazing/gateway/pull/283) ([jitsni](https://github.com/jitsni))
- Fixing NPE when connector is null [\#281](https://github.com/kaazing/gateway/pull/281) ([jitsni](https://github.com/jitsni))
- Forgot to commit acceptor rule for wsn [\#272](https://github.com/kaazing/gateway/pull/272) ([jitsni](https://github.com/jitsni))
- Add wsn spec tests [\#270](https://github.com/kaazing/gateway/pull/270) ([jitsni](https://github.com/jitsni))
- Resolves \#264 [\#265](https://github.com/kaazing/gateway/pull/265) ([dpwspoon](https://github.com/dpwspoon))
- Added ability to filter IP addresses [\#263](https://github.com/kaazing/gateway/pull/263) ([sanjay-saxena](https://github.com/sanjay-saxena))
- WSE Spec Test [\#262](https://github.com/kaazing/gateway/pull/262) ([pkhanal](https://github.com/pkhanal))
- fixing md for html I hope [\#261](https://github.com/kaazing/gateway/pull/261) ([michaelcretzman](https://github.com/michaelcretzman))
- output formatting error [\#260](https://github.com/kaazing/gateway/pull/260) ([michaelcretzman](https://github.com/michaelcretzman))
- fixing link errors [\#259](https://github.com/kaazing/gateway/pull/259) ([michaelcretzman](https://github.com/michaelcretzman))
- Format errors [\#258](https://github.com/kaazing/gateway/pull/258) ([michaelcretzman](https://github.com/michaelcretzman))
- Adding a wse balancer test [\#256](https://github.com/kaazing/gateway/pull/256) ([jitsni](https://github.com/jitsni))
- Update kaazing-glossary.md [\#252](https://github.com/kaazing/gateway/pull/252) ([michaelcretzman](https://github.com/michaelcretzman))
- Remove html from glossary \#250 [\#251](https://github.com/kaazing/gateway/pull/251) ([michaelcretzman](https://github.com/michaelcretzman))
- doc link changes [\#249](https://github.com/kaazing/gateway/pull/249) ([chadpowers](https://github.com/chadpowers))
- Fix links [\#248](https://github.com/kaazing/gateway/pull/248) ([michaelcretzman](https://github.com/michaelcretzman))
- Link errors [\#247](https://github.com/kaazing/gateway/pull/247) ([michaelcretzman](https://github.com/michaelcretzman))
- Update r\_configure\_gateway\_service.md [\#242](https://github.com/kaazing/gateway/pull/242) ([michaelcretzman](https://github.com/michaelcretzman))
- Update r\_configure\_gateway\_element\_skeleton.md [\#241](https://github.com/kaazing/gateway/pull/241) ([michaelcretzman](https://github.com/michaelcretzman))
- APNs removed [\#240](https://github.com/kaazing/gateway/pull/240) ([michaelcretzman](https://github.com/michaelcretzman))
-  Remove protocol injection [\#239](https://github.com/kaazing/gateway/pull/239) ([michaelcretzman](https://github.com/michaelcretzman))
- Link errors [\#238](https://github.com/kaazing/gateway/pull/238) ([michaelcretzman](https://github.com/michaelcretzman))
- Add tcp spec tests [\#235](https://github.com/kaazing/gateway/pull/235) ([chadpowers](https://github.com/chadpowers))
- APNs removed [\#233](https://github.com/kaazing/gateway/pull/233) ([michaelcretzman](https://github.com/michaelcretzman))
- Port OOM bug fix on JMX from gateway 4.0 to 5.0 [\#232](https://github.com/kaazing/gateway/pull/232) ([mgherghe](https://github.com/mgherghe))
- Configuration incorrect for ticket granting gateway \#26 [\#231](https://github.com/kaazing/gateway/pull/231) ([michaelcretzman](https://github.com/michaelcretzman))
- Added SPI note for issue \#116 [\#229](https://github.com/kaazing/gateway/pull/229) ([vjwang](https://github.com/vjwang))
- fixed broken links [\#227](https://github.com/kaazing/gateway/pull/227) ([michaelcretzman](https://github.com/michaelcretzman))
- Fixed nits in http.proxy doc [\#225](https://github.com/kaazing/gateway/pull/225) ([veschup](https://github.com/veschup))
- Added changes for supporting multiple gateways on the same host and merged refactoring from Marina's fork \(removed StringManager and latestException\) [\#222](https://github.com/kaazing/gateway/pull/222) ([irina-mitrea-luxoft](https://github.com/irina-mitrea-luxoft))
- Remove the Session service \(\<type\>\) from gateway documentation [\#218](https://github.com/kaazing/gateway/pull/218) ([veschup](https://github.com/veschup))
- Removed breadcrumbs. [\#217](https://github.com/kaazing/gateway/pull/217) ([chadpowers](https://github.com/chadpowers))
- Updated upgrade guide [\#216](https://github.com/kaazing/gateway/pull/216) ([vjwang](https://github.com/vjwang))
- Upstream and downstream shouldn't have to go through authentication. [\#212](https://github.com/kaazing/gateway/pull/212) ([jitsni](https://github.com/jitsni))
- wse create binding doesn't include ;e or ;e/ct  etc. [\#210](https://github.com/kaazing/gateway/pull/210) ([jitsni](https://github.com/jitsni))
- Have the cobertura build happen during normal building rather than during after\_success [\#207](https://github.com/kaazing/gateway/pull/207) ([ahousing](https://github.com/ahousing))

## [5.0.1.28](https://github.com/kaazing/gateway/tree/5.0.1.28) (2015-06-30)
[Full Changelog](https://github.com/kaazing/gateway/compare/5.0.1.27...5.0.1.28)

**Closed issues:**

-  Create AmqpProxyServiceExtensionSpi in gateway [\#159](https://github.com/kaazing/gateway/issues/159)

**Merged pull requests:**

- Tcp extensions API and implementation [\#213](https://github.com/kaazing/gateway/pull/213) ([cmebarrow](https://github.com/cmebarrow))
- \#203 Avoid syntax like read /:.\*/ "\n" in transport/sse k3po test scr… [\#211](https://github.com/kaazing/gateway/pull/211) ([cmebarrow](https://github.com/cmebarrow))
- Removing after moving file to enterprise.gateway [\#206](https://github.com/kaazing/gateway/pull/206) ([vjwang](https://github.com/vjwang))
- adding link to Redis doc and EE graphic [\#204](https://github.com/kaazing/gateway/pull/204) ([michaelcretzman](https://github.com/michaelcretzman))
- Eliminate ugly exceptions stacks in build output by removing calls to… [\#202](https://github.com/kaazing/gateway/pull/202) ([cmebarrow](https://github.com/cmebarrow))
- Removed docker build to unstable-gateway docker hub [\#201](https://github.com/kaazing/gateway/pull/201) ([dpwspoon](https://github.com/dpwspoon))
- Update Doc links [\#200](https://github.com/kaazing/gateway/pull/200) ([chadpowers](https://github.com/chadpowers))
- Fixed \#94 for AMQP Identity Promotion [\#199](https://github.com/kaazing/gateway/pull/199) ([vjwang](https://github.com/vjwang))
- Fixing two issues with WsnConnector impl w.r.t handshake [\#198](https://github.com/kaazing/gateway/pull/198) ([jitsni](https://github.com/jitsni))
- Adding a helper class to log exceptions [\#196](https://github.com/kaazing/gateway/pull/196) ([jitsni](https://github.com/jitsni))
- Adding the glossary in MD format [\#195](https://github.com/kaazing/gateway/pull/195) ([vjwang](https://github.com/vjwang))
- Add JMS entries into index.md and backed out JMX updates from monitoring topics [\#194](https://github.com/kaazing/gateway/pull/194) ([veschup](https://github.com/veschup))
- Fix for unit test in Mac OS [\#190](https://github.com/kaazing/gateway/pull/190) ([irina-mitrea-luxoft](https://github.com/irina-mitrea-luxoft))

## [5.0.1.27](https://github.com/kaazing/gateway/tree/5.0.1.27) (2015-06-15)
[Full Changelog](https://github.com/kaazing/gateway/compare/5.0.1.26...5.0.1.27)

**Fixed bugs:**

- WSE not working in F5 BIG-IP topologies with multiple gateways on port - BUG \[Duplicate\] [\#187](https://github.com/kaazing/gateway/issues/187)
- Update to latest community to consume fixes for animal sniffer plugin [\#176](https://github.com/kaazing/gateway/issues/176)
- Log files are overflowing 4MB maximum on travis when tests fail [\#166](https://github.com/kaazing/gateway/issues/166)

**Closed issues:**

- NioDatagramAcceptorExIT.shouldConnect not all expectations were satisfied \[Duplicate\] [\#185](https://github.com/kaazing/gateway/issues/185)
- Long start time on systems with low entropy \(containers\) due to calls to getSecureBytes. [\#167](https://github.com/kaazing/gateway/issues/167)
- \(doc\) Doc markdown needs to use "bash" for the code type [\#124](https://github.com/kaazing/gateway/issues/124)

**Merged pull requests:**

- Fix up the timing of when proxy service extensions are invoked \(initAcceptSession\) [\#192](https://github.com/kaazing/gateway/pull/192) ([krismcqueen](https://github.com/krismcqueen))
- update to the latest community [\#191](https://github.com/kaazing/gateway/pull/191) ([krismcqueen](https://github.com/krismcqueen))
- Deleting file because it was moved [\#189](https://github.com/kaazing/gateway/pull/189) ([michaelcretzman](https://github.com/michaelcretzman))
- added JMS troubleshooting links to topic [\#188](https://github.com/kaazing/gateway/pull/188) ([michaelcretzman](https://github.com/michaelcretzman))
- Expose extension point for amqp.proxy service [\#186](https://github.com/kaazing/gateway/pull/186) ([krismcqueen](https://github.com/krismcqueen))
- Added changes for supporting additional service session-level counters [\#184](https://github.com/kaazing/gateway/pull/184) ([irina-mitrea-luxoft](https://github.com/irina-mitrea-luxoft))
- Remove or update svn references [\#181](https://github.com/kaazing/gateway/pull/181) ([ahousing](https://github.com/ahousing))
- Added reference to kaazing gateway on Docker Hub. [\#180](https://github.com/kaazing/gateway/pull/180) ([brennangaunce](https://github.com/brennangaunce))
-  http.keepalive.max.connections config to cache http idle connections. [\#178](https://github.com/kaazing/gateway/pull/178) ([jitsni](https://github.com/jitsni))
- kaazing/gateway\#176  Update to latest community to get animal-sniffer… [\#177](https://github.com/kaazing/gateway/pull/177) ([krismcqueen](https://github.com/krismcqueen))
- Move Enterprise Shield docs [\#174](https://github.com/kaazing/gateway/pull/174) ([vjwang](https://github.com/vjwang))
- Ignored test that is buggy on mac os, see https://github.com/kaazing/… [\#172](https://github.com/kaazing/gateway/pull/172) ([dpwspoon](https://github.com/dpwspoon))
- Resolves kaazing/gateway\#167: slow start ups on machines with low entropy [\#169](https://github.com/kaazing/gateway/pull/169) ([dpwspoon](https://github.com/dpwspoon))
- Added quiet flag to mvn builds in travis [\#168](https://github.com/kaazing/gateway/pull/168) ([dpwspoon](https://github.com/dpwspoon))
- Updated README.md with docker instructions [\#165](https://github.com/kaazing/gateway/pull/165) ([brennangaunce](https://github.com/brennangaunce))
- Move truststore to its own repository, gateway.truststore [\#163](https://github.com/kaazing/gateway/pull/163) ([ahousing](https://github.com/ahousing))
- Removed empty folder/subproject [\#160](https://github.com/kaazing/gateway/pull/160) ([dpwspoon](https://github.com/dpwspoon))
- Updates to allow build for java 8 build, and updated readme to say th… [\#158](https://github.com/kaazing/gateway/pull/158) ([dpwspoon](https://github.com/dpwspoon))
- New ws extension spi [\#155](https://github.com/kaazing/gateway/pull/155) ([cmebarrow](https://github.com/cmebarrow))
- New Enterprise Shield content and graphics [\#148](https://github.com/kaazing/gateway/pull/148) ([veschup](https://github.com/veschup))
- Adding gateway counters to Agrona [\#133](https://github.com/kaazing/gateway/pull/133) ([irina-mitrea-luxoft](https://github.com/irina-mitrea-luxoft))

## [5.0.1.26](https://github.com/kaazing/gateway/tree/5.0.1.26) (2015-05-26)
[Full Changelog](https://github.com/kaazing/gateway/compare/5.0.1.25...5.0.1.26)

**Closed issues:**

- Allow ProxyService extensions [\#130](https://github.com/kaazing/gateway/issues/130)
- Allow NIOSocketConnector to use a proxy transport if configured [\#122](https://github.com/kaazing/gateway/issues/122)

**Merged pull requests:**

- HTTP response without Content-Length or chunked encoding causes parsing loop [\#147](https://github.com/kaazing/gateway/pull/147) ([jitsni](https://github.com/jitsni))

## [5.0.1.25](https://github.com/kaazing/gateway/tree/5.0.1.25) (2015-05-21)
[Full Changelog](https://github.com/kaazing/gateway/compare/5.0.1.24...5.0.1.25)

**Fixed bugs:**

- testSendLargeFile\(org.apache.mina.transport.socket.nio.NioFileRegionTest\) occasionally failing [\#150](https://github.com/kaazing/gateway/issues/150)

**Merged pull requests:**

- Use localhost for binding in AbstractFileRegionTest.java [\#151](https://github.com/kaazing/gateway/pull/151) ([dpwspoon](https://github.com/dpwspoon))
- Renamed files and updated references [\#149](https://github.com/kaazing/gateway/pull/149) ([vjwang](https://github.com/vjwang))
- Added module that builds a docker image from a release [\#139](https://github.com/kaazing/gateway/pull/139) ([dpwspoon](https://github.com/dpwspoon))
- Wseb transport session [\#137](https://github.com/kaazing/gateway/pull/137) ([cmebarrow](https://github.com/cmebarrow))

## [5.0.1.24](https://github.com/kaazing/gateway/tree/5.0.1.24) (2015-05-20)
[Full Changelog](https://github.com/kaazing/gateway/compare/5.0.1.23...5.0.1.24)

**Fixed bugs:**

- Test is using ephemeral port to bind [\#114](https://github.com/kaazing/gateway/issues/114)
- ws.inactivity.timeout occurring on incorrect WebSocket layer when there are multiple WebSocket layers in the stack [\#62](https://github.com/kaazing/gateway/issues/62)
- JmxManagementServiceHandlerTest\#testNoJMXBindingNameConflictsOnMultiServicesUsingSameAccept failing and has been ignored [\#32](https://github.com/kaazing/gateway/issues/32)

**Closed issues:**

- \(doc\) Typos [\#142](https://github.com/kaazing/gateway/issues/142)
- SseSameOriginIT sseIe8HttpxeConnectAndGetData fails intermittently [\#141](https://github.com/kaazing/gateway/issues/141)
- Ensure schemes are resolved in a dynamic manner [\#72](https://github.com/kaazing/gateway/issues/72)
- accept-options / connect options should be "sequence" to allows extenstions  [\#69](https://github.com/kaazing/gateway/issues/69)

**Merged pull requests:**

- Removed the explicit closing from the test as it should be unspecifie… [\#144](https://github.com/kaazing/gateway/pull/144) ([dpwspoon](https://github.com/dpwspoon))
- Update ts\_security.md [\#140](https://github.com/kaazing/gateway/pull/140) ([michaelcretzman](https://github.com/michaelcretzman))
- Fix failures in SseCrossOriginIT [\#138](https://github.com/kaazing/gateway/pull/138) ([cmebarrow](https://github.com/cmebarrow))
- Fixed an issue where gateway wasn't flushing PING frame followed by RECONNECT frame in long polling mode [\#135](https://github.com/kaazing/gateway/pull/135) ([pkhanal](https://github.com/pkhanal))
- Embedded Gateway docs [\#132](https://github.com/kaazing/gateway/pull/132) ([vjwang](https://github.com/vjwang))
- Allow proxy service to be extended [\#131](https://github.com/kaazing/gateway/pull/131) ([krismcqueen](https://github.com/krismcqueen))
- For \#273 - many fixes to MD [\#129](https://github.com/kaazing/gateway/pull/129) ([veschup](https://github.com/veschup))
- EE graphic for docs [\#127](https://github.com/kaazing/gateway/pull/127) ([vjwang](https://github.com/vjwang))
- Community Edition server images and samples [\#126](https://github.com/kaazing/gateway/pull/126) ([vjwang](https://github.com/vjwang))
- Cleanup [\#125](https://github.com/kaazing/gateway/pull/125) ([jitsni](https://github.com/jitsni))
- Allow a proxy.connector to be injected into the N… [\#123](https://github.com/kaazing/gateway/pull/123) ([krismcqueen](https://github.com/krismcqueen))
- Updated doc references and typos in the README.md [\#119](https://github.com/kaazing/gateway/pull/119) ([vjwang](https://github.com/vjwang))
- Feature/md docs [\#117](https://github.com/kaazing/gateway/pull/117) ([vjwang](https://github.com/vjwang))
- Restore strongly typed accept-options / connect-options [\#116](https://github.com/kaazing/gateway/pull/116) ([krismcqueen](https://github.com/krismcqueen))
- resolves \#114 and better describes xsd [\#115](https://github.com/kaazing/gateway/pull/115) ([dpwspoon](https://github.com/dpwspoon))
- Many misc fixes during verification pass [\#113](https://github.com/kaazing/gateway/pull/113) ([veschup](https://github.com/veschup))
- Feature/new ws extension api, ordering of extensions [\#112](https://github.com/kaazing/gateway/pull/112) ([dpwspoon](https://github.com/dpwspoon))
- Add resource.address.httpxe test dependency to server\pom.xml since it i... [\#111](https://github.com/kaazing/gateway/pull/111) ([cmebarrow](https://github.com/cmebarrow))
- Cleanup [\#110](https://github.com/kaazing/gateway/pull/110) ([jitsni](https://github.com/jitsni))
- Let the origin server header flow through http.proxy service [\#109](https://github.com/kaazing/gateway/pull/109) ([jitsni](https://github.com/jitsni))
- Writing of HTTP server header is controlled by an accept option. [\#108](https://github.com/kaazing/gateway/pull/108) ([jitsni](https://github.com/jitsni))
- Removing hop-by-hop headers in response messages too. Adding a test cas... [\#107](https://github.com/kaazing/gateway/pull/107) ([jitsni](https://github.com/jitsni))
- Exposing accept-options and connect-options in the management layer... [\#105](https://github.com/kaazing/gateway/pull/105) ([krismcqueen](https://github.com/krismcqueen))
- No use in declaring static final methods. Removing final keyword [\#104](https://github.com/kaazing/gateway/pull/104) ([jitsni](https://github.com/jitsni))
- WebSocketExtensionFactory: implement method negotiateWebSocketExtensions... [\#103](https://github.com/kaazing/gateway/pull/103) ([dpwspoon](https://github.com/dpwspoon))
- fixed remaining gateway files [\#102](https://github.com/kaazing/gateway/pull/102) ([michaelcretzman](https://github.com/michaelcretzman))
- New ws extension api: preliminary proposal [\#101](https://github.com/kaazing/gateway/pull/101) ([cmebarrow](https://github.com/cmebarrow))
- updated admin-reference, about, high-availability [\#100](https://github.com/kaazing/gateway/pull/100) ([michaelcretzman](https://github.com/michaelcretzman))
- http.proxy supports upgrade [\#98](https://github.com/kaazing/gateway/pull/98) ([jitsni](https://github.com/jitsni))

## [5.0.1.23](https://github.com/kaazing/gateway/tree/5.0.1.23) (2015-04-17)
[Full Changelog](https://github.com/kaazing/gateway/compare/5.0.1.22...5.0.1.23)

**Closed issues:**

- Enterprise Shield +1 connection fails when using different transport URIs over SOCKS [\#96](https://github.com/kaazing/gateway/issues/96)

**Merged pull requests:**

- Fix how ws.inactivity.timeout was applied though accept-options [\#97](https://github.com/kaazing/gateway/pull/97) ([krismcqueen](https://github.com/krismcqueen))
- Changes for apns ws extension [\#95](https://github.com/kaazing/gateway/pull/95) ([cmebarrow](https://github.com/cmebarrow))
- Update Accept/Connect Options to be generic such that they can be extended [\#94](https://github.com/kaazing/gateway/pull/94) ([krismcqueen](https://github.com/krismcqueen))
- Minor cleanup : removing unnecessary casts, keywords etc [\#93](https://github.com/kaazing/gateway/pull/93) ([jitsni](https://github.com/jitsni))
- Removing unused imports [\#92](https://github.com/kaazing/gateway/pull/92) ([jitsni](https://github.com/jitsni))
- Http.proxy.persistence [\#88](https://github.com/kaazing/gateway/pull/88) ([jitsni](https://github.com/jitsni))

## [5.0.1.22](https://github.com/kaazing/gateway/tree/5.0.1.22) (2015-04-09)
[Full Changelog](https://github.com/kaazing/gateway/compare/5.0.1.21...5.0.1.22)

**Fixed bugs:**

- SseSameOriginIT.sseIe8HttpxeConnectAndGetData sometimes fails [\#60](https://github.com/kaazing/gateway/issues/60)
- WsebTransportIT.testEchoAlignedDownstream sporadically failing [\#41](https://github.com/kaazing/gateway/issues/41)
- WsnConnectorTest.shouldNotHangOnToHttpConnectSessionsWhenEstablishingAndTearingDownWsnConnectorSessions sporadically hanging  [\#37](https://github.com/kaazing/gateway/issues/37)

**Closed issues:**

- Use sequence numbers to detect out of order requests in wseb [\#73](https://github.com/kaazing/gateway/issues/73)
- Add Visitor to reorder elements in accept/connect options [\#70](https://github.com/kaazing/gateway/issues/70)
- Configuration doesn't allow \<connect-options\> in \<service-defaults\> [\#64](https://github.com/kaazing/gateway/issues/64)

**Merged pull requests:**

- Add a connect\(ResourceAddress...\) variant to ServiceContext. [\#91](https://github.com/kaazing/gateway/pull/91) ([cmebarrow](https://github.com/cmebarrow))
- barriers are not required in these scripts [\#90](https://github.com/kaazing/gateway/pull/90) ([jitsni](https://github.com/jitsni))
- Added test for service defaults getting parsed from xml, and generified ... [\#89](https://github.com/kaazing/gateway/pull/89) ([dpwspoon](https://github.com/dpwspoon))
- Allow service-defaults to allow connect options [\#87](https://github.com/kaazing/gateway/pull/87) ([dpwspoon](https://github.com/dpwspoon))
- Added timeout back into WsnConnectorTest [\#86](https://github.com/kaazing/gateway/pull/86) ([dpwspoon](https://github.com/dpwspoon))

## [5.0.1.21](https://github.com/kaazing/gateway/tree/5.0.1.21) (2015-04-03)
[Full Changelog](https://github.com/kaazing/gateway/compare/5.0.1.20...5.0.1.21)

**Fixed bugs:**

- Build failed in mina.netty: NioDatagramConnectorExIT [\#67](https://github.com/kaazing/gateway/issues/67)

**Closed issues:**

- Create maven BOM of gateway [\#75](https://github.com/kaazing/gateway/issues/75)

**Merged pull requests:**

- Updated to latest k3po [\#85](https://github.com/kaazing/gateway/pull/85) ([dpwspoon](https://github.com/dpwspoon))
- Add dependencies to the bom so it includes everything in assembly, remove duplicate version numbers [\#84](https://github.com/kaazing/gateway/pull/84) ([cmebarrow](https://github.com/cmebarrow))
- Add support for Docker via $hostname property, config from url [\#83](https://github.com/kaazing/gateway/pull/83) ([dpwspoon](https://github.com/dpwspoon))
- Wseb sequencing [\#82](https://github.com/kaazing/gateway/pull/82) ([jitsni](https://github.com/jitsni))
- Added exec to linux startup script [\#81](https://github.com/kaazing/gateway/pull/81) ([dpwspoon](https://github.com/dpwspoon))

## [5.0.1.20](https://github.com/kaazing/gateway/tree/5.0.1.20) (2015-03-26)
[Full Changelog](https://github.com/kaazing/gateway/compare/5.0.1.19...5.0.1.20)

**Fixed bugs:**

- gateway.transport.wsn: IdleTimeoutExtensionPongsIT.shouldGetPongsFromidleTimeoutExtensionWhenWriterIdle spurious failures [\#34](https://github.com/kaazing/gateway/issues/34)
- gateway.transport.wsn: UnresolvableHostnameIT\#connectingOnService1ShouldNotGetAccessToService2 is failing on travis [\#33](https://github.com/kaazing/gateway/issues/33)
- Small tweaks for readme [\#18](https://github.com/kaazing/gateway/issues/18)
- Incorrect port number in README.md [\#15](https://github.com/kaazing/gateway/issues/15)
- Incorrect filenames in README.md [\#14](https://github.com/kaazing/gateway/issues/14)

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
- \[bugout\] npm no longer required [\#9](https://github.com/kaazing/gateway/issues/9)

**Merged pull requests:**

- Changed packaging type to pom to signal no real artifact is uploaded to ... [\#78](https://github.com/kaazing/gateway/pull/78) ([dpwspoon](https://github.com/dpwspoon))
- Removed windows newlines [\#76](https://github.com/kaazing/gateway/pull/76) ([dpwspoon](https://github.com/dpwspoon))
- \#67 Adjust an expectation in NioDatagramConnectorExIT to avoid a sporadi... [\#74](https://github.com/kaazing/gateway/pull/74) ([cmebarrow](https://github.com/cmebarrow))
- Misc minor code changes [\#68](https://github.com/kaazing/gateway/pull/68) ([jitsni](https://github.com/jitsni))
- Minor clean up work renaming of classes and updating/adding JavaDoc [\#66](https://github.com/kaazing/gateway/pull/66) ([dpwspoon](https://github.com/dpwspoon))
- Added coveralls setup to pom and .travis [\#65](https://github.com/kaazing/gateway/pull/65) ([dpwspoon](https://github.com/dpwspoon))
- Transport fixes from 4.0 [\#61](https://github.com/kaazing/gateway/pull/61) ([cmebarrow](https://github.com/cmebarrow))
- \#55 Ensure Subject gets correctly propagated up the transport layers [\#57](https://github.com/kaazing/gateway/pull/57) ([cmebarrow](https://github.com/cmebarrow))
- http.proxy service implementation [\#56](https://github.com/kaazing/gateway/pull/56) ([jitsni](https://github.com/jitsni))
- \#52: add IoSessionEx APIs to get and set Subject and listen for Subject changes [\#54](https://github.com/kaazing/gateway/pull/54) ([cmebarrow](https://github.com/cmebarrow))
- \#49: allow Gateway to start using Azul Zing JVM by not checking java poi... [\#50](https://github.com/kaazing/gateway/pull/50) ([cmebarrow](https://github.com/cmebarrow))
- Integration tests failing in HTTP directory service [\#48](https://github.com/kaazing/gateway/pull/48) ([cmebarrow](https://github.com/cmebarrow))
- Renamed artifact id of gateway.distribution [\#46](https://github.com/kaazing/gateway/pull/46) ([dpwspoon](https://github.com/dpwspoon))
- Add m2e lifecycle-mapping configurations to ignore unsupported plug-ins [\#45](https://github.com/kaazing/gateway/pull/45) ([cmebarrow](https://github.com/cmebarrow))
- Deleted test that is no longer needed due to Threading model change [\#43](https://github.com/kaazing/gateway/pull/43) ([dpwspoon](https://github.com/dpwspoon))
- Enabling SSLv3 via security property for the test [\#42](https://github.com/kaazing/gateway/pull/42) ([jitsni](https://github.com/jitsni))
- Removed gateway prefix in submodules [\#40](https://github.com/kaazing/gateway/pull/40) ([dpwspoon](https://github.com/dpwspoon))
- Update to latest robot to avoid race conditions in integration-tests [\#39](https://github.com/kaazing/gateway/pull/39) ([krismcqueen](https://github.com/krismcqueen))
- Set version at develop-SNAPSHOT. [\#38](https://github.com/kaazing/gateway/pull/38) ([dpwspoon](https://github.com/dpwspoon))
- Removed bower.json and package.json from gateway.distribution.  Fixed bu... [\#29](https://github.com/kaazing/gateway/pull/29) ([dpwspoon](https://github.com/dpwspoon))
- Consolidated gateway repositories [\#28](https://github.com/kaazing/gateway/pull/28) ([dpwspoon](https://github.com/dpwspoon))
- Added .travis.yml and badge to README.md [\#26](https://github.com/kaazing/gateway/pull/26) ([dpwspoon](https://github.com/dpwspoon))
- Add a Gitter chat badge to README.md [\#21](https://github.com/kaazing/gateway/pull/21) ([gitter-badger](https://github.com/gitter-badger))
- Fixed README nits [\#19](https://github.com/kaazing/gateway/pull/19) ([vjwang](https://github.com/vjwang))

## [5.0.1.19](https://github.com/kaazing/gateway/tree/5.0.1.19) (2015-01-09)
[Full Changelog](https://github.com/kaazing/gateway/compare/5.0.1.18...5.0.1.19)

## [5.0.1.18](https://github.com/kaazing/gateway/tree/5.0.1.18) (2014-12-20)
[Full Changelog](https://github.com/kaazing/gateway/compare/5.0.1.17...5.0.1.18)

**Merged pull requests:**

- Got rid of copyright in pom because they are not auto updated from licen... [\#25](https://github.com/kaazing/gateway/pull/25) ([dpwspoon](https://github.com/dpwspoon))

## [5.0.1.17](https://github.com/kaazing/gateway/tree/5.0.1.17) (2014-12-18)
[Full Changelog](https://github.com/kaazing/gateway/compare/5.0.1.16...5.0.1.17)

**Closed issues:**

- building gateway on MacOSX 10.7+ can yield " group id '2119906183' is too big \( \> 2097151 \)" assembly error [\#23](https://github.com/kaazing/gateway/issues/23)

**Merged pull requests:**

- Apply fix for http://jira.codehaus.org/browse/MASSEMBLY-728  [\#24](https://github.com/kaazing/gateway/pull/24) ([nowucca](https://github.com/nowucca))
- Updated project to appendAssemblyId false so that artifacts don't need v... [\#22](https://github.com/kaazing/gateway/pull/22) ([dpwspoon](https://github.com/dpwspoon))

## [5.0.1.16](https://github.com/kaazing/gateway/tree/5.0.1.16) (2014-12-17)
[Full Changelog](https://github.com/kaazing/gateway/compare/5.0.1.15...5.0.1.16)

## [5.0.1.15](https://github.com/kaazing/gateway/tree/5.0.1.15) (2014-12-17)
[Full Changelog](https://github.com/kaazing/gateway/compare/5.0.1.14...5.0.1.15)

## [5.0.1.14](https://github.com/kaazing/gateway/tree/5.0.1.14) (2014-12-16)
[Full Changelog](https://github.com/kaazing/gateway/compare/5.0.0.16...5.0.1.14)

## [5.0.0.16](https://github.com/kaazing/gateway/tree/5.0.0.16) (2014-12-16)
[Full Changelog](https://github.com/kaazing/gateway/compare/5.0.0.15...5.0.0.16)

## [5.0.0.15](https://github.com/kaazing/gateway/tree/5.0.0.15) (2014-12-12)
[Full Changelog](https://github.com/kaazing/gateway/compare/5.0.0.14...5.0.0.15)

**Merged pull requests:**

- Update distribution names and Gateway startup log [\#17](https://github.com/kaazing/gateway/pull/17) ([dpwspoon](https://github.com/dpwspoon))
- Changed filenames and port \# [\#16](https://github.com/kaazing/gateway/pull/16) ([vjwang](https://github.com/vjwang))
- Updated naming and content [\#13](https://github.com/kaazing/gateway/pull/13) ([vjwang](https://github.com/vjwang))

## [5.0.0.14](https://github.com/kaazing/gateway/tree/5.0.0.14) (2014-12-11)
[Full Changelog](https://github.com/kaazing/gateway/compare/5.0.0.13...5.0.0.14)

**Merged pull requests:**

- KG-13558:  Add an echo service, update the 'It Works' page to connect to... [\#11](https://github.com/kaazing/gateway/pull/11) ([krismcqueen](https://github.com/krismcqueen))
- Updated project to use kaazing sigar dist [\#7](https://github.com/kaazing/gateway/pull/7) ([dpwspoon](https://github.com/dpwspoon))

## [5.0.0.13](https://github.com/kaazing/gateway/tree/5.0.0.13) (2014-12-10)
[Full Changelog](https://github.com/kaazing/gateway/compare/5.0.0.12...5.0.0.13)

## [5.0.0.12](https://github.com/kaazing/gateway/tree/5.0.0.12) (2014-12-10)
[Full Changelog](https://github.com/kaazing/gateway/compare/5.0.0.11...5.0.0.12)

**Merged pull requests:**

- Updated name of bower-maven-plugin [\#6](https://github.com/kaazing/gateway/pull/6) ([dpwspoon](https://github.com/dpwspoon))

## [5.0.0.11](https://github.com/kaazing/gateway/tree/5.0.0.11) (2014-12-04)
[Full Changelog](https://github.com/kaazing/gateway/compare/5.0.0.10...5.0.0.11)

## [5.0.0.10](https://github.com/kaazing/gateway/tree/5.0.0.10) (2014-12-04)
[Full Changelog](https://github.com/kaazing/gateway/compare/5.0.0.9...5.0.0.10)

## [5.0.0.9](https://github.com/kaazing/gateway/tree/5.0.0.9) (2014-12-04)
**Merged pull requests:**

- Updated project to use bower-dependency-maven-plugin to get bower compon... [\#5](https://github.com/kaazing/gateway/pull/5) ([dpwspoon](https://github.com/dpwspoon))
- KG-14359: fixed problem with 000 root-directory permissions [\#4](https://github.com/kaazing/gateway/pull/4) ([davecombs](https://github.com/davecombs))
- KG-13959: gateway.distribution with Command Center data coming from a bower-based project [\#3](https://github.com/kaazing/gateway/pull/3) ([davecombs](https://github.com/davecombs))
- Using repository's NOTICE.txt in distribution as well [\#2](https://github.com/kaazing/gateway/pull/2) ([jitsni](https://github.com/jitsni))
- Modifying NOTICE.txt with all the info [\#1](https://github.com/kaazing/gateway/pull/1) ([jitsni](https://github.com/jitsni))



\* *This Change Log was automatically generated by [github_changelog_generator](https://github.com/skywinder/Github-Changelog-Generator)*
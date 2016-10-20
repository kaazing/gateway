Analysis of netty-3.10.5.Final code compared to mina.netty
==========================================================
mina.netty contains some classes copied from Netty and then altered. This file documents what changes were made.

Note that some of our copies of the Netty classes were copied from Netty version 3.6.3.Final.
We only later upgraded our dependency to 3.10.5.Final. One purpose of this document is to check whether we pulled
all the Netty changes between 3.6.3 and 3.10.5 into our copies.

Netty version 3.6.3.Final is tag netty-3.6.3.Final (commit d70e3c1561f9)

All classes are in sync with Netty 3.5.10.Final unless otherwise marked with an asterisk below.

AbstractChannel
(copied from netty 3.10.5.Final)
- mina.netty adds a single line code change to ensure id is positive

AbstractNioChannel
(originally copied from netty 3.6.3.Final, all desirable changes from netty 3.10.5.Final have been incorporated)
- mina.netty adds setWorker method (for thread migration)
- mina.netty version deliberately does not include the changes from the following commit:
  452df045 Always fire interestChanged from IO thread
  because in Kaazing the poll and offer methods on the WriteRequestQueue will always be called on the IO thread
  so there is no need to incur the overhead of handling potential calls from other threads.

*AbstractNioSelector (not in sync)
(appears to be based on netty-3.6.3.Final with no attempt at pulling in changes from 3.10.5.Final)
- mina.netty version adds cap on processTaskQueue duration plus some diagnostic log messages
- mina.netty version adds processRead for udp
- netty version now has no mention of the epoll bug
- netty version has different logic in run() to tell if key should be cancelled (inc. fix for https://github.com/netty/netty/issues/2931)
- netty version has logic in run() to deal with case where thread was interrupted during select (another bug fix)
- netty version uses SelectorUtil.open() instead of Selector.open()

AbstractNioWorker
(originally copied from netty 3.6.3.Final, all of the changes from netty 3.10.5.Final have been incorporated)
- mina.netty version adds support for udp
- mina.netty version adds deregister and register methods for thread migration
- mina.netty version reduces GC by using a single writeCompletionEvent object
- mina.netty version checks worker != null in isIoThread for migration support

NioClientBoss
(copied from netty-3.10.5.Final)
- mina.netty version adds int return from process (workDone)

NioDatagramChannel
(copied from netty-3.10.5.Final)
- mina.netty version adds a getWorker method
- mina.netty version moves fireChannelConnected from NioDatagramPipelineSink to NioDatagramWorker

NioDatagramPipelineSink
(copied from netty-3.10.5.Final)
- mina.netty version moves fireChannelConnected from NioDatagramPipelineSink to NioDatagramWorker

NioServiceBoss
(originally copied from netty 3.6.3.Final, but no changes were made in Netty between that and 3.10.5.Final)
- mina.netty version has a major fix to close() method for Windows (which also necessitated changes to process method).

NioWorker
(originally copied from netty 3.6.3.Final, but the only change made between that and 3.10.5.Final has been applied)
- mina.netty version has extra code added to limit how long we spend in processTasks() to avoid a lengthy task queue from excessively delaying processing of I/O events.

NioChildDatagramChannel
NioChildDatagramPipelineSink
NioClientDatagramChannelFactory
NioDatagramBossPool
NioServerDatagramBoss
NioServerDatagramChannelFactory
- do not exist in netty 3.6.3.Final


Analysis of netty-3.10.5.Final code compared to mina.netty
==========================================================
mina.netty contains some classes copied from Netty and then altered. This file documents what changes were made.

Note that some of our copies of the Netty classes were copied from Netty version 3.6.3.Final.
Netty version 3.6.3.Final is tag netty-3.6.3.Final (commit d70e3c1561f9).
We only later upgraded our dependency to 3.10.5.Final (tag netty-3.10.5.Final).
All changes made in Netty between 3.6.3 and 3.10.5 have now been applied to our copies.
So if our copies are diff'd against https://github.com/netty/netty/tree/netty-3.10.5.Final
the changes seen are only those we deliberaately made for Kaazing usage.

AbstractChannel
- mina.netty adds a single line code change to ensure id is positive

AbstractNioChannel
- mina.netty adds setWorker method (for thread migration)
- mina.netty version deliberately does not include the changes from the following commit:
  452df045 Always fire interestChanged from IO thread
  because in Kaazing the poll and offer methods on the WriteRequestQueue will always be called on the IO thread
  so there is no need to incur the overhead of handling potential calls from other threads.

AbstractNioSelector
- mina.netty version adds cap on processTaskQueue duration plus some diagnostic log messages
- mina.netty version adds processRead for udp
- mina.netty version only walks through keys and tests for thread interrupted in case where EPOLL_WORKAROUND 
  is enabled. This is an optimization compared to Netty 3.10.5 and was done while apply Netty commit 
  cfa10742 "[#2426] Not cause busy loop when interrupt Thread of AbstractNioSelector".

AbstractNioWorker
- mina.netty version adds support for udp
- mina.netty version adds deregister and register methods for thread migration
- mina.netty version reduces GC by using a single writeCompletionEvent object
- mina.netty version checks worker != null in isIoThread for migration support

NioClientBoss
- mina.netty version adds int return from process (workDone)

NioDatagramChannel
- mina.netty version adds a getWorker method
- mina.netty version moves fireChannelConnected from NioDatagramPipelineSink to NioDatagramWorker

NioDatagramPipelineSink
- mina.netty version moves fireChannelConnected from NioDatagramPipelineSink to NioDatagramWorker

NioServiceBoss
- mina.netty version has a major fix to close() method for Windows (which also necessitated changes to process method).

NioWorker
- mina.netty version has extra code added to limit how long we spend in processTasks() to avoid a lengthy task queue from excessively delaying processing of I/O events.

SelectorUtil
- mina.netty version adds method select(Selector selector, long timeout)

SocketSendBufferPool
- mina.netty version has changes to reduce GC by using shared objects (send buffer) when possible.

NioChildDatagramChannel
NioChildDatagramPipelineSink
NioClientDatagramChannelFactory
NioDatagramBossPool
NioServerDatagramBoss
NioServerDatagramChannelFactory
- do not exist in netty 3.6.3.Final


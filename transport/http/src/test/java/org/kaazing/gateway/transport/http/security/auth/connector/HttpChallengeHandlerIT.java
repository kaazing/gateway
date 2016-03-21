package org.kaazing.gateway.transport.http.security.auth.connector;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.Assert.assertTrue;
import static org.kaazing.gateway.transport.http.HttpMethod.GET;
import static org.kaazing.test.util.ITUtil.createRuleChain;

import java.util.concurrent.CountDownLatch;

import org.apache.mina.core.future.ConnectFuture;
import org.apache.mina.core.service.IoHandler;
import org.apache.mina.core.session.IdleStatus;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.core.session.IoSessionInitializer;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.api.Invocation;
import org.jmock.lib.action.CustomAction;
import org.jmock.lib.concurrent.Synchroniser;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.kaazing.gateway.transport.http.HttpConnectSession;
import org.kaazing.gateway.transport.http.HttpConnectorRule;
import org.kaazing.gateway.transport.http.HttpSession;
import org.kaazing.gateway.transport.http.HttpStatus;
import org.kaazing.k3po.junit.annotation.Specification;
import org.kaazing.k3po.junit.rules.K3poRule;
import org.kaazing.mina.core.session.IoSessionEx;

public class HttpChallengeHandlerIT {

    private final HttpConnectorRule connector = new HttpConnectorRule();
    private final K3poRule k3po = new K3poRule();

    @Rule
    public TestRule chain = createRuleChain(connector, k3po);

    private Mockery context;

    @Before
    public void initialize() {
        context = new Mockery();
        context.setThreadingPolicy(new Synchroniser());
    }

    @Specification("basic.challenge.and.accept")
    @Test
    public void basicChallengeAndAccept() throws Exception {
        final IoHandler handler = new IoHandler() {

            @Override
            public void sessionOpened(IoSession arg0) throws Exception {
                System.out.println("sessionOpened");
                HttpSession httpSession = (HttpSession) arg0;
                HttpStatus status = httpSession.getStatus();
                System.out.println("status: " + status);
            }

            @Override
            public void sessionIdle(IoSession arg0, IdleStatus arg1) throws Exception {
                System.out.println("sessionIdle");
            }

            @Override
            public void sessionCreated(IoSession arg0) throws Exception {
                System.out.println("sessionCreated");
                HttpSession httpSession = (HttpSession) arg0;
                HttpStatus status = httpSession.getStatus();
                System.out.println("status: " + status);
            }

            @Override
            public void sessionClosed(IoSession arg0) throws Exception {
                System.out.println("sessionClosed");
                HttpSession httpSession = (HttpSession) arg0;
                HttpStatus status = httpSession.getStatus();
                System.out.println("status: " + status);
            }

            @Override
            public void messageSent(IoSession arg0, Object arg1) throws Exception {
                System.out.println("messageSent");
            }

            @Override
            public void messageReceived(IoSession arg0, Object arg1) throws Exception {
                System.out.println("messageReceived");
            }

            @Override
            public void exceptionCaught(IoSession arg0, Throwable arg1) throws Exception {
                System.out.println("exceptionCaught");
            }
        };
        // final IoHandler handler = context.mock(IoHandler.class);
        // final CountDownLatch latch = new CountDownLatch(1);
        //
        // context.checking(new Expectations() {
        // {
        // oneOf(handler).sessionCreated(with(any(IoSessionEx.class)));
        // oneOf(handler).sessionOpened(with(any(IoSessionEx.class)));
        // allowing(handler).messageReceived(with(any(IoSessionEx.class)), with(any(Object.class)));
        // allowing(handler).exceptionCaught(with(any(IoSessionEx.class)), with(any(Throwable.class)));
        // oneOf(handler).sessionClosed(with(any(IoSessionEx.class)));
        // will(new CustomAction("Latch countdown") {
        // @Override
        // public Object invoke(Invocation invocation) throws Throwable {
        // latch.countDown();
        // return null;
        // }
        // });
        // }
        // });
        connector.connect("http://localhost:8080/resource", handler, new IoSessionInitializer<ConnectFuture>() {
            @Override
            public void initializeSession(IoSession session, ConnectFuture future) {
                HttpConnectSession connectSession = (HttpConnectSession) session;
                connectSession.setMethod(GET);
            }
        });

        // assertTrue(latch.await(10, SECONDS));
        k3po.finish();
        context.assertIsSatisfied();
    }

}

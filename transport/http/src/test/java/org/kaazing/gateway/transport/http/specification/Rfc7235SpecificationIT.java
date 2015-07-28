package org.kaazing.gateway.transport.http.specification;

import static org.junit.rules.RuleChain.outerRule;
import static java.util.concurrent.TimeUnit.SECONDS;

import java.io.File;
import java.net.URI;

import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.DisableOnDebug;
import org.junit.rules.TestRule;
import org.junit.rules.Timeout;
import org.kaazing.gateway.server.test.GatewayRule;
import org.kaazing.gateway.server.test.config.GatewayConfiguration;
import org.kaazing.gateway.server.test.config.builder.GatewayConfigurationBuilder;
import org.kaazing.k3po.junit.annotation.Specification;
import org.kaazing.k3po.junit.rules.K3poRule;
import org.kaazing.test.util.MethodExecutionTrace;

public class Rfc7235SpecificationIT {

    private TestRule trace = new MethodExecutionTrace();
    private TestRule timeout = new DisableOnDebug(new Timeout(4, SECONDS));
    private final K3poRule robot = new K3poRule().setScriptRoot("org/kaazing/specification/http/rfc7235");

    public GatewayRule gateway = new GatewayRule() { 
       {
           GatewayConfiguration configuration = new GatewayConfigurationBuilder()
                    .webRootDirectory(new File("src/test/webapp"))
                    .service()
                        .accept(URI.create("http://localhost:8000"))
                        .type("directory")
                        .property("directory", "/public")
                        .property("welcome-file", "resource")
                        .realmName("demo")
                        .authorization()
                            .requireRole("AUTHORIZED")
                        .done()
                        .crossOrigin()
                            .allowOrigin("*")
                        .done()
                    .done()
                    .security()
                        .realm()
                            .name("demo")
                            .description("Kaazing Gateway Demo")
                            .httpChallengeScheme("Basic")
                            .authorizationMode("challenge")
                            .loginModule()
                                 .type("file")
                                 .success("required")
                                 .option("file", "src/test/resources/gateway/conf/jaas-config.xml")
                            .done()
                        .done()
                    .done()
                .done();
            init(configuration);
        }
    };

    @Rule
    public TestRule chain = outerRule(trace).around(robot).around(gateway).around(timeout);

    @Test
    @Specification("status/valid.credentials/request")
    public void shouldRespond200WithValidCredentials() throws Exception {
        robot.finish();
    }

    @Test
    @Specification("status/multiple.invalid.requests/request")
    public void shouldRespondWithMultiple401sWithMultipleInvalidRequests() throws Exception {
        robot.finish();
    }

    @Test
    @Specification("headers/invalid.user/request")
    public void shouldRespond401ToInvalidUser() throws Exception {
        robot.finish();
    }

    @Ignore("Gateway doesn't respond with Forbidden Status code.(403)")
    @Test
    @Specification("framework/forbidden/request")
    public void shouldRespondWithForbiddenStatusCode() throws Exception {
        robot.finish();
    }

    @Test
    @Specification("framework/invalid.then.valid.credentials/request")
    public void shouldRespondWithUnauthorizedStatusCodeWithInvalidCredentials() throws Exception {
        robot.finish();
    }

    @Test
    @Specification("framework/missing.then.valid.credentials/request")
    public void shouldRespondWithUnauthorizedStatusCodeWithMissingCredentials() throws Exception {
        robot.finish();
    }

    @Test
    @Specification("framework/partial.then.valid.credentials/request")
    public void shouldRespondWithUnauthorizedStatusCodeWithPartialCredentials() throws Exception {
        robot.finish();
    }

    @Ignore("Gateway doesn't recognize proxy requests?")
    @Test
    @Specification("headers/invalid.proxy.user/request")
    public void secureProxyShouldSend407ToAnyUnAuthorizedRequest() throws Exception {
        robot.finish();
    }

}


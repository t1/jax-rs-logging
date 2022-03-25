package test;

import com.github.t1.testcontainers.jee.JeeContainer;
import com.github.t1.testcontainers.tools.LogLine;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import test.Ping.Payload;

import static com.github.t1.testcontainers.jee.AddLibMod.addLib;
import static com.github.t1.testcontainers.tools.DeployableBuilder.war;
import static javax.ws.rs.client.Entity.json;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON_TYPE;
import static org.assertj.core.api.BDDAssertions.then;
import static org.slf4j.event.Level.DEBUG;
import static org.slf4j.event.Level.INFO;
import static test.CustomAssertions.thenLogsIn;

@Testcontainers
@Slf4j
class InContainerIT {
    @Container static JeeContainer SERVER = JeeContainer.create()
        .withDeployment(war("ROOT").withClasses(Ping.class, Ping.Payload.class, Ping.Api.class, REST.class),
            addLib("target/jax-rs-logging.jar"))
        .withLogLevel(Ping.class, DEBUG)
        .withLogLevel(Ping.Api.class, DEBUG)
        .withMainPortBoundToFixedPort(8080); // makes manual testing and debugging easier

    @Test void shouldPing() {
        var webTarget = SERVER.target().path("ping");
        log.debug("ping {}", webTarget.getUri());

        var pong = webTarget.request(APPLICATION_JSON_TYPE).post(json(new Payload("test"))).readEntity(String.class);

        log.debug("ping returned {}", pong);
        then(pong).isEqualTo("{\"payload\":\"pong:test\"}");
        thenLogsIn(SERVER)
            .hasFollowingMessage("got POST request http://localhost:8080/ping")
            .hasFollowingMessage(">>> Accept: application/json")
            .hasFollowingMessage(">>> Content-Type: application/json")
            .hasFollowingMessage(">>> {\"payload\":\"test\"}")
            .hasFollowing(LogLine.message("got pinged Ping.Payload(payload=test)").withLevel(INFO).withLogger(Ping.class.getName()))
            .hasFollowingMessage("sending response for POST http://localhost:8080/ping")
            .hasFollowingMessage("<<< Status: 200 OK")
            .hasFollowingMessage("<<< Content-Type: application/json")
            .hasFollowingMessage("<<< {\"payload\":\"pong:test\"}");
    }

    @Test void shouldGetIndirectPing() {
        var webTarget = SERVER.target().path("ping/indirect");
        log.debug("indirect ping: {}", webTarget.getUri());
        var pong = webTarget.request(APPLICATION_JSON_TYPE).get(String.class);

        log.debug("ping returned {}", pong);
        then(pong).isEqualTo("indirect:pong:indirect");
        thenLogsIn(SERVER).thread("default task-1")
            .hasFollowingMessage("got GET request http://localhost:8080/ping/indirect")
            .hasFollowingMessage(">>> Accept: application/json")
            //
            .hasFollowingMessage("got indirect")
            //
            .hasFollowingMessage("sending POST request http://localhost:8080/ping")
            .hasFollowingMessage(">> Accept: application/json")
            .hasFollowingMessage(">> Content-Type: application/json")
            .hasFollowingMessage(">> {\"payload\":\"indirect\"}")
            //
            .hasFollowingMessage("got response for POST http://localhost:8080/ping")
            .hasFollowingMessage("<< Status: 200 OK")
            .hasFollowingMessage("<< Content-Type: application/json")
            .hasFollowingMessage("<< {\"payload\":\"pong:indirect\"}")
            //
            .hasFollowingMessage("sending response for GET http://localhost:8080/ping/indirect")
            .hasFollowingMessage("<<< Status: 200 OK")
            .hasFollowingMessage("<<< Content-Type: application/json")
            .hasFollowingMessage("<<< indirect:pong:indirect");
        thenLogsIn(SERVER).thread("default task-2")
            .hasFollowingMessage("got POST request http://localhost:8080/ping")
            .hasFollowingMessage(">>> Accept: application/json")
            .hasFollowingMessage(">>> Content-Type: application/json")
            .hasFollowingMessage(">>> {\"payload\":\"indirect\"}")
            //
            .hasFollowingMessage("got pinged Ping.Payload(payload=indirect)")
            //
            .hasFollowingMessage("sending response for POST http://localhost:8080/ping")
            .hasFollowingMessage("<<< Status: 200 OK")
            .hasFollowingMessage("<<< Content-Type: application/json")
            .hasFollowingMessage("<<< {\"payload\":\"pong:indirect\"}");
    }
}

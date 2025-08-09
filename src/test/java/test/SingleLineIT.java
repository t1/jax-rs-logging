package test;

import com.github.t1.testcontainers.jee.JeeContainer;
import com.github.t1.testcontainers.jee.WildflyContainer;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static com.github.t1.logging.clientfilter.LoggingTools.SINGLE;
import static com.github.t1.testcontainers.jee.AddLibMod.addLib;
import static com.github.t1.testcontainers.tools.DeployableBuilder.war;
import static jakarta.ws.rs.client.Entity.json;
import static jakarta.ws.rs.core.HttpHeaders.AUTHORIZATION;
import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON_TYPE;
import static jakarta.ws.rs.core.MediaType.TEXT_PLAIN_TYPE;
import static org.assertj.core.api.BDDAssertions.then;
import static org.slf4j.event.Level.DEBUG;

@SuppressWarnings("resource")
@Testcontainers
@Slf4j
class SingleLineIT {
    public static final String FOO_BAR = "Zm9vOmJhcg=="; // foo:bar

    @Container
    static JeeContainer SERVER = WildflyContainer.create()
            .withDeployment(war("ROOT").withClasses(Ping.class, Ping.Payload.class, Ping.Api.class, REST.class),
                    addLib("target/jax-rs-logging.jar"))
            // container/server side
            .withLogLevel(Ping.class.getName() + ".indirect" + SINGLE, DEBUG)
            .withLogLevel(Ping.class.getName() + ".ping" + SINGLE, DEBUG)
            .withLogLevel(Ping.class.getName() + SINGLE, DEBUG) // the text ping via the class logger
            // client side
            .withLogLevel(Ping.Api.class.getName() + ".ping" + SINGLE, DEBUG)
            .withLogLevel(Ping.Api.class.getName() + ".indirect" + SINGLE, DEBUG)
            //
            .withMainPortBoundToFixedPort(8080) // makes manual testing and debugging easier
            .withPortBoundToFixedPort(8787, 8787) // debug
            .withPortBoundToFixedPort(9990, 9990); // management

    @Test
    void shouldPing() {
        var webTarget = SERVER.target().path("ping");
        log.debug("ping {}", webTarget.getUri());

        var pong = webTarget.request(APPLICATION_JSON_TYPE)
                .header(AUTHORIZATION, "Basic " + FOO_BAR)
                .post(json(new Ping.Payload("test")))
                .readEntity(String.class);

        then(pong).isEqualTo("{\"payload\":\"pong:test\"}");
        then(SERVER.getLogs()).contains("""
                        got POST request http://localhost:8080/ping
                        >>> Accept: application/json
                        >>> Authorization: <hidden>
                        """)
                .contains(">>> Content-Type: application/json\n")
                .contains(">>> {\"payload\":\"test\"}\n")
                .contains("got pinged Ping.Payload(payload=test)")
                .contains("""
                        sending response for POST http://localhost:8080/ping
                        <<< Status: 200 OK
                        <<< Content-Type: application/json
                        """)
                .contains("<<< {\"payload\":\"pong:test\"}\n");
        then(SERVER.getLogs()).doesNotContain(FOO_BAR);
    }

    @Test
    void shouldTextPing() {
        var webTarget = SERVER.target().path("ping");
        log.debug("text ping {}", webTarget.getUri());

        var pong = webTarget.request()
                .header(AUTHORIZATION, "Basic " + FOO_BAR)
                .get()
                .readEntity(String.class);

        then(pong).isEqualTo("pong");
        then(SERVER.getLogs()).contains("""
                        got GET request http://localhost:8080/ping
                        >>> Authorization: <hidden>
                        """)
                .doesNotContain(">>> Accept: ")
                .contains("got pinged for text")
                .contains("""
                        sending response for GET http://localhost:8080/ping
                        <<< Status: 200 OK
                        <<< Content-Type: text/plain;charset=UTF-8
                        """);
    }

    @Test
    void shouldGetIndirectPing() {
        var webTarget = SERVER.target().path("ping/indirect");
        log.debug("indirect ping: {}", webTarget.getUri());
        var pong = webTarget.request(TEXT_PLAIN_TYPE).get(String.class);

        then(pong).isEqualTo("indirect:pong:indirect");
        // the client-side POST (with body) is logged when the stream is closed,
        // which happens after the server-side has sent the response... looks weird, but is correct
        then(SERVER.getLogs())
                .contains("""
                        DEBUG [test.Ping.indirect..single] (default task-2) got GET request http://localhost:8080/ping/indirect
                        >>> Accept: text/plain
                        """)
                .contains("INFO  [test.Ping] (default task-2) got indirect")
                .contains("""
                        DEBUG [test.Ping.ping..single] (default task-3) got POST request http://localhost:8080/ping
                        >>> Accept: application/json
                        >>> Authorization: foo:<hidden>
                        """)
                .contains(">>> Content-Type: application/json")
                .contains(">>> {\"payload\":\"indirect\"}")
                .contains("INFO  [test.Ping] (default task-3) got pinged Ping.Payload(payload=indirect)")
                .contains("""
                        DEBUG [test.Ping.ping..single] (default task-3) sending response for POST http://localhost:8080/ping
                        <<< Status: 200 OK
                        <<< Content-Type: application/json
                        <<< {"payload":"pong:indirect"}
                        """)
                .contains("""
                        DEBUG [test.Ping$Api.ping..single] (default task-2) sending POST request http://localhost:8080/ping
                        >> Accept: application/json
                        >> Authorization: foo:<hidden>
                        >> Content-Type: application/json
                        >> {"payload":"indirect"}
                        """)
                .contains("""
                        DEBUG [test.Ping$Api.ping..single] (default task-2) got response for POST http://localhost:8080/ping
                        << Status: 200 OK
                        << Connection: keep-alive
                        << Content-Type: application/json
                        """)
                .contains("<< {\"payload\":\"pong:indirect\"}")
                .contains("""
                        DEBUG [test.Ping.indirect..single] (default task-2) sending response for GET http://localhost:8080/ping/indirect
                        <<< Status: 200 OK
                        <<< Content-Type: text/plain;charset=UTF-8
                        <<< indirect:pong:indirect
                        """);
    }
}

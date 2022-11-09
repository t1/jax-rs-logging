package test;

import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import org.eclipse.microprofile.rest.client.inject.RestClient;

import static jakarta.ws.rs.core.HttpHeaders.AUTHORIZATION;
import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;

@Path("/ping")
@Slf4j
public class Ping {
    static final String LONG_AUTH = "Basic Zm9vOjEyMzQ1Njc4OTAxMjM0NTY="; // foo:1234567890123456

    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    @POST public Payload ping(Payload in) {
        log.info("got pinged {}", in);
        return new Payload("pong:" + ((in == null) ? null : in.getPayload()));
    }

    @AllArgsConstructor @NoArgsConstructor(force = true)
    @Data public static class Payload {
        String payload;
    }

    @RegisterRestClient(baseUri = "http://localhost:8080/ping")
    public interface Api {
        @POST Payload ping(@HeaderParam(AUTHORIZATION) String auth, Payload in);
    }

    @Inject @RestClient Api api;

    @Path("/indirect")
    @GET public String indirect() {
        log.info("got indirect");
        return "indirect:" + api.ping(LONG_AUTH, new Payload("indirect")).payload;
    }
}

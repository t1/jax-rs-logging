package test;

import jakarta.inject.Inject;
import jakarta.ws.rs.BadRequestException;
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
import static jakarta.ws.rs.core.MediaType.APPLICATION_OCTET_STREAM;
import static jakarta.ws.rs.core.MediaType.TEXT_PLAIN;
import static java.nio.charset.StandardCharsets.UTF_8;

@Path("/ping")
@Slf4j
public class Ping {
    static final String LONG_AUTH = "Basic Zm9vOjEyMzQ1Njc4OTAxMjM0NTY="; // foo:1234567890123456

    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    @POST
    public Payload ping(Payload in) {
        log.info("got pinged {}", in);
        return new Payload("pong:" + ((in == null) ? null : in.getPayload()));
    }

    @GET
    @Produces(TEXT_PLAIN)
    public String textPing() {
        log.info("got pinged for text");
        return "pong";
    }

    @POST
    @Consumes(APPLICATION_OCTET_STREAM)
    @Produces(APPLICATION_OCTET_STREAM)
    public byte[] binaryPing(byte[] in) {
        log.info("got binary pinged: {}", new String(in, UTF_8));
        return "binary-pong".getBytes(UTF_8);
    }

    @GET
    @Path("/failing")
    public Payload failing() {
        log.info("got pinged for failing");
        throw new BadRequestException("failing");
    }

    @AllArgsConstructor
    @NoArgsConstructor(force = true)
    @Data
    public static class Payload {
        String payload;
    }

    @RegisterRestClient(baseUri = "http://localhost:8080/ping")
    public interface Api {
        @POST
        Payload ping(@HeaderParam(AUTHORIZATION) String auth, Payload in);

        @POST
        @Consumes(APPLICATION_OCTET_STREAM)
        @Produces(APPLICATION_OCTET_STREAM)
        byte[] binaryPing(@HeaderParam(AUTHORIZATION) String auth, byte[] in);
    }

    @Inject
    @RestClient
    Api api;

    @Path("/indirect")
    @GET
    @Produces(TEXT_PLAIN)
    public String indirect() {
        log.info("got indirect");
        return "indirect:" + api.ping(LONG_AUTH, new Payload("indirect")).payload;
    }

    @Path("/indirect")
    @POST
    @Consumes(APPLICATION_OCTET_STREAM)
    @Produces(APPLICATION_OCTET_STREAM)
    public byte[] binaryIndirect(byte[] in) {
        log.info("got binary indirect: {}", new String(in, UTF_8));
        var indirect = new String(api.binaryPing(LONG_AUTH, "indirect".getBytes(UTF_8)), UTF_8);
        log.info("got binary indirect response: {}", indirect);
        return ("indirect:binary:" + indirect).getBytes(UTF_8);
    }
}

package com.github.t1.logging.clientfilter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientRequestFilter;
import javax.ws.rs.client.ClientResponseContext;
import javax.ws.rs.client.ClientResponseFilter;
import javax.ws.rs.core.MediaType;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.nio.charset.Charset;

import static java.nio.charset.StandardCharsets.ISO_8859_1;
import static javax.ws.rs.core.MediaType.CHARSET_PARAMETER;
import static javax.ws.rs.core.MediaType.TEXT_PLAIN_TYPE;

/**
 * <p>
 * Note: there is no <code>&#64;Provider</code> annotation, as we register it via the {@link RegisterLoggingClientFilter},
 * which works also when CDI is not available, and we don't want to have it registered twice.
 */
public class LoggingClientFilter implements ClientRequestFilter, ClientResponseFilter {
    private static final String LOGGING_OUTPUT_STREAM_PROPERTY = LoggingOutputStream.class.getName();

    @Override public void filter(ClientRequestContext requestContext) {
        var log = getLog(requestContext);
        if (!log.isDebugEnabled())
            return;
        log.debug("sending {} request {}", requestContext.getMethod(), requestContext.getUri());
        requestContext.getStringHeaders().forEach((name, values) -> log.debug(">> {}: {}", name,
            "Authorization".equals(name) ? "*** censored *** " : String.join(" ", values)));
        if (requestContext.hasEntity() && isLoggable(requestContext.getMediaType())) {
            OutputStream stream = new LoggingOutputStream(requestContext.getEntityStream(), ">>", log);
            requestContext.setProperty(LOGGING_OUTPUT_STREAM_PROPERTY, stream);
            requestContext.setEntityStream(stream);
        }
    }

    @Override public void filter(ClientRequestContext requestContext, ClientResponseContext responseContext) throws IOException {
        var log = getLog(requestContext);
        if (!log.isDebugEnabled())
            return;
        var loggingOutputStream = (LoggingOutputStream) requestContext.getProperty(LOGGING_OUTPUT_STREAM_PROPERTY);
        if (loggingOutputStream != null)
            loggingOutputStream.close();

        log.debug("got response for {} {}", requestContext.getMethod(), requestContext.getUri());
        log.debug("<< Status: {} {}", responseContext.getStatus(), responseContext.getStatusInfo().getReasonPhrase());
        responseContext.getHeaders().forEach((name, values) -> log.debug("<< {}: {}", name, String.join(" ", values)));
        if (log.isDebugEnabled() && responseContext.hasEntity() && isLoggable(responseContext.getMediaType())) {
            var charset = Charset.forName(responseContext.getMediaType().getParameters().getOrDefault(CHARSET_PARAMETER, ISO_8859_1.name()));
            var entity = new String(responseContext.getEntityStream().readAllBytes(), charset);
            entity.lines().forEach(line -> log.debug("<< {}", line));
            responseContext.setEntityStream(new ByteArrayInputStream(entity.getBytes(charset)));
        }
    }

    private Logger getLog(ClientRequestContext requestContext) {
        var properties = requestContext.getConfiguration().getProperties();
        var method = (Method) properties.get("org.eclipse.microprofile.rest.client.invokedMethod");
        var loggerClass = (method == null) ? LoggingClientFilter.class : method.getDeclaringClass();
        return LoggerFactory.getLogger(loggerClass);
    }

    private boolean isLoggable(MediaType mediaType) {
        return isApplication(mediaType, "json")
               || isApplication(mediaType, "xml")
               || mediaType.isCompatible(TEXT_PLAIN_TYPE);
    }

    private boolean isApplication(MediaType mediaType, String subType) {
        return mediaType.getType().equals("application")
               && (mediaType.getSubtype().equals(subType) || mediaType.getSubtype().endsWith("+" + subType));
    }
}

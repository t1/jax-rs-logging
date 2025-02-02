package com.github.t1.logging.clientfilter;

import jakarta.annotation.Priority;
import jakarta.ws.rs.client.ClientRequestContext;
import jakarta.ws.rs.client.ClientRequestFilter;
import jakarta.ws.rs.client.ClientResponseContext;
import jakarta.ws.rs.client.ClientResponseFilter;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Method;

import static com.github.t1.logging.clientfilter.LoggingTools.isLoggable;
import static com.github.t1.logging.clientfilter.LoggingTools.merge;
import static com.github.t1.logging.clientfilter.LoggingTools.safe;
import static jakarta.ws.rs.Priorities.USER;

/**
 * Note: there is no <code>&#64;Provider</code> annotation, as we register it via the {@link RegisterLoggingClientFilter},
 * which works also when CDI is not available (e.g. in tests), and we don't want to have it registered twice.
 */
@Priority(USER + 900)
public class LoggingClientFilter implements ClientRequestFilter, ClientResponseFilter {
    private static final String LOGGING_OUTPUT_STREAM_PROPERTY = LoggingOutputStream.class.getName();

    @Override
    public void filter(ClientRequestContext requestContext) {
        var log = getLog(requestContext);
        try {
            if (log.off()) return; // the OFF logger doesn't have to be closed
            log.debug("sending {} request {}", requestContext.getMethod(), requestContext.getUri());
            requestContext.getStringHeaders().forEach((name, values) -> log.debug(">> {}: {}", name, safe(name, values)));
            if (requestContext.hasEntity() && isLoggable(requestContext.getMediaType())) {
                // if we need to log the entity, we close the log when the stream is closed
                try {
                    var entityStream = requestContext.getEntityStream();
                    OutputStream stream = new LoggingOutputStream(entityStream, ">>", log, log::close);
                    requestContext.setProperty(LOGGING_OUTPUT_STREAM_PROPERTY, stream);
                    requestContext.setEntityStream(stream);
                } catch (RuntimeException e) {
                    log.debug("can't read entity stream... will log toString. Cause: {}", e.toString());
                    log.debug(">> {}", requestContext.getEntity());
                }
            } else {
                // otherwise we close it right away
                log.close();
            }
        } catch (RuntimeException e) {
            log.warn("error logging client request", e);
        }
    }

    @Override
    public void filter(ClientRequestContext requestContext, ClientResponseContext responseContext) throws IOException {
        var log = getLog(requestContext);
        try {
            if (log.off()) return;
            var loggingOutputStream = (LoggingOutputStream) requestContext.getProperty(LOGGING_OUTPUT_STREAM_PROPERTY);
            if (loggingOutputStream != null)
                loggingOutputStream.close();

            log.debug("got response for {} {}", requestContext.getMethod(), requestContext.getUri());
            log.debug("<< Status: {} {}", responseContext.getStatus(), responseContext.getStatusInfo().getReasonPhrase());
            var headers = responseContext.getHeaders();
            if (headers != null)
                headers.forEach((name, values) -> log.debug("<< {}: {}", name, merge(values)));
            if (responseContext.hasEntity() && isLoggable(responseContext.getMediaType())) {
                var charset = LoggingTools.charset(responseContext.getMediaType());
                var entity = new String(responseContext.getEntityStream().readAllBytes(), charset);
                entity.lines().forEach(line -> log.debug("<< {}", line));
                responseContext.setEntityStream(new ByteArrayInputStream(entity.getBytes(charset)));
            }
        } catch (RuntimeException e) {
            log.warn("error logging client response", e);
        } finally {
            try {
                log.close();
            } catch (RuntimeException e2) {
                log.warn("error closing log", e2);
            }
        }
    }

    private LogWrapper getLog(ClientRequestContext requestContext) {
        var properties = requestContext.getConfiguration().getProperties();
        var method = (Method) properties.get("org.eclipse.microprofile.rest.client.invokedMethod");
        var loggerName = (method == null) ? LoggingClientFilter.class.getName()
                : method.getDeclaringClass().getName() + "." + method.getName();
        return LogWrapper.of(loggerName);
    }
}

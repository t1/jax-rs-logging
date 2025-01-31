package com.github.t1.logging.clientfilter;

import jakarta.annotation.Priority;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.ext.Provider;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.lang.reflect.Method;

import static com.github.t1.logging.clientfilter.LoggingTools.isLoggable;
import static com.github.t1.logging.clientfilter.LoggingTools.merge;
import static com.github.t1.logging.clientfilter.LoggingTools.safe;
import static jakarta.ws.rs.Priorities.USER;

@Provider
@Priority(USER + 900)
public class LoggingContainerFilter implements ContainerRequestFilter, ContainerResponseFilter {
    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        try (var log = getLog(requestContext)) {
            if (log.off()) return;
            log.debug("got {} request {}", requestContext.getMethod(), requestContext.getUriInfo().getRequestUri());
            requestContext.getHeaders().forEach((name, values) -> log.debug(">>> {}: {}", name, safe(name, values)));
            if (requestContext.hasEntity() && isLoggable(requestContext.getMediaType())) {
                var charset = LoggingTools.charset(requestContext.getMediaType());
                var entity = new String(requestContext.getEntityStream().readAllBytes(), charset);
                entity.lines().forEach(line -> log.debug(">>> {}", line));
                requestContext.setEntityStream(new ByteArrayInputStream(entity.getBytes(charset)));
            }
        }
    }

    @Override
    public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) {
        var log = getLog(requestContext);
        try {
            if (log.off()) return;
            log.debug("sending response for {} {}", requestContext.getMethod(), requestContext.getUriInfo().getRequestUri());
            log.debug("<<< Status: {} {}", responseContext.getStatus(), responseContext.getStatusInfo().getReasonPhrase());
            responseContext.getStringHeaders().forEach((name, values) -> log.debug("<<< {}: {}", name, merge(values)));
            if (responseContext.hasEntity() && isLoggable(responseContext.getMediaType())) {
                // if we need to log the entity, we close the log when the stream is closed
                responseContext.setEntityStream(new LoggingOutputStream(responseContext.getEntityStream(), "<<<", log, log::close));
            } else {
                // otherwise we close it right away
                log.close();
            }
        } catch (RuntimeException e) {
            log.warn("error logging response", e);
            try {
                log.close();
            } catch (RuntimeException e2) {
                log.warn("error closing log", e2);
            }
        }
    }

    private LogWrapper getLog(ContainerRequestContext requestContext) {
        var loggerName = LoggingContainerFilter.class.getName();
        var resourceMethodInvoker = requestContext.getProperty("org.jboss.resteasy.core.ResourceMethodInvoker");
        if (resourceMethodInvoker != null) {
            var method = getMethod(resourceMethodInvoker);
            if (method != null) {
                loggerName = method.getDeclaringClass().getName() + "." + method.getName();
            }
        }
        return LogWrapper.of(loggerName);
    }

    private Method getMethod(Object resourceMethodInvoker) {
        try {
            var method = resourceMethodInvoker.getClass().getMethod("getMethod");
            return (Method) method.invoke(resourceMethodInvoker);
        } catch (ReflectiveOperationException e) {
            return null;
        }
    }
}

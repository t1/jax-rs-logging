package com.github.t1.logging.clientfilter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Priority;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.ext.Provider;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.charset.Charset;

import static com.github.t1.logging.clientfilter.LoggingTools.merge;
import static com.github.t1.logging.clientfilter.LoggingTools.safe;
import static java.nio.charset.StandardCharsets.ISO_8859_1;
import static javax.ws.rs.Priorities.USER;
import static javax.ws.rs.core.MediaType.CHARSET_PARAMETER;
import static javax.ws.rs.core.MediaType.TEXT_PLAIN_TYPE;

@Provider
@Priority(USER + 900)
public class LoggingContainerFilter implements ContainerRequestFilter, ContainerResponseFilter {
    @Override public void filter(ContainerRequestContext requestContext) throws IOException {
        var log = getLog(requestContext);
        if (!log.isDebugEnabled())
            return;
        log.debug("got {} request {}", requestContext.getMethod(), requestContext.getUriInfo().getRequestUri());
        requestContext.getHeaders().forEach((name, values) -> log.debug(">>> {}: {}", name, safe(name, values)));
        if (log.isDebugEnabled() && requestContext.hasEntity() && isLoggable(requestContext.getMediaType())) {
            var charset = Charset.forName(requestContext.getMediaType().getParameters().getOrDefault(CHARSET_PARAMETER, ISO_8859_1.name()));
            var entity = new String(requestContext.getEntityStream().readAllBytes(), charset);
            entity.lines().forEach(line -> log.debug(">>> {}", line));
            requestContext.setEntityStream(new ByteArrayInputStream(entity.getBytes(charset)));
        }
    }

    @Override public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) {
        var log = getLog(requestContext);
        if (!log.isDebugEnabled())
            return;

        log.debug("sending response for {} {}", requestContext.getMethod(), requestContext.getUriInfo().getRequestUri());
        log.debug("<<< Status: {} {}", responseContext.getStatus(), responseContext.getStatusInfo().getReasonPhrase());
        responseContext.getStringHeaders().forEach((name, values) -> log.debug("<<< {}: {}", name, merge(values)));
        if (log.isDebugEnabled() && responseContext.hasEntity() && isLoggable(responseContext.getMediaType())) {
            responseContext.setEntityStream(new LoggingOutputStream(responseContext.getEntityStream(), "<<<", log));
        }
    }

    private Logger getLog(ContainerRequestContext requestContext) {
        Class<?> loggerClass = LoggingContainerFilter.class;
        var resourceMethodInvoker = requestContext.getProperty("org.jboss.resteasy.core.ResourceMethodInvoker");
        if (resourceMethodInvoker != null) {
            var method = getMethod(resourceMethodInvoker);
            if (method != null) {
                loggerClass = method.getDeclaringClass();
            }
        }
        return LoggerFactory.getLogger(loggerClass);
    }

    private Method getMethod(Object resourceMethodInvoker) {
        try {
            var method = resourceMethodInvoker.getClass().getMethod("getMethod");
            return (Method) method.invoke(resourceMethodInvoker);
        } catch (ReflectiveOperationException e) {
            return null;
        }
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

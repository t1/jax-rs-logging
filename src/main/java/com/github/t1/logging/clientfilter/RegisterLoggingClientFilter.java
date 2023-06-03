package com.github.t1.logging.clientfilter;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.rest.client.RestClientBuilder;
import org.eclipse.microprofile.rest.client.spi.RestClientListener;

/**
 * @see LoggingClientFilter
 */
@Slf4j
public class RegisterLoggingClientFilter implements RestClientListener {
    @Override
    public void onNewClient(Class<?> serviceInterface, RestClientBuilder builder) {
        log.debug("register logging client for {}", serviceInterface.getName());
        builder.register(LoggingClientFilter.class);
    }
}

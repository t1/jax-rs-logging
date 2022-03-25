package com.github.t1.logging.clientfilter;

import org.eclipse.microprofile.rest.client.RestClientBuilder;
import org.eclipse.microprofile.rest.client.spi.RestClientListener;

/**
 * @see LoggingClientFilter
 */
public class RegisterLoggingClientFilter implements RestClientListener {
    @Override public void onNewClient(Class<?> serviceInterface, RestClientBuilder builder) {
        builder.register(LoggingClientFilter.class);
    }
}

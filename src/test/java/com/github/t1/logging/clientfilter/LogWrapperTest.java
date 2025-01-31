package com.github.t1.logging.clientfilter;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;

import java.lang.reflect.Proxy;

import static com.github.t1.logging.clientfilter.LoggingTools.format;
import static org.assertj.core.api.Assertions.catchRuntimeException;
import static org.assertj.core.api.BDDAssertions.then;

class LogWrapperTest {
    @Test void shouldLogOff() {
        RuntimeException exception;
        try (var log = new LogWrapper.OffLogWrapper()) {
            exception = catchRuntimeException(() -> log.debug("foo"));
        }

        then(exception).isInstanceOf(UnsupportedOperationException.class).hasMessage("off");
    }

    @Test void shouldLogStandard() {
        var mockLogger = new MockLogger();

        try (var log = new LogWrapper.StandardLogWrapper(mockLogger.logger())) {
            log.debug("foo {}", "bar");
            log.warn("qux");
            log.debug("baz");
        }

        then(mockLogger.toString()).isEqualTo("foo bar\nqux\nbaz\n");
    }

    @Test void shouldLogSingle() {
        var mockLogger = new MockLogger();

        try (var log = new LogWrapper.SingleLogWrapper(mockLogger.logger())) {
            log.debug("foo {}", "bar");
            log.warn("qux");
            log.debug("baz");
        }

        then(mockLogger.toString()).isEqualTo("foo bar\nWARN: qux\nbaz\n");
    }

    private static class MockLogger {
        private final StringBuilder buffer = new StringBuilder();

        @Override public String toString() {return buffer.toString();}

        public Logger logger() {
            return (Logger) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[]{Logger.class},
                    (proxy, method, args) -> {
                        if (method.getDeclaringClass().equals(Logger.class)) {
                            format(buffer, (String) args[0], (Object[]) ((args.length == 1) ? null : args[1]));
                        }
                        //noinspection SuspiciousInvocationHandlerImplementation
                        return null;
                    });
        }
    }
}

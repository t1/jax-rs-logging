package com.github.t1.logging.clientfilter;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.slf4j.Logger;

import java.lang.reflect.Proxy;

import static com.github.t1.logging.clientfilter.LoggingTools.format;
import static org.assertj.core.api.Assertions.catchRuntimeException;
import static org.assertj.core.api.BDDAssertions.then;

class LogWrapperTest {
    @ParameterizedTest
    @CsvSource({
            // for the OFF tests to work, both the root logger and the `..single` logger have to be disabled.
            // we want to see almost all DEBUG output, so we want the root logger to be DEBUG;
            // and when we disable the root `..single` logger, the root-SINGLE test fails
            // "test, OFF",
            // "test.X, OFF",
            // "test.X.y, OFF",
            "test.A, STANDARD:test.A",
            "test.B, SINGLE:test.B..single",
            "test.C.ping, SINGLE:test.C..single",
            "test.C.ping.pong, SINGLE:test.C..single",
            // this passes even when the `..single` logger is off, but the root logger is on...
            // so to _really_ test it, the root logger has to be off, too, but we don't want that.
            "test.D, SINGLE:..single"
    })
    void shouldFindLogWrapper(String loggerName, String expectedToString) {
        var log = LogWrapper.of(loggerName);

        then(log).hasToString(expectedToString);
    }

    @Test void shouldFailToLogOff() {
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
                        if (method.getName().equals("isDebugEnabled")) {
                            return true;
                        } else if (method.getDeclaringClass().equals(Logger.class)) {
                            format(buffer, (String) args[0], (Object[]) ((args.length == 1) ? null : args[1]));
                        }
                        //noinspection SuspiciousInvocationHandlerImplementation
                        return null;
                    });
        }
    }
}

package com.github.t1.logging.clientfilter;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.slf4j.Logger;
import org.slf4j.Marker;
import org.slf4j.event.Level;
import org.slf4j.helpers.AbstractLogger;

import static com.github.t1.logging.clientfilter.LogWrapper.parentLogger;
import static com.github.t1.logging.clientfilter.LoggingTools.format;
import static org.assertj.core.api.Assertions.catchRuntimeException;
import static org.assertj.core.api.BDDAssertions.then;
import static org.slf4j.event.Level.DEBUG;
import static org.slf4j.event.Level.ERROR;
import static org.slf4j.event.Level.INFO;
import static org.slf4j.event.Level.TRACE;
import static org.slf4j.event.Level.WARN;

class LogWrapperTest {
    @ParameterizedTest
    @CsvSource({
            "'', STANDARD:",
            "X, STANDARD:X",
            "off, STANDARD:",
            "off.on, STANDARD:off.on",
            "off.ons, SINGLE:off.ons..single",
            "off.ons.ping, SINGLE:off.ons..single",
            "off.ons.ping.pong, SINGLE:off.ons..single",
            "off.X, STANDARD:",
            "ons, STANDARD:ons",
            "ons.ping, STANDARD:ons.ping",
    })
    void shouldFindLogWrapperWithRootSingleAndRootDebug(String loggerName, String expectedToString) {
        var log = LogWrapper.of(loggerName, name -> testLoggerFactory(name, DEBUG, DEBUG));

        then(log).hasToString(expectedToString);
    }

    @ParameterizedTest
    @CsvSource({
            "'', STANDARD:",
            "X, STANDARD:X",
            "off, STANDARD:",
            "off.on, STANDARD:off.on",
            "off.ons, SINGLE:off.ons..single",
            "off.ons.ping, SINGLE:off.ons..single",
            "off.ons.ping.pong, SINGLE:off.ons..single",
            "off.X, STANDARD:",
            "ons, STANDARD:ons",
            "ons.ping, STANDARD:ons.ping",
    })
    void shouldFindLogWrapperWithoutRootSingleButRootDebug(String loggerName, String expectedToString) {
        var log = LogWrapper.of(loggerName, name -> testLoggerFactory(name, DEBUG, INFO));

        then(log).hasToString(expectedToString);
    }

    @ParameterizedTest
    @CsvSource({
            "'', OFF",
            "X, OFF",
            "off, OFF",
            "off.on, STANDARD:off.on",
            "off.on.ping, STANDARD:off.on.ping",
            "off.ons, SINGLE:off.ons..single",
            "off.ons.ping, SINGLE:off.ons..single",
            "off.ons.ping.pong, SINGLE:off.ons..single",
            "off.X, OFF",
            "off.X.y, OFF",
            "ons, SINGLE:ons..single",
            "ons.ping, SINGLE:ons..single",
    })
    void shouldFindLogWrapperWithoutRootSingleAndRootInfo(String loggerName, String expectedToString) {
        var log = LogWrapper.of(loggerName, name -> testLoggerFactory(name, INFO, INFO));

        then(log).hasToString(expectedToString);
    }

    @ParameterizedTest
    @CsvSource({
            "'', SINGLE:..single",
            "X, SINGLE:..single",
            "off, SINGLE:..single",
            "off.on, STANDARD:off.on",
            "off.ons, SINGLE:off.ons..single",
            "off.ons.ping, SINGLE:off.ons..single",
            "off.ons.ping.pong, SINGLE:off.ons..single",
            "off.X, SINGLE:..single",
            "ons, SINGLE:ons..single",
            "ons.ping, SINGLE:ons..single",
    })
    void shouldFindLogWrapperWithRootSingleAndRootInfo(String loggerName, String expectedToString) {
        var log = LogWrapper.of(loggerName, name -> testLoggerFactory(name, INFO, DEBUG));

        then(log).hasToString(expectedToString);
    }

    private Logger testLoggerFactory(String name, Level rootLevel, Level rootSingle) {
        return new MockLogger(name, level(name, rootLevel, rootSingle)).logger();
    }

    @SuppressWarnings("DuplicateBranchesInSwitch")
    private static Level level(String name, Level rootLevel, Level rootSingle) {
        return switch (name) {
            case "" -> rootLevel;
            case "..single" -> rootSingle;
            case "on" -> DEBUG;
            case "ons..single" -> DEBUG;
            case "off" -> INFO;
            case "off.on" -> DEBUG;
            case "off.ons..single" -> DEBUG;
            default -> level(parentLogger(name), rootLevel, rootSingle);
        };
    }

    @Test void shouldFailToLogOff() {
        RuntimeException exception;
        try (var log = new LogWrapper.OffLogWrapper()) {
            exception = catchRuntimeException(() -> log.debug("foo"));
        }

        then(exception).isInstanceOf(UnsupportedOperationException.class).hasMessage("off");
    }

    @Test void shouldLogStandard() {
        var mockLogger = new MockLogger("foo", DEBUG);

        try (var log = new LogWrapper.StandardLogWrapper(mockLogger.logger())) {
            log.debug("foo {}", "bar");
            log.warn("qux");
            log.debug("baz");
        }

        then(mockLogger.out()).isEqualTo("foo bar\nqux\nbaz\n");
    }

    @Test void shouldLogSingle() {
        var mockLogger = new MockLogger("foo", DEBUG);

        try (var log = new LogWrapper.SingleLogWrapper(mockLogger.logger())) {
            log.debug("foo {}", "bar");
            log.warn("qux");
            log.debug("baz");
        }

        then(mockLogger.out()).isEqualTo("foo bar\nWARN: qux\nbaz\n");
    }

    @RequiredArgsConstructor
    private static class MockLogger {
        private final String name;
        private final Level level;
        private final StringBuilder buffer = new StringBuilder();

        @Override public String toString() {return name + ":" + level;}

        public String out() {return buffer.toString();}

        public Logger logger() {
            return new AbstractLogger() {
                @Override public String toString() {return MockLogger.this.toString();}

                @Override public String getName() {return MockLogger.this.name;}

                @Override protected String getFullyQualifiedCallerName() {return null;}

                @Override
                protected void handleNormalizedLoggingCall(Level level, Marker marker, String messagePattern, Object[] arguments, Throwable throwable) {
                    if (isEnabled(level)) format(buffer, messagePattern, arguments);
                }

                private boolean isEnabled(Level level) {return MockLogger.this.level.toInt() <= level.toInt();}

                @Override public boolean isTraceEnabled() {return isEnabled(TRACE);}

                @Override public boolean isTraceEnabled(Marker marker) {return isTraceEnabled();}

                @Override public boolean isDebugEnabled() {return isEnabled(DEBUG);}

                @Override public boolean isDebugEnabled(Marker marker) {return isDebugEnabled();}

                @Override public boolean isInfoEnabled() {return isEnabled(INFO);}

                @Override public boolean isInfoEnabled(Marker marker) {return isInfoEnabled();}

                @Override public boolean isWarnEnabled() {return isEnabled(WARN);}

                @Override public boolean isWarnEnabled(Marker marker) {return isWarnEnabled();}

                @Override public boolean isErrorEnabled() {return isEnabled(ERROR);}

                @Override public boolean isErrorEnabled(Marker marker) {return isErrorEnabled();}
            };
        }
    }
}

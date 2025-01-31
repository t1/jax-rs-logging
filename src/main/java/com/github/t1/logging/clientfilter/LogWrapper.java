package com.github.t1.logging.clientfilter;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.github.t1.logging.clientfilter.LoggingTools.SINGLE;
import static com.github.t1.logging.clientfilter.LoggingTools.format;

/// Can be standard, single, or off.
public interface LogWrapper extends AutoCloseable {
    static LogWrapper of(String loggerName) {
        var logger = LoggerFactory.getLogger(loggerName);
        if (logger.isDebugEnabled()) return new StandardLogWrapper(logger);
        logger = LoggerFactory.getLogger(loggerName + SINGLE);
        if (logger.isDebugEnabled()) return new SingleLogWrapper(logger);
        return new OffLogWrapper();
    }

    default boolean off() {return false;}

    void debug(String message, Object... args);

    void warn(String message, Object... args);

    default void close() {}

    @RequiredArgsConstructor
    class StandardLogWrapper implements LogWrapper {
        private final Logger logger;

        @Override public void debug(String message, Object... args) {logger.debug(message, args);}

        @Override public void warn(String message, Object... args) {logger.warn(message, args);}
    }

    @RequiredArgsConstructor
    class SingleLogWrapper implements LogWrapper {
        private final Logger logger;
        private final StringBuilder buffer = new StringBuilder();
        private boolean closed = false;

        @Override public void debug(String message, Object... args) {
            checkNotClosed();
            format(buffer, message, args);
        }

        @Override public void warn(String message, Object... args) {
            checkNotClosed();
            buffer.append("WARN: ");
            format(buffer, message, args);
        }

        @Override public void close() {
            checkNotClosed();
            closed = true;
            if (!buffer.isEmpty() && buffer.charAt(buffer.length() - 1) == '\n') buffer.setLength(buffer.length() - 1);
            logger.debug(buffer.toString());
        }

        private void checkNotClosed() {
            if (closed) throw new IllegalStateException("already closed");
        }
    }

    @RequiredArgsConstructor
    class OffLogWrapper implements LogWrapper {
        @Override public boolean off() {return true;}

        @Override public void debug(String message, Object... args) {
            throw new UnsupportedOperationException("off");
        }

        @Override public void warn(String message, Object... args) {
            throw new UnsupportedOperationException("off");
        }
    }
}

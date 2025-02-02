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
        while (true) {
            var singleLogger = LoggerFactory.getLogger(loggerName + SINGLE);
            if (singleLogger.isDebugEnabled()) return new SingleLogWrapper(singleLogger);
            if (loggerName.isEmpty()) return new OffLogWrapper();
            int lastDot = loggerName.lastIndexOf('.');
            loggerName = (lastDot < 0) ? "" : loggerName.substring(0, lastDot);
        }
    }

    default boolean off() {return false;}

    void debug(String message, Object... args);

    void warn(String message, Object... args);

    default void close() {}


    class StandardLogWrapper implements LogWrapper {
        StandardLogWrapper(Logger logger) {
            assert logger.isDebugEnabled();
            this.logger = logger;
        }

        @Override public String toString() {return "STANDARD:" + logger.getName();}

        private final Logger logger;

        @Override public void debug(String message, Object... args) {logger.debug(message, args);}

        @Override public void warn(String message, Object... args) {logger.warn(message, args);}
    }

    class SingleLogWrapper implements LogWrapper {
        SingleLogWrapper(Logger logger) {
            assert logger.isDebugEnabled();
            this.logger = logger;
        }

        @Override public String toString() {return "SINGLE:" + logger.getName();}

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
            if (!buffer.isEmpty() && buffer.charAt(buffer.length() - 1) == '\n') buffer.setLength(buffer.length() - 1);
            logger.debug(buffer.toString());
            checkNotClosed();
            closed = true;
        }

        private void checkNotClosed() {
            if (closed) throw new IllegalStateException("already closed");
        }
    }

    @RequiredArgsConstructor
    class OffLogWrapper implements LogWrapper {
        @Override public String toString() {return "OFF";}

        @Override public boolean off() {return true;}

        @Override public void debug(String message, Object... args) {
            throw new UnsupportedOperationException("off");
        }

        @Override public void warn(String message, Object... args) {
            throw new UnsupportedOperationException("off");
        }
    }
}

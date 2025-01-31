package com.github.t1.logging.clientfilter;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;

class LoggingOutputStream extends FilterOutputStream {
    private final String direction;
    private final StringBuilder buffer = new StringBuilder();
    private final LogWrapper log;
    private final Runnable closeAction;

    public LoggingOutputStream(OutputStream stream, String direction, LogWrapper log, Runnable closeAction) {
        super(stream);
        this.direction = direction;
        this.log = log;
        this.closeAction = closeAction;
    }

    @Override
    public void write(int b) throws IOException {
        super.write(b);
        buffer.appendCodePoint(b);
    }

    @Override
    public void close() throws IOException {
        super.close();
        buffer.toString().lines().forEach(line -> log.debug("{} {}", direction, line));
        closeAction.run();
    }
}

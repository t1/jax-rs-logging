package com.github.t1.logging.clientfilter;

import org.slf4j.Logger;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;

class LoggingOutputStream extends FilterOutputStream {
    private final String direction;
    private final StringBuilder buffer = new StringBuilder();
    private final Logger log;

    public LoggingOutputStream(OutputStream stream, String direction, Logger log) {
        super(stream);
        this.direction = direction;
        this.log = log;
    }

    @Override public void write(int b) throws IOException {
        super.write(b);
        buffer.appendCodePoint(b);
    }

    @Override public void close() throws IOException {
        super.close();
        buffer.toString().lines().forEach(line -> log.debug("{} {}", direction, line));
    }
}

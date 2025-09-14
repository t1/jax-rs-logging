package com.github.t1.logging.clientfilter;

import jakarta.ws.rs.core.MediaType;
import org.junit.jupiter.api.Test;

import static com.github.t1.logging.clientfilter.LoggingTools.isLoggable;
import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON_TYPE;
import static jakarta.ws.rs.core.MediaType.APPLICATION_OCTET_STREAM_TYPE;
import static jakarta.ws.rs.core.MediaType.APPLICATION_XHTML_XML_TYPE;
import static jakarta.ws.rs.core.MediaType.APPLICATION_XML_TYPE;
import static jakarta.ws.rs.core.MediaType.TEXT_HTML_TYPE;
import static jakarta.ws.rs.core.MediaType.TEXT_PLAIN_TYPE;
import static jakarta.ws.rs.core.MediaType.TEXT_XML_TYPE;
import static org.assertj.core.api.BDDAssertions.then;

/// We'd need MessageBodyReaders/Writers to integration-test xml and yaml;
/// but this should be good enough
class LoggingToolsTest {
    private static void shouldBeLoggable(String type) {shouldBeLoggable(MediaType.valueOf(type));}

    private static void shouldBeLoggable(MediaType type) {then(isLoggable(type)).isTrue();}

    private static void shouldNotBeLoggable(String type) {shouldNotBeLoggable(MediaType.valueOf(type));}

    private static void shouldNotBeLoggable(MediaType type) {then(isLoggable(type)).isFalse();}

    @Test void jsonShouldBeLoggable() {
        shouldBeLoggable(APPLICATION_JSON_TYPE);
        shouldBeLoggable("application/json;charset=UTF-8");
        shouldBeLoggable("application/vnd.some.vendor.type+json");
    }

    @Test void yamlShouldBeLoggable() {
        shouldBeLoggable("application/xml");
        shouldBeLoggable("application/yaml;charset=UTF-8");
        shouldBeLoggable("application/vnd.some.vendor.type+yaml");
    }

    @Test void xmlShouldBeLoggable() {
        shouldBeLoggable(APPLICATION_XML_TYPE);
        shouldBeLoggable("application/xml;charset=UTF-8");
        shouldBeLoggable("application/vnd.some.vendor.type+xml");
        shouldBeLoggable(TEXT_XML_TYPE);
        shouldBeLoggable("text/xml;charset=UTF-8");
    }

    @Test void textShouldBeLoggable() {
        shouldBeLoggable(TEXT_PLAIN_TYPE);
        shouldBeLoggable("text/plain;charset=UTF-8");
        shouldBeLoggable("text/plain;charset=ISO-8859-1");
    }

    @Test void htmlShouldBeLoggable() {
        shouldBeLoggable(TEXT_HTML_TYPE);
        shouldBeLoggable("text/html;charset=UTF-8");
        shouldBeLoggable(APPLICATION_XHTML_XML_TYPE);
        shouldBeLoggable("application/xhtml+xml;charset=UTF-8");
    }

    @Test void octetStreamShouldNotBeLoggable() {
        shouldNotBeLoggable(APPLICATION_OCTET_STREAM_TYPE);
    }

    @Test void pngShouldNotBeLoggable() {
        shouldNotBeLoggable("image/png");
    }

    @Test void pdfShouldNotBeLoggable() {
        shouldNotBeLoggable("application/pdf");
    }

    @Test void zipShouldNotBeLoggable() {
        shouldNotBeLoggable("application/zip");
    }

    @Test void mpegShouldNotBeLoggable() {
        shouldNotBeLoggable("audio/mpeg");
    }

    @Test void fontShouldNotBeLoggable() {
        shouldNotBeLoggable("font/ttf");
    }

    @Test void videoShouldNotBeLoggable() {
        shouldNotBeLoggable("video/mp4");
    }

    @Test void grpcShouldBeLoggableDependingOnSuffix() {
        shouldNotBeLoggable("application/grpc");
        shouldNotBeLoggable("application/grpc+proto");
        shouldBeLoggable("application/grpc+json");
    }

    @Test void svgXmlShouldBeLoggable() {
        shouldBeLoggable("image/svg+xml");
    }
}

package com.github.t1.logging.clientfilter;

import jakarta.ws.rs.core.MediaType;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Optional;

import static jakarta.ws.rs.core.HttpHeaders.AUTHORIZATION;
import static jakarta.ws.rs.core.MediaType.CHARSET_PARAMETER;
import static java.nio.charset.StandardCharsets.ISO_8859_1;

public class LoggingTools {
    /// The suffix for the single-line logger.
    public static final String SINGLE = "..single";

    /**
     * We consider passwords longer than this to be safe enough, so we can log the username,
     * which basically makes debugging easier, as it often happens that you use the <i>wrong credentials</i>,
     * but much less likely that you use the <i>wrong password</i> for the correct user.
     */
    private static final int SAFE_PASSWORD_LEN = 12;

    static String safe(String name, List<String> values) {
        if (AUTHORIZATION.equalsIgnoreCase(name)) {
            var safeValues = new ArrayList<String>(values.size());
            for (var value : values) {
                var safeValue = "<hidden>";
                var split = value.split(" ", 2);
                if (split.length > 1 && "Basic".equalsIgnoreCase(split[0])) {
                    var decoded = new String(Base64.getDecoder().decode(split[1])).split(":", 2);
                    if (decoded.length > 1 && decoded[1].length() >= SAFE_PASSWORD_LEN) {
                        var username = decoded[0];
                        safeValue = username + ":" + safeValue;
                    }
                }
                safeValues.add(safeValue);
            }
            values = safeValues;
        }
        return merge(values);
    }

    static String merge(List<String> values) {
        return String.join(", ", values);
    }


    static boolean isLoggable(MediaType mediaType) {
        if (mediaType == null) return false;
        return mediaType.getType().equals("text")
               || is(mediaType, "json")
               || is(mediaType, "yaml")
               || is(mediaType, "xml");
    }

    private static boolean is(MediaType mediaType, String subType) {
        return mediaType.getSubtype().equals(subType) || mediaType.getSubtype().endsWith("+" + subType);
    }

    static Charset charset(MediaType mediaType) {
        return Optional.ofNullable(mediaType)
                .map(MediaType::getParameters)
                .flatMap(params -> Optional.ofNullable(params.get(CHARSET_PARAMETER)))
                .map(Charset::forName)
                .orElse(ISO_8859_1);
    }

    static void format(StringBuilder buffer, String message, Object[] args) {
        int argIndex = 0;
        // we don't need a perfect state machine here, as this is only for a well known set of messages
        for (var c : message.toCharArray()) {
            switch (c) {
                case '{' -> {}
                case '}' -> buffer.append(args[argIndex++]);
                default -> buffer.append(c);
            }
        }
        buffer.append('\n');
    }
}

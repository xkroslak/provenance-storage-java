package cz.muni.fi.distributed_prov_system.utils;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.TemporalAccessor;
import java.time.temporal.TemporalQueries;
import java.util.Base64;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class MetaProvenanceUtils {
    private static final Pattern META_ID_PATTERN =
            Pattern.compile("/api/v1/documents/meta/([^\"'<>#\\s]+)");

    private MetaProvenanceUtils() {
    }

    public static String resolveMetaBundleId(String base64Document, String format) {
        byte[] decoded = Base64.getDecoder().decode(base64Document);
        String content = new String(decoded, StandardCharsets.UTF_8);
        Matcher matcher = META_ID_PATTERN.matcher(content);
        if (matcher.find()) {
            return matcher.group(1);
        }
        throw new IllegalArgumentException("Unable to resolve meta bundle id from document.");
    }

    public static String computeGenEntityId(String newEntityId) {
        int lastUnderscore = newEntityId.lastIndexOf('_');
        if (lastUnderscore == -1) {
            return newEntityId + "_gen";
        }

        int secondLastUnderscore = newEntityId.lastIndexOf('_', lastUnderscore - 1);
        String part0 = secondLastUnderscore == -1
                ? newEntityId.substring(0, lastUnderscore)
                : newEntityId.substring(0, secondLastUnderscore);
        String part2 = secondLastUnderscore == -1
                ? null
                : newEntityId.substring(lastUnderscore + 1);

        StringBuilder builder = new StringBuilder(part0);
        if (part2 != null && !part2.isBlank()) {
            builder.append("_").append(part2);
        }
        builder.append("_gen");
        return builder.toString();
    }

    public static LocalDateTime toLocalDateTimeFromEpochSeconds(Object epochSeconds) {
        if (epochSeconds == null) {
            return null;
        }
        String text = epochSeconds.toString().trim();
        if ((text.startsWith("\"") && text.endsWith("\"")) || (text.startsWith("'") && text.endsWith("'"))) {
            text = text.substring(1, text.length() - 1).trim();
        }
        try {
            long seconds = Long.parseLong(text);
            return LocalDateTime.ofInstant(Instant.ofEpochSecond(seconds), ZoneOffset.UTC);
        } catch (NumberFormatException ex) {
            try {
                DateTimeFormatter fmt = new DateTimeFormatterBuilder()
                        .append(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                        .optionalStart()
                        .appendOffsetId()
                        .optionalEnd()
                        .toFormatter();

                TemporalAccessor ta = fmt.parse(text);
                ZoneOffset offset = TemporalQueries.offset().queryFrom(ta);
                if (offset != null) {
                    return OffsetDateTime.from(ta).toLocalDateTime();
                }
                return LocalDateTime.from(ta);
            } catch (Exception ignored) {
                return LocalDateTime.ofInstant(Instant.parse(text), ZoneOffset.UTC);
            }
        }
    }
}

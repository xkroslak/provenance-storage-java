package cz.muni.fi.trusted_party.data.enums;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum DocumentType {
    GRAPH,
    DOMAIN_SPECIFIC,
    BACKBONE,
    META;

    @JsonCreator
    public static DocumentType fromValue(String value) {
        if (value == null) {
            return null;
        }

        String normalized = value.trim()
                .replace('-', '_')
                .toUpperCase();

        return DocumentType.valueOf(normalized);
    }
}

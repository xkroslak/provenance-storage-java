package cz.muni.fi.trusted_party.data.enums;

public enum HashFunction {
    SHA256("SHA256"),
    SHA512("SHA512"),
    SHA3_256("SHA3-256"),
    SHA3_512("SHA3-512");

    private final String value;

    HashFunction(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}

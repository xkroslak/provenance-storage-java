package cz.muni.fi.distributed_prov_system.utils;

public final class ProvConstants {
    private ProvConstants() {
    }

    public static final String PROV_TYPE = "prov:type";
    public static final String PROV_ATTR_BUNDLE = "prov:Bundle";

    public static final String PAV_VERSION = "http://purl.org/pav/version";

    private static final String CPM_BASE = "https://www.commonprovenancemodel.org/cpm-namespace-v1-0/";

    public static final String CPM_ID = cpm("id");
    public static final String CPM_SENDER_AGENT = cpm("senderAgent");
    public static final String CPM_RECEIVER_AGENT = cpm("receiverAgent");
    public static final String CPM_MAIN_ACTIVITY = cpm("mainActivity");
    public static final String CPM_BACKWARD_CONNECTOR = cpm("backwardConnector");
    public static final String CPM_FORWARD_CONNECTOR = cpm("forwardConnector");

    public static final String CPM_REFERENCED_BUNDLE_ID = cpm("referencedBundleId");
    public static final String CPM_REFERENCED_META_BUNDLE_ID = cpm("referencedMetaBundleId");
    public static final String CPM_REFERENCED_BUNDLE_HASH_VALUE = cpm("referencedBundleHashValue");
    public static final String CPM_HASH_ALG = cpm("hashAlg");

    public static final String CPM_TOKEN_GENERATION = cpm("tokenGeneration");
    public static final String CPM_TOKEN = cpm("token");
    public static final String CPM_TRUSTED_PARTY = cpm("trustedParty");
    public static final String CPM_TRUSTED_PARTY_URI = cpm("trustedPartyUri");
    public static final String CPM_TRUSTED_PARTY_CERTIFICATE = cpm("trustedPartyCertificate");

    public static String cpm(String localPart) {
        return CPM_BASE + localPart;
    }
}

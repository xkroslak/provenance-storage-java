package cz.muni.fi.trusted_party.api.Token;

import java.time.LocalDateTime;

public class TokenDTO {

    private Data data;
    private String signature;

    public TokenDTO() {}

    public TokenDTO(Data data, String signature) {
        this.data = data;
        this.signature = signature;
    }

    public Data getData() { return data; }
    public void setData(Data data) { this.data = data; }

    public String getSignature() { return signature; }
    public void setSignature(String signature) { this.signature = signature; }

    public static class Data {
        private String originatorId;
        private String authorityId;
        private LocalDateTime tokenTimestamp;
        private LocalDateTime documentCreationTimestamp;
        private String documentDigest;
        private AdditionalData additionalData;

        public Data() {}

        public Data(String originatorId, String authorityId, LocalDateTime tokenTimestamp,
                    LocalDateTime documentCreationTimestamp, String documentDigest, AdditionalData additionalData) {
            this.originatorId = originatorId;
            this.authorityId = authorityId;
            this.tokenTimestamp = tokenTimestamp;
            this.documentCreationTimestamp = documentCreationTimestamp;
            this.documentDigest = documentDigest;
            this.additionalData = additionalData;
        }

        public String getOriginatorId() { return originatorId; }
        public void setOriginatorId(String originatorId) { this.originatorId = originatorId; }

        public String getAuthorityId() { return authorityId; }
        public void setAuthorityId(String authorityId) { this.authorityId = authorityId; }

        public LocalDateTime getTokenTimestamp() { return tokenTimestamp; }
        public void setTokenTimestamp(LocalDateTime tokenTimestamp) { this.tokenTimestamp = tokenTimestamp; }

        public LocalDateTime getDocumentCreationTimestamp() { return documentCreationTimestamp; }
        public void setDocumentCreationTimestamp(LocalDateTime documentCreationTimestamp) { this.documentCreationTimestamp = documentCreationTimestamp; }

        public String getDocumentDigest() { return documentDigest; }
        public void setDocumentDigest(String documentDigest) { this.documentDigest = documentDigest; }

        public AdditionalData getAdditionalData() { return additionalData; }
        public void setAdditionalData(AdditionalData additionalData) { this.additionalData = additionalData; }

        public static class AdditionalData {
            private String bundle;
            private String hashFunction;
            private String trustedPartyUri;
            private String trustedPartyCertificate;

            public AdditionalData() {}

            public AdditionalData(String bundle, String hashFunction, String trustedPartyUri, String trustedPartyCertificate) {
                this.bundle = bundle;
                this.hashFunction = hashFunction;
                this.trustedPartyUri = trustedPartyUri;
                this.trustedPartyCertificate = trustedPartyCertificate;
            }

            public String getBundle() { return bundle; }
            public void setBundle(String bundle) { this.bundle = bundle; }

            public String getHashFunction() { return hashFunction; }
            public void setHashFunction(String hashFunction) { this.hashFunction = hashFunction; }

            public String getTrustedPartyUri() { return trustedPartyUri; }
            public void setTrustedPartyUri(String trustedPartyUri) { this.trustedPartyUri = trustedPartyUri; }

            public String getTrustedPartyCertificate() { return trustedPartyCertificate; }
            public void setTrustedPartyCertificate(String trustedPartyCertificate) { this.trustedPartyCertificate = trustedPartyCertificate; }
        }
    }
}

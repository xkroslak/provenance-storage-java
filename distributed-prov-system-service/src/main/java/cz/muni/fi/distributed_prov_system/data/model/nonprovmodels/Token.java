package cz.muni.fi.distributed_prov_system.data.model.nonprovmodels;

import org.springframework.data.neo4j.core.schema.*;

import java.util.Map;

@Node
public class Token {
    //TODO: Check what is id in token for prov service, I guess neo4j generate id
    @Id
    //@GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    //TODO: is this id?
    @Property
    private String signature;

    @Property
    private String hash;

    @Property(name = "originator_id")
    private String originatorId;

    @Property(name = "authority_id")
    private String authorityId;

    @Property(name = "token_timestamp")
    private Long tokenTimestamp;

    @Property(name = "document_creation_timestamp")
    private Long documentCreationTimestamp;

    @Property(name = "document_digest")
    private String documentDigest;

    @Property(name = "message_timestamp")
    private Long messageTimestamp;

    @CompositeProperty(prefix = "additional_data")
    private Map<String, String> additionalData;

    @Relationship(type = "belongs_to")
    private Document belongsTo;

    @Relationship(type = "was_issued_by")
    private TrustedParty wasIssuedBy;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getSignature() {
        return signature;
    }

    public void setSignature(String signature) {
        this.signature = signature;
    }

    public String getHash() {
        return hash;
    }

    public void setHash(String hash) {
        this.hash = hash;
    }

    public String getOriginatorId() {
        return originatorId;
    }

    public void setOriginatorId(String originatorId) {
        this.originatorId = originatorId;
    }

    public String getAuthorityId() {
        return authorityId;
    }

    public void setAuthorityId(String authorityId) {
        this.authorityId = authorityId;
    }

    public Long getTokenTimestamp() {
        return tokenTimestamp;
    }

    public void setTokenTimestamp(Long tokenTimestamp) {
        this.tokenTimestamp = tokenTimestamp;
    }

    public Long getDocumentCreationTimestamp() {
        return documentCreationTimestamp;
    }

    public void setDocumentCreationTimestamp(Long documentCreationTimestamp) {
        this.documentCreationTimestamp = documentCreationTimestamp;
    }

    public String getDocumentDigest() {
        return documentDigest;
    }

    public void setDocumentDigest(String documentDigest) {
        this.documentDigest = documentDigest;
    }

    public Long getMessageTimestamp() {
        return messageTimestamp;
    }

    public void setMessageTimestamp(Long messageTimestamp) {
        this.messageTimestamp = messageTimestamp;
    }

    public Map<String, String> getAdditionalData() {
        return additionalData;
    }

    public void setAdditionalData(Map<String, String> additionalData) {
        this.additionalData = additionalData;
    }

    public Document getBelongsTo() {
        return belongsTo;
    }

    public void setBelongsTo(Document belongsTo) {
        this.belongsTo = belongsTo;
    }

    public TrustedParty getWasIssuedBy() {
        return wasIssuedBy;
    }

    public void setWasIssuedBy(TrustedParty wasIssuedBy) {
        this.wasIssuedBy = wasIssuedBy;
    }
}

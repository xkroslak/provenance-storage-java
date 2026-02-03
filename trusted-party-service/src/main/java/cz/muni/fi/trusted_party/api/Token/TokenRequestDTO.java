package cz.muni.fi.trusted_party.api.Token;

import cz.muni.fi.trusted_party.data.enums.DocumentType;
import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class TokenRequestDTO {
    @NotBlank(message = "organizationId is mandatory")
    @JsonAlias({"organizationId", "organizationName", "originatorId"})
    @JsonProperty("organizationId")
    private String organizationId;

    /** Raw PROV graph Base64 OR JSON depending on format */
    @NotBlank(message = "document is mandatory")
    @JsonAlias({"document", "graph"})
    @JsonProperty("document")
    private String Document;

    @NotBlank(message = "documentFormat is mandatory")
    @JsonAlias({"documentFormat", "doc_format"})
    private String documentFormat;

    @NotNull(message = "Empty or incorrect type, must be one of [subgraph|meta|graph]!")
    @JsonAlias({"type", "documentType"})
    private DocumentType documentType;

    @NotBlank(message = "createdOn timestamp is mandatory")
    private String createdOn;

    private String signature;

    public TokenRequestDTO() {}

    public String getOrganizationId() {
        return organizationId;
    }

    public void setOrganizationId(String organizationId) {
        this.organizationId = organizationId;
    }

    public String getDocument() {
        return Document;
    }

    public void setDocument(String document) {
        Document = document;
    }

    public String getCreatedOn() {
        return createdOn;
    }

    public void setCreatedOn(String createdOn) {
        this.createdOn = createdOn;
    }

    public String getDocumentFormat() {
        return documentFormat;
    }

    public void setDocumentFormat(String documentFormat) {
        this.documentFormat = documentFormat;
    }

    public String getSignature() {
        return signature;
    }

    public void setSignature(String signature) {
        this.signature = signature;
    }

    public DocumentType getDocumentType() {
        return documentType;
    }

    public void setDocumentType(DocumentType documentType) {
        this.documentType = documentType;
    }
}
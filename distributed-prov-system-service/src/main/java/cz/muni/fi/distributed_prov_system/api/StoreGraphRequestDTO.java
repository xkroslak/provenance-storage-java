package cz.muni.fi.distributed_prov_system.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

@Schema(description = "DTO representing a graph")
public class StoreGraphRequestDTO {
    @JsonProperty("document")
    private String document; // base64-encoded PROV document

    @JsonProperty("documentFormat")
    private String documentFormat; // "json", "rdf", "xml", "provn"

    @JsonProperty("signature")
    private String signature; // optional if TP disabled, required if enabled

    @JsonProperty("createdOn")
    private String createdOn; // ISO-8601 timestamp

    // Optionally, add organizationId if you want to support it in the payload
    // @JsonProperty("organizationId")
    // private String organizationId;

    public StoreGraphRequestDTO() {}

    public String getDocument() {
        return document;
    }

    public void setDocument(String document) {
        this.document = document;
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

    public String getCreatedOn() {
        return createdOn;
    }

    public void setCreatedOn(String createdOn) {
        this.createdOn = createdOn;
    }
}
package cz.muni.fi.distributed_prov_system.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.oas.annotations.media.Schema;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Response for subgraph retrieval")
public class SubgraphResponseDTO {

    @Schema(description = "Base64-encoded document graph")
    private String document;

    @Schema(description = "Issued token from trusted party")
    private JsonNode token;

    public SubgraphResponseDTO() {
    }

    public SubgraphResponseDTO(String document, JsonNode token) {
        this.document = document;
        this.token = token;
    }

    public String getDocument() {
        return document;
    }

    public void setDocument(String document) {
        this.document = document;
    }

    public JsonNode getToken() {
        return token;
    }

    public void setToken(JsonNode token) {
        this.token = token;
    }
}

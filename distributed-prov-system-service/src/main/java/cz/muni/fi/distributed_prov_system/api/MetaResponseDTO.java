package cz.muni.fi.distributed_prov_system.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Response for meta graph retrieval")
public class MetaResponseDTO {

    @Schema(description = "Base64-encoded meta provenance graph")
    private String graph;

    @Schema(description = "Issued token from trusted party")
    private Object token;

    public MetaResponseDTO() {
    }

    public MetaResponseDTO(String graph, Object token) {
        this.graph = graph;
        this.token = token;
    }

    public String getGraph() {
        return graph;
    }

    public void setGraph(String graph) {
        this.graph = graph;
    }

    public Object getToken() {
        return token;
    }

    public void setToken(Object token) {
        this.token = token;
    }
}
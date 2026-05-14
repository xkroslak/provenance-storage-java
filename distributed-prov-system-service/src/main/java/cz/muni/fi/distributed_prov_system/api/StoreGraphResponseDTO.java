package cz.muni.fi.distributed_prov_system.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.oas.annotations.media.Schema;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Response for store graph")
public class StoreGraphResponseDTO {

    @Schema(description = "Issued token from trusted party")
    private JsonNode token;

    @Schema(description = "Informational message")
    private String info;

    @Schema(description = "True if a new meta-component was created, false if an existing one was reused")
    private boolean metaComponentCreated;

    public StoreGraphResponseDTO() {
    }

    public StoreGraphResponseDTO(JsonNode token, String info, boolean metaComponentCreated) {
        this.token = token;
        this.info = info;
        this.metaComponentCreated = metaComponentCreated;
    }

    public JsonNode getToken() {
        return token;
    }

    public void setToken(JsonNode token) {
        this.token = token;
    }

    public String getInfo() {
        return info;
    }

    public void setInfo(String info) {
        this.info = info;
    }

    public boolean isMetaComponentCreated() {
        return metaComponentCreated;
    }

    public void setMetaComponentCreated(boolean metaComponentCreated) {
        this.metaComponentCreated = metaComponentCreated;
    }
}

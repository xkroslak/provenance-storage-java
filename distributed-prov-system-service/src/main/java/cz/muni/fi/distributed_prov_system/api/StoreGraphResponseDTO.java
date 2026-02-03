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

    public StoreGraphResponseDTO() {
    }

    public StoreGraphResponseDTO(JsonNode token, String info) {
        this.token = token;
        this.info = info;
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
}

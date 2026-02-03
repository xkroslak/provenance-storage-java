package cz.muni.fi.distributed_prov_system.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

@Schema(description = "DTO representing a organization")
public class RegisterOrganizationRequestDTO {

    @NotBlank
    @Schema(description = "Certificate of client")
    private String clientCertificate; // PEM

    @NotEmpty
    private List<String> intermediateCertificates; // PEMs

    @JsonProperty("TrustedPartyUri")
    private String trustedPartyUri;

    public String getClientCertificate() {
        return clientCertificate;
    }

    public void setClientCertificate(String clientCertificate) {
        this.clientCertificate = clientCertificate;
    }

    public List<String> getIntermediateCertificates() {
        return intermediateCertificates;
    }

    public void setIntermediateCertificates(List<String> intermediateCertificates) {
        this.intermediateCertificates = intermediateCertificates;
    }

    public String getTrustedPartyUri() {
        return trustedPartyUri;
    }

    public void setTrustedPartyUri(String trustedPartyUri) {
        this.trustedPartyUri = trustedPartyUri;
    }
}
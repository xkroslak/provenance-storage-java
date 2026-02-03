package cz.muni.fi.trusted_party.api.Organization;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public class StoreCertOrganizationDTO {

    @NotEmpty(message = "Organization id must not be empty")
    private String organizationId;

    @NotEmpty(message = "Client certificate must not be empty")
    private String clientCertificate;

    @NotNull
    private List<@NotEmpty(message = "Intermediate certificate must not be empty")String> intermediateCertificates;

    public StoreCertOrganizationDTO() {}

    public String getOrganizationId() {
        return organizationId;
    }

    public void setOrganizationId(String organizationId) {
        this.organizationId = organizationId;
    }

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
}

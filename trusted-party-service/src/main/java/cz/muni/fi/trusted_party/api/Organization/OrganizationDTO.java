package cz.muni.fi.trusted_party.api.Organization;

import java.util.List;

public class OrganizationDTO {
    private String organizationId;
    private String clientCertificate;
    private List<String> revokedCertificates;

    public OrganizationDTO() {}

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

    public List<String> getRevokedCertificates() {
        return revokedCertificates;
    }

    public void setRevokedCertificates(List<String> revokedCertificates) {
        this.revokedCertificates = revokedCertificates;
    }
}
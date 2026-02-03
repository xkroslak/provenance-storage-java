package cz.muni.fi.trusted_party.api.Organization;

import java.util.List;

public class UpdateCertificatesDTO {

    private String organizationId;
    private String clientCertificate;
    private List<String> intermediateCertificates;

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

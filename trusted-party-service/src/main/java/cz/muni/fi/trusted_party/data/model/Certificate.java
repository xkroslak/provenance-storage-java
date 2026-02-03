package cz.muni.fi.trusted_party.data.model;

import cz.muni.fi.trusted_party.data.enums.CertificateType;
import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.Objects;

@Entity
public class Certificate {

    @Id
    @Column(length = 64)
    private String certDigest;

    @Column(columnDefinition = "TEXT")
    private String cert;

    @Enumerated(EnumType.STRING)
    private CertificateType certificateType;

    private boolean isRevoked;

    //TODO: check if Date isnt better
    private LocalDateTime received_on;

    @ManyToOne
    @JoinColumn(name = "organization", referencedColumnName = "orgName")
    private Organization organization;

    public String getCertDigest() {
        return certDigest;
    }

    public void setCertDigest(String certDigest) {
        this.certDigest = certDigest;
    }

    public String getCert() {
        return cert;
    }

    public void setCert(String cert) {
        this.cert = cert;
    }

    public CertificateType getCertificateType() {
        return certificateType;
    }

    public void setCertificateType(CertificateType certificateType) {
        this.certificateType = certificateType;
    }

    public boolean getIsRevoked() {
        return isRevoked;
    }

    public void setIsRevoked(boolean revoked) {
        isRevoked = revoked;
    }

    public LocalDateTime getReceived_on() {
        return received_on;
    }

    public void setReceived_on(LocalDateTime received_on) {
        this.received_on = received_on;
    }

    public Organization getOrganization() {
        return organization;
    }

    public void setOrganization(Organization organization) {
        this.organization = organization;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Certificate that)) return false;
        return isRevoked == that.isRevoked
            && Objects.equals(certDigest, that.certDigest)
                && Objects.equals(cert, that.cert)
                && certificateType == that.certificateType
                && Objects.equals(organization, that.organization);
    }

    @Override
    public int hashCode() {
        return Objects.hash(certDigest, cert, certificateType, isRevoked, organization);
    }

    @Override
    public String toString() {
        return "Certificate{" +
                "certDigest='" + certDigest + '\'' +
                ", cert='" + cert + '\'' +
                ", certificateType=" + certificateType +
                ", isRevoked=" + isRevoked +
                ", received_on=" + received_on +
                ", organization=" + organization +
                '}';
    }
}

package cz.muni.fi.trusted_party.data.model;

import cz.muni.fi.trusted_party.data.enums.DocumentType;
import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.Objects;

@Entity
public class Document {

    @Id
    private String identifier;

    @Lob
    private String docFormat;

    @ManyToOne
    @JoinColumn(name = "certificate", referencedColumnName = "certDigest")
    private Certificate certificate;

    @ManyToOne
    @JoinColumn(name = "organization", referencedColumnName = "orgName")
    private Organization organization;

    @Enumerated(EnumType.STRING)
    private DocumentType documentType;

    @Lob
    private String documentText;

    private LocalDateTime createdOn;

    @Lob
    private String signature;

    public String getSignature() {
        return signature;
    }

    public void setSignature(String signature) {
        this.signature = signature;
    }

    public LocalDateTime getCreatedOn() {
        return createdOn;
    }

    public void setCreatedOn(LocalDateTime createdOn) {
        this.createdOn = createdOn;
    }

    public String getDocumentText() {
        return documentText;
    }

    public void setDocumentText(String documentText) {
        this.documentText = documentText;
    }

    public DocumentType getDocumentType() {
        return documentType;
    }

    public void setDocumentType(DocumentType documentType) {
        this.documentType = documentType;
    }

    public Organization getOrganization() {
        return organization;
    }

    public void setOrganization(Organization organization) {
        this.organization = organization;
    }

    public Certificate getCertificate() {
        return certificate;
    }

    public void setCertificate(Certificate certificate) {
        this.certificate = certificate;
    }

    public String getDocFormat() {
        return docFormat;
    }

    public void setDocFormat(String docFormat) {
        this.docFormat = docFormat;
    }

    public String getIdentifier() {
        return identifier;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Document document)) return false;
        return Objects.equals(identifier, document.identifier)
                && Objects.equals(docFormat, document.docFormat)
                && Objects.equals(certificate, document.certificate)
                && Objects.equals(organization, document.organization)
                && documentType == document.documentType
                && Objects.equals(documentText, document.documentText)
                && Objects.equals(createdOn, document.createdOn)
                && Objects.equals(signature, document.signature);
    }

    @Override
    public int hashCode() {
        return Objects.hash(identifier, docFormat, certificate, organization,
                documentType, documentText, createdOn, signature);
    }

    @Override
    public String toString() {
        return "Document{" +
                "identifier='" + identifier + '\'' +
                ", docFormat='" + docFormat + '\'' +
                ", certificate=" + certificate +
                ", organization=" + organization +
                ", documentType=" + documentType +
                ", documentText='" + documentText + '\'' +
                ", createdOn=" + createdOn +
                ", signature='" + signature + '\'' +
                '}';
    }
}

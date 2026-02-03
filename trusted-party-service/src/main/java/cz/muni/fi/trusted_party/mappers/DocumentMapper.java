package cz.muni.fi.trusted_party.mappers;

import cz.muni.fi.trusted_party.api.Document.DocumentDTO;
import cz.muni.fi.trusted_party.data.model.Document;
import org.springframework.stereotype.Component;

@Component
public class DocumentMapper {

    public DocumentDTO mapToDTO(Document document) {
        DocumentDTO dto = new DocumentDTO();
        dto.setIdentifier(document.getIdentifier());
        dto.setDocumentFormat(document.getDocFormat());
        dto.setCertDigest(document.getCertificate().getCertDigest());
        dto.setOrganizationId(document.getOrganization().getOrgName());
        dto.setDocumentType(document.getDocumentType());
        dto.setDocumentText(document.getDocumentText());
        dto.setCreatedOn(document.getCreatedOn());
        dto.setSignature(document.getSignature());
        return dto;
    }
}

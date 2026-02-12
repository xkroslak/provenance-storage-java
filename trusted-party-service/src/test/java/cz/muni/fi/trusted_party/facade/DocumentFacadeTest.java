package cz.muni.fi.trusted_party.facade;

import cz.muni.fi.trusted_party.api.Document.DocumentDTO;
import cz.muni.fi.trusted_party.data.model.Certificate;
import cz.muni.fi.trusted_party.data.model.Document;
import cz.muni.fi.trusted_party.data.model.Organization;
import cz.muni.fi.trusted_party.data.enums.DocumentType;
import cz.muni.fi.trusted_party.mappers.DocumentMapper;
import cz.muni.fi.trusted_party.service.DocumentService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DocumentFacadeTest {

    @Mock
    private DocumentService documentService;

    @Mock
    private DocumentMapper documentMapper;

    @InjectMocks
    private DocumentFacadeImpl documentFacade;

    @Test
    void getDocument_existingDocument_returnsMappedDto() {
        Organization organization = new Organization();
        organization.setOrgName("org-1");
        Certificate certificate = new Certificate();
        certificate.setCertDigest("cert-1");
        Document document = new Document();
        document.setIdentifier("doc-1");
        document.setDocFormat("json");
        document.setOrganization(organization);
        document.setCertificate(certificate);
        document.setDocumentType(DocumentType.GRAPH);
        document.setDocumentText("{}");
        document.setCreatedOn(LocalDateTime.now());
        document.setSignature("sig");

        DocumentDTO dto = new DocumentDTO();
        dto.setIdentifier("doc-1");

        when(documentService.getDocument("org-1", "doc-1", "json")).thenReturn(document);
        when(documentMapper.mapToDTO(document)).thenReturn(dto);

        DocumentDTO result = documentFacade.getDocument("org-1", "doc-1", "json");

        assertThat(result).isSameAs(dto);
    }
}

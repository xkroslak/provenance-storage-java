package cz.muni.fi.trusted_party.service;

import cz.muni.fi.trusted_party.data.model.Document;
import cz.muni.fi.trusted_party.data.model.Organization;
import cz.muni.fi.trusted_party.data.repository.DocumentRepository;
import cz.muni.fi.trusted_party.data.repository.OrganizationRepository;
import cz.muni.fi.trusted_party.exceptions.DocumentNotFoundException;
import cz.muni.fi.trusted_party.exceptions.OrganizationNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DocumentServiceTest {

    @Mock
    private DocumentRepository documentRepository;

    @Mock
    private OrganizationRepository organizationRepository;

    @InjectMocks
    private DocumentService documentService;

    @Test
    void getDocument_existingDocument_returnsDocument() {
        Organization organization = new Organization();
        organization.setOrgName("org-1");

        Document document = new Document();
        document.setIdentifier("doc-1");
        document.setDocFormat("json");

        when(organizationRepository.findById("org-1")).thenReturn(Optional.of(organization));
        when(documentRepository.findByIdentifierAndDocFormatAndOrganization("doc-1", "json", organization))
                .thenReturn(Optional.of(document));

        Document result = documentService.getDocument("org-1", "doc-1", "json");

        assertThat(result).isSameAs(document);
        verify(documentRepository).findByIdentifierAndDocFormatAndOrganization("doc-1", "json", organization);
    }

    @Test
    void getDocument_missingOrganization_throwsOrganizationNotFoundException() {
        when(organizationRepository.findById("missing-org")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> documentService.getDocument("missing-org", "doc-1", "json"))
                .isInstanceOf(OrganizationNotFoundException.class)
                .hasMessageContaining("missing-org");
    }

    @Test
    void getDocument_missingDocument_throwsDocumentNotFoundException() {
        Organization organization = new Organization();
        organization.setOrgName("org-1");

        when(organizationRepository.findById("org-1")).thenReturn(Optional.of(organization));
        when(documentRepository.findByIdentifierAndDocFormatAndOrganization("doc-404", "json", organization))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> documentService.getDocument("org-1", "doc-404", "json"))
                .isInstanceOf(DocumentNotFoundException.class)
                .hasMessageContaining("doc-404");
    }
}

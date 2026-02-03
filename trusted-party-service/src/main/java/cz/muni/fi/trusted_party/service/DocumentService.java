package cz.muni.fi.trusted_party.service;

import cz.muni.fi.trusted_party.data.model.Document;
import cz.muni.fi.trusted_party.data.model.Organization;
import cz.muni.fi.trusted_party.data.repository.DocumentRepository;
import cz.muni.fi.trusted_party.data.repository.OrganizationRepository;
import cz.muni.fi.trusted_party.exceptions.DocumentNotFoundException;
import cz.muni.fi.trusted_party.exceptions.OrganizationNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class DocumentService {

    private final DocumentRepository documentRepository;
    private final OrganizationRepository organizationRepository;

    @Autowired
    public DocumentService(DocumentRepository documentRepository, OrganizationRepository organizationRepository) {
        this.documentRepository = documentRepository;
        this.organizationRepository = organizationRepository;
    }

    public Document getDocument(String organizationName, String documentId, String documentFormat) {
        Organization organization = organizationRepository
                .findById(organizationName)
                .orElseThrow(() -> new OrganizationNotFoundException(organizationName));

        return documentRepository
                .findByIdentifierAndDocFormatAndOrganization(documentId, documentFormat, organization)
                .orElseThrow(() -> new DocumentNotFoundException(
                        "No document with id " + documentId
                                + " in format " + documentFormat
                                + "exists for organization " + organizationName));
    }
}
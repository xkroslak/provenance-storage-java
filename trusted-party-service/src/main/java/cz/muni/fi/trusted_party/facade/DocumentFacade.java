package cz.muni.fi.trusted_party.facade;

import cz.muni.fi.trusted_party.api.Document.DocumentDTO;

public interface DocumentFacade {
    DocumentDTO getDocument(String organizationId, String documentId, String documentFormat);
}
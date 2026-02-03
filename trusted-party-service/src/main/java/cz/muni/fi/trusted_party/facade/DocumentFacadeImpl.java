package cz.muni.fi.trusted_party.facade;

import cz.muni.fi.trusted_party.api.Document.DocumentDTO;
import cz.muni.fi.trusted_party.mappers.DocumentMapper;
import cz.muni.fi.trusted_party.service.DocumentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class DocumentFacadeImpl implements DocumentFacade {

    private final DocumentService documentService;
    private final DocumentMapper documentMapper;

    @Autowired
    public DocumentFacadeImpl(DocumentService documentService, DocumentMapper documentMapper) {
        this.documentService = documentService;
        this.documentMapper = documentMapper;
    }

    @Override
    public DocumentDTO getDocument(String organizationId, String documentId, String documentFormat) {
        return documentMapper.mapToDTO(documentService.getDocument(organizationId, documentId, documentFormat));
    }
}
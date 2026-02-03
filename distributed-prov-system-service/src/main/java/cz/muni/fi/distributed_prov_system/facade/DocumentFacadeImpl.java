package cz.muni.fi.distributed_prov_system.facade;

import cz.muni.fi.distributed_prov_system.api.StoreGraphRequestDTO;
import cz.muni.fi.distributed_prov_system.api.StoreGraphResponseDTO;
import cz.muni.fi.distributed_prov_system.api.SubgraphResponseDTO;
import cz.muni.fi.distributed_prov_system.service.DocumentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class DocumentFacadeImpl implements DocumentFacade {

    private final DocumentService documentService;

    @Autowired
    public DocumentFacadeImpl(DocumentService documentService) {
        this.documentService = documentService;
    }

    @Override
    public StoreGraphResponseDTO storeDocument(String organizationId, String documentId, StoreGraphRequestDTO body) {
        return documentService.storeDocument(organizationId, documentId, body);
    }

    @Override
    public StoreGraphResponseDTO updateDocument(String organizationId, String documentId, StoreGraphRequestDTO body) {
        // Validate, call TP if needed, update
        return documentService.updateDocument(organizationId, documentId, body);
    }

    @Override
    public Object getDocument(String organizationId, String documentId) {
        // Retrieve from DB
        return documentService.getDocument(organizationId, documentId);
    }

    @Override
    public boolean documentExists(String organizationId, String documentId) {
        // Check existence
        return documentService.documentExists(organizationId, documentId);
    }

    @Override
    public SubgraphResponseDTO getDomainSpecificSubgraph(String organizationId, String documentId, String format) {
        // Retrieve domain-specific subgraph
        return documentService.getDomainSpecificSubgraph(organizationId, documentId, format);
    }

    @Override
    public SubgraphResponseDTO getBackboneSubgraph(String organizationId, String documentId, String format) {
        // Retrieve backbone subgraph
        return documentService.getBackboneSubgraph(organizationId, documentId, format);
    }
}
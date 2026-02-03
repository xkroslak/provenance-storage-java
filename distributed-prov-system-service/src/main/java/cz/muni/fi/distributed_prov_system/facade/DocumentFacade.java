package cz.muni.fi.distributed_prov_system.facade;

import cz.muni.fi.distributed_prov_system.api.StoreGraphRequestDTO;
import cz.muni.fi.distributed_prov_system.api.StoreGraphResponseDTO;
import cz.muni.fi.distributed_prov_system.api.SubgraphResponseDTO;

public interface DocumentFacade {
    //TODO: Check the object return type
    StoreGraphResponseDTO storeDocument(String organizationId, String documentId, StoreGraphRequestDTO body);
    StoreGraphResponseDTO updateDocument(String organizationId, String documentId, StoreGraphRequestDTO body);
    Object getDocument(String organizationId, String documentId);
    boolean documentExists(String organizationId, String documentId);
    SubgraphResponseDTO getDomainSpecificSubgraph(String organizationId, String documentId, String format);
    SubgraphResponseDTO getBackboneSubgraph(String organizationId, String documentId, String format);
}
package cz.muni.fi.distributed_prov_system.facade;

import cz.muni.fi.distributed_prov_system.api.MetaResponseDTO;

public interface MetaFacade {

    boolean metaBundleExists(String metaId);

    MetaResponseDTO getMeta(String metaId, String format, String organizationId);
}
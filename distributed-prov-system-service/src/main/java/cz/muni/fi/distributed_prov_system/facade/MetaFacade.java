package cz.muni.fi.distributed_prov_system.facade;

public interface MetaFacade {

    boolean metaBundleExists(String metaId);

    //TODO: change object
    Object getMeta(String metaId, String format, String organizationId);
}
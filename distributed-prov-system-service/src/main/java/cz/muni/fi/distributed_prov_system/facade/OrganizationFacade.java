package cz.muni.fi.distributed_prov_system.facade;

import cz.muni.fi.distributed_prov_system.api.RegisterOrganizationRequestDTO;

public interface OrganizationFacade {
    void register(String organizationId, RegisterOrganizationRequestDTO body);
    void modify(String organizationId, RegisterOrganizationRequestDTO body);
}
package cz.muni.fi.trusted_party.facade;

import cz.muni.fi.trusted_party.api.Organization.OrganizationDTO;
import cz.muni.fi.trusted_party.api.Organization.StoreCertOrganizationDTO;

import java.util.List;

public interface OrganizationFacade {
    List<OrganizationDTO> getAllOrganizations();
    OrganizationDTO getOrganization(String organizationId);
    void registerOrganization(String organizationId, StoreCertOrganizationDTO body);
    OrganizationDTO retrieveCertificates(String organizationId);
    void updateCertificates(String organizationId, StoreCertOrganizationDTO body);
}
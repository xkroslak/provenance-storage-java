package cz.muni.fi.trusted_party.facade;

import cz.muni.fi.trusted_party.api.Organization.OrganizationDTO;
import cz.muni.fi.trusted_party.api.Organization.StoreCertOrganizationDTO;
import cz.muni.fi.trusted_party.api.Organization.UpdateCertificatesDTO;
import cz.muni.fi.trusted_party.mappers.OrganizationMapper;
import cz.muni.fi.trusted_party.service.OrganizationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class OrganizationFacadeImpl implements OrganizationFacade {

    private final OrganizationService organizationService;
    private final OrganizationMapper organizationMapper;

    @Autowired
    public OrganizationFacadeImpl(OrganizationService organizationService, OrganizationMapper organizationMapper) {
        this.organizationService = organizationService;
        this.organizationMapper = organizationMapper;
    }

    @Override
    public List<OrganizationDTO> getAllOrganizations() {
        return organizationMapper.mapToList(organizationService.getAllOrganizations());
    }

    @Override
    public OrganizationDTO getOrganization(String organizationId) {
        return organizationMapper.mapToOrganizationDTO(organizationService.getOrganization(organizationId), false);
    }

    @Override
    public void updateCertificates(String organizationId, StoreCertOrganizationDTO body) {
        organizationService.updateCertificates(organizationId, body);
    }

    @Override
    public OrganizationDTO retrieveCertificates(String organizationId) {
        return organizationMapper.mapToOrganizationDTO(organizationService.getOrganization(organizationId), true);
    }


    @Override
    public void registerOrganization(String organizationId, StoreCertOrganizationDTO body) {
        organizationService.storeCertToOrganization(organizationId, body);
    }

}
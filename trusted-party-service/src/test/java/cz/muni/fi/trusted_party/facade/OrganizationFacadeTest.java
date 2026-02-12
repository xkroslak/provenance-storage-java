package cz.muni.fi.trusted_party.facade;

import cz.muni.fi.trusted_party.api.Organization.OrganizationDTO;
import cz.muni.fi.trusted_party.api.Organization.StoreCertOrganizationDTO;
import cz.muni.fi.trusted_party.data.model.Organization;
import cz.muni.fi.trusted_party.data.records.OrganizationAndCertificates;
import cz.muni.fi.trusted_party.mappers.OrganizationMapper;
import cz.muni.fi.trusted_party.service.OrganizationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrganizationFacadeTest {

    @Mock
    private OrganizationService organizationService;

    @Mock
    private OrganizationMapper organizationMapper;

    @InjectMocks
    private OrganizationFacadeImpl organizationFacade;

    @Test
    void getAllOrganizations_returnsMappedList() {
        Organization organization = new Organization();
        organization.setOrgName("org-1");
        OrganizationAndCertificates orgCert = new OrganizationAndCertificates(organization, null, null);
        OrganizationDTO dto = new OrganizationDTO();
        dto.setOrganizationId("org-1");

        when(organizationService.getAllOrganizations()).thenReturn(List.of(orgCert));
        when(organizationMapper.mapToList(List.of(orgCert))).thenReturn(List.of(dto));

        List<OrganizationDTO> result = organizationFacade.getAllOrganizations();

        assertThat(result).containsExactly(dto);
    }

    @Test
    void getOrganization_returnsMappedOrganization() {
        Organization organization = new Organization();
        organization.setOrgName("org-1");
        OrganizationAndCertificates orgCert = new OrganizationAndCertificates(organization, null, List.of());
        OrganizationDTO dto = new OrganizationDTO();
        dto.setOrganizationId("org-1");

        when(organizationService.getOrganization("org-1")).thenReturn(orgCert);
        when(organizationMapper.mapToOrganizationDTO(orgCert, false)).thenReturn(dto);

        OrganizationDTO result = organizationFacade.getOrganization("org-1");

        assertThat(result).isSameAs(dto);
        verify(organizationMapper).mapToOrganizationDTO(orgCert, false);
    }

    @Test
    void retrieveCertificates_includesRevoked() {
        Organization organization = new Organization();
        organization.setOrgName("org-1");
        OrganizationAndCertificates orgCert = new OrganizationAndCertificates(organization, null, List.of());
        OrganizationDTO dto = new OrganizationDTO();
        dto.setOrganizationId("org-1");

        when(organizationService.getOrganization("org-1")).thenReturn(orgCert);
        when(organizationMapper.mapToOrganizationDTO(orgCert, true)).thenReturn(dto);

        OrganizationDTO result = organizationFacade.retrieveCertificates("org-1");

        assertThat(result).isSameAs(dto);
        verify(organizationMapper).mapToOrganizationDTO(orgCert, true);
    }

    @Test
    void updateCertificates_delegatesToService() {
        StoreCertOrganizationDTO body = new StoreCertOrganizationDTO();
        body.setOrganizationId("org-1");

        organizationFacade.updateCertificates("org-1", body);

        verify(organizationService).updateCertificates("org-1", body);
    }

    @Test
    void registerOrganization_delegatesToService() {
        StoreCertOrganizationDTO body = new StoreCertOrganizationDTO();
        body.setOrganizationId("org-1");

        organizationFacade.registerOrganization("org-1", body);

        verify(organizationService).storeCertToOrganization("org-1", body);
    }
}

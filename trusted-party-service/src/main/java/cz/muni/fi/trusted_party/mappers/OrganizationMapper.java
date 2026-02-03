package cz.muni.fi.trusted_party.mappers;

import cz.muni.fi.trusted_party.api.Organization.OrganizationDTO;
import cz.muni.fi.trusted_party.data.model.Certificate;
import cz.muni.fi.trusted_party.data.records.OrganizationAndCertificates;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class OrganizationMapper {

    public OrganizationDTO mapToOrganizationDTO(OrganizationAndCertificates orgCertEntity, boolean includeRevoked) {
        OrganizationDTO organizationResponseDTO = new OrganizationDTO();
        organizationResponseDTO.setOrganizationId(orgCertEntity.organization().getOrgName());
        organizationResponseDTO.setClientCertificate(orgCertEntity.activeCertificate().getCert());

        if (includeRevoked) {
            List<String> revokedCertificates = new ArrayList<>();
            for (Certificate cert : orgCertEntity.revokedCertificates()) {
                revokedCertificates.add(cert.getCert());
            }
            organizationResponseDTO.setRevokedCertificates(revokedCertificates);
        }

        return organizationResponseDTO;
    }

    public List<OrganizationDTO> mapToList(List<OrganizationAndCertificates> orgCertEntities) {
        return orgCertEntities
                .stream()
                .map(orgCertEntity -> mapToOrganizationDTO(orgCertEntity, false))
                .toList();
    }
}

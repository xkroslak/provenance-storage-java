package cz.muni.fi.trusted_party.utils;

import cz.muni.fi.trusted_party.api.Organization.OrganizationDTO;
import cz.muni.fi.trusted_party.api.Organization.StoreCertOrganizationDTO;
import cz.muni.fi.trusted_party.api.Token.TokenDTO;
import cz.muni.fi.trusted_party.api.Token.TokenRequestDTO;
import cz.muni.fi.trusted_party.data.enums.DocumentType;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public final class TestDataFactory {

    private TestDataFactory() {
    }

    public static TokenRequestDTO tokenRequest() {
        TokenRequestDTO body = new TokenRequestDTO();
        body.setOrganizationId("org-1");
        body.setDocument("ZHVtbXktZG9j");
        body.setDocumentFormat("json");
        body.setDocumentType(DocumentType.GRAPH);
        body.setCreatedOn(LocalDateTime.of(2024, 1, 1, 12, 0).toString());
        body.setSignature("sig");
        return body;
    }

    public static StoreCertOrganizationDTO storeCertRequest() {
        StoreCertOrganizationDTO body = new StoreCertOrganizationDTO();
        body.setOrganizationId("org-1");
        body.setClientCertificate("client-cert");
        body.setIntermediateCertificates(List.of("intermediate-1"));
        return body;
    }

    public static OrganizationDTO organizationDto(String orgId) {
        OrganizationDTO dto = new OrganizationDTO();
        dto.setOrganizationId(orgId);
        return dto;
    }

    public static List<TokenDTO> tokenDtoList(int size) {
        List<TokenDTO> result = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            result.add(new TokenDTO());
        }
        return result;
    }
}

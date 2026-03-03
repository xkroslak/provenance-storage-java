package cz.muni.fi.distributed_prov_system.utils;

import cz.muni.fi.distributed_prov_system.api.MetaResponseDTO;
import cz.muni.fi.distributed_prov_system.api.RegisterOrganizationRequestDTO;
import cz.muni.fi.distributed_prov_system.api.StoreGraphRequestDTO;
import cz.muni.fi.distributed_prov_system.api.StoreGraphResponseDTO;
import cz.muni.fi.distributed_prov_system.api.SubgraphResponseDTO;

import java.util.List;
import java.util.Map;

public final class TestDataFactory {

    private TestDataFactory() {
    }

    public static StoreGraphRequestDTO storeGraphRequest() {
        StoreGraphRequestDTO dto = new StoreGraphRequestDTO();
        dto.setDocument("Z3JhcGg=");
        dto.setDocumentFormat("json");
        dto.setCreatedOn("2026-01-01T10:00:00Z");
        return dto;
    }

    public static StoreGraphResponseDTO storeGraphResponse(String info) {
        StoreGraphResponseDTO dto = new StoreGraphResponseDTO();
        dto.setInfo(info);
        return dto;
    }

    public static SubgraphResponseDTO subgraphResponse(String document) {
        SubgraphResponseDTO dto = new SubgraphResponseDTO();
        dto.setDocument(document);
        return dto;
    }

    public static RegisterOrganizationRequestDTO registerOrganizationRequest() {
        RegisterOrganizationRequestDTO dto = new RegisterOrganizationRequestDTO();
        dto.setClientCertificate("client-cert");
        dto.setIntermediateCertificates(List.of("intermediate-cert"));
        dto.setTrustedPartyUri("tp.local");
        return dto;
    }

    public static MetaResponseDTO metaResponse(String graph, Object token) {
        return new MetaResponseDTO(graph, token);
    }

    public static Map<String, String> documentPayload(String graph) {
        return Map.of("graph", graph);
    }
}
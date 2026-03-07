package cz.muni.fi.distributed_prov_system.utils;

import cz.muni.fi.distributed_prov_system.api.MetaResponseDTO;
import cz.muni.fi.distributed_prov_system.api.RegisterOrganizationRequestDTO;
import cz.muni.fi.distributed_prov_system.api.StoreGraphRequestDTO;
import cz.muni.fi.distributed_prov_system.api.StoreGraphResponseDTO;
import cz.muni.fi.distributed_prov_system.api.SubgraphResponseDTO;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
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

    /**
     * Builds a minimal CPM-compatible PROV-JSON payload for component tests.
     * The payload contains one main activity and one forward connector and is
     * shaped to satisfy InputGraphChecker id and meta URL expectations.
     */
    public static String minimalCpmProvJson(String storageBaseUrl, String orgId, String docId) {
        String normalizedBase = storageBaseUrl.endsWith("/")
                ? storageBaseUrl.substring(0, storageBaseUrl.length() - 1)
                : storageBaseUrl;
        String storagePrefix = normalizedBase + "/api/v1/organizations/" + orgId + "/documents/";
        String metaPrefix = normalizedBase + "/api/v1/documents/meta/";

        return String.format("""
                        {
                            "prefix": {
                                "xsd": "http://www.w3.org/2001/XMLSchema#",
                                "cpm": "https://www.commonprovenancemodel.org/cpm-namespace-v1-0/",
                                "storage": "%s",
                                "dct": "http://purl.org/dc/terms/",
                                "prov": "http://www.w3.org/ns/prov#",
                                "meta": "%s"
                            },
                            "bundle": {
                                "storage:%s": {
                                    "@id": "storage:%s",
                                    "entity": {
                                        "cpm:%s-connector": {
                                            "prov:type": [
                                                {
                                                    "type": "prov:QUALIFIED_NAME",
                                                    "$": "cpm:forwardConnector"
                                                }
                                            ]
                                        }
                                    },
                                    "activity": {
                                        "cpm:bundleActivity%s": {
                                            "prov:type": [
                                                {
                                                    "type": "prov:QUALIFIED_NAME",
                                                    "$": "cpm:mainActivity"
                                                }
                                            ],
                                            "cpm:referencedMetaBundleId": [
                                                {
                                                    "type": "prov:QUALIFIED_NAME",
                                                    "$": "meta:%s_meta"
                                                }
                                            ]
                                        }
                                    },
                                    "wasGeneratedBy": {
                                        "_:n0": {
                                            "prov:entity": "cpm:%s-connector",
                                            "prov:activity": "cpm:bundleActivity%s"
                                        }
                                    }
                                }
                            }
                        }
                        """,
                storagePrefix,
                metaPrefix,
                docId,
                docId,
                docId,
                docId,
                docId,
                docId,
                docId
        );
    }

    public static StoreGraphRequestDTO componentStoreGraphRequest(String storageBaseUrl, String orgId, String docId) {
        String json = minimalCpmProvJson(storageBaseUrl, orgId, docId);
        String encoded = Base64.getEncoder().encodeToString(json.getBytes(StandardCharsets.UTF_8));

        StoreGraphRequestDTO dto = new StoreGraphRequestDTO();
        dto.setDocument(encoded);
        dto.setDocumentFormat("json");
        dto.setCreatedOn(Instant.now().toString());
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
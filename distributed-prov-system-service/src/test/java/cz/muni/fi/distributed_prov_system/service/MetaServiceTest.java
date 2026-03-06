package cz.muni.fi.distributed_prov_system.service;

import cz.muni.fi.distributed_prov_system.client.TrustedPartyClient;
import cz.muni.fi.distributed_prov_system.config.AppProperties;
import cz.muni.fi.distributed_prov_system.data.repository.BundleRepository;
import cz.muni.fi.distributed_prov_system.exceptions.MetaNotFoundException;
import cz.muni.fi.distributed_prov_system.utils.TokenUtils;
import cz.muni.fi.distributed_prov_system.utils.prov.ProvToolboxUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.openprovenance.prov.model.Bundle;
import org.openprovenance.prov.model.Document;
import org.openprovenance.prov.model.Statement;
import org.springframework.data.neo4j.core.Neo4jClient;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MetaServiceTest {

    private static final String NODE_QUERY =
            "MATCH (b:Bundle {identifier:$id})-[:contains]->(n) " +
                    "RETURN labels(n) as labels, n.identifier as identifier, n.attributes as attributes, " +
                    "n.startTime as startTime, n.endTime as endTime";

    private static final String REL_QUERY =
            "MATCH (b:Bundle {identifier:$id})-[:contains]->(a)-[r]->(b2)<-[:contains]-(b) " +
                    "RETURN type(r) as type, a.identifier as fromId, b2.identifier as toId, properties(r) as props";

    @Mock
    private AppProperties appProperties;

    @Mock
    private BundleRepository bundleRepository;

    @Mock
    private OrganizationService organizationService;

    @Mock
    private TrustedPartyClient trustedPartyClient;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private Neo4jClient neo4jClient;

    private MetaService metaService;

    @BeforeEach
    void setUp() {
        metaService = new MetaService(
                appProperties,
                bundleRepository,
                organizationService,
                trustedPartyClient,
                neo4jClient
        );
    }

    @Test
    void metaBundleExists_WhenRepositoryReturnsTrue_ReturnsTrue() {
        when(bundleRepository.existsById("meta-1")).thenReturn(true);

        boolean result = metaService.metaBundleExists("meta-1");

        assertThat(result).isTrue();
    }

    @Test
    void metaBundleExists_WhenRepositoryReturnsFalse_ReturnsFalse() {
        when(bundleRepository.existsById("meta-1")).thenReturn(false);

        boolean result = metaService.metaBundleExists("meta-1");

        assertThat(result).isFalse();
    }

    @Test
    void getB64EncodedMetaProvenance_WhenMetaBundleDoesNotExist_ThrowsMetaNotFoundException() {
        when(bundleRepository.existsById("meta-1")).thenReturn(false);

        assertThatThrownBy(() -> metaService.getB64EncodedMetaProvenance("meta-1", "json"))
                .isInstanceOf(MetaNotFoundException.class)
                .hasMessageContaining("meta-1");
    }

    @Test
    void getB64EncodedMetaProvenance_WhenMetaBundleExists_ReturnsSerializedDocument() {
        when(bundleRepository.existsById("meta-1")).thenReturn(true);
        when(appProperties.getFqdn()).thenReturn("https://app.local");

        Map<String, Object> entityRow = Map.of(
                "labels", List.of("Entity"),
                "identifier", "org-1_doc-1",
                "attributes", Map.of("prov:type", "prov:Bundle")
        );

        when(neo4jClient.query(NODE_QUERY).bind("meta-1").to("id").fetch().all())
                .thenReturn(List.of(entityRow));
        when(neo4jClient.query(REL_QUERY).bind("meta-1").to("id").fetch().all())
                .thenReturn(List.of());

        try (MockedStatic<ProvToolboxUtils> provToolboxUtils = mockStatic(ProvToolboxUtils.class)) {
            provToolboxUtils.when(() -> ProvToolboxUtils.serializeDocumentToBase64(any(Document.class), eq("json")))
                    .thenReturn("b64-meta");

            String result = metaService.getB64EncodedMetaProvenance("meta-1", "json");

            assertThat(result).isEqualTo("b64-meta");
        }
    }

            @Test
            void getB64EncodedMetaProvenance_WhenContainsEntityActivityAgentAndValidRelation_BuildsAllStatements() {
            when(bundleRepository.existsById("meta-1")).thenReturn(true);
            when(appProperties.getFqdn()).thenReturn("https://app.local");

            Map<String, Object> entityRow = Map.of(
                "labels", List.of("Entity"),
                "identifier", "org-1_doc-1",
                "attributes", Map.of("prov:type", "prov:Bundle")
            );
            Map<String, Object> activityRow = Map.of(
                "labels", List.of("Activity"),
                "identifier", "org-1_tokenGeneration",
                "attributes", Map.of("pav:version", 1),
                "startTime", LocalDateTime.of(2026, 1, 1, 10, 0),
                "endTime", LocalDateTime.of(2026, 1, 1, 10, 1)
            );
            Map<String, Object> agentRow = Map.of(
                "labels", List.of("Agent"),
                "identifier", "tp-1",
                "attributes", Map.of("https://www.commonprovenancemodel.org/cpm-namespace-v1-0/trustedPartyUri", "https://tp.local")
            );

            Map<String, Object> usedRelation = Map.of(
                "type", "used",
                "fromId", "org-1_tokenGeneration",
                "toId", "org-1_doc-1",
                "props", Map.of("time", LocalDateTime.of(2026, 1, 1, 10, 0))
            );

            when(neo4jClient.query(NODE_QUERY).bind("meta-1").to("id").fetch().all())
                .thenReturn(List.of(entityRow, activityRow, agentRow));
            when(neo4jClient.query(REL_QUERY).bind("meta-1").to("id").fetch().all())
                .thenReturn(List.of(usedRelation));

            AtomicReference<Document> capturedDocument = new AtomicReference<>();

            try (MockedStatic<ProvToolboxUtils> provToolboxUtils = mockStatic(ProvToolboxUtils.class)) {
                provToolboxUtils.when(() -> ProvToolboxUtils.serializeDocumentToBase64(any(Document.class), eq("json")))
                    .thenAnswer(invocation -> {
                    capturedDocument.set(invocation.getArgument(0));
                    return "b64-meta";
                    });

                String result = metaService.getB64EncodedMetaProvenance("meta-1", "json");

                assertThat(result).isEqualTo("b64-meta");
                assertThat(capturedDocument.get()).isNotNull();

                Bundle bundle = (Bundle) capturedDocument.get().getStatementOrBundle().get(0);
                List<Statement> statements = bundle.getStatement();
                assertThat(statements).hasSize(4);
            }
            }

            @Test
            void getB64EncodedMetaProvenance_WhenRelationTargetsUnknownNode_SkipsRelationStatement() {
            when(bundleRepository.existsById("meta-1")).thenReturn(true);
            when(appProperties.getFqdn()).thenReturn("https://app.local");

            Map<String, Object> entityRow = Map.of(
                "labels", List.of("Entity"),
                "identifier", "org-1_doc-1",
                "attributes", Map.of("prov:type", "prov:Bundle")
            );

            Map<String, Object> danglingRelation = Map.of(
                "type", "used",
                "fromId", "missing-node",
                "toId", "org-1_doc-1",
                "props", Map.of()
            );

            when(neo4jClient.query(NODE_QUERY).bind("meta-1").to("id").fetch().all())
                .thenReturn(List.of(entityRow));
            when(neo4jClient.query(REL_QUERY).bind("meta-1").to("id").fetch().all())
                .thenReturn(List.of(danglingRelation));

            AtomicReference<Document> capturedDocument = new AtomicReference<>();

            try (MockedStatic<ProvToolboxUtils> provToolboxUtils = mockStatic(ProvToolboxUtils.class)) {
                provToolboxUtils.when(() -> ProvToolboxUtils.serializeDocumentToBase64(any(Document.class), eq("json")))
                    .thenAnswer(invocation -> {
                    capturedDocument.set(invocation.getArgument(0));
                    return "b64-meta";
                    });

                String result = metaService.getB64EncodedMetaProvenance("meta-1", "json");

                assertThat(result).isEqualTo("b64-meta");

                Bundle bundle = (Bundle) capturedDocument.get().getStatementOrBundle().get(0);
                assertThat(bundle.getStatement()).hasSize(1);
            }
            }

            @Test
            void getB64EncodedMetaProvenance_WhenRelationTypeIsUnknown_IgnoresRelation() {
            when(bundleRepository.existsById("meta-1")).thenReturn(true);
            when(appProperties.getFqdn()).thenReturn("https://app.local");

            Map<String, Object> entityFromRow = Map.of(
                "labels", List.of("Entity"),
                "identifier", "org-1_doc-1",
                "attributes", Map.of("unknown:custom", "cpm:token")
            );
            Map<String, Object> entityToRow = Map.of(
                "labels", List.of("Entity"),
                "identifier", "org-1_doc-2",
                "attributes", Map.of("prov:type", "prov:Bundle")
            );

            Map<String, Object> unknownRelation = Map.of(
                "type", "some_unknown_relation",
                "fromId", "org-1_doc-1",
                "toId", "org-1_doc-2",
                "props", Map.of("plan", "https://plan.local/1")
            );

            when(neo4jClient.query(NODE_QUERY).bind("meta-1").to("id").fetch().all())
                .thenReturn(List.of(entityFromRow, entityToRow));
            when(neo4jClient.query(REL_QUERY).bind("meta-1").to("id").fetch().all())
                .thenReturn(List.of(unknownRelation));

            AtomicReference<Document> capturedDocument = new AtomicReference<>();

            try (MockedStatic<ProvToolboxUtils> provToolboxUtils = mockStatic(ProvToolboxUtils.class)) {
                provToolboxUtils.when(() -> ProvToolboxUtils.serializeDocumentToBase64(any(Document.class), eq("json")))
                    .thenAnswer(invocation -> {
                    capturedDocument.set(invocation.getArgument(0));
                    return "b64-meta";
                    });

                String result = metaService.getB64EncodedMetaProvenance("meta-1", "json");

                assertThat(result).isEqualTo("b64-meta");

                Bundle bundle = (Bundle) capturedDocument.get().getStatementOrBundle().get(0);
                assertThat(bundle.getStatement()).hasSize(2);
            }
            }

    @Test
    void isTrustedPartyDisabled_WhenPropertyIsTrue_ReturnsTrue() {
        when(appProperties.isDisableTrustedParty()).thenReturn(true);

        boolean result = metaService.isTrustedPartyDisabled();

        assertThat(result).isTrue();
    }

    @Test
    void getTpUrlByOrganization_WhenOrganizationExists_DelegatesToOrganizationService() {
        when(organizationService.getTpUrlByOrganization("org-1")).thenReturn("tp.local");

        String result = metaService.getTpUrlByOrganization("org-1");

        assertThat(result).isEqualTo("tp.local");
        verify(organizationService).getTpUrlByOrganization("org-1");
    }

    @Test
    void buildMetaTokenPayload_WhenCalled_ReturnsPayloadWithExpectedFields() {
        when(appProperties.getId()).thenReturn("app-1");

        Object payload = metaService.buildMetaTokenPayload("graph-b64", "meta-1", "json", "org-1");

        assertThat(payload).isInstanceOf(MetaService.MetaTokenPayload.class);
        MetaService.MetaTokenPayload tokenPayload = (MetaService.MetaTokenPayload) payload;

        assertThat(tokenPayload.document).isEqualTo("graph-b64");
        assertThat(tokenPayload.graphId).isEqualTo("meta-1");
        assertThat(tokenPayload.documentFormat).isEqualTo("json");
        assertThat(tokenPayload.organizationId).isEqualTo("app-1");
        assertThat(tokenPayload.type).isEqualTo("meta");
        assertThat(tokenPayload.createdOn).isPositive();
    }

    @Test
    void sendTokenRequestToTp_WhenTrustedPartyRespondsWith2xx_ReturnsParsedToken() {
        Object payload = new Object();

        when(trustedPartyClient.issueToken(payload, "tp.local"))
                .thenReturn(ResponseEntity.ok("{\"token\":\"abc\"}"));

        try (MockedStatic<TokenUtils> tokenUtils = mockStatic(TokenUtils.class)) {
            tokenUtils.when(() -> TokenUtils.parseTokenResponse("{\"token\":\"abc\"}"))
                    .thenReturn(Map.of("token", "abc"));

            Object result = metaService.sendTokenRequestToTp(payload, "tp.local");

            assertThat(result).isEqualTo(Map.of("token", "abc"));
        }
    }

    @Test
    void sendTokenRequestToTp_WhenTrustedPartyRespondsWithNon2xx_ThrowsIllegalStateException() {
        Object payload = new Object();

        when(trustedPartyClient.issueToken(payload, "tp.local"))
                .thenReturn(ResponseEntity.status(HttpStatus.BAD_GATEWAY).body("error"));

        assertThatThrownBy(() -> metaService.sendTokenRequestToTp(payload, "tp.local"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Could not issue token");
    }

    @Test
    void sendTokenRequestToTp_WhenTrustedPartyRespondsWith2xxAndNullBody_ParsesNullBody() {
        Object payload = new Object();

        when(trustedPartyClient.issueToken(payload, "tp.local"))
                .thenReturn(ResponseEntity.ok(null));

        try (MockedStatic<TokenUtils> tokenUtils = mockStatic(TokenUtils.class)) {
            tokenUtils.when(() -> TokenUtils.parseTokenResponse(null))
                    .thenReturn(Map.of("token", "fallback"));

            Object result = metaService.sendTokenRequestToTp(payload, "tp.local");

            assertThat(result).isEqualTo(Map.of("token", "fallback"));
        }
    }

    @Test
    void sendTokenRequestToTp_WhenTokenParsingFails_PropagatesException() {
        Object payload = new Object();

        when(trustedPartyClient.issueToken(payload, "tp.local"))
                .thenReturn(ResponseEntity.ok("invalid-body"));

        try (MockedStatic<TokenUtils> tokenUtils = mockStatic(TokenUtils.class)) {
            tokenUtils.when(() -> TokenUtils.parseTokenResponse("invalid-body"))
                    .thenThrow(new IllegalArgumentException("parse failed"));

            assertThatThrownBy(() -> metaService.sendTokenRequestToTp(payload, "tp.local"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("parse failed");
        }
    }
}

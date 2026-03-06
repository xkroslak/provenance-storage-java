package cz.muni.fi.distributed_prov_system.service;

import cz.muni.fi.distributed_prov_system.api.StoreGraphRequestDTO;
import cz.muni.fi.distributed_prov_system.config.AppProperties;
import cz.muni.fi.distributed_prov_system.data.model.nodes.Activity;
import cz.muni.fi.distributed_prov_system.data.model.nodes.Agent;
import cz.muni.fi.distributed_prov_system.data.model.nodes.Bundle;
import cz.muni.fi.distributed_prov_system.data.model.nodes.Entity;
import cz.muni.fi.distributed_prov_system.data.model.nonprovmodels.Document;
import cz.muni.fi.distributed_prov_system.data.repository.ActivityRepository;
import cz.muni.fi.distributed_prov_system.data.repository.AgentRepository;
import cz.muni.fi.distributed_prov_system.data.repository.BundleRepository;
import cz.muni.fi.distributed_prov_system.data.repository.DocumentRepository;
import cz.muni.fi.distributed_prov_system.data.repository.EntityRepository;
import cz.muni.fi.distributed_prov_system.exceptions.NotFoundException;
import cz.muni.fi.distributed_prov_system.utils.ProvConstants;
import cz.muni.fi.distributed_prov_system.utils.TokenUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ImportGraphServiceTest {

    @Mock
    private DocumentRepository documentRepository;

    @Mock
    private BundleRepository bundleRepository;

    @Mock
    private EntityRepository entityRepository;

    @Mock
    private ActivityRepository activityRepository;

    @Mock
    private AgentRepository agentRepository;

    @Mock
    private AppProperties appProperties;

    private ImportGraphService importGraphService;

    @BeforeEach
    void setUp() {
        importGraphService = new ImportGraphService(
                documentRepository,
                bundleRepository,
                entityRepository,
                activityRepository,
                agentRepository,
                appProperties
        );
    }

    @Test
    void importGraph_WhenCreateAndMetaBundleMissing_SavesDocumentAndMetaBundle() {
        StoreGraphRequestDTO request = request("graph-b64", "json");
        Map<String, Object> tokenData = tokenData("org-1", "tp-1", 1700000000L);

        when(bundleRepository.findById("meta-1")).thenReturn(Optional.empty());

        Agent existingAgent = new Agent();
        existingAgent.setIdentifier("tp-1");
        when(agentRepository.findById("tp-1")).thenReturn(Optional.of(existingAgent));

        try (MockedStatic<TokenUtils> tokenUtils = mockStatic(TokenUtils.class)) {
            tokenUtils.when(() -> TokenUtils.normalizeTokenData(any(Map.class))).thenReturn(tokenData);

            importGraphService.importGraph(request, Map.of("any", "value"), "org-1", "doc-1", "meta-1", false);
        }

        ArgumentCaptor<Document> docCaptor = ArgumentCaptor.forClass(Document.class);
        verify(documentRepository).save(docCaptor.capture());
        assertThat(docCaptor.getValue().getIdentifier()).isEqualTo("org-1_doc-1");
        assertThat(docCaptor.getValue().getGraph()).isEqualTo("graph-b64");
        assertThat(docCaptor.getValue().getFormat()).isEqualTo("json");

        ArgumentCaptor<Bundle> bundleCaptor = ArgumentCaptor.forClass(Bundle.class);
        verify(bundleRepository, atLeastOnce()).save(bundleCaptor.capture());
        Bundle lastSavedBundle = bundleCaptor.getAllValues().get(bundleCaptor.getAllValues().size() - 1);
        assertThat(lastSavedBundle.getIdentifier()).isEqualTo("meta-1");
        assertThat(lastSavedBundle.getContainsProvs()).isNotNull();
        assertThat(lastSavedBundle.getContainsProvs().size()).isGreaterThanOrEqualTo(4);
    }

    @Test
    void importGraph_WhenCreateAndAgentMissing_SavesNewAgentWithTrustedPartyAttributes() {
        StoreGraphRequestDTO request = request("graph-b64", "json");

        Map<String, Object> tokenData = tokenData("org-1", "tp-2", 1700000001L);
        Map<String, Object> additionalData = new HashMap<>();
        additionalData.put("trustedPartyUri", "tp.local");
        additionalData.put("trustedPartyCertificate", "cert-xyz");
        tokenData.put("additionalData", additionalData);

        Bundle metaBundle = new Bundle();
        metaBundle.setIdentifier("meta-2");
        when(bundleRepository.findById("meta-2")).thenReturn(Optional.of(metaBundle));

        when(agentRepository.findById("tp-2")).thenReturn(Optional.empty());
        when(appProperties.isDisableTrustedParty()).thenReturn(false);

        try (MockedStatic<TokenUtils> tokenUtils = mockStatic(TokenUtils.class)) {
            tokenUtils.when(() -> TokenUtils.normalizeTokenData(any(Map.class))).thenReturn(tokenData);

            importGraphService.importGraph(request, Map.of("env", "value"), "org-1", "doc-2", "meta-2", false);
        }

        ArgumentCaptor<Agent> agentCaptor = ArgumentCaptor.forClass(Agent.class);
        verify(agentRepository).save(agentCaptor.capture());

        Agent savedAgent = agentCaptor.getValue();
        assertThat(savedAgent.getIdentifier()).isEqualTo("tp-2");
        assertThat(savedAgent.getAttributes())
                .containsEntry(ProvConstants.CPM_TRUSTED_PARTY_URI, "tp.local")
                .containsEntry(ProvConstants.CPM_TRUSTED_PARTY_CERTIFICATE, "cert-xyz");
    }

    @Test
    void importGraph_WhenCreateAndTrustedPartyDisabled_DoesNotStoreTrustedPartyExtraAttributes() {
        StoreGraphRequestDTO request = request("graph-b64", "json");

        Map<String, Object> tokenData = tokenData("org-1", "tp-3", 1700000002L);
        Map<String, Object> additionalData = new HashMap<>();
        additionalData.put("trustedPartyUri", "tp.local");
        additionalData.put("trustedPartyCertificate", "cert-xyz");
        tokenData.put("additionalData", additionalData);

        Bundle metaBundle = new Bundle();
        metaBundle.setIdentifier("meta-3");
        when(bundleRepository.findById("meta-3")).thenReturn(Optional.of(metaBundle));

        when(agentRepository.findById("tp-3")).thenReturn(Optional.empty());
        when(appProperties.isDisableTrustedParty()).thenReturn(true);

        try (MockedStatic<TokenUtils> tokenUtils = mockStatic(TokenUtils.class)) {
            tokenUtils.when(() -> TokenUtils.normalizeTokenData(any(Map.class))).thenReturn(tokenData);

            importGraphService.importGraph(request, Map.of("env", "value"), "org-1", "doc-3", "meta-3", false);
        }

        ArgumentCaptor<Agent> agentCaptor = ArgumentCaptor.forClass(Agent.class);
        verify(agentRepository).save(agentCaptor.capture());
        Agent savedAgent = agentCaptor.getValue();

        assertThat(savedAgent.getAttributes())
                .containsEntry(ProvConstants.PROV_TYPE, ProvConstants.CPM_TRUSTED_PARTY)
                .doesNotContainKey(ProvConstants.CPM_TRUSTED_PARTY_URI)
                .doesNotContainKey(ProvConstants.CPM_TRUSTED_PARTY_CERTIFICATE);
    }

    @Test
    void importGraph_WhenUpdateAndMetaBundleMissing_ThrowsNotFoundException() {
        StoreGraphRequestDTO request = request("graph-b64", "json");
        Map<String, Object> tokenData = tokenData("org-1", "tp-1", 1700000000L);

        when(bundleRepository.findById("meta-missing")).thenReturn(Optional.empty());

        try (MockedStatic<TokenUtils> tokenUtils = mockStatic(TokenUtils.class)) {
            tokenUtils.when(() -> TokenUtils.normalizeTokenData(any(Map.class))).thenReturn(tokenData);

            assertThatThrownBy(() -> importGraphService.importGraph(
                    request,
                    Map.of("env", "value"),
                    "org-1",
                    "doc-1",
                    "meta-missing",
                    true
            )).isInstanceOf(NotFoundException.class)
                    .hasMessageContaining("Meta bundle [meta-missing] not found");
        }
    }

    @Test
    void importGraph_WhenUpdateAndTargetEntityMissing_ThrowsNotFoundException() {
        StoreGraphRequestDTO request = request("graph-b64", "json");
        Map<String, Object> tokenData = tokenData("org-1", "tp-1", 1700000000L);

        Bundle metaBundle = new Bundle();
        metaBundle.setIdentifier("meta-1");
        when(bundleRepository.findById("meta-1")).thenReturn(Optional.of(metaBundle));
        when(entityRepository.findById("org-1_doc-1")).thenReturn(Optional.empty());

        try (MockedStatic<TokenUtils> tokenUtils = mockStatic(TokenUtils.class)) {
            tokenUtils.when(() -> TokenUtils.normalizeTokenData(any(Map.class))).thenReturn(tokenData);

            assertThatThrownBy(() -> importGraphService.importGraph(
                    request,
                    Map.of("env", "value"),
                    "org-1",
                    "doc-1",
                    "meta-1",
                    true
            )).isInstanceOf(NotFoundException.class)
                    .hasMessageContaining("Entity [org-1_doc-1] not found");
        }
    }

    @Test
    void importGraph_WhenUpdateAndVersionChainInvalid_ThrowsIllegalStateException() {
        StoreGraphRequestDTO request = request("graph-b64", "json");
        Map<String, Object> tokenData = tokenData("org-1", "tp-1", 1700000000L);

        Bundle metaBundle = new Bundle();
        metaBundle.setIdentifier("meta-1");
        when(bundleRepository.findById("meta-1")).thenReturn(Optional.of(metaBundle));

        Entity entityToUpdate = new Entity();
        entityToUpdate.setIdentifier("org-1_doc-1");
        entityToUpdate.setSpecializationOf(List.of());
        when(entityRepository.findById("org-1_doc-1")).thenReturn(Optional.of(entityToUpdate));

        try (MockedStatic<TokenUtils> tokenUtils = mockStatic(TokenUtils.class)) {
            tokenUtils.when(() -> TokenUtils.normalizeTokenData(any(Map.class))).thenReturn(tokenData);

            assertThatThrownBy(() -> importGraphService.importGraph(
                    request,
                    Map.of("env", "value"),
                    "org-1",
                    "doc-1",
                    "meta-1",
                    true
            )).isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Expected exactly one generator entity");
        }
    }

    @Test
    void importGraph_WhenUpdateIsValid_SavesNewVersionWithIncrementedVersion() {
        StoreGraphRequestDTO request = request("graph-b64-updated", "json");
        Map<String, Object> tokenData = tokenData("org-1", "tp-1", 1700000100L);

        Bundle metaBundle = new Bundle();
        metaBundle.setIdentifier("meta-1");
        when(bundleRepository.findById("meta-1")).thenReturn(Optional.of(metaBundle));

        Entity genEntity = new Entity();
        genEntity.setIdentifier("org_gen");

        Entity existingVersion = new Entity();
        existingVersion.setIdentifier("org-1_doc-1");
        existingVersion.setSpecializationOf(List.of(genEntity));
        Map<String, Object> attrs = new HashMap<>();
        attrs.put(ProvConstants.PAV_VERSION, 2);
        existingVersion.setAttributes(attrs);

        when(entityRepository.findById("org-1_doc-1")).thenReturn(Optional.of(existingVersion));

        Agent existingAgent = new Agent();
        existingAgent.setIdentifier("tp-1");
        when(agentRepository.findById("tp-1")).thenReturn(Optional.of(existingAgent));

        try (MockedStatic<TokenUtils> tokenUtils = mockStatic(TokenUtils.class)) {
            tokenUtils.when(() -> TokenUtils.normalizeTokenData(any(Map.class))).thenReturn(tokenData);

            importGraphService.importGraph(request, Map.of("env", "value"), "org-1", "doc-1", "meta-1", true);
        }

        verify(documentRepository).save(any(Document.class));
        verify(bundleRepository).save(eq(metaBundle));

        ArgumentCaptor<Entity> entityCaptor = ArgumentCaptor.forClass(Entity.class);
        verify(entityRepository, atLeastOnce()).save(entityCaptor.capture());

        Entity newVersion = entityCaptor.getAllValues().stream()
                .filter(entity -> "org-1_doc-1".equals(entity.getIdentifier()))
                .findFirst()
                .orElseThrow();

        assertThat(newVersion.getAttributes()).containsEntry(ProvConstants.PAV_VERSION, 3);
        assertThat(newVersion.getWasRevisionOf()).isNotNull();
        assertThat(newVersion.getWasRevisionOf()).hasSize(1);

        verify(agentRepository, never()).save(any(Agent.class));
    }

    @Test
    void importGraph_WhenUpdateVersionIsMissing_DefaultsToOne() {
        StoreGraphRequestDTO request = request("graph-b64-updated", "json");
        Map<String, Object> tokenData = tokenData("org-1", "tp-1", 1700000200L);

        Bundle metaBundle = new Bundle();
        metaBundle.setIdentifier("meta-1");
        when(bundleRepository.findById("meta-1")).thenReturn(Optional.of(metaBundle));

        Entity genEntity = new Entity();
        genEntity.setIdentifier("org_gen");

        Entity existingVersion = new Entity();
        existingVersion.setIdentifier("org-1_doc-1");
        existingVersion.setSpecializationOf(List.of(genEntity));
        existingVersion.setAttributes(new HashMap<>());

        when(entityRepository.findById("org-1_doc-1")).thenReturn(Optional.of(existingVersion));

        Agent existingAgent = new Agent();
        existingAgent.setIdentifier("tp-1");
        when(agentRepository.findById("tp-1")).thenReturn(Optional.of(existingAgent));

        try (MockedStatic<TokenUtils> tokenUtils = mockStatic(TokenUtils.class)) {
            tokenUtils.when(() -> TokenUtils.normalizeTokenData(any(Map.class))).thenReturn(tokenData);

            importGraphService.importGraph(request, Map.of("env", "value"), "org-1", "doc-1", "meta-1", true);
        }

        ArgumentCaptor<Entity> entityCaptor = ArgumentCaptor.forClass(Entity.class);
        verify(entityRepository, atLeastOnce()).save(entityCaptor.capture());

        Entity newVersion = entityCaptor.getAllValues().stream()
                .filter(entity -> "org-1_doc-1".equals(entity.getIdentifier()))
                .findFirst()
                .orElseThrow();

        assertThat(newVersion.getAttributes()).containsEntry(ProvConstants.PAV_VERSION, 1);
    }

    @Test
    void importGraph_WhenAuthorityIdMissing_ThrowsNullPointerException() {
        StoreGraphRequestDTO request = request("graph-b64", "json");
        Map<String, Object> tokenData = tokenData("org-1", "tp-1", 1700000300L);
        tokenData.remove("authorityId");

        when(bundleRepository.findById("meta-1")).thenReturn(Optional.empty());

        try (MockedStatic<TokenUtils> tokenUtils = mockStatic(TokenUtils.class)) {
            tokenUtils.when(() -> TokenUtils.normalizeTokenData(any(Map.class))).thenReturn(tokenData);

            assertThatThrownBy(() -> importGraphService.importGraph(
                    request,
                    Map.of("env", "value"),
                    "org-1",
                    "doc-1",
                    "meta-1",
                    false
            )).isInstanceOf(NullPointerException.class);
        }
    }

    @Test
    void importGraph_WhenTokenTimestampMissing_SetsNullActivityTimes() {
        StoreGraphRequestDTO request = request("graph-b64", "json");
        Map<String, Object> tokenData = tokenData("org-1", "tp-1", 1700000400L);
        tokenData.remove("tokenTimestamp");

        when(bundleRepository.findById("meta-1")).thenReturn(Optional.empty());

        Agent existingAgent = new Agent();
        existingAgent.setIdentifier("tp-1");
        when(agentRepository.findById("tp-1")).thenReturn(Optional.of(existingAgent));

        try (MockedStatic<TokenUtils> tokenUtils = mockStatic(TokenUtils.class)) {
            tokenUtils.when(() -> TokenUtils.normalizeTokenData(any(Map.class))).thenReturn(tokenData);

            importGraphService.importGraph(
                    request,
                    Map.of("env", "value"),
                    "org-1",
                    "doc-1",
                    "meta-1",
                    false
            );
        }

        ArgumentCaptor<Activity> activityCaptor = ArgumentCaptor.forClass(Activity.class);
        verify(activityRepository).save(activityCaptor.capture());
        Activity savedActivity = activityCaptor.getValue();

        assertThat(savedActivity.getStartTime()).isNull();
        assertThat(savedActivity.getEndTime()).isNull();
    }

    private StoreGraphRequestDTO request(String document, String format) {
        StoreGraphRequestDTO request = new StoreGraphRequestDTO();
        request.setDocument(document);
        request.setDocumentFormat(format);
        return request;
    }

    private Map<String, Object> tokenData(String originatorId, String authorityId, long tokenTimestamp) {
        Map<String, Object> tokenData = new HashMap<>();
        tokenData.put("originatorId", originatorId);
        tokenData.put("authorityId", authorityId);
        tokenData.put("tokenTimestamp", tokenTimestamp);
        tokenData.put("documentCreationTimestamp", tokenTimestamp);
        tokenData.put("documentDigest", "digest");
        tokenData.put("messageTimestamp", tokenTimestamp);
        tokenData.put("signature", "signature");
        return tokenData;
    }
}
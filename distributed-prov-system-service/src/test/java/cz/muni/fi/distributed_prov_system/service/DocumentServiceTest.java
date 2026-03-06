package cz.muni.fi.distributed_prov_system.service;

import cz.muni.fi.distributed_prov_system.api.StoreGraphRequestDTO;
import cz.muni.fi.distributed_prov_system.api.StoreGraphResponseDTO;
import cz.muni.fi.distributed_prov_system.api.SubgraphResponseDTO;
import cz.muni.fi.distributed_prov_system.client.TrustedPartyClient;
import cz.muni.fi.distributed_prov_system.config.AppProperties;
import cz.muni.fi.distributed_prov_system.data.model.nodes.Bundle;
import cz.muni.fi.distributed_prov_system.data.model.nodes.Entity;
import cz.muni.fi.distributed_prov_system.data.model.nonprovmodels.Document;
import cz.muni.fi.distributed_prov_system.data.model.nonprovmodels.Token;
import cz.muni.fi.distributed_prov_system.data.model.nonprovmodels.TrustedParty;
import cz.muni.fi.distributed_prov_system.data.model.nonprovmodels.DefaultTrustedParty;
import cz.muni.fi.distributed_prov_system.data.repository.BundleRepository;
import cz.muni.fi.distributed_prov_system.data.repository.DefaultTrustedPartyRepository;
import cz.muni.fi.distributed_prov_system.data.repository.DocumentRepository;
import cz.muni.fi.distributed_prov_system.data.repository.EntityRepository;
import cz.muni.fi.distributed_prov_system.data.repository.TokenRepository;
import cz.muni.fi.distributed_prov_system.data.repository.TrustedPartyRepository;
import cz.muni.fi.distributed_prov_system.exceptions.BadRequestException;
import cz.muni.fi.distributed_prov_system.exceptions.ConflictException;
import cz.muni.fi.distributed_prov_system.exceptions.NotFoundException;
import cz.muni.fi.distributed_prov_system.exceptions.UnauthorizedException;
import cz.muni.fi.distributed_prov_system.utils.TokenUtils;
import cz.muni.fi.distributed_prov_system.utils.prov.InputGraphChecker;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DocumentServiceTest {

    @Mock
    private AppProperties appProperties;

    @Mock
    private TrustedPartyClient tpClient;

    @Mock
    private OrganizationService organizationService;

    @Mock
    private ImportGraphService importGraphService;

    @Mock
    private DocumentRepository documentRepository;

    @Mock
    private TokenRepository tokenRepository;

    @Mock
    private EntityRepository entityRepository;

    @Mock
    private BundleRepository bundleRepository;

    @Mock
    private TrustedPartyRepository trustedPartyRepository;

    @Mock
    private DefaultTrustedPartyRepository defaultTrustedPartyRepository;

    private DocumentService documentService;

    @BeforeEach
    void setUp() {
        documentService = new DocumentService(
                appProperties,
                tpClient,
                organizationService,
                importGraphService,
                documentRepository,
                tokenRepository,
                entityRepository,
                bundleRepository,
                trustedPartyRepository,
                defaultTrustedPartyRepository
        );
    }

    @Test
    void storeDocument_WhenDocumentFieldsMissing_ThrowsIllegalArgumentException() {
        StoreGraphRequestDTO request = new StoreGraphRequestDTO();

        assertThatThrownBy(() -> documentService.storeDocument("org-1", "doc-1", request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Missing document or documentFormat");
    }

    @Test
    void storeDocument_WhenCheckerParsingFails_ThrowsBadRequestException() {
        StoreGraphRequestDTO request = request();

        try (MockedConstruction<InputGraphChecker> checkerConstruction = mockConstruction(
                InputGraphChecker.class,
            (checker, context) -> doThrow(new IllegalArgumentException("bad graph")).when(checker).parseGraph()
        )) {
            assertThatThrownBy(() -> documentService.storeDocument("org-1", "doc-1", request))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("bad graph");

            assertThat(checkerConstruction.constructed()).hasSize(1);
        }
    }

    @Test
    void storeDocument_WhenDocumentAlreadyExists_ThrowsConflictException() {
        StoreGraphRequestDTO request = request();

        try (MockedConstruction<InputGraphChecker> checkerConstruction = checkerConstruction("doc-1", "meta-1")) {
            when(documentRepository.existsById("org-1_doc-1")).thenReturn(true);

            assertThatThrownBy(() -> documentService.storeDocument("org-1", "doc-1", request))
                    .isInstanceOf(ConflictException.class)
                    .hasMessageContaining("already exists");

            assertThat(checkerConstruction.constructed()).hasSize(1);
        }
    }

    @Test
    void storeDocument_WhenTrustedPartyEnabledAndOrganizationNotRegistered_ThrowsNotFoundException() {
        StoreGraphRequestDTO request = request();

        try (MockedConstruction<InputGraphChecker> checkerConstruction = checkerConstruction("doc-1", "meta-1")) {
            when(documentRepository.existsById("org-1_doc-1")).thenReturn(false);
            when(appProperties.isDisableTrustedParty()).thenReturn(false);
            when(organizationService.isRegistered("org-1")).thenReturn(false);

            assertThatThrownBy(() -> documentService.storeDocument("org-1", "doc-1", request))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessageContaining("not registered");

            assertThat(checkerConstruction.constructed()).hasSize(1);
        }
    }

    @Test
    void storeDocument_WhenTrustedPartyEnabledAndMissingSignature_ThrowsBadRequestException() {
        StoreGraphRequestDTO request = request();
        request.setSignature(null);

        try (MockedConstruction<InputGraphChecker> checkerConstruction = checkerConstruction("doc-1", "meta-1")) {
            when(documentRepository.existsById("org-1_doc-1")).thenReturn(false);
            when(appProperties.isDisableTrustedParty()).thenReturn(false);
            when(organizationService.isRegistered("org-1")).thenReturn(true);

            assertThatThrownBy(() -> documentService.storeDocument("org-1", "doc-1", request))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("Missing signature or createdOn");

            assertThat(checkerConstruction.constructed()).hasSize(1);
        }
    }

    @Test
    void storeDocument_WhenVerifySignatureFails_ThrowsUnauthorizedException() {
        StoreGraphRequestDTO request = request();
        Map<String, Object> payload = Map.of("k", "v");

        try (MockedConstruction<InputGraphChecker> checkerConstruction = checkerConstruction("doc-1", "meta-1");
             MockedStatic<TokenUtils> tokenUtils = mockStatic(TokenUtils.class)) {
            when(documentRepository.existsById("org-1_doc-1")).thenReturn(false);
            when(appProperties.isDisableTrustedParty()).thenReturn(false);
            when(organizationService.isRegistered("org-1")).thenReturn(true);
            when(organizationService.getTpUrlByOrganization("org-1")).thenReturn("tp.local");

            tokenUtils.when(() -> TokenUtils.buildTokenPayload(request, "org-1", "doc-1")).thenReturn(payload);
            when(tpClient.verifySignature(payload, "tp.local"))
                    .thenReturn(ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("nope"));

            assertThatThrownBy(() -> documentService.storeDocument("org-1", "doc-1", request))
                    .isInstanceOf(UnauthorizedException.class)
                    .hasMessageContaining("Unverifiable signature");

            assertThat(checkerConstruction.constructed()).hasSize(1);
        }
    }

    @Test
    void storeDocument_WhenTrustedPartyEnabledAndValidToken_ImportsGraphAndStoresToken() {
        StoreGraphRequestDTO request = request();
        Map<String, Object> payload = Map.of("k", "v");
        Map<String, Object> envelope = tokenEnvelope();
        Map<String, Object> normalized = normalizedTokenData();

        try (MockedConstruction<InputGraphChecker> checkerConstruction = checkerConstruction("doc-1", "meta-1");
             MockedStatic<TokenUtils> tokenUtils = mockStatic(TokenUtils.class)) {
            when(documentRepository.existsById("org-1_doc-1")).thenReturn(false);
            when(appProperties.isDisableTrustedParty()).thenReturn(false);
            when(organizationService.isRegistered("org-1")).thenReturn(true);
            when(organizationService.getTpUrlByOrganization("org-1")).thenReturn("tp.local");

            tokenUtils.when(() -> TokenUtils.buildTokenPayload(request, "org-1", "doc-1")).thenReturn(payload);
            when(tpClient.verifySignature(payload, "tp.local")).thenReturn(ResponseEntity.ok("ok"));
            when(tpClient.issueToken(payload, "tp.local")).thenReturn(ResponseEntity.ok("{token}"));
            tokenUtils.when(() -> TokenUtils.parseTokenResponse("{token}")).thenReturn(envelope);
            tokenUtils.when(() -> TokenUtils.normalizeTokenData(envelope)).thenReturn(normalized);

            Document document = new Document();
            document.setIdentifier("org-1_doc-1");
            when(documentRepository.findById("org-1_doc-1")).thenReturn(Optional.of(document));

            TrustedParty trustedParty = new TrustedParty();
            trustedParty.setIdentifier("tp-1");
            when(trustedPartyRepository.findById("tp-1")).thenReturn(Optional.of(trustedParty));

            StoreGraphResponseDTO response = documentService.storeDocument("org-1", "doc-1", request);

            assertThat(response.getToken()).isNotNull();
            assertThat(response.getInfo()).isNull();

            verify(importGraphService).importGraph(request, envelope, "org-1", "doc-1", "meta-1", false);

            ArgumentCaptor<Token> tokenCaptor = ArgumentCaptor.forClass(Token.class);
            verify(tokenRepository).save(tokenCaptor.capture());
            Token savedToken = tokenCaptor.getValue();

            assertThat(savedToken.getAuthorityId()).isEqualTo("tp-1");
            assertThat(savedToken.getOriginatorId()).isEqualTo("org-1");
            assertThat(savedToken.getBelongsTo()).isSameAs(document);
            assertThat(savedToken.getWasIssuedBy()).isSameAs(trustedParty);

            assertThat(checkerConstruction.constructed()).hasSize(1);
        }
    }

    @Test
    void storeDocument_WhenTrustedPartyDisabled_ImportsGraphWithDummyTokenAndReturnsInfo() {
        StoreGraphRequestDTO request = request();
        Map<String, Object> dummyToken = Map.of("dummy", true);

        try (MockedConstruction<InputGraphChecker> checkerConstruction = checkerConstruction("doc-1", "meta-1");
             MockedStatic<TokenUtils> tokenUtils = mockStatic(TokenUtils.class)) {
            when(documentRepository.existsById("org-1_doc-1")).thenReturn(false);
            when(appProperties.isDisableTrustedParty()).thenReturn(true);
            tokenUtils.when(() -> TokenUtils.createDummyToken("org-1")).thenReturn(dummyToken);

            StoreGraphResponseDTO response = documentService.storeDocument("org-1", "doc-1", request);

            assertThat(response.getToken()).isNull();
            assertThat(response.getInfo()).contains("Trusted party is disabled");
            verify(importGraphService).importGraph(request, dummyToken, "org-1", "doc-1", "meta-1", false);

            assertThat(checkerConstruction.constructed()).hasSize(1);
        }
    }

    @Test
    void updateDocument_WhenDocumentFieldsMissing_ThrowsIllegalArgumentException() {
        StoreGraphRequestDTO request = new StoreGraphRequestDTO();

        assertThatThrownBy(() -> documentService.updateDocument("org-1", "doc-1", request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Missing document or documentFormat");
    }

    @Test
    void updateDocument_WhenMetaBundleDoesNotExist_ThrowsBadRequestException() {
        StoreGraphRequestDTO request = request();

        try (MockedConstruction<InputGraphChecker> checkerConstruction = checkerConstruction("doc-1", "meta-1")) {
            Entity entity = new Entity();
            entity.setIdentifier("org-1_doc-1");
            when(entityRepository.findById("org-1_doc-1")).thenReturn(Optional.of(entity));
            when(bundleRepository.existsById("meta-1")).thenReturn(false);

            assertThatThrownBy(() -> documentService.updateDocument("org-1", "doc-1", request))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("Meta provenance");

            assertThat(checkerConstruction.constructed()).hasSize(1);
        }
    }

    @Test
    void updateDocument_WhenEntityDoesNotExist_ThrowsNotFoundException() {
        StoreGraphRequestDTO request = request();

        try (MockedConstruction<InputGraphChecker> checkerConstruction = checkerConstruction("doc-1", "meta-1")) {
            when(entityRepository.findById("org-1_doc-1")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> documentService.updateDocument("org-1", "doc-1", request))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessageContaining("does not exist under organization");

            assertThat(checkerConstruction.constructed()).hasSize(1);
        }
    }

    @Test
    void updateDocument_WhenEntityContainsMultipleMetaBundles_ThrowsBadRequestException() {
        StoreGraphRequestDTO request = request();

        try (MockedConstruction<InputGraphChecker> checkerConstruction = checkerConstruction("doc-1", "meta-1")) {
            Entity entity = new Entity();
            Bundle b1 = new Bundle();
            b1.setIdentifier("meta-1");
            Bundle b2 = new Bundle();
            b2.setIdentifier("meta-2");
            entity.setContains(List.of(b1, b2));

            when(entityRepository.findById("org-1_doc-1")).thenReturn(Optional.of(entity));
            when(bundleRepository.existsById("meta-1")).thenReturn(true);

            assertThatThrownBy(() -> documentService.updateDocument("org-1", "doc-1", request))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("more than one meta bundles");

            assertThat(checkerConstruction.constructed()).hasSize(1);
        }
    }

    @Test
    void updateDocument_WhenEntityMetaBundleDiffersFromCheckerMeta_ThrowsBadRequestException() {
        StoreGraphRequestDTO request = request();

        try (MockedConstruction<InputGraphChecker> checkerConstruction = checkerConstruction("doc-1", "meta-1")) {
            Entity entity = new Entity();
            Bundle actualMeta = new Bundle();
            actualMeta.setIdentifier("meta-other");
            entity.setContains(List.of(actualMeta));

            when(entityRepository.findById("org-1_doc-1")).thenReturn(Optional.of(entity));
            when(bundleRepository.existsById("meta-1")).thenReturn(true);

            assertThatThrownBy(() -> documentService.updateDocument("org-1", "doc-1", request))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("different id");

            assertThat(checkerConstruction.constructed()).hasSize(1);
        }
    }

    @Test
    void updateDocument_WhenValidatedButMainDocumentMissing_ThrowsNotFoundException() {
        StoreGraphRequestDTO request = request();

        try (MockedConstruction<InputGraphChecker> checkerConstruction = checkerConstruction("doc-1", "meta-1")) {
            Entity entity = new Entity();
            Bundle bundle = new Bundle();
            bundle.setIdentifier("meta-1");
            entity.setContains(List.of(bundle));

            when(entityRepository.findById("org-1_doc-1")).thenReturn(Optional.of(entity));
            when(bundleRepository.existsById("meta-1")).thenReturn(true);
            when(documentRepository.existsById("org-1_doc-1")).thenReturn(false);

            assertThatThrownBy(() -> documentService.updateDocument("org-1", "doc-1", request))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessageContaining("Please check whether the ID you have given is correct");

            assertThat(checkerConstruction.constructed()).hasSize(1);
        }
    }

    @Test
    void updateDocument_WhenTrustedPartyEnabledAndVerificationFails_ThrowsUnauthorizedException() {
        StoreGraphRequestDTO request = request();
        Map<String, Object> payload = Map.of("k", "v");

        try (MockedConstruction<InputGraphChecker> checkerConstruction = checkerConstruction("doc-1", "meta-1");
             MockedStatic<TokenUtils> tokenUtils = mockStatic(TokenUtils.class)) {
            Entity entity = new Entity();
            Bundle bundle = new Bundle();
            bundle.setIdentifier("meta-1");
            entity.setContains(List.of(bundle));

            when(entityRepository.findById("org-1_doc-1")).thenReturn(Optional.of(entity));
            when(bundleRepository.existsById("meta-1")).thenReturn(true);
            when(documentRepository.existsById("org-1_doc-1")).thenReturn(true);
            when(appProperties.isDisableTrustedParty()).thenReturn(false);
            when(organizationService.isRegistered("org-1")).thenReturn(true);
            when(organizationService.getTpUrlByOrganization("org-1")).thenReturn("tp.local");

            tokenUtils.when(() -> TokenUtils.buildTokenPayload(request, "org-1", "doc-1")).thenReturn(payload);
            when(tpClient.verifySignature(payload, "tp.local"))
                    .thenReturn(ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("no"));

            assertThatThrownBy(() -> documentService.updateDocument("org-1", "doc-1", request))
                    .isInstanceOf(UnauthorizedException.class)
                    .hasMessageContaining("Unverifiable signature");

            assertThat(checkerConstruction.constructed()).hasSize(1);
        }
    }

    @Test
    void updateDocument_WhenTrustedPartyEnabledAndTokenIssuanceFails_ThrowsIllegalStateException() {
        StoreGraphRequestDTO request = request();
        Map<String, Object> payload = Map.of("k", "v");

        try (MockedConstruction<InputGraphChecker> checkerConstruction = checkerConstruction("doc-1", "meta-1");
             MockedStatic<TokenUtils> tokenUtils = mockStatic(TokenUtils.class)) {
            Entity entity = new Entity();
            Bundle bundle = new Bundle();
            bundle.setIdentifier("meta-1");
            entity.setContains(List.of(bundle));

            when(entityRepository.findById("org-1_doc-1")).thenReturn(Optional.of(entity));
            when(bundleRepository.existsById("meta-1")).thenReturn(true);
            when(documentRepository.existsById("org-1_doc-1")).thenReturn(true);
            when(appProperties.isDisableTrustedParty()).thenReturn(false);
            when(organizationService.isRegistered("org-1")).thenReturn(true);
            when(organizationService.getTpUrlByOrganization("org-1")).thenReturn("tp.local");

            tokenUtils.when(() -> TokenUtils.buildTokenPayload(request, "org-1", "doc-1")).thenReturn(payload);
            when(tpClient.verifySignature(payload, "tp.local")).thenReturn(ResponseEntity.ok("ok"));
            when(tpClient.issueToken(payload, "tp.local"))
                    .thenReturn(ResponseEntity.status(HttpStatus.BAD_GATEWAY).body("err"));

            assertThatThrownBy(() -> documentService.updateDocument("org-1", "doc-1", request))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Could not issue token");

            assertThat(checkerConstruction.constructed()).hasSize(1);
        }
    }

    @Test
    void updateDocument_WhenTrustedPartyEnabledAndValidToken_ImportsUpdateAndStoresToken() {
        StoreGraphRequestDTO request = request();
        Map<String, Object> payload = Map.of("k", "v");
        Map<String, Object> envelope = tokenEnvelope();
        Map<String, Object> normalized = normalizedTokenData();

        try (MockedConstruction<InputGraphChecker> checkerConstruction = checkerConstruction("doc-1", "meta-1");
             MockedStatic<TokenUtils> tokenUtils = mockStatic(TokenUtils.class)) {
            Entity entity = new Entity();
            Bundle bundle = new Bundle();
            bundle.setIdentifier("meta-1");
            entity.setContains(List.of(bundle));

            when(entityRepository.findById("org-1_doc-1")).thenReturn(Optional.of(entity));
            when(bundleRepository.existsById("meta-1")).thenReturn(true);
            when(documentRepository.existsById("org-1_doc-1")).thenReturn(true);
            when(appProperties.isDisableTrustedParty()).thenReturn(false);
            when(organizationService.isRegistered("org-1")).thenReturn(true);
            when(organizationService.getTpUrlByOrganization("org-1")).thenReturn("tp.local");

            tokenUtils.when(() -> TokenUtils.buildTokenPayload(request, "org-1", "doc-1")).thenReturn(payload);
            when(tpClient.verifySignature(payload, "tp.local")).thenReturn(ResponseEntity.ok("ok"));
            when(tpClient.issueToken(payload, "tp.local")).thenReturn(ResponseEntity.ok("{token}"));
            tokenUtils.when(() -> TokenUtils.parseTokenResponse("{token}")).thenReturn(envelope);
            tokenUtils.when(() -> TokenUtils.normalizeTokenData(envelope)).thenReturn(normalized);

            Document document = new Document();
            document.setIdentifier("org-1_doc-1");
            when(documentRepository.findById("org-1_doc-1")).thenReturn(Optional.of(document));

            TrustedParty trustedParty = new TrustedParty();
            trustedParty.setIdentifier("tp-1");
            when(trustedPartyRepository.findById("tp-1")).thenReturn(Optional.of(trustedParty));

            StoreGraphResponseDTO response = documentService.updateDocument("org-1", "doc-1", request);

            assertThat(response.getToken()).isNotNull();
            assertThat(response.getInfo()).isNull();
            verify(importGraphService).importGraph(request, envelope, "org-1", "doc-1", "meta-1", true);
            verify(tokenRepository).save(any(Token.class));

            assertThat(checkerConstruction.constructed()).hasSize(1);
        }
    }

    @Test
    void updateDocument_WhenTrustedPartyDisabled_ImportsUpdateAndReturnsInfo() {
        StoreGraphRequestDTO request = request();

        try (MockedConstruction<InputGraphChecker> checkerConstruction = checkerConstruction("doc-1", "meta-1");
             MockedStatic<TokenUtils> tokenUtils = mockStatic(TokenUtils.class)) {
            Entity entity = new Entity();
            Bundle bundle = new Bundle();
            bundle.setIdentifier("meta-1");
            entity.setContains(List.of(bundle));

            when(entityRepository.findById("org-1_doc-1")).thenReturn(Optional.of(entity));
            when(bundleRepository.existsById("meta-1")).thenReturn(true);
            when(documentRepository.existsById("org-1_doc-1")).thenReturn(true);
            when(appProperties.isDisableTrustedParty()).thenReturn(true);

            Map<String, Object> dummyToken = Map.of("dummy", true);
            tokenUtils.when(() -> TokenUtils.createDummyToken("org-1")).thenReturn(dummyToken);

            StoreGraphResponseDTO response = documentService.updateDocument("org-1", "doc-1", request);

            assertThat(response.getToken()).isNull();
            assertThat(response.getInfo()).contains("Trusted party is disabled");
            verify(importGraphService).importGraph(request, dummyToken, "org-1", "doc-1", "meta-1", true);

            assertThat(checkerConstruction.constructed()).hasSize(1);
        }
    }

    @Test
    void getDocument_WhenDocumentDoesNotExist_ThrowsNotFoundException() {
        when(documentRepository.findById("org-1_doc-1")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> documentService.getDocument("org-1", "doc-1"))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("does not exist");
    }

    @Test
    void getDocument_WhenTrustedPartyDisabled_ReturnsDocumentWithoutToken() {
        Document document = new Document();
        document.setIdentifier("org-1_doc-1");
        document.setGraph("graph-b64");

        when(documentRepository.findById("org-1_doc-1")).thenReturn(Optional.of(document));
        when(appProperties.isDisableTrustedParty()).thenReturn(true);

        Object response = documentService.getDocument("org-1", "doc-1");

        assertThat(response).isInstanceOf(Map.class);
        @SuppressWarnings("unchecked")
        Map<String, Object> responseMap = (Map<String, Object>) response;
        assertThat(responseMap.get("document")).isEqualTo("graph-b64");
        assertThat(responseMap).doesNotContainKey("token");
    }

    @Test
    void getDocument_WhenTrustedPartyEnabledAndTokenExists_ReturnsStoredTokenEnvelope() {
        Document document = new Document();
        document.setIdentifier("org-1_doc-1");
        document.setGraph("graph-b64");

        Token token = new Token();
        token.setSignature("sig");
        token.setOriginatorId("org-1");
        token.setAuthorityId("tp-1");
        token.setTokenTimestamp(1L);
        token.setMessageTimestamp(2L);
        token.setDocumentDigest("digest");
        token.setAdditionalData(Map.of("x", "y"));

        TrustedParty trustedParty = new TrustedParty();
        trustedParty.setIdentifier("tp-1");

        when(documentRepository.findById("org-1_doc-1")).thenReturn(Optional.of(document));
        when(appProperties.isDisableTrustedParty()).thenReturn(false);
        when(organizationService.isRegistered("org-1")).thenReturn(true);
        when(organizationService.getTrustedPartyForOrganization("org-1")).thenReturn(trustedParty);
        when(tokenRepository.findLatestByDocumentIdentifierAndTpId("org-1_doc-1", "tp-1"))
                .thenReturn(Optional.of(token));

        Object response = documentService.getDocument("org-1", "doc-1");

        assertThat(response).isInstanceOf(Map.class);
        @SuppressWarnings("unchecked")
        Map<String, Object> responseMap = (Map<String, Object>) response;
        assertThat(responseMap.get("document")).isEqualTo("graph-b64");
        assertThat(responseMap).containsKey("token");

        verify(tpClient, never()).issueToken(any(), any());
    }

    @Test
    void getDocument_WhenNoStoredToken_IssuesTokenAndStoresIt() {
        Document document = new Document();
        document.setIdentifier("org-1_doc-1");
        document.setGraph("graph-b64");

        Map<String, Object> envelope = tokenEnvelope();
        Map<String, Object> normalized = normalizedTokenData();

        when(documentRepository.findById("org-1_doc-1")).thenReturn(Optional.of(document));
        when(appProperties.isDisableTrustedParty()).thenReturn(false);
        when(organizationService.isRegistered("org-1")).thenReturn(false);
        when(appProperties.getTpFqdn()).thenReturn("tp.default");
        when(tokenRepository.findLatestByDocumentIdentifierAndDefaultTp("org-1_doc-1"))
                .thenReturn(Optional.empty());
        when(tpClient.issueToken(any(), eq("tp.default"))).thenReturn(ResponseEntity.ok("{token}"));

        TrustedParty trustedParty = new TrustedParty();
        trustedParty.setIdentifier("tp-1");
        when(trustedPartyRepository.findById("tp-1")).thenReturn(Optional.of(trustedParty));

        try (MockedStatic<TokenUtils> tokenUtils = mockStatic(TokenUtils.class)) {
            tokenUtils.when(() -> TokenUtils.parseTokenResponse("{token}")).thenReturn(envelope);
            tokenUtils.when(() -> TokenUtils.normalizeTokenData(envelope)).thenReturn(normalized);

            Object response = documentService.getDocument("org-1", "doc-1");

            assertThat(response).isInstanceOf(Map.class);
            @SuppressWarnings("unchecked")
            Map<String, Object> responseMap = (Map<String, Object>) response;
            assertThat(responseMap.get("document")).isEqualTo("graph-b64");
            assertThat(responseMap).containsKey("token");

            verify(tokenRepository).save(any(Token.class));
        }
    }

            @Test
            void getDocument_WhenNoStoredTokenAndIssuanceFails_ThrowsIllegalStateException() {
            Document document = new Document();
            document.setIdentifier("org-1_doc-1");
            document.setGraph("graph-b64");

            when(documentRepository.findById("org-1_doc-1")).thenReturn(Optional.of(document));
            when(appProperties.isDisableTrustedParty()).thenReturn(false);
            when(organizationService.isRegistered("org-1")).thenReturn(false);
            when(appProperties.getTpFqdn()).thenReturn("tp.default");
            when(tokenRepository.findLatestByDocumentIdentifierAndDefaultTp("org-1_doc-1"))
                .thenReturn(Optional.empty());
            when(tpClient.issueToken(any(), eq("tp.default")))
                .thenReturn(ResponseEntity.status(HttpStatus.BAD_GATEWAY).body("err"));

            assertThatThrownBy(() -> documentService.getDocument("org-1", "doc-1"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Could not issue token");
            }

    @Test
    void documentExists_WhenRepositoryReturnsTrue_ReturnsTrue() {
        when(documentRepository.existsById("org-1_doc-1")).thenReturn(true);

        boolean result = documentService.documentExists("org-1", "doc-1");

        assertThat(result).isTrue();
    }

    @Test
    void getDomainSpecificSubgraph_WhenUnsupportedFormat_ThrowsBadRequestException() {
        assertThatThrownBy(() -> documentService.getDomainSpecificSubgraph("org-1", "doc-1", "yaml"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("not supported");
    }

    @Test
    void getDomainSpecificSubgraph_WhenSubgraphExistsAndTrustedPartyDisabled_ReturnsWithoutToken() {
        Document subgraph = new Document();
        subgraph.setIdentifier("org-1_doc-1_domain");
        subgraph.setFormat("json");
        subgraph.setGraph("domain-b64");

        when(documentRepository.findByIdentifierAndFormat("org-1_doc-1_domain", "json"))
                .thenReturn(Optional.of(subgraph));
        when(appProperties.isDisableTrustedParty()).thenReturn(true);

        SubgraphResponseDTO response = documentService.getDomainSpecificSubgraph("org-1", "doc-1", "json");

        assertThat(response.getDocument()).isEqualTo("domain-b64");
        assertThat(response.getToken()).isNull();
    }

    @Test
    void getBackboneSubgraph_WhenSubgraphExistsAndNoToken_IssuesTokenAndStoresIt() {
        Document subgraph = new Document();
        subgraph.setIdentifier("org-1_doc-1_backbone");
        subgraph.setFormat("rdf");
        subgraph.setGraph("backbone-b64");

        when(documentRepository.findByIdentifierAndFormat("org-1_doc-1_backbone", "rdf"))
                .thenReturn(Optional.of(subgraph));
        when(appProperties.isDisableTrustedParty()).thenReturn(false);
        when(tokenRepository.findLatestByDocumentIdentifier("org-1_doc-1_backbone"))
                .thenReturn(Optional.empty());
        when(organizationService.getTpUrlByOrganization("org-1")).thenReturn("tp.local");
        when(tpClient.issueToken(any(), eq("tp.local"))).thenReturn(ResponseEntity.ok("{token}"));

        Document storedBackboneDoc = new Document();
        storedBackboneDoc.setIdentifier("org-1_doc-1_backbone");
        when(documentRepository.findById("org-1_doc-1_backbone")).thenReturn(Optional.of(storedBackboneDoc));

        Map<String, Object> envelope = tokenEnvelope();
        Map<String, Object> normalized = normalizedTokenData();
        TrustedParty trustedParty = new TrustedParty();
        trustedParty.setIdentifier("tp-1");
        when(trustedPartyRepository.findById("tp-1")).thenReturn(Optional.of(trustedParty));

        try (MockedStatic<TokenUtils> tokenUtils = mockStatic(TokenUtils.class)) {
            tokenUtils.when(() -> TokenUtils.parseTokenResponse("{token}")).thenReturn(envelope);
            tokenUtils.when(() -> TokenUtils.normalizeTokenData(envelope)).thenReturn(normalized);

            SubgraphResponseDTO response = documentService.getBackboneSubgraph("org-1", "doc-1", "rdf");

            assertThat(response.getDocument()).isEqualTo("backbone-b64");
            assertThat(response.getToken()).isNotNull();
            verify(tokenRepository).save(any(Token.class));
        }
    }

            @Test
            void getBackboneSubgraph_WhenSubgraphExistsAndCachedTokenPresent_ReturnsCachedTokenWithoutTpCall() {
            Document subgraph = new Document();
            subgraph.setIdentifier("org-1_doc-1_backbone");
            subgraph.setFormat("rdf");
            subgraph.setGraph("backbone-b64");

            Token cachedToken = new Token();
            cachedToken.setSignature("sig");
            cachedToken.setAuthorityId("tp-1");
            cachedToken.setOriginatorId("org-1");
            cachedToken.setTokenTimestamp(1L);
            cachedToken.setMessageTimestamp(2L);
            cachedToken.setDocumentDigest("digest");

            when(documentRepository.findByIdentifierAndFormat("org-1_doc-1_backbone", "rdf"))
                .thenReturn(Optional.of(subgraph));
            when(appProperties.isDisableTrustedParty()).thenReturn(false);
            when(tokenRepository.findLatestByDocumentIdentifier("org-1_doc-1_backbone"))
                .thenReturn(Optional.of(cachedToken));

            SubgraphResponseDTO response = documentService.getBackboneSubgraph("org-1", "doc-1", "rdf");

            assertThat(response.getDocument()).isEqualTo("backbone-b64");
            assertThat(response.getToken()).isNotNull();
            verify(tpClient, never()).issueToken(any(), any());
            }

            @Test
            void getBackboneSubgraph_WhenSubgraphExistsAndTokenIssuanceFails_ThrowsIllegalStateException() {
            Document subgraph = new Document();
            subgraph.setIdentifier("org-1_doc-1_backbone");
            subgraph.setFormat("rdf");
            subgraph.setGraph("backbone-b64");

            when(documentRepository.findByIdentifierAndFormat("org-1_doc-1_backbone", "rdf"))
                .thenReturn(Optional.of(subgraph));
            when(appProperties.isDisableTrustedParty()).thenReturn(false);
            when(tokenRepository.findLatestByDocumentIdentifier("org-1_doc-1_backbone"))
                .thenReturn(Optional.empty());
            when(organizationService.getTpUrlByOrganization("org-1")).thenReturn("tp.local");
            when(tpClient.issueToken(any(), eq("tp.local")))
                .thenReturn(ResponseEntity.status(HttpStatus.BAD_GATEWAY).body("err"));

            assertThatThrownBy(() -> documentService.getBackboneSubgraph("org-1", "doc-1", "rdf"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Could not issue token");
            }

            @Test
            void getBackboneSubgraph_WhenSubgraphMissing_CreatesSubgraphAndReturnsIt() {
            Document main = new Document();
            main.setIdentifier("org-1_doc-1");
            main.setFormat("json");
            main.setGraph("main-b64");

            when(documentRepository.findByIdentifierAndFormat("org-1_doc-1_backbone", "rdf"))
                .thenReturn(Optional.empty());
            when(documentRepository.findById("org-1_doc-1")).thenReturn(Optional.of(main));

            Document savedSubgraph = new Document();
            savedSubgraph.setIdentifier("org-1_doc-1_backbone");
            savedSubgraph.setFormat("rdf");
            savedSubgraph.setGraph("generated-b64");
            when(documentRepository.save(any(Document.class))).thenReturn(savedSubgraph);

            when(appProperties.isDisableTrustedParty()).thenReturn(true);

            try (MockedStatic<cz.muni.fi.distributed_prov_system.utils.prov.SubgraphUtils> subgraphUtils = mockStatic(cz.muni.fi.distributed_prov_system.utils.prov.SubgraphUtils.class)) {
                subgraphUtils.when(() -> cz.muni.fi.distributed_prov_system.utils.prov.SubgraphUtils.buildSubgraphBase64(
                    "main-b64", "json", "rdf", false
                )).thenReturn("generated-b64");

                SubgraphResponseDTO response = documentService.getBackboneSubgraph("org-1", "doc-1", "rdf");

                assertThat(response.getDocument()).isEqualTo("generated-b64");
                assertThat(response.getToken()).isNull();
                verify(documentRepository).save(any(Document.class));
            }
            }

            @Test
            void getDocument_WhenAuthorityUnknown_UsesDefaultTrustedParty() {
            Document document = new Document();
            document.setIdentifier("org-1_doc-1");
            document.setGraph("graph-b64");

            Map<String, Object> envelope = tokenEnvelope();
            Map<String, Object> normalized = normalizedTokenData();
            normalized.put("authorityId", "unknown-tp");

            when(documentRepository.findById("org-1_doc-1")).thenReturn(Optional.of(document));
            when(appProperties.isDisableTrustedParty()).thenReturn(false);
            when(organizationService.isRegistered("org-1")).thenReturn(false);
            when(appProperties.getTpFqdn()).thenReturn("tp.default");
            when(tokenRepository.findLatestByDocumentIdentifierAndDefaultTp("org-1_doc-1"))
                .thenReturn(Optional.empty());
            when(tpClient.issueToken(any(), eq("tp.default"))).thenReturn(ResponseEntity.ok("{token}"));
            when(trustedPartyRepository.findById("unknown-tp")).thenReturn(Optional.empty());

            DefaultTrustedParty defaultTp = new DefaultTrustedParty();
            defaultTp.setIdentifier("unknown-tp");
            defaultTp.setUrl("tp.default");
            when(defaultTrustedPartyRepository.findById("unknown-tp")).thenReturn(Optional.empty());
            when(defaultTrustedPartyRepository.save(any(DefaultTrustedParty.class))).thenReturn(defaultTp);

            try (MockedStatic<TokenUtils> tokenUtils = mockStatic(TokenUtils.class)) {
                tokenUtils.when(() -> TokenUtils.parseTokenResponse("{token}")).thenReturn(envelope);
                tokenUtils.when(() -> TokenUtils.normalizeTokenData(envelope)).thenReturn(normalized);

                Object response = documentService.getDocument("org-1", "doc-1");

                assertThat(response).isInstanceOf(Map.class);
                verify(defaultTrustedPartyRepository).save(any(DefaultTrustedParty.class));
                verify(tokenRepository).save(any(Token.class));
            }
            }

    private MockedConstruction<InputGraphChecker> checkerConstruction(String bundleId, String metaId) {
        return mockConstruction(InputGraphChecker.class, (checker, context) -> {
            when(checker.getBundleId()).thenReturn(bundleId);
            when(checker.getMetaProvenanceId()).thenReturn(metaId);
        });
    }

    private StoreGraphRequestDTO request() {
        StoreGraphRequestDTO request = new StoreGraphRequestDTO();
        request.setDocument("graph-b64");
        request.setDocumentFormat("json");
        request.setSignature("signature");
        request.setCreatedOn("2026-01-01T00:00:00Z");
        return request;
    }

    private Map<String, Object> tokenEnvelope() {
        Map<String, Object> envelope = new HashMap<>();
        envelope.put("signature", "sig");
        envelope.put("data", Map.of("k", "v"));
        return envelope;
    }

    private Map<String, Object> normalizedTokenData() {
        Map<String, Object> tokenData = new HashMap<>();
        tokenData.put("signature", "sig");
        tokenData.put("originatorId", "org-1");
        tokenData.put("authorityId", "tp-1");
        tokenData.put("tokenTimestamp", 1700000000L);
        tokenData.put("documentCreationTimestamp", 1700000000L);
        tokenData.put("documentDigest", "digest");
        tokenData.put("messageTimestamp", 1700000001L);
        tokenData.put("additionalData", Map.of("trustedPartyUri", "https://tp.local"));
        return tokenData;
    }
}

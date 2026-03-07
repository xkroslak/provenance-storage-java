package cz.muni.fi.distributed_prov_system.rest;

import cz.muni.fi.distributed_prov_system.api.StoreGraphRequestDTO;
import cz.muni.fi.distributed_prov_system.config.AppProperties;
import cz.muni.fi.distributed_prov_system.data.repository.BundleRepository;
import cz.muni.fi.distributed_prov_system.data.repository.DefaultTrustedPartyRepository;
import cz.muni.fi.distributed_prov_system.data.repository.DocumentRepository;
import cz.muni.fi.distributed_prov_system.data.repository.EntityRepository;
import cz.muni.fi.distributed_prov_system.data.repository.TokenRepository;
import cz.muni.fi.distributed_prov_system.data.repository.TrustedPartyRepository;
import cz.muni.fi.distributed_prov_system.data.model.nodes.Bundle;
import cz.muni.fi.distributed_prov_system.data.model.nodes.Entity;
import cz.muni.fi.distributed_prov_system.data.model.nonprovmodels.Document;
import cz.muni.fi.distributed_prov_system.facade.DocumentFacadeImpl;
import cz.muni.fi.distributed_prov_system.service.DocumentService;
import cz.muni.fi.distributed_prov_system.service.ImportGraphService;
import cz.muni.fi.distributed_prov_system.service.OrganizationService;
import cz.muni.fi.distributed_prov_system.client.TrustedPartyClient;
import cz.muni.fi.distributed_prov_system.utils.TestDataFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.head;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DocumentRestController.class)
@Import({DocumentFacadeImpl.class, DocumentService.class})
class DocumentApiComponentTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AppProperties appProperties;

    @MockitoBean
    private TrustedPartyClient tpClient;

    @MockitoBean
    private OrganizationService organizationService;

    @MockitoBean
    private ImportGraphService importGraphService;

    @MockitoBean
    private DocumentRepository documentRepository;

    @MockitoBean
    private TokenRepository tokenRepository;

    @MockitoBean
    private EntityRepository entityRepository;

    @MockitoBean
    private BundleRepository bundleRepository;

    @MockitoBean
    private TrustedPartyRepository trustedPartyRepository;

    @MockitoBean
    private DefaultTrustedPartyRepository defaultTrustedPartyRepository;

    @BeforeEach
    void setUp() {
        when(appProperties.isDisableTrustedParty()).thenReturn(true);
        when(appProperties.getFqdn()).thenReturn("prov-storage-pathology:8000");
        when(documentRepository.existsById(any())).thenReturn(false);
        doNothing().when(importGraphService).importGraph(any(), any(), any(), any(), any(), eq(false));
    }

    @Test
    void storeDocument_WithValidMinimalCpmPayload_ReturnsCreated() throws Exception {
        StoreGraphRequestDTO request = TestDataFactory.componentStoreGraphRequest(
            "http://prov-storage-pathology:8000",
            "org-1",
            "doc-1"
        );

        mockMvc.perform(post("/api/v1/organizations/org-1/documents/doc-1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.info").value("Trusted party is disabled therefore no token has been issued, however graph has been stored."));
    }

    @Test
    void storeDocument_WhenBundleIdDoesNotMatchPath_ReturnsBadRequest() throws Exception {
        StoreGraphRequestDTO request = TestDataFactory.componentStoreGraphRequest(
            "http://prov-storage-pathology:8000",
            "org-1",
            "another-doc-id"
        );

        mockMvc.perform(post("/api/v1/organizations/org-1/documents/doc-1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("The bundle id [another-doc-id] does not match requested id [doc-1]."));
    }

    @Test
    void storeDocument_WhenDocumentAlreadyExists_ReturnsConflict() throws Exception {
        StoreGraphRequestDTO request = TestDataFactory.componentStoreGraphRequest(
            "http://prov-storage-pathology:8000",
            "org-1",
            "doc-1"
        );

        when(documentRepository.existsById("org-1_doc-1")).thenReturn(true);

        mockMvc.perform(post("/api/v1/organizations/org-1/documents/doc-1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.message").value("Document with id [doc-1] already exists under organization [org-1]."));
    }

    @Test
    void getDocument_WhenExistsAndTrustedPartyDisabled_ReturnsDocumentWithoutToken() throws Exception {
        Document stored = new Document();
        stored.setIdentifier("org-1_doc-1");
        stored.setGraph("graph-content-b64");
        stored.setFormat("json");

        when(documentRepository.findById("org-1_doc-1")).thenReturn(Optional.of(stored));

        mockMvc.perform(get("/api/v1/organizations/org-1/documents/doc-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.document").value("graph-content-b64"))
                .andExpect(jsonPath("$.token").doesNotExist());
    }

    @Test
    void headDocument_WhenExists_ReturnsOk() throws Exception {
        when(documentRepository.existsById("org-1_doc-1")).thenReturn(true);

        mockMvc.perform(head("/api/v1/organizations/org-1/documents/doc-1"))
                .andExpect(status().isOk());
    }

    @Test
    void headDocument_WhenNotExists_ReturnsNotFound() throws Exception {
        when(documentRepository.existsById("org-1_doc-1")).thenReturn(false);

        mockMvc.perform(head("/api/v1/organizations/org-1/documents/doc-1"))
                .andExpect(status().isNotFound());
    }

    @Test
    void updateDocument_WhenMetaBundleCannotBeResolved_ReturnsBadRequest() throws Exception {
        StoreGraphRequestDTO request = TestDataFactory.componentStoreGraphRequest(
                "http://prov-storage-pathology:8000",
                "org-1",
                "doc-1"
        );

        Entity entity = new Entity();
        Bundle metaBundle = new Bundle();
        metaBundle.setIdentifier("doc-1_meta");
        entity.setContains(List.of(metaBundle));

        when(entityRepository.findById("org-1_doc-1")).thenReturn(Optional.of(entity));
        when(bundleRepository.existsById(any())).thenReturn(false);
        when(documentRepository.existsById("org-1_doc-1")).thenReturn(true);
        doNothing().when(importGraphService).importGraph(any(), any(), any(), any(), any(), eq(true));

        mockMvc.perform(put("/api/v1/organizations/org-1/documents/doc-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message", containsString("Meta provenance with id [")))
            .andExpect(jsonPath("$.message", containsString("] does not exist!")));
    }
}

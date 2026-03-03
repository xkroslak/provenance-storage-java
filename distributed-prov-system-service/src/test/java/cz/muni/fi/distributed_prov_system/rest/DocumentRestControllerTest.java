package cz.muni.fi.distributed_prov_system.rest;

import cz.muni.fi.distributed_prov_system.api.StoreGraphRequestDTO;
import cz.muni.fi.distributed_prov_system.api.StoreGraphResponseDTO;
import cz.muni.fi.distributed_prov_system.api.SubgraphResponseDTO;
import cz.muni.fi.distributed_prov_system.exceptions.NotFoundException;
import cz.muni.fi.distributed_prov_system.facade.DocumentFacade;
import cz.muni.fi.distributed_prov_system.utils.TestDataFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.head;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DocumentRestController.class)
class DocumentRestControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private DocumentFacade documentFacade;

    @Test
    void storeDocument_WhenRequestIsValid_ReturnsCreated() throws Exception {
        StoreGraphRequestDTO request = TestDataFactory.storeGraphRequest();
        StoreGraphResponseDTO response = TestDataFactory.storeGraphResponse("stored");

        when(documentFacade.storeDocument(eq("org-1"), eq("doc-1"), any(StoreGraphRequestDTO.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/organizations/org-1/documents/doc-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.info").value("stored"));
    }

    @Test
    void updateDocument_WhenRequestIsValid_ReturnsOk() throws Exception {
        StoreGraphRequestDTO request = TestDataFactory.storeGraphRequest();
        StoreGraphResponseDTO response = TestDataFactory.storeGraphResponse("updated");

        when(documentFacade.updateDocument(eq("org-1"), eq("doc-1"), any(StoreGraphRequestDTO.class))).thenReturn(response);

        mockMvc.perform(put("/api/v1/organizations/org-1/documents/doc-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.info").value("updated"));
    }

    @Test
    void getDocument_WhenDocumentExists_ReturnsOk() throws Exception {
        when(documentFacade.getDocument("org-1", "doc-1")).thenReturn(TestDataFactory.documentPayload("abc"));

        mockMvc.perform(get("/api/v1/organizations/org-1/documents/doc-1")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.graph").value("abc"));
    }

    @Test
    void headDocument_WhenDocumentExists_ReturnsOk() throws Exception {
        when(documentFacade.documentExists("org-1", "doc-1")).thenReturn(true);

        mockMvc.perform(head("/api/v1/organizations/org-1/documents/doc-1"))
                .andExpect(status().isOk());
    }

    @Test
    void headDocument_WhenDocumentDoesNotExist_ReturnsNotFound() throws Exception {
        when(documentFacade.documentExists("org-1", "doc-1")).thenReturn(false);

        mockMvc.perform(head("/api/v1/organizations/org-1/documents/doc-1"))
                .andExpect(status().isNotFound());
    }

    @Test
    void getDomainSpecificSubgraph_WhenFormatProvided_ReturnsOk() throws Exception {
        SubgraphResponseDTO response = TestDataFactory.subgraphResponse("domain-b64");

        when(documentFacade.getDomainSpecificSubgraph("org-1", "doc-1", "json")).thenReturn(response);

        mockMvc.perform(get("/api/v1/organizations/org-1/documents/doc-1/domain-specific")
                        .param("format", "json")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.document").value("domain-b64"));
    }

    @Test
    void getBackboneSubgraph_WhenFormatMissing_UsesDefaultRdf() throws Exception {
        SubgraphResponseDTO response = TestDataFactory.subgraphResponse("backbone-b64");

        when(documentFacade.getBackboneSubgraph("org-1", "doc-1", "rdf")).thenReturn(response);

        mockMvc.perform(get("/api/v1/organizations/org-1/documents/doc-1/backbone")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.document").value("backbone-b64"));
    }

    @Test
    void getDocument_WhenFacadeThrowsNotFound_ReturnsNotFoundApiError() throws Exception {
        when(documentFacade.getDocument("org-1", "doc-404"))
                .thenThrow(new NotFoundException("Document not found"));

        mockMvc.perform(get("/api/v1/organizations/org-1/documents/doc-404")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Document not found"));
    }

    @Test
    void storeDocument_WhenJsonIsMalformed_ReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/v1/organizations/org-1/documents/doc-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"document\": \"abc\""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Malformed JSON request"));
    }
}
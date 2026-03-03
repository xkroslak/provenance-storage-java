package cz.muni.fi.distributed_prov_system.rest;

import cz.muni.fi.distributed_prov_system.api.MetaResponseDTO;
import cz.muni.fi.distributed_prov_system.exceptions.MetaNotFoundException;
import cz.muni.fi.distributed_prov_system.facade.MetaFacade;
import cz.muni.fi.distributed_prov_system.utils.TestDataFactory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.head;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(MetaRestController.class)
class MetaRestControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MetaFacade metaFacade;

    @Test
    void headMeta_WhenMetaExists_ReturnsOk() throws Exception {
        when(metaFacade.metaBundleExists("meta-1")).thenReturn(true);

        mockMvc.perform(head("/api/v1/documents/meta/meta-1"))
                .andExpect(status().isOk());
    }

    @Test
    void headMeta_WhenMetaDoesNotExist_ReturnsNotFound() throws Exception {
        when(metaFacade.metaBundleExists("meta-1")).thenReturn(false);

        mockMvc.perform(head("/api/v1/documents/meta/meta-1"))
                .andExpect(status().isNotFound());
    }

    @Test
    void getMeta_WhenInputIsValid_ReturnsOk() throws Exception {
        MetaResponseDTO response = TestDataFactory.metaResponse("b64-graph", null);
        when(metaFacade.getMeta("meta-1", "json", "org-1")).thenReturn(response);

        mockMvc.perform(get("/api/v1/documents/meta/meta-1")
                        .param("format", "json")
                        .param("organizationId", "org-1")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.graph").value("b64-graph"));
    }

    @Test
    void getMeta_WhenFormatIsInvalid_ReturnsBadRequest() throws Exception {
        mockMvc.perform(get("/api/v1/documents/meta/meta-1")
                        .param("format", "yaml")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Requested format [yaml] is not supported!"));
    }

    @Test
    void getMeta_WhenMetaDoesNotExist_ReturnsNotFound() throws Exception {
        when(metaFacade.getMeta("meta-1", "rdf", null)).thenThrow(new MetaNotFoundException("missing"));

        mockMvc.perform(get("/api/v1/documents/meta/meta-1")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("The meta-provenance with id [meta-1] does not exist."));
    }

    @Test
    void getMeta_WhenFacadeThrowsUnexpectedException_ReturnsInternalServerError() throws Exception {
        when(metaFacade.getMeta("meta-1", "rdf", null)).thenThrow(new RuntimeException("boom"));

        mockMvc.perform(get("/api/v1/documents/meta/meta-1")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message").value("boom"));
    }
}
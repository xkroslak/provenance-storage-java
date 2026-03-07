package cz.muni.fi.distributed_prov_system.rest;

import cz.muni.fi.distributed_prov_system.exceptions.MetaNotFoundException;
import cz.muni.fi.distributed_prov_system.facade.MetaFacadeImpl;
import cz.muni.fi.distributed_prov_system.service.MetaService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.head;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(MetaRestController.class)
@Import(MetaFacadeImpl.class)
class MetaApiComponentTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MetaService metaService;

    @Test
    void headMeta_WhenMetaExists_ReturnsOk() throws Exception {
        when(metaService.metaBundleExists("meta-1")).thenReturn(true);

        mockMvc.perform(head("/api/v1/documents/meta/meta-1"))
                .andExpect(status().isOk());
    }

    @Test
    void headMeta_WhenMetaDoesNotExist_ReturnsNotFound() throws Exception {
        when(metaService.metaBundleExists("meta-1")).thenReturn(false);

        mockMvc.perform(head("/api/v1/documents/meta/meta-1"))
                .andExpect(status().isNotFound());
    }

    @Test
    void getMeta_WhenTrustedPartyDisabled_ReturnsGraphWithoutToken() throws Exception {
        when(metaService.getB64EncodedMetaProvenance("meta-1", "json")).thenReturn("b64-graph");
        when(metaService.isTrustedPartyDisabled()).thenReturn(true);

        mockMvc.perform(get("/api/v1/documents/meta/meta-1")
                        .param("format", "json")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.graph").value("b64-graph"))
                .andExpect(jsonPath("$.token").doesNotExist());
    }

    @Test
    void getMeta_WhenTrustedPartyEnabledAndOrganizationProvided_ReturnsGraphWithToken() throws Exception {
        when(metaService.getB64EncodedMetaProvenance("meta-1", "json")).thenReturn("b64-graph");
        when(metaService.isTrustedPartyDisabled()).thenReturn(false);
        when(metaService.getTpUrlByOrganization("org-1")).thenReturn("tp.local");
        when(metaService.buildMetaTokenPayload("b64-graph", "meta-1", "json", "org-1")).thenReturn("payload");
        when(metaService.sendTokenRequestToTp(eq("payload"), eq("tp.local"))).thenReturn(Map.of("jwt", "token-1"));

        mockMvc.perform(get("/api/v1/documents/meta/meta-1")
                        .param("format", "json")
                        .param("organizationId", "org-1")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.graph").value("b64-graph"))
                .andExpect(jsonPath("$.token.jwt").value("token-1"));
    }

    @Test
    void getMeta_WhenTrustedPartyEnabledWithoutOrganizationId_ReturnsGraphWithToken() throws Exception {
        when(metaService.getB64EncodedMetaProvenance("meta-1", "rdf")).thenReturn("b64-graph");
        when(metaService.isTrustedPartyDisabled()).thenReturn(false);
        when(metaService.buildMetaTokenPayload("b64-graph", "meta-1", "rdf", null)).thenReturn("payload");
        when(metaService.sendTokenRequestToTp(eq("payload"), eq(null))).thenReturn(Map.of("jwt", "token-2"));

        mockMvc.perform(get("/api/v1/documents/meta/meta-1")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.graph").value("b64-graph"))
                .andExpect(jsonPath("$.token.jwt").value("token-2"));
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
        when(metaService.getB64EncodedMetaProvenance("meta-1", "rdf")).thenThrow(new MetaNotFoundException("missing"));

        mockMvc.perform(get("/api/v1/documents/meta/meta-1")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("The meta-provenance with id [meta-1] does not exist."));
    }

    @Test
    void getMeta_WhenFacadeThrowsUnexpectedException_ReturnsInternalServerError() throws Exception {
        when(metaService.getB64EncodedMetaProvenance(any(), any())).thenThrow(new RuntimeException("boom"));

        mockMvc.perform(get("/api/v1/documents/meta/meta-1")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message").value("boom"));
    }
}

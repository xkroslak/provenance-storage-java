package cz.muni.fi.distributed_prov_system.rest;

import cz.muni.fi.distributed_prov_system.api.RegisterOrganizationRequestDTO;
import cz.muni.fi.distributed_prov_system.client.TrustedPartyClient;
import cz.muni.fi.distributed_prov_system.config.AppProperties;
import cz.muni.fi.distributed_prov_system.facade.OrganizationFacadeImpl;
import cz.muni.fi.distributed_prov_system.service.OrganizationService;
import cz.muni.fi.distributed_prov_system.utils.TestDataFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(OrganizationRestController.class)
@Import(OrganizationFacadeImpl.class)
class OrganizationApiComponentTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private OrganizationService organizationService;

    @MockitoBean
    private TrustedPartyClient tpClient;

    @MockitoBean
    private AppProperties appProperties;

    @Test
    void register_WhenTrustedPartyDisabled_ReturnsServiceUnavailable() throws Exception {
        RegisterOrganizationRequestDTO request = TestDataFactory.registerOrganizationRequest();
        when(appProperties.isDisableTrustedParty()).thenReturn(true);

        mockMvc.perform(post("/api/v1/organizations/org-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.message").value("Since Trusted party is disabled, registration is also disabled"));
    }

    @Test
    void register_WhenOrganizationAlreadyExists_ReturnsConflict() throws Exception {
        RegisterOrganizationRequestDTO request = TestDataFactory.registerOrganizationRequest();
        when(appProperties.isDisableTrustedParty()).thenReturn(false);
        when(organizationService.isRegistered("org-1")).thenReturn(true);

        mockMvc.perform(post("/api/v1/organizations/org-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Organization with id [org-1] is already registered. If you want to modify it, send PUT request!"));
    }

    @Test
    void register_WhenBodyFailsValidation_ReturnsBadRequest() throws Exception {
        RegisterOrganizationRequestDTO request = TestDataFactory.registerOrganizationRequest();
        request.setClientCertificate(" ");

        mockMvc.perform(post("/api/v1/organizations/org-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("clientCertificate must not be blank"));
    }

    @Test
    void register_WhenInputIsValidAndTrustedPartyEnabled_ReturnsCreated() throws Exception {
        RegisterOrganizationRequestDTO request = TestDataFactory.registerOrganizationRequest();
        when(appProperties.isDisableTrustedParty()).thenReturn(false);
        when(organizationService.isRegistered("org-1")).thenReturn(false);
        when(tpClient.registerOrganization(eq("org-1"), any(RegisterOrganizationRequestDTO.class)))
                .thenReturn(ResponseEntity.status(201).body("ok"));

        mockMvc.perform(post("/api/v1/organizations/org-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        verify(organizationService).createOrganization(
                eq("org-1"),
                eq("client-cert"),
                eq(List.of("intermediate-cert")),
                eq("tp.local")
        );
    }

        @Test
        void register_WhenTrustedPartyRejectsCertificateChain_ReturnsUnauthorized() throws Exception {
                RegisterOrganizationRequestDTO request = TestDataFactory.registerOrganizationRequest();
                when(appProperties.isDisableTrustedParty()).thenReturn(false);
                when(organizationService.isRegistered("org-1")).thenReturn(false);
                when(tpClient.registerOrganization(eq("org-1"), any(RegisterOrganizationRequestDTO.class)))
                                .thenReturn(ResponseEntity.status(401).body("unverified"));

                mockMvc.perform(post("/api/v1/organizations/org-1")
                                                .contentType(MediaType.APPLICATION_JSON)
                                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isUnauthorized())
                                .andExpect(jsonPath("$.message").value("Trusted party was unable to verify certificate chain!"));
        }

    @Test
    void modify_WhenOrganizationDoesNotExist_ReturnsNotFound() throws Exception {
        RegisterOrganizationRequestDTO request = TestDataFactory.registerOrganizationRequest();
        when(organizationService.isRegistered("org-1")).thenReturn(false);

        mockMvc.perform(put("/api/v1/organizations/org-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Organization with id [org-1] is not registered!"));
    }

    @Test
    void modify_WhenTrustedPartyDisabledAndInputValid_ReturnsOk() throws Exception {
        RegisterOrganizationRequestDTO request = TestDataFactory.registerOrganizationRequest();
        when(organizationService.isRegistered("org-1")).thenReturn(true);
        when(appProperties.isDisableTrustedParty()).thenReturn(true);

        mockMvc.perform(put("/api/v1/organizations/org-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        verify(tpClient, never()).updateOrganization(any(), any());
        verify(organizationService).modifyOrganization(
                eq("org-1"),
                eq("client-cert"),
                eq(List.of("intermediate-cert")),
                eq("tp.local")
        );
    }

    @Test
    void modify_WhenTrustedPartyReturnsServiceUnavailable_ReturnsBadGateway() throws Exception {
        RegisterOrganizationRequestDTO request = TestDataFactory.registerOrganizationRequest();
        when(organizationService.isRegistered("org-1")).thenReturn(true);
        when(appProperties.isDisableTrustedParty()).thenReturn(false);
        when(tpClient.updateOrganization(eq("org-1"), any(RegisterOrganizationRequestDTO.class)))
                .thenReturn(ResponseEntity.status(503).body("tp down"));

        mockMvc.perform(put("/api/v1/organizations/org-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.message").value("Trusted party unavailable: tp down"));
    }

        @Test
        void modify_WhenBodyFailsValidation_ReturnsBadRequest() throws Exception {
                RegisterOrganizationRequestDTO request = TestDataFactory.registerOrganizationRequest();
                request.setClientCertificate(" ");
                when(organizationService.isRegistered("org-1")).thenReturn(true);

                mockMvc.perform(put("/api/v1/organizations/org-1")
                                                .contentType(MediaType.APPLICATION_JSON)
                                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.message").value("clientCertificate must not be blank"));
        }
}

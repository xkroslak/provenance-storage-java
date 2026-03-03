package cz.muni.fi.distributed_prov_system.rest;

import cz.muni.fi.distributed_prov_system.api.RegisterOrganizationRequestDTO;
import cz.muni.fi.distributed_prov_system.exceptions.ConflictException;
import cz.muni.fi.distributed_prov_system.exceptions.TrustedPartyDisabledException;
import cz.muni.fi.distributed_prov_system.facade.OrganizationFacade;
import cz.muni.fi.distributed_prov_system.utils.TestDataFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(OrganizationRestController.class)
class OrganizationRestControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private OrganizationFacade organizationFacade;

    @Test
    void register_WhenRequestIsValid_ReturnsCreated() throws Exception {
        RegisterOrganizationRequestDTO request = TestDataFactory.registerOrganizationRequest();

        mockMvc.perform(post("/api/v1/organizations/org-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());
    }

    @Test
    void register_WhenClientCertificateIsMissing_ReturnsBadRequest() throws Exception {
        RegisterOrganizationRequestDTO request = TestDataFactory.registerOrganizationRequest();
        request.setClientCertificate(" ");

        mockMvc.perform(post("/api/v1/organizations/org-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void modify_WhenRequestIsValid_ReturnsOk() throws Exception {
        RegisterOrganizationRequestDTO request = TestDataFactory.registerOrganizationRequest();

        mockMvc.perform(put("/api/v1/organizations/org-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    void modify_WhenIntermediateCertificatesMissing_ReturnsBadRequest() throws Exception {
        RegisterOrganizationRequestDTO request = TestDataFactory.registerOrganizationRequest();
        request.setIntermediateCertificates(List.of());

        mockMvc.perform(put("/api/v1/organizations/org-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void register_WhenOrganizationAlreadyExists_ReturnsConflict() throws Exception {
        RegisterOrganizationRequestDTO request = TestDataFactory.registerOrganizationRequest();
        doThrow(new ConflictException("Organization already exists"))
                .when(organizationFacade).register(eq("org-1"), any(RegisterOrganizationRequestDTO.class));

        mockMvc.perform(post("/api/v1/organizations/org-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Organization already exists"));
    }

    @Test
    void modify_WhenTrustedPartyIsDisabled_ReturnsServiceUnavailable() throws Exception {
        RegisterOrganizationRequestDTO request = TestDataFactory.registerOrganizationRequest();
        doThrow(new TrustedPartyDisabledException("TP disabled"))
                .when(organizationFacade).modify(eq("org-1"), any(RegisterOrganizationRequestDTO.class));

        mockMvc.perform(put("/api/v1/organizations/org-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.message").value("TP disabled"));
    }

}
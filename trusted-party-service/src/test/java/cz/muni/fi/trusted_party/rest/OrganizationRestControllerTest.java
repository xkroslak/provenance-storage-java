package cz.muni.fi.trusted_party.rest;

import cz.muni.fi.trusted_party.api.Organization.OrganizationDTO;
import cz.muni.fi.trusted_party.api.Organization.StoreCertOrganizationDTO;
import cz.muni.fi.trusted_party.facade.OrganizationFacade;
import cz.muni.fi.trusted_party.utils.TestDataFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
    void getAllOrganizations_returnsList() throws Exception {
        OrganizationDTO dto = TestDataFactory.organizationDto("org-1");

        when(organizationFacade.getAllOrganizations()).thenReturn(List.of(dto));

        mockMvc.perform(get("/api/v1/organizations").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].organizationId").value("org-1"));
    }

    @Test
    void getOrganization_returnsOrganization() throws Exception {
        OrganizationDTO dto = TestDataFactory.organizationDto("org-1");

        when(organizationFacade.getOrganization("org-1")).thenReturn(dto);

        mockMvc.perform(get("/api/v1/organizations/org-1").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.organizationId").value("org-1"));
    }

    @Test
    void registerOrganization_validRequest_returnsCreated() throws Exception {
        StoreCertOrganizationDTO body = TestDataFactory.storeCertRequest();

        mockMvc.perform(post("/api/v1/organizations/org-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isCreated());
    }

    @Test
    void registerOrganization_missingField_returnsBadRequest() throws Exception {
        StoreCertOrganizationDTO body = TestDataFactory.storeCertRequest();
        body.setClientCertificate("");

        mockMvc.perform(post("/api/v1/organizations/org-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void retrieveCertificates_returnsOrganization() throws Exception {
        OrganizationDTO dto = TestDataFactory.organizationDto("org-1");

        when(organizationFacade.retrieveCertificates("org-1")).thenReturn(dto);

        mockMvc.perform(get("/api/v1/organizations/org-1/certs")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.organizationId").value("org-1"));
    }

    @Test
    void updateCertificates_validRequest_returnsCreated() throws Exception {
        StoreCertOrganizationDTO body = TestDataFactory.storeCertRequest();

        mockMvc.perform(put("/api/v1/organizations/org-1/certs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isCreated());
    }

    @Test
    void updateCertificates_missingField_returnsBadRequest() throws Exception {
        StoreCertOrganizationDTO body = TestDataFactory.storeCertRequest();
        body.setIntermediateCertificates(null);

        mockMvc.perform(put("/api/v1/organizations/org-1/certs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

}

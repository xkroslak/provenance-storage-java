package cz.muni.fi.trusted_party.rest;

import cz.muni.fi.trusted_party.api.Document.DocumentDTO;
import cz.muni.fi.trusted_party.data.enums.DocumentType;
import cz.muni.fi.trusted_party.facade.DocumentFacade;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DocumentRestController.class)
class DocumentRestControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DocumentFacade documentFacade;

    @Test
    void getDocument_returnsDocument() throws Exception {
        DocumentDTO dto = new DocumentDTO();
        dto.setIdentifier("doc-1");
        dto.setDocumentFormat("json");
        dto.setCertDigest("cert-1");
        dto.setOrganizationId("org-1");
        dto.setDocumentType(DocumentType.GRAPH);
        dto.setDocumentText("{}");
        dto.setCreatedOn(LocalDateTime.of(2024, 1, 1, 12, 0));
        dto.setSignature("sig");

        when(documentFacade.getDocument("org-1", "doc-1", "json")).thenReturn(dto);

        mockMvc.perform(get("/api/v1/organizations/org-1/documents/doc-1/json")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.identifier").value("doc-1"))
                .andExpect(jsonPath("$.documentFormat").value("json"))
                .andExpect(jsonPath("$.organizationId").value("org-1"))
                .andExpect(jsonPath("$.documentType").value("GRAPH"));
    }
}

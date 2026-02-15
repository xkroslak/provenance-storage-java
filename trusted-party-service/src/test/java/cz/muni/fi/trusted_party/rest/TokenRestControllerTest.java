package cz.muni.fi.trusted_party.rest;

import cz.muni.fi.trusted_party.api.Token.TokenDTO;
import cz.muni.fi.trusted_party.api.Token.TokenRequestDTO;
import cz.muni.fi.trusted_party.facade.TokenFacade;
import cz.muni.fi.trusted_party.utils.TestDataFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TokenRestController.class)
class TokenRestControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private TokenFacade tokenFacade;

    @Test
    void getToken_returnsTokenList() throws Exception {
        when(tokenFacade.getToken("org-1", "doc-1", "json"))
                .thenReturn(List.of(new TokenDTO(), new TokenDTO()));

        mockMvc.perform(get("/api/v1/organizations/org-1/tokens/doc-1/json")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void getAllTokens_returnsTokenList() throws Exception {
        when(tokenFacade.getAllTokens("org-1")).thenReturn(List.of(new TokenDTO()));

        mockMvc.perform(get("/api/v1/organizations/org-1/tokens")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void issueToken_validRequest_returnsCreated() throws Exception {
        TokenRequestDTO body = TestDataFactory.tokenRequest();
        when(tokenFacade.issueToken(any(TokenRequestDTO.class))).thenReturn(List.of(new TokenDTO()));

        mockMvc.perform(post("/api/v1/issueToken")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void issueToken_missingRequiredField_returnsBadRequest() throws Exception {
        TokenRequestDTO body = TestDataFactory.tokenRequest();
        body.setDocument(null);

        mockMvc.perform(post("/api/v1/issueToken")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void issueToken_missingOrganizationId_returnsBadRequest() throws Exception {
        TokenRequestDTO body = TestDataFactory.tokenRequest();
        body.setOrganizationId(" ");

        mockMvc.perform(post("/api/v1/issueToken")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void verifySignature_valid_returnsOk() throws Exception {
        TokenRequestDTO body = TestDataFactory.tokenRequest();
        when(tokenFacade.verifySignature(any(TokenRequestDTO.class))).thenReturn(true);

        mockMvc.perform(post("/api/v1/verifySignature")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk());
    }

    @Test
    void verifySignature_invalid_returnsBadRequest() throws Exception {
        TokenRequestDTO body = TestDataFactory.tokenRequest();
        when(tokenFacade.verifySignature(any(TokenRequestDTO.class))).thenReturn(false);

        mockMvc.perform(post("/api/v1/verifySignature")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

}

package cz.muni.fi.trusted_party.rest;

import cz.muni.fi.trusted_party.api.InfoResponseDTO;
import cz.muni.fi.trusted_party.facade.InfoFacade;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(InfoRestController.class)
class InfoRestControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private InfoFacade infoFacade;

    @Test
    void getInfo_returnsInfo() throws Exception {
        when(infoFacade.getInfo()).thenReturn(new InfoResponseDTO("tp-1", "cert-data"));

        mockMvc.perform(get("/api/v1/info").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("tp-1"))
                .andExpect(jsonPath("$.certificate").value("cert-data"));
    }
}

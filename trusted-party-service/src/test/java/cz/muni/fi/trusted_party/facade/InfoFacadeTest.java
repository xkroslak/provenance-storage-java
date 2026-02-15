package cz.muni.fi.trusted_party.facade;

import cz.muni.fi.trusted_party.api.InfoResponseDTO;
import cz.muni.fi.trusted_party.service.InfoService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InfoFacadeTest {

    @Mock
    private InfoService infoService;

    @InjectMocks
    private InfoFacadeImpl infoFacade;

    @Test
    void getInfo_returnsServiceResponse() {
        InfoResponseDTO dto = new InfoResponseDTO("tp-1", "cert");
        when(infoService.getInfo()).thenReturn(dto);

        InfoResponseDTO result = infoFacade.getInfo();

        assertThat(result).isSameAs(dto);
    }
}

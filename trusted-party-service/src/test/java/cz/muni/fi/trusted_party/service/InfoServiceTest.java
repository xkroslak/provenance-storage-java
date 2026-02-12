package cz.muni.fi.trusted_party.service;

import cz.muni.fi.trusted_party.api.InfoResponseDTO;
import cz.muni.fi.trusted_party.config.AppProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InfoServiceTest {

    @Mock
    private AppProperties appProperties;

    @InjectMocks
    private InfoService infoService;

    @Test
    void getInfo_returnsConfiguredInfo() {
        when(appProperties.getId()).thenReturn("tp-1");
        when(appProperties.getCertificate()).thenReturn("cert-data");

        InfoResponseDTO result = infoService.getInfo();

        assertThat(result.getId()).isEqualTo("tp-1");
        assertThat(result.getCertificate()).isEqualTo("cert-data");
    }
}

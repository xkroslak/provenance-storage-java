package cz.muni.fi.distributed_prov_system.facade;

import cz.muni.fi.distributed_prov_system.api.MetaResponseDTO;
import cz.muni.fi.distributed_prov_system.exceptions.MetaNotFoundException;
import cz.muni.fi.distributed_prov_system.service.MetaService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MetaFacadeImplTest {

    @Mock
    private MetaService metaService;

    @InjectMocks
    private MetaFacadeImpl metaFacade;

    @Test
    void metaBundleExists_WhenServiceReturnsTrue_ReturnsTrue() {
        when(metaService.metaBundleExists("meta-1")).thenReturn(true);

        boolean result = metaFacade.metaBundleExists("meta-1");

        assertThat(result).isTrue();
        verify(metaService).metaBundleExists("meta-1");
    }

    @Test
    void getMeta_WhenFormatIsUnsupported_ThrowsIllegalArgumentException() {
        assertThatThrownBy(() -> metaFacade.getMeta("meta-1", "yaml", "org-1"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("not supported");
    }

    @Test
    void getMeta_WhenMetaIsMissing_PropagatesMetaNotFoundException() {
        when(metaService.getB64EncodedMetaProvenance("meta-1", "json"))
                .thenThrow(new MetaNotFoundException("missing"));

        assertThatThrownBy(() -> metaFacade.getMeta("meta-1", "json", "org-1"))
                .isInstanceOf(MetaNotFoundException.class)
                .hasMessageContaining("missing");
    }

    @Test
    void getMeta_WhenTrustedPartyIsDisabled_ReturnsGraphWithoutToken() {
        when(metaService.getB64EncodedMetaProvenance("meta-1", "json")).thenReturn("b64-graph");
        when(metaService.isTrustedPartyDisabled()).thenReturn(true);

        MetaResponseDTO result = metaFacade.getMeta("meta-1", "json", "org-1");

        assertThat(result.getGraph()).isEqualTo("b64-graph");
        assertThat(result.getToken()).isNull();

        verify(metaService, never()).buildMetaTokenPayload("b64-graph", "meta-1", "json", "org-1");
        verify(metaService, never()).sendTokenRequestToTp(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void getMeta_WhenTrustedPartyIsEnabledAndOrganizationProvided_ReturnsGraphAndToken() {
        Object payload = new Object();
        Object token = new Object();

        when(metaService.getB64EncodedMetaProvenance("meta-1", "json")).thenReturn("b64-graph");
        when(metaService.isTrustedPartyDisabled()).thenReturn(false);
        when(metaService.getTpUrlByOrganization("org-1")).thenReturn("tp.local");
        when(metaService.buildMetaTokenPayload("b64-graph", "meta-1", "json", "org-1")).thenReturn(payload);
        when(metaService.sendTokenRequestToTp(payload, "tp.local")).thenReturn(token);

        MetaResponseDTO result = metaFacade.getMeta("meta-1", "json", "org-1");

        assertThat(result.getGraph()).isEqualTo("b64-graph");
        assertThat(result.getToken()).isSameAs(token);
    }

    @Test
    void getMeta_WhenTrustedPartyIsEnabledAndOrganizationMissing_UsesNullTpUrl() {
        Object payload = new Object();
        Object token = new Object();

        when(metaService.getB64EncodedMetaProvenance("meta-1", "json")).thenReturn("b64-graph");
        when(metaService.isTrustedPartyDisabled()).thenReturn(false);
        when(metaService.buildMetaTokenPayload("b64-graph", "meta-1", "json", null)).thenReturn(payload);
        when(metaService.sendTokenRequestToTp(payload, null)).thenReturn(token);

        MetaResponseDTO result = metaFacade.getMeta("meta-1", "json", null);

        assertThat(result.getGraph()).isEqualTo("b64-graph");
        assertThat(result.getToken()).isSameAs(token);

        verify(metaService, never()).getTpUrlByOrganization("org-1");
    }
}
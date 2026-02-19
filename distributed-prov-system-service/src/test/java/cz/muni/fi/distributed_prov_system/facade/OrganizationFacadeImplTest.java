package cz.muni.fi.distributed_prov_system.facade;

import cz.muni.fi.distributed_prov_system.api.RegisterOrganizationRequestDTO;
import cz.muni.fi.distributed_prov_system.client.TrustedPartyClient;
import cz.muni.fi.distributed_prov_system.config.AppProperties;
import cz.muni.fi.distributed_prov_system.exceptions.*;
import cz.muni.fi.distributed_prov_system.service.OrganizationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrganizationFacadeImplTest {

    @Mock
    private OrganizationService organizationService;

    @Mock
    private TrustedPartyClient trustedPartyClient;

    @Mock
    private AppProperties appProperties;

    @InjectMocks
    private OrganizationFacadeImpl organizationFacade;

    @Test
    void register_WhenTrustedPartyIsDisabled_ThrowsTrustedPartyDisabledException() {
        RegisterOrganizationRequestDTO request = validRequest();
        when(appProperties.isDisableTrustedParty()).thenReturn(true);

        assertThatThrownBy(() -> organizationFacade.register("org-1", request))
                .isInstanceOf(TrustedPartyDisabledException.class)
                .hasMessageContaining("registration is also disabled");
    }

    @Test
    void register_WhenOrganizationAlreadyExists_ThrowsConflictException() {
        RegisterOrganizationRequestDTO request = validRequest();
        when(appProperties.isDisableTrustedParty()).thenReturn(false);
        when(organizationService.isRegistered("org-1")).thenReturn(true);

        assertThatThrownBy(() -> organizationFacade.register("org-1", request))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("already registered");
    }

    @Test
    void register_WhenClientCertificateIsMissing_ThrowsBadRequestException() {
        RegisterOrganizationRequestDTO request = validRequest();
        request.setClientCertificate(" ");

        when(appProperties.isDisableTrustedParty()).thenReturn(false);
        when(organizationService.isRegistered("org-1")).thenReturn(false);

        assertThatThrownBy(() -> organizationFacade.register("org-1", request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("clientCertificate");
    }

    @Test
    void register_WhenIntermediateCertificatesAreMissing_ThrowsBadRequestException() {
        RegisterOrganizationRequestDTO request = validRequest();
        request.setIntermediateCertificates(List.of());

        when(appProperties.isDisableTrustedParty()).thenReturn(false);
        when(organizationService.isRegistered("org-1")).thenReturn(false);

        assertThatThrownBy(() -> organizationFacade.register("org-1", request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("intermediateCertificates");
    }

    @Test
    void register_WhenTrustedPartyReturnsSuccess_CreatesOrganization() {
        RegisterOrganizationRequestDTO request = validRequest();

        when(appProperties.isDisableTrustedParty()).thenReturn(false);
        when(organizationService.isRegistered("org-1")).thenReturn(false);
        when(trustedPartyClient.registerOrganization("org-1", request)).thenReturn(ResponseEntity.ok("ok"));

        organizationFacade.register("org-1", request);

        verify(organizationService).createOrganization(
                "org-1",
                request.getClientCertificate(),
                request.getIntermediateCertificates(),
                request.getTrustedPartyUri()
        );
    }

    @Test
    void register_WhenTrustedPartyReturnsUnauthorized_ThrowsUnauthorizedException() {
        RegisterOrganizationRequestDTO request = validRequest();

        when(appProperties.isDisableTrustedParty()).thenReturn(false);
        when(organizationService.isRegistered("org-1")).thenReturn(false);
        when(trustedPartyClient.registerOrganization("org-1", request))
                .thenReturn(ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("unauthorized"));

        assertThatThrownBy(() -> organizationFacade.register("org-1", request))
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void register_WhenTrustedPartyReturnsBadRequest_ThrowsBadRequestException() {
        RegisterOrganizationRequestDTO request = validRequest();

        when(appProperties.isDisableTrustedParty()).thenReturn(false);
        when(organizationService.isRegistered("org-1")).thenReturn(false);
        when(trustedPartyClient.registerOrganization("org-1", request))
                .thenReturn(ResponseEntity.status(HttpStatus.BAD_REQUEST).body("bad request"));

        assertThatThrownBy(() -> organizationFacade.register("org-1", request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Trusted party rejected request");
    }

    @Test
    void register_WhenTrustedPartyReturnsConflict_ThrowsConflictException() {
        RegisterOrganizationRequestDTO request = validRequest();

        when(appProperties.isDisableTrustedParty()).thenReturn(false);
        when(organizationService.isRegistered("org-1")).thenReturn(false);
        when(trustedPartyClient.registerOrganization("org-1", request))
                .thenReturn(ResponseEntity.status(HttpStatus.CONFLICT).body("conflict"));

        assertThatThrownBy(() -> organizationFacade.register("org-1", request))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("Trusted party reports conflict");
    }

    @Test
    void register_WhenTrustedPartyReturnsUnavailable_ThrowsTrustedPartyUnavailableException() {
        RegisterOrganizationRequestDTO request = validRequest();

        when(appProperties.isDisableTrustedParty()).thenReturn(false);
        when(organizationService.isRegistered("org-1")).thenReturn(false);
        when(trustedPartyClient.registerOrganization("org-1", request))
                .thenReturn(ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body("down"));

        assertThatThrownBy(() -> organizationFacade.register("org-1", request))
                .isInstanceOf(TrustedPartyUnavailableException.class)
                .hasMessageContaining("unavailable");
    }

    @Test
    void register_WhenTrustedPartyReturnsUnexpectedError_ThrowsTrustedPartyErrorException() {
        RegisterOrganizationRequestDTO request = validRequest();

        when(appProperties.isDisableTrustedParty()).thenReturn(false);
        when(organizationService.isRegistered("org-1")).thenReturn(false);
        when(trustedPartyClient.registerOrganization("org-1", request))
                .thenReturn(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("boom"));

        assertThatThrownBy(() -> organizationFacade.register("org-1", request))
                .isInstanceOf(TrustedPartyErrorException.class)
                .hasMessageContaining("Trusted party error");
    }

    @Test
    void modify_WhenOrganizationDoesNotExist_ThrowsNotFoundException() {
        RegisterOrganizationRequestDTO request = validRequest();
        when(organizationService.isRegistered("org-1")).thenReturn(false);

        assertThatThrownBy(() -> organizationFacade.modify("org-1", request))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("is not registered");
    }

    @Test
    void modify_WhenClientCertificateIsMissing_ThrowsBadRequestException() {
        RegisterOrganizationRequestDTO request = validRequest();
        request.setClientCertificate(" ");

        when(organizationService.isRegistered("org-1")).thenReturn(true);

        assertThatThrownBy(() -> organizationFacade.modify("org-1", request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("clientCertificate");
    }

    @Test
    void modify_WhenTrustedPartyIsDisabled_SkipsTpAndModifiesOrganization() {
        RegisterOrganizationRequestDTO request = validRequest();

        when(organizationService.isRegistered("org-1")).thenReturn(true);
        when(appProperties.isDisableTrustedParty()).thenReturn(true);

        organizationFacade.modify("org-1", request);

        verify(trustedPartyClient, never()).updateOrganization("org-1", request);
        verify(organizationService).modifyOrganization(
                "org-1",
                request.getClientCertificate(),
                request.getIntermediateCertificates(),
                request.getTrustedPartyUri()
        );
    }

    @Test
    void modify_WhenTrustedPartyEnabledAndReturnsSuccess_ModifiesOrganization() {
        RegisterOrganizationRequestDTO request = validRequest();

        when(organizationService.isRegistered("org-1")).thenReturn(true);
        when(appProperties.isDisableTrustedParty()).thenReturn(false);
        when(trustedPartyClient.updateOrganization("org-1", request)).thenReturn(ResponseEntity.ok("ok"));

        organizationFacade.modify("org-1", request);

        verify(trustedPartyClient).updateOrganization("org-1", request);
        verify(organizationService).modifyOrganization(
                "org-1",
                request.getClientCertificate(),
                request.getIntermediateCertificates(),
                request.getTrustedPartyUri()
        );
    }

    @Test
    void modify_WhenTrustedPartyEnabledAndReturnsUnavailable_ThrowsTrustedPartyUnavailableException() {
        RegisterOrganizationRequestDTO request = validRequest();

        when(organizationService.isRegistered("org-1")).thenReturn(true);
        when(appProperties.isDisableTrustedParty()).thenReturn(false);
        when(trustedPartyClient.updateOrganization("org-1", request))
                .thenReturn(ResponseEntity.status(HttpStatus.BAD_GATEWAY).body("down"));

        assertThatThrownBy(() -> organizationFacade.modify("org-1", request))
                .isInstanceOf(TrustedPartyUnavailableException.class);

        verify(organizationService, never()).modifyOrganization(
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyList(),
                org.mockito.ArgumentMatchers.any()
        );
    }

    private RegisterOrganizationRequestDTO validRequest() {
        RegisterOrganizationRequestDTO dto = new RegisterOrganizationRequestDTO();
        dto.setClientCertificate("client-cert");
        dto.setIntermediateCertificates(List.of("int-cert"));
        dto.setTrustedPartyUri("tp.local");
        return dto;
    }
}
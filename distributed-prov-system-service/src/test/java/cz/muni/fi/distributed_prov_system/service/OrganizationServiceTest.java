package cz.muni.fi.distributed_prov_system.service;

import cz.muni.fi.distributed_prov_system.config.AppProperties;
import cz.muni.fi.distributed_prov_system.data.model.nonprovmodels.Organization;
import cz.muni.fi.distributed_prov_system.data.model.nonprovmodels.TrustedParty;
import cz.muni.fi.distributed_prov_system.data.repository.OrganizationRepository;
import cz.muni.fi.distributed_prov_system.data.repository.TrustedPartyRepository;
import cz.muni.fi.distributed_prov_system.exceptions.TrustedPartyErrorException;
import cz.muni.fi.distributed_prov_system.exceptions.TrustedPartyUnavailableException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrganizationServiceTest {

    @Mock
    private OrganizationRepository organizationRepository;

    @Mock
    private TrustedPartyRepository trustedPartyRepository;

    @Mock
    private AppProperties appProperties;

    @Mock
    private RestTemplate restTemplate;

    private OrganizationService organizationService;

    @BeforeEach
    void setUp() {
        organizationService = new OrganizationService(
                organizationRepository,
                trustedPartyRepository,
                appProperties,
                restTemplate
        );
    }

    @Test
    void isRegistered_WhenOrganizationExists_ReturnsTrue() {
        when(organizationRepository.existsById("org-1")).thenReturn(true);

        boolean result = organizationService.isRegistered("org-1");

        assertThat(result).isTrue();
    }

        @Test
        void isRegistered_WhenOrganizationDoesNotExist_ReturnsFalse() {
                when(organizationRepository.existsById("org-1")).thenReturn(false);

                boolean result = organizationService.isRegistered("org-1");

                assertThat(result).isFalse();
        }

    @Test
    void getTrustedPartyForOrganization_WhenOrganizationHasTrustedParty_ReturnsFirstTrustedParty() {
        TrustedParty trustedParty = new TrustedParty();
        trustedParty.setIdentifier("tp-1");

        Organization organization = new Organization();
        organization.setIdentifier("org-1");
        organization.setTrusts(List.of(trustedParty));

        when(organizationRepository.findById("org-1")).thenReturn(Optional.of(organization));

        TrustedParty result = organizationService.getTrustedPartyForOrganization("org-1");

        assertThat(result).isSameAs(trustedParty);
    }

    @Test
    void getTrustedPartyForOrganization_WhenOrganizationDoesNotExist_ReturnsNull() {
        when(organizationRepository.findById("org-1")).thenReturn(Optional.empty());

        TrustedParty result = organizationService.getTrustedPartyForOrganization("org-1");

        assertThat(result).isNull();
    }

        @Test
        void getTrustedPartyForOrganization_WhenTrustedPartyListIsEmpty_ReturnsNull() {
                Organization organization = new Organization();
                organization.setIdentifier("org-1");
                organization.setTrusts(List.of());

                when(organizationRepository.findById("org-1")).thenReturn(Optional.of(organization));

                TrustedParty result = organizationService.getTrustedPartyForOrganization("org-1");

                assertThat(result).isNull();
        }

        @Test
        void getTrustedPartyForOrganization_WhenTrustedPartyListIsNull_ReturnsNull() {
                Organization organization = new Organization();
                organization.setIdentifier("org-1");
                organization.setTrusts(null);

                when(organizationRepository.findById("org-1")).thenReturn(Optional.of(organization));

                TrustedParty result = organizationService.getTrustedPartyForOrganization("org-1");

                assertThat(result).isNull();
        }

    @Test
    void getTpUrlByOrganization_WhenTrustedPartyExists_ReturnsTrustedPartyUrl() {
        TrustedParty trustedParty = new TrustedParty();
        trustedParty.setUrl("tp.local");

        Organization organization = new Organization();
        organization.setIdentifier("org-1");
        organization.setTrusts(List.of(trustedParty));

        when(organizationRepository.findById("org-1")).thenReturn(Optional.of(organization));

        String result = organizationService.getTpUrlByOrganization("org-1");

        assertThat(result).isEqualTo("tp.local");
    }

    @Test
    void getTpUrlByOrganization_WhenTrustedPartyDoesNotExist_ReturnsNull() {
        when(organizationRepository.findById("org-1")).thenReturn(Optional.empty());

        String result = organizationService.getTpUrlByOrganization("org-1");

        assertThat(result).isNull();
    }

        @Test
        void getTpUrlByOrganization_WhenTrustedPartyUrlIsNull_ReturnsNull() {
                TrustedParty trustedParty = new TrustedParty();
                trustedParty.setUrl(null);

                Organization organization = new Organization();
                organization.setIdentifier("org-1");
                organization.setTrusts(List.of(trustedParty));

                when(organizationRepository.findById("org-1")).thenReturn(Optional.of(organization));

                String result = organizationService.getTpUrlByOrganization("org-1");

                assertThat(result).isNull();
        }

    @Test
    void createOrganization_WhenTrustedPartyExists_SavesOrganizationWithExistingTrustedParty() {
        OrganizationService.TrustedPartyInfoResponse info = trustedPartyInfo("tp-1", "cert-1");
        when(restTemplate.getForEntity(
                "http://tp.local/api/v1/info",
                OrganizationService.TrustedPartyInfoResponse.class)
        ).thenReturn(ResponseEntity.ok(info));

        TrustedParty existingTrustedParty = new TrustedParty();
        existingTrustedParty.setIdentifier("tp-1");
        existingTrustedParty.setUrl("tp.local");

        when(trustedPartyRepository.findById("tp-1")).thenReturn(Optional.of(existingTrustedParty));

        organizationService.createOrganization("org-1", "client-cert", List.of("intermediate-cert"), "tp.local");

        ArgumentCaptor<Organization> orgCaptor = ArgumentCaptor.forClass(Organization.class);
        verify(organizationRepository).save(orgCaptor.capture());

        Organization savedOrg = orgCaptor.getValue();
        assertThat(savedOrg.getIdentifier()).isEqualTo("org-1");
        assertThat(savedOrg.getClientCert()).isEqualTo("client-cert");
        assertThat(savedOrg.getIntermediateCerts()).containsExactly("intermediate-cert");
        assertThat(savedOrg.getTrusts()).containsExactly(existingTrustedParty);

        verify(trustedPartyRepository, never()).save(any(TrustedParty.class));
    }

    @Test
    void createOrganization_WhenTrustedPartyDoesNotExist_CreatesTrustedPartyAndSavesOrganization() {
        OrganizationService.TrustedPartyInfoResponse info = trustedPartyInfo("tp-1", "cert-1");
        when(restTemplate.getForEntity(
                "http://tp.local/api/v1/info",
                OrganizationService.TrustedPartyInfoResponse.class)
        ).thenReturn(ResponseEntity.ok(info));

        when(trustedPartyRepository.findById("tp-1")).thenReturn(Optional.empty());

        TrustedParty createdTrustedParty = new TrustedParty();
        createdTrustedParty.setIdentifier("tp-1");
        createdTrustedParty.setUrl("tp.local");
        createdTrustedParty.setCertificate("cert-1");
        createdTrustedParty.setChecked(false);
        createdTrustedParty.setValid(false);

        when(trustedPartyRepository.save(any(TrustedParty.class))).thenReturn(createdTrustedParty);

        organizationService.createOrganization("org-1", "client-cert", List.of("intermediate-cert"), "tp.local");

        verify(trustedPartyRepository).save(any(TrustedParty.class));

        ArgumentCaptor<Organization> orgCaptor = ArgumentCaptor.forClass(Organization.class);
        verify(organizationRepository).save(orgCaptor.capture());

        assertThat(orgCaptor.getValue().getTrusts()).containsExactly(createdTrustedParty);
    }

    @Test
    void createOrganization_WhenTpUriIsNull_UsesDefaultTpUrlFromProperties() {
        when(appProperties.getTpFqdn()).thenReturn("default-tp.local");

        OrganizationService.TrustedPartyInfoResponse info = trustedPartyInfo("tp-1", "cert-1");
        when(restTemplate.getForEntity(
                "http://default-tp.local/api/v1/info",
                OrganizationService.TrustedPartyInfoResponse.class)
        ).thenReturn(ResponseEntity.ok(info));

        TrustedParty existingTrustedParty = new TrustedParty();
        existingTrustedParty.setIdentifier("tp-1");
        when(trustedPartyRepository.findById("tp-1")).thenReturn(Optional.of(existingTrustedParty));

        organizationService.createOrganization("org-1", "client-cert", List.of("intermediate-cert"), null);

        verify(restTemplate).getForEntity(
                "http://default-tp.local/api/v1/info",
                OrganizationService.TrustedPartyInfoResponse.class
        );
    }

    @Test
    void createOrganization_WhenTpCallFails_ThrowsTrustedPartyUnavailableException() {
        when(restTemplate.getForEntity(
                "http://tp.local/api/v1/info",
                OrganizationService.TrustedPartyInfoResponse.class)
        ).thenThrow(new RuntimeException("boom"));

        assertThatThrownBy(() -> organizationService.createOrganization(
                "org-1",
                "client-cert",
                List.of("intermediate-cert"),
                "tp.local"
        )).isInstanceOf(TrustedPartyUnavailableException.class)
                .hasMessageContaining("Couldn't retrieve info from TP at http://tp.local/api/v1/info");
    }

    @Test
    void createOrganization_WhenTpResponseIsNotSuccessful_ThrowsTrustedPartyUnavailableException() {
        OrganizationService.TrustedPartyInfoResponse info = trustedPartyInfo("tp-1", "cert-1");
        when(restTemplate.getForEntity(
                "http://tp.local/api/v1/info",
                OrganizationService.TrustedPartyInfoResponse.class)
        ).thenReturn(ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(info));

        assertThatThrownBy(() -> organizationService.createOrganization(
                "org-1",
                "client-cert",
                List.of("intermediate-cert"),
                "tp.local"
        )).isInstanceOf(TrustedPartyUnavailableException.class)
                .hasMessageContaining("HTTP 503");
    }

    @Test
    void createOrganization_WhenTpResponseIdIsMissing_ThrowsTrustedPartyErrorException() {
        OrganizationService.TrustedPartyInfoResponse info = trustedPartyInfo(" ", "cert-1");
        when(restTemplate.getForEntity(
                "http://tp.local/api/v1/info",
                OrganizationService.TrustedPartyInfoResponse.class)
        ).thenReturn(ResponseEntity.ok(info));

        assertThatThrownBy(() -> organizationService.createOrganization(
                "org-1",
                "client-cert",
                List.of("intermediate-cert"),
                "tp.local"
        )).isInstanceOf(TrustedPartyErrorException.class)
                .hasMessageContaining("missing required field 'id'");
    }

    @Test
    void modifyOrganization_WhenOrganizationDoesNotExist_ThrowsNoSuchElementException() {
        when(organizationRepository.findById("org-1")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> organizationService.modifyOrganization(
                "org-1",
                "client-cert",
                List.of("intermediate-cert"),
                "tp.local"
        )).isInstanceOf(NoSuchElementException.class);
    }

    @Test
    void modifyOrganization_WhenOrganizationExists_UpdatesAndSavesOrganization() {
        Organization existingOrg = new Organization();
        existingOrg.setIdentifier("org-1");
        existingOrg.setClientCert("old-client-cert");
        existingOrg.setIntermediateCerts(List.of("old-int"));

        when(organizationRepository.findById("org-1")).thenReturn(Optional.of(existingOrg));

        OrganizationService.TrustedPartyInfoResponse info = trustedPartyInfo("tp-1", "cert-1");
        when(restTemplate.getForEntity(
                "http://tp.local/api/v1/info",
                OrganizationService.TrustedPartyInfoResponse.class)
        ).thenReturn(ResponseEntity.ok(info));

        TrustedParty trustedParty = new TrustedParty();
        trustedParty.setIdentifier("tp-1");
        when(trustedPartyRepository.findById("tp-1")).thenReturn(Optional.of(trustedParty));

        organizationService.modifyOrganization(
                "org-1",
                "new-client-cert",
                List.of("new-int"),
                "tp.local"
        );

        assertThat(existingOrg.getClientCert()).isEqualTo("new-client-cert");
        assertThat(existingOrg.getIntermediateCerts()).containsExactly("new-int");
        assertThat(existingOrg.getTrusts()).containsExactly(trustedParty);

        verify(organizationRepository).save(existingOrg);
    }

    private OrganizationService.TrustedPartyInfoResponse trustedPartyInfo(String id, String certificate) {
        OrganizationService.TrustedPartyInfoResponse info = new OrganizationService.TrustedPartyInfoResponse();
        info.setId(id);
        info.setCertificate(certificate);
        return info;
    }
}
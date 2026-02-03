package cz.muni.fi.distributed_prov_system.facade;

import cz.muni.fi.distributed_prov_system.api.RegisterOrganizationRequestDTO;
import cz.muni.fi.distributed_prov_system.client.TrustedPartyClient;
import cz.muni.fi.distributed_prov_system.config.AppProperties;
import cz.muni.fi.distributed_prov_system.exceptions.*;
import cz.muni.fi.distributed_prov_system.service.OrganizationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Service
public class OrganizationFacadeImpl implements OrganizationFacade {

    private final OrganizationService service;
    private final TrustedPartyClient tpClient;
    private final AppProperties props;

    @Autowired
    public OrganizationFacadeImpl(OrganizationService service,
                                  TrustedPartyClient tpClient,
                                  AppProperties props) {
        this.service = service;
        this.tpClient = tpClient;
        this.props = props;
    }

    @Override
    @Transactional
    public void register(String organizationId, RegisterOrganizationRequestDTO body) {
        if (props.isDisableTrustedParty()) {
            throw new TrustedPartyDisabledException("Since Trusted party is disabled, registration is also disabled");
        }

        if (service.isRegistered(organizationId)) {
            throw new ConflictException("Organization with id [" + organizationId + "] is already registered. If you want to modify it, send PUT request!");
        }
        validate(body);

        ResponseEntity<String> resp = tpClient.registerOrganization(organizationId, body);
        handleTpResponse(resp);

        service.createOrganization(
                organizationId,
                body.getClientCertificate(),
                body.getIntermediateCertificates(),
                body.getTrustedPartyUri()
        );
    }

    @Override
    @Transactional
    public void modify(String organizationId, RegisterOrganizationRequestDTO body) {
        if (!service.isRegistered(organizationId)) {
            throw new NotFoundException("Organization with id [" + organizationId + "] is not registered!");
        }
        validate(body);

        if (!props.isDisableTrustedParty()) {
            ResponseEntity<String> resp = tpClient.updateOrganization(organizationId, body);
            handleTpResponse(resp);
        }

        service.modifyOrganization(
                organizationId,
                body.getClientCertificate(),
                body.getIntermediateCertificates(),
                body.getTrustedPartyUri()
        );
    }

    private void handleTpResponse(ResponseEntity<String> resp) {
        if (resp.getStatusCode().is2xxSuccessful()) return;

        int code = resp.getStatusCode().value();
        String details = resp.getBody();

        if (code == 401) {
            throw new UnauthorizedException("Trusted party was unable to verify certificate chain!");
        }
        if (code == 400) {
            throw new BadRequestException("Trusted party rejected request: " + details);
        }
        if (code == 409) {
            throw new ConflictException("Trusted party reports conflict: " + details);
        }
        if (code == 502 || code == 503 || code == 504) {
            throw new TrustedPartyUnavailableException("Trusted party unavailable: " + details);
        }

        throw new TrustedPartyErrorException("Trusted party error: HTTP " + code + " " + details);
    }

    private void validate(RegisterOrganizationRequestDTO body) {
        if (body == null || body.getClientCertificate() == null || body.getClientCertificate().isBlank()) {
            throw new BadRequestException("Mandatory field [clientCertificate] not present in request!");
        }
        if (body.getIntermediateCertificates() == null || body.getIntermediateCertificates().isEmpty()) {
            throw new BadRequestException("Mandatory field [intermediateCertificates] not present in request!");
        }
    }
}

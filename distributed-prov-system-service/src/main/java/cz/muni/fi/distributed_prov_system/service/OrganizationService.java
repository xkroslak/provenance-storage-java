package cz.muni.fi.distributed_prov_system.service;

import cz.muni.fi.distributed_prov_system.config.AppProperties;
import cz.muni.fi.distributed_prov_system.data.model.nonprovmodels.Organization;
import cz.muni.fi.distributed_prov_system.data.model.nonprovmodels.TrustedParty;
import cz.muni.fi.distributed_prov_system.data.repository.OrganizationRepository;
import cz.muni.fi.distributed_prov_system.data.repository.TrustedPartyRepository;
import cz.muni.fi.distributed_prov_system.exceptions.TrustedPartyErrorException;
import cz.muni.fi.distributed_prov_system.exceptions.TrustedPartyUnavailableException;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@Service
public class OrganizationService {

    private final OrganizationRepository organizationRepository;
    private final TrustedPartyRepository trustedPartyRepository;
    private final AppProperties appProperties;
    private final RestTemplate restTemplate;

    public OrganizationService(OrganizationRepository organizationRepository,
                               TrustedPartyRepository trustedPartyRepository,
                               AppProperties appProperties,
                               RestTemplate restTemplate) {
        this.organizationRepository = organizationRepository;
        this.trustedPartyRepository = trustedPartyRepository;
        this.appProperties = appProperties;
        this.restTemplate = restTemplate;
    }

    public boolean isRegistered(String organizationId) {
        return organizationRepository.existsById(organizationId);
    }

    public TrustedParty getTrustedPartyForOrganization(String organizationId) {
        return organizationRepository.findById(organizationId)
                .map(Organization::getTrusts)
                .filter(list -> list != null && !list.isEmpty())
                .map(List::getFirst)
                .orElse(null);
    }

    public String getTpUrlByOrganization(String organizationId) {
        TrustedParty tp = getTrustedPartyForOrganization(organizationId);
        return tp != null ? tp.getUrl() : null;
    }

    public void createOrganization(String organizationId,
                                   String clientCert,
                                   List<String> intermediates,
                                   String tpUri) {
        TpLookup tpLookup = getTp(tpUri);

        Organization org = new Organization();
        org.setIdentifier(organizationId);
        org.setClientCert(clientCert);
        org.setIntermediateCerts(intermediates);

        org.setTrusts(List.of(tpLookup.tp()));

        organizationRepository.save(org);
    }

    public void modifyOrganization(String organizationId,
                                   String clientCert,
                                   List<String> intermediates,
                                   String tpUri) {
        Organization org = organizationRepository.findById(organizationId).orElseThrow();

        TpLookup tpLookup = getTp(tpUri);

        org.setClientCert(clientCert);
        org.setIntermediateCerts(intermediates);
        org.setTrusts(List.of(tpLookup.tp()));

        organizationRepository.save(org);
    }

    private TpLookup getTp(String url) {
        String effectiveUrl = (url == null) ? appProperties.getTpFqdn() : url;

        String infoUrl = "http://" + effectiveUrl + "/api/v1/info";

        ResponseEntity<TrustedPartyInfoResponse> resp;
        try {
            resp = restTemplate.getForEntity(infoUrl, TrustedPartyInfoResponse.class);
        } catch (Exception ex) {
            throw new TrustedPartyUnavailableException("Couldn't retrieve info from TP at " + infoUrl + ": " + ex.getMessage());
        }

        if (!resp.getStatusCode().is2xxSuccessful() || resp.getBody() == null) {
            throw new TrustedPartyUnavailableException("Couldn't retrieve info from TP at " + infoUrl + ": HTTP " + resp.getStatusCode().value());
        }

        TrustedPartyInfoResponse info = resp.getBody();
        if (info.getId() == null || info.getId().isBlank()) {
            throw new TrustedPartyErrorException("TP info response missing required field 'id' from " + infoUrl);
        }

        TrustedParty existingOrNew = trustedPartyRepository.findById(info.getId())
                .orElseGet(() -> {
                    TrustedParty created = new TrustedParty();
                    created.setIdentifier(info.getId());
                    created.setUrl(effectiveUrl);

                    // If your TrustedParty model uses a different field name, adjust this line.
                    created.setCertificate(info.getCertificate());

                    created.setChecked(false);
                    created.setValid(false);
                    return trustedPartyRepository.save(created);
                });

        return new TpLookup(existingOrNew);
    }

    private record TpLookup(TrustedParty tp) { }

    public static class TrustedPartyInfoResponse {
        private String id;
        private String certificate;

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }

        public String getCertificate() { return certificate; }
        public void setCertificate(String certificate) { this.certificate = certificate; }
    }
}
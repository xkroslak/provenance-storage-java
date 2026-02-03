package cz.muni.fi.distributed_prov_system.client;

import cz.muni.fi.distributed_prov_system.api.RegisterOrganizationRequestDTO;
import cz.muni.fi.distributed_prov_system.config.AppProperties;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Component
public class TrustedPartyClient {

    private final RestTemplate restTemplate;
    private final String defaultTpUrl;

    public TrustedPartyClient(RestTemplate restTemplate, AppProperties appProperties) {
        this.restTemplate = restTemplate;
        this.defaultTpUrl = appProperties.getTpFqdn();
    }

    public ResponseEntity<String> registerOrganization(String organizationId, RegisterOrganizationRequestDTO body) {
        return sendRegisterOrUpdateRequest(organizationId, body, true);
    }

    public ResponseEntity<String> updateOrganization(String organizationId, RegisterOrganizationRequestDTO body) {
        return sendRegisterOrUpdateRequest(organizationId, body, false);
    }

    private ResponseEntity<String> sendRegisterOrUpdateRequest(String organizationId,
                                                               RegisterOrganizationRequestDTO body,
                                                               boolean isPost) {
        String tpUrl = (body != null && body.getTrustedPartyUri() != null && !body.getTrustedPartyUri().isBlank())
                ? body.getTrustedPartyUri()
                : defaultTpUrl;

        String url = "http://" + tpUrl + "/api/v1/organizations/" + organizationId;

        Map<String, Object> payload = new HashMap<>();
        payload.put("organizationId", organizationId);
        payload.put("clientCertificate", body.getClientCertificate());
        payload.put("intermediateCertificates", body.getIntermediateCertificates());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(java.util.List.of(MediaType.APPLICATION_JSON));

        HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(payload, headers);

        try {
            HttpMethod method = isPost ? HttpMethod.POST : HttpMethod.PUT;
            return restTemplate.exchange(url, method, requestEntity, String.class);
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                    .body("Trusted Party unavailable: " + ex.getMessage());
        }
    }

    public ResponseEntity<String> issueToken(Object payload, String tpUrlOverride) {
        return postJson("/api/v1/issueToken", payload, tpUrlOverride);
    }

    public ResponseEntity<String> verifySignature(Object payload, String tpUrlOverride) {
        return postJson("/api/v1/verifySignature", payload, tpUrlOverride);
    }

    private ResponseEntity<String> postJson(String path, Object payload, String tpUrlOverride) {
        String base = (tpUrlOverride == null || tpUrlOverride.isBlank()) ? defaultTpUrl : tpUrlOverride;
        String url = "http://" + base + path;

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(java.util.List.of(MediaType.APPLICATION_JSON));

        HttpEntity<Object> entity = new HttpEntity<>(payload, headers);
        return restTemplate.postForEntity(url, entity, String.class);
    }
}
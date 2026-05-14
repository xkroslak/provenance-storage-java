package cz.muni.fi.distributed_prov_system.utils.prov;

import cz.muni.fi.distributed_prov_system.utils.ProvConstants;
import org.openprovenance.prov.model.Entity;
import org.openprovenance.prov.model.QualifiedName;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.List;

import static cz.muni.fi.distributed_prov_system.utils.prov.ProvToolboxValidationUtils.getOtherAttributeValue;

@Component
public class ConnectorResolvabilityChecker {

    private final RestTemplate restTemplate;

    public ConnectorResolvabilityChecker(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public CPMValidator.ValidationResult checkResolvability(List<Entity> connectors) {
        if (connectors == null || connectors.isEmpty()) {
            return new CPMValidator.ValidationResult(true, "ok");
        }

        for (Entity connector : connectors) {
            String bundleUri = extractUri(getOtherAttributeValue(connector, ProvConstants.CPM_REFERENCED_BUNDLE_ID));
            String metaBundleUri = extractUri(getOtherAttributeValue(connector, ProvConstants.CPM_REFERENCED_META_BUNDLE_ID));

            if (bundleUri == null && metaBundleUri == null) {
                continue;
            }

            if (bundleUri != null && !isReachable(bundleUri)) {
                return new CPMValidator.ValidationResult(false,
                        "Connector references unreachable bundle at [" + bundleUri + "].");
            }
            if (metaBundleUri != null && !isReachable(metaBundleUri)) {
                return new CPMValidator.ValidationResult(false,
                        "Connector references unreachable meta-bundle at [" + metaBundleUri + "].");
            }
        }

        return new CPMValidator.ValidationResult(true, "ok");
    }

    private boolean isReachable(String uri) {
        try {
            restTemplate.headForHeaders(uri);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private String extractUri(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof QualifiedName qn) {
            return qn.getUri() != null ? qn.getUri() : qn.toString();
        }
        return value.toString();
    }
}

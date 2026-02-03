package cz.muni.fi.distributed_prov_system.utils.prov;

import cz.muni.fi.distributed_prov_system.config.AppProperties;
import cz.muni.fi.distributed_prov_system.utils.ProvConstants;
import org.openprovenance.prov.model.*;

import java.util.ArrayList;
import java.util.List;

import static cz.muni.fi.distributed_prov_system.utils.prov.ProvToolboxValidationUtils.*;

public class InputGraphChecker {
    private static final String NOT_YET_PARSED_ERROR = "Graph not yet parsed.";
    private static final String NOT_YET_VALIDATED_ERROR = "Graph not yet validated.";

    private final String graph;
    private final String format;
    private final String requestPath;
    private final AppProperties appProperties;
    private final CPMValidator cpmValidator;
    private final ProvDocumentValidator provValidator;

    private Document document;
    private Bundle bundle;
    private Activity mainActivity;
    private String metaProvenanceId;
    private List<Entity> forwardConnectors;
    private List<Entity> backwardConnectors;

    public InputGraphChecker(String graph,
                             String format,
                             String requestPath,
                             AppProperties appProperties,
                             CPMValidator cpmValidator,
                             ProvDocumentValidator provValidator) {
        this.graph = graph;
        this.format = format;
        this.requestPath = requestPath;
        this.appProperties = appProperties;
        this.cpmValidator = cpmValidator;
        this.provValidator = provValidator;
    }

    public Document getDocument() {
        ensureParsed();
        return document;
    }

    public String getBundleId() {
        ensureParsed();
        return bundle.getId().getLocalPart();
    }

    public String getMetaProvenanceId() {
        ensureParsed();
        return metaProvenanceId;
    }

    public List<Entity> getForwardConnectors() {
        ensureValidated();
        return forwardConnectors;
    }

    public List<Entity> getBackwardConnectors() {
        ensureValidated();
        return backwardConnectors;
    }

    public void parseGraph() {
        this.document = ProvToolboxUtils.parseDocument(graph, format);
        this.bundle = extractSingleBundle(document);
        this.mainActivity = findMainActivity(bundle);
        this.metaProvenanceId = resolveMetaProvenanceId(mainActivity);
        this.forwardConnectors = findConnectors(bundle, ProvConstants.CPM_FORWARD_CONNECTOR);
        this.backwardConnectors = findConnectors(bundle, ProvConstants.CPM_BACKWARD_CONNECTOR);
    }

    public void checkIdsMatch(String graphId) {
        ensureParsed();
        String bundleId = bundle.getId().getLocalPart();
        if (!bundleId.equals(graphId)) {
            throw new IllegalArgumentException(
                    "The bundle id [" + bundleId + "] does not match requested id [" + graphId + "].");
        }
        if (bundle.getId().getUri() != null && requestPath != null &&
                !bundle.getId().getUri().endsWith(requestPath)) {
            throw new IllegalArgumentException(
                    "The bundle identifier does not end with request path [" + requestPath + "].");
        }
    }

    public void validateGraph() {
        ensureParsed();
        if (document.getStatementOrBundle() == null || bundle == null) {
            throw new IllegalArgumentException("There are no bundles inside the document!");
        }

        if (!cpmValidator.checkBackwardConnectorsAttributes(backwardConnectors)) {
            throw new IllegalArgumentException("Backward connector(s) missing mandatory attributes.");
        }
        if (!cpmValidator.checkForwardConnectorsAttributes(forwardConnectors)) {
            throw new IllegalArgumentException("Forward connector(s) missing mandatory attributes.");
        }

        CPMValidator.ValidationResult result = cpmValidator.checkCpmConstraints(
                bundle, forwardConnectors, backwardConnectors, mainActivity);
        if (!result.ok()) {
            throw new IllegalArgumentException("CPM problem: " + result.message());
        }

        if (provValidator != null && !provValidator.isValid(document)) {
            throw new IllegalArgumentException("The bundle is not valid according to PROV standard.");
        }
    }

    private Bundle extractSingleBundle(Document document) {
        Bundle first = null;
        for (StatementOrBundle sb : document.getStatementOrBundle()) {
            if (sb instanceof Bundle b) {
                if (first != null) {
                    throw new IllegalArgumentException("Only one bundle expected in document!");
                }
                first = b;
            }
        }
        if (first == null) {
            throw new IllegalArgumentException("There are no bundles inside the document!");
        }
        return first;
    }

    private Activity findMainActivity(Bundle bundle) {
        for (Statement statement : bundle.getStatement()) {
            if (statement instanceof Activity activity) {
                if (hasType(activity, ProvConstants.CPM_MAIN_ACTIVITY)) {
                    return activity;
                }
            }
        }
        throw new IllegalArgumentException("Main activity not found in CPM bundle.");
    }

    private String resolveMetaProvenanceId(Activity activity) {
        Object value = getOtherAttributeValue(activity, ProvConstants.CPM_REFERENCED_META_BUNDLE_ID);
        if (value == null) {
            throw new IllegalArgumentException("Main activity missing required attribute 'referencedMetaBundleId'.");
        }
        String valueStr = value.toString();
        if (!valueStr.contains("/api/v1/documents/meta/")) {
            throw new IllegalArgumentException("Main activity URI is not a valid metabundle location: " + valueStr);
        }
        String id = ProvToolboxValidationUtils.getMetaBundleId(activity);
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Unable to resolve meta provenance id.");
        }
        String fqdn = appProperties.getFqdn();
        if (fqdn != null && !fqdn.isBlank()) {
            if (!valueStr.contains(fqdn)) {
                throw new IllegalArgumentException("Main activity URI is expected to be local to this server.");
            }
        }
        return id;
    }

    private List<Entity> findConnectors(Bundle bundle, String connectorType) {
        List<Entity> connectors = new ArrayList<>();
        for (Statement statement : bundle.getStatement()) {
            if (statement instanceof Entity entity) {
                if (hasType(entity, connectorType)) {
                    connectors.add(entity);
                }
            }
        }
        return connectors;
    }

    private boolean hasType(HasType element, String type) {
        if (element == null || element.getType() == null) {
            return false;
        }
        for (Type t : element.getType()) {
            Object value = t.getValue();
            if (value == null) {
                continue;
            }
            if (value instanceof QualifiedName qn) {
                if (matchesQualifiedName(qn, type)) {
                    return true;
                }
            } else if (type.equals(value.toString())) {
                return true;
            }
        }
        return false;
    }

    private void ensureParsed() {
        if (document == null || bundle == null) {
            throw new IllegalStateException(NOT_YET_PARSED_ERROR);
        }
    }

    private void ensureValidated() {
        if (forwardConnectors == null || backwardConnectors == null) {
            throw new IllegalStateException(NOT_YET_VALIDATED_ERROR);
        }
    }
}

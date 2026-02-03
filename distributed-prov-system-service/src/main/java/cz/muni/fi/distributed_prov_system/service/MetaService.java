package cz.muni.fi.distributed_prov_system.service;

import cz.muni.fi.distributed_prov_system.client.TrustedPartyClient;
import cz.muni.fi.distributed_prov_system.config.AppProperties;
import cz.muni.fi.distributed_prov_system.data.repository.BundleRepository;
import cz.muni.fi.distributed_prov_system.exceptions.MetaNotFoundException;
import cz.muni.fi.distributed_prov_system.utils.TokenUtils;
import cz.muni.fi.distributed_prov_system.utils.prov.ProvToolboxUtils;
import org.openprovenance.prov.model.*;
import org.openprovenance.prov.vanilla.ProvFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.neo4j.core.Neo4jClient;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;

@Service
public class MetaService {

    private final AppProperties appProperties;
    private final BundleRepository bundleRepository;
    private final OrganizationService organizationService;
    private final TrustedPartyClient trustedPartyClient;
    private final Neo4jClient neo4jClient;

    @Autowired
    public MetaService(AppProperties appProperties,
                       BundleRepository bundleRepository,
                       OrganizationService organizationService,
                       TrustedPartyClient trustedPartyClient,
                       Neo4jClient neo4jClient) {
        this.appProperties = appProperties;
        this.bundleRepository = bundleRepository;
        this.organizationService = organizationService;
        this.trustedPartyClient = trustedPartyClient;
        this.neo4jClient = neo4jClient;
    }

    public boolean metaBundleExists(String metaId) {
        return bundleRepository.existsById(metaId);
    }

    public String getB64EncodedMetaProvenance(String metaId, String format) {
        if (!metaBundleExists(metaId)) {
            throw new MetaNotFoundException("The meta-provenance with id [" + metaId + "] does not exist.");
        }

        List<MetaNode> nodes = loadMetaNodes(metaId);
        List<MetaRelation> relations = loadMetaRelations(metaId);

        Document document = buildMetaDocument(metaId, nodes, relations);
        return ProvToolboxUtils.serializeDocumentToBase64(document, format);
    }

    public boolean isTrustedPartyDisabled() {
        return appProperties.isDisableTrustedParty();
    }

    public String getTpUrlByOrganization(String organizationId) {
        return organizationService.getTpUrlByOrganization(organizationId);
    }

    public Object buildMetaTokenPayload(String graph, String metaId, String format, String organizationId) {
        // Build payload for TP token request
        return new MetaTokenPayload(graph, metaId, format, organizationId, appProperties.getId());
    }

    public Object sendTokenRequestToTp(Object payload, String tpUrl) {
        ResponseEntity<String> resp = trustedPartyClient.issueToken(payload, tpUrl);
        if (!resp.getStatusCode().is2xxSuccessful()) {
            throw new IllegalStateException("Could not issue token.");
        }
        return TokenUtils.parseTokenResponse(resp.getBody());
    }

    // Example DTO for token payload
    public static class MetaTokenPayload {
        public String document;
        public long createdOn;
        public String type = "meta";
        public String organizationId;
        public String documentFormat;
        public String graphId;
        public MetaTokenPayload(String document, String graphId, String documentFormat, String organizationId, String appId) {
            this.document = document;
            this.createdOn = System.currentTimeMillis() / 1000;
            this.organizationId = appId;
            this.documentFormat = documentFormat;
            this.graphId = graphId;
        }
    }

    private List<MetaNode> loadMetaNodes(String metaId) {
        return neo4jClient.query(
                        "MATCH (b:Bundle {identifier:$id})-[:contains]->(n) " +
                        "RETURN labels(n) as labels, n.identifier as identifier, n.attributes as attributes, " +
                        "n.startTime as startTime, n.endTime as endTime")
                .bind(metaId).to("id")
                .fetch()
                .all()
                .stream()
                .map(this::toMetaNode)
                .toList();
    }

    private List<MetaRelation> loadMetaRelations(String metaId) {
        return neo4jClient.query(
                        "MATCH (b:Bundle {identifier:$id})-[:contains]->(a)-[r]->(b2)<-[:contains]-(b) " +
                        "RETURN type(r) as type, a.identifier as fromId, b2.identifier as toId, properties(r) as props")
                .bind(metaId).to("id")
                .fetch()
                .all()
                .stream()
                .map(this::toMetaRelation)
                .toList();
    }

    private MetaNode toMetaNode(Map<String, Object> row) {
        @SuppressWarnings("unchecked")
        List<String> labels = (List<String>) row.getOrDefault("labels", List.of());
        @SuppressWarnings("unchecked")
        Map<String, Object> attributes = (Map<String, Object>) row.get("attributes");
        LocalDateTime startTime = (LocalDateTime) row.get("startTime");
        LocalDateTime endTime = (LocalDateTime) row.get("endTime");
        return new MetaNode(row.get("identifier").toString(), new HashSet<>(labels), attributes, startTime, endTime);
    }

    private MetaRelation toMetaRelation(Map<String, Object> row) {
        @SuppressWarnings("unchecked")
        Map<String, Object> props = (Map<String, Object>) row.get("props");
        return new MetaRelation(row.get("type").toString(), row.get("fromId").toString(), row.get("toId").toString(), props);
    }

    private Document buildMetaDocument(String metaId, List<MetaNode> nodes, List<MetaRelation> relations) {
        ProvFactory factory = ProvFactory.getFactory();
        Namespace namespace = new Namespace();
        String base = appProperties.getFqdn();
        String metaNs = base + "/api/v1/documents/meta/";

        namespace.register("prov", "http://www.w3.org/ns/prov#");
        namespace.register("cpm", "https://www.commonprovenancemodel.org/cpm-namespace-v1-0/");
        namespace.register("pav", "http://purl.org/pav/");
        namespace.register("dct", "http://purl.org/dc/terms/");
        namespace.register("meta", metaNs);
        namespace.setDefaultNamespace(base);

        Map<String, QualifiedName> qnameById = new HashMap<>();
        Map<String, MetaNode> nodeById = new HashMap<>();

        for (MetaNode node : nodes) {
            nodeById.put(node.identifier(), node);
            QualifiedName qn = buildQualifiedName(node, namespace, factory, base);
            qnameById.put(node.identifier(), qn);
        }

        Document document = factory.newDocument();
        document.setNamespace(namespace);

        QualifiedName bundleId = factory.newQualifiedName(metaNs, metaId, "meta");
        Bundle bundle = factory.newNamedBundle(bundleId, namespace, new ArrayList<>());

        for (MetaNode node : nodes) {
            QualifiedName qn = qnameById.get(node.identifier());
            List<Attribute> attrs = convertAttributes(node.attributes(), namespace, factory);
            if (node.isActivity()) {
                XMLGregorianCalendar start = toXmlGregorian(node.startTime());
                XMLGregorianCalendar end = toXmlGregorian(node.endTime());
                bundle.getStatement().add(factory.newActivity(qn, start, end, attrs));
            } else if (node.isAgent()) {
                bundle.getStatement().add(factory.newAgent(qn, attrs));
            } else if (node.isEntity()) {
                bundle.getStatement().add(factory.newEntity(qn, attrs));
            }
        }

        for (MetaRelation rel : relations) {
            QualifiedName from = qnameById.get(rel.fromId());
            QualifiedName to = qnameById.get(rel.toId());
            if (from == null || to == null) {
                continue;
            }
            List<Attribute> attrs = convertAttributes(rel.props(), namespace, factory);
            Statement stmt = toRelationStatement(factory, rel, from, to, attrs);
            if (stmt != null) {
                bundle.getStatement().add(stmt);
            }
        }

        document.getStatementOrBundle().add(bundle);
        return document;
    }

    private QualifiedName buildQualifiedName(MetaNode node, Namespace namespace, ProvFactory factory, String base) {
        String id = node.identifier();

        if (node.isActivity() && id.contains("_")) {
            String local = id.substring(id.indexOf('_') + 1);
            return factory.newQualifiedName(namespace.getDefaultNamespace(), local, "def");
        }

        if (node.isEntity() && id.contains("_") && !id.endsWith("_token") && !id.endsWith("_tokenGeneration")) {
            String org = id.substring(0, id.indexOf('_'));
            String local = id.substring(id.indexOf('_') + 1);
            String orgNs = base + "/api/v1/organizations/" + org + "/graphs/";
            namespace.register(org, orgNs);
            return factory.newQualifiedName(orgNs, local, org);
        }

        return factory.newQualifiedName(namespace.getDefaultNamespace(), id, "def");
    }

    private List<Attribute> convertAttributes(Map<String, Object> attributes,
                                              Namespace namespace,
                                              ProvFactory factory) {
        if (attributes == null || attributes.isEmpty()) {
            return List.of();
        }
        List<Attribute> result = new ArrayList<>();
        for (Map.Entry<String, Object> entry : attributes.entrySet()) {
            QualifiedName name = toAttributeName(entry.getKey(), namespace, factory);
            Object value = toAttributeValue(entry.getValue(), namespace, factory);
            if (name != null) {
                result.add(factory.newAttribute(name, value, null));
            }
        }
        return result;
    }

    private QualifiedName toAttributeName(String key, Namespace namespace, ProvFactory factory) {
        if (key == null) {
            return null;
        }
        if (key.startsWith("http://") || key.startsWith("https://")) {
            int idx = Math.max(key.lastIndexOf('#'), key.lastIndexOf('/'));
            if (idx > 0 && idx < key.length() - 1) {
                String ns = key.substring(0, idx + 1);
                String local = key.substring(idx + 1);
                return factory.newQualifiedName(ns, local, null);
            }
            return factory.newQualifiedName(key, "", null);
        }

        if (key.contains(":")) {
            String[] parts = key.split(":", 2);
            String prefix = parts[0];
            String local = parts[1];
            String ns = namespace.getNamespaces().get(prefix);
            if (ns == null) {
                return factory.newQualifiedName(namespace.getDefaultNamespace(), key, "def");
            }
            return factory.newQualifiedName(ns, local, prefix);
        }

        return factory.newQualifiedName(namespace.getDefaultNamespace(), key, "def");
    }

    private Object toAttributeValue(Object value, Namespace namespace, ProvFactory factory) {
        if (value instanceof String str && str.contains(":")) {
            String[] parts = str.split(":", 2);
            String ns = namespace.getNamespaces().get(parts[0]);
            if (ns != null) {
                return factory.newQualifiedName(ns, parts[1], parts[0]);
            }
        }
        return value;
    }

    private Statement toRelationStatement(ProvFactory factory,
                                          MetaRelation rel,
                                          QualifiedName from,
                                          QualifiedName to,
                                          List<Attribute> attrs) {
        String type = rel.type();
        XMLGregorianCalendar time = toXmlGregorian(rel.getLocalDateTime("time"));

        return switch (type) {
            case "specialization_of" -> factory.newSpecializationOf(from, to);
            case "was_revision_of" -> {
                List<Attribute> revisionAttrs = new ArrayList<>(attrs);
                QualifiedName provType = factory.newQualifiedName("http://www.w3.org/ns/prov#", "type", "prov");
                revisionAttrs.add(factory.newAttribute(provType, "prov:revisionOf", null));
                yield factory.newWasDerivedFrom(null, from, to, null, null, null, revisionAttrs);
            }
            case "was_derived_from" -> factory.newWasDerivedFrom(null, from, to,
                    qnFromString(factory, rel.getString("activity")),
                    qnFromString(factory, rel.getString("generation")),
                    qnFromString(factory, rel.getString("usage")),
                    attrs);
            case "used" -> factory.newUsed(null, from, to, time, attrs);
            case "was_generated_by" -> factory.newWasGeneratedBy(null, from, to, time, attrs);
            case "was_associated_with" -> factory.newWasAssociatedWith(null, from, to, qnFromString(factory, rel.getString("plan")), attrs);
            case "was_attributed_to" -> factory.newWasAttributedTo(null, from, to, attrs);
            case "was_informed_by" -> factory.newWasInformedBy(null, from, to, attrs);
            case "was_started_by" -> factory.newWasStartedBy(null, from, to, null, time, attrs);
            case "was_ended_by" -> factory.newWasEndedBy(null, from, to, null, time, attrs);
            case "was_invalidated_by" -> factory.newWasInvalidatedBy(null, from, to, time, attrs);
            case "acted_on_behalf_of" -> factory.newActedOnBehalfOf(null, from, to, qnFromString(factory, rel.getString("activity")), attrs);
            case "was_influenced_by" -> factory.newWasInfluencedBy(null, from, to, attrs);
            case "alternate_of" -> factory.newAlternateOf(from, to);
            case "had_member" -> factory.newHadMember(from, to);
            default -> null;
        };
    }

    private QualifiedName qnFromString(ProvFactory factory, String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        if (value.startsWith("http://") || value.startsWith("https://")) {
            int idx = Math.max(value.lastIndexOf('#'), value.lastIndexOf('/'));
            if (idx > 0 && idx < value.length() - 1) {
                return factory.newQualifiedName(value.substring(0, idx + 1), value.substring(idx + 1), null);
            }
        }
        return factory.newQualifiedName(appProperties.getFqdn(), value, "def");
    }

    private XMLGregorianCalendar toXmlGregorian(LocalDateTime time) {
        if (time == null) {
            return null;
        }
        try {
            return DatatypeFactory.newInstance().newXMLGregorianCalendar(time.toString() + "Z");
        } catch (Exception ex) {
            return null;
        }
    }

    private record MetaNode(String identifier,
                            Set<String> labels,
                            Map<String, Object> attributes,
                            LocalDateTime startTime,
                            LocalDateTime endTime) {
        boolean isEntity() { return labels.contains("Entity"); }
        boolean isActivity() { return labels.contains("Activity"); }
        boolean isAgent() { return labels.contains("Agent"); }
    }

    private record MetaRelation(String type, String fromId, String toId, Map<String, Object> props) {
        String getString(String key) {
            Object v = props != null ? props.get(key) : null;
            return v == null ? null : v.toString();
        }
        LocalDateTime getLocalDateTime(String key) {
            Object v = props != null ? props.get(key) : null;
            return v instanceof LocalDateTime dt ? dt : null;
        }
    }
}
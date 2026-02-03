package cz.muni.fi.distributed_prov_system.utils.prov;

import cz.muni.fi.distributed_prov_system.utils.ProvConstants;
import org.openprovenance.prov.model.*;
import org.openprovenance.prov.vanilla.ProvFactory;

import java.util.*;

public final class SubgraphUtils {

    private static final String PROV_NS = "http://www.w3.org/ns/prov#";
    private static final String DCT_NS = "http://purl.org/dc/terms/";
    private static final String CPM_NS = "https://www.commonprovenancemodel.org/cpm-namespace-v1-0/";

    private SubgraphUtils() {
    }

    public static String buildSubgraphBase64(String base64Graph,
                                             String graphFormat,
                                             String outputFormat,
                                             boolean domainSpecific) {
        Document document = ProvToolboxUtils.parseDocument(base64Graph, graphFormat);
        Bundle bundle = getFirstBundle(document);
        if (bundle == null) {
            throw new IllegalArgumentException("No bundle found in PROV document.");
        }

        List<Statement> statements = bundle.getStatement();
        List<Statement> elements = new ArrayList<>();
        List<Statement> relations = new ArrayList<>();
        for (Statement statement : statements) {
            if (statement instanceof Entity || statement instanceof Activity || statement instanceof Agent) {
                elements.add(statement);
            } else {
                relations.add(statement);
            }
        }

        List<Statement> recordsBb = new ArrayList<>();
        List<Statement> recordsDs = new ArrayList<>();
        Set<QualifiedName> recordsBbIds = new HashSet<>();

        Map<QualifiedName, Statement> elementById = new HashMap<>();
        for (Statement element : elements) {
            QualifiedName id = getId(element);
            if (id != null) {
                elementById.put(id, element);
            }
        }

        List<SpecializationOf> specializations = new ArrayList<>();
        for (Statement relation : relations) {
            if (relation instanceof SpecializationOf spec) {
                specializations.add(spec);
            }
        }

        for (Statement element : elements) {
            if (isBackboneElement(element, bundle, elementById, specializations)) {
                recordsBb.add(element);
                QualifiedName id = getId(element);
                if (id != null) {
                    recordsBbIds.add(id);
                }
            } else {
                recordsDs.add(element);
            }
        }

        for (Statement relation : relations) {
            if (relationBelongsToBb(recordsBbIds, relation)) {
                recordsBb.add(relation);
            } else {
                recordsDs.add(relation);
            }
        }

        List<Statement> selected = domainSpecific ? recordsDs : recordsBb;

        ProvFactory factory = new ProvFactory();
        Document newDocument = factory.newDocument();
        newDocument.setNamespace(document.getNamespace());

        Bundle newBundle = factory.newNamedBundle(bundle.getId(), bundle.getNamespace(), selected);
        newDocument.getStatementOrBundle().add(newBundle);

        return ProvToolboxUtils.serializeDocumentToBase64(newDocument, outputFormat);
    }

    private static Bundle getFirstBundle(Document document) {
        if (document == null) {
            return null;
        }
        for (StatementOrBundle sb : document.getStatementOrBundle()) {
            if (sb instanceof Bundle b) {
                return b;
            }
        }
        return null;
    }

    private static QualifiedName getId(Statement statement) {
        if (statement instanceof Identifiable identifiable) {
            return identifiable.getId();
        }
        return null;
    }

    private static boolean isBackboneElement(Statement record,
                                             Bundle bundle,
                                             Map<QualifiedName, Statement> elementById,
                                             List<SpecializationOf> specializations) {
        if (containsNonBackboneAttribute(record)) {
            return false;
        }

        boolean hasCpmType = hasAnyCpmType(record);
        List<QualifiedName> generalElements = new ArrayList<>();
        QualifiedName recordId = getId(record);
        if (recordId != null) {
            for (SpecializationOf specialization : specializations) {
                if (recordId.equals(specialization.getSpecificEntity())) {
                    generalElements.add(specialization.getGeneralEntity());
                }
            }
        }

        if (hasCpmType && generalElements.isEmpty()) {
            return true;
        }

        if (generalElements.size() != 1) {
            return false;
        }

        Statement generalElement = elementById.get(generalElements.get(0));
        if (generalElement == null) {
            return false;
        }

        if (containsNonBackboneAttribute(generalElement)) {
            return false;
        }

        String generalType = firstProvType(generalElement);
        String recordType = firstProvType(record);

        return ProvConstants.CPM_FORWARD_CONNECTOR.equals(generalType)
                && (!hasCpmType || ProvConstants.CPM_FORWARD_CONNECTOR.equals(recordType));
    }

    private static boolean hasAnyCpmType(Statement record) {
        List<Attribute> attributes = getAttributes(record);
        if (attributes.isEmpty()) {
            return false;
        }
        for (Attribute attribute : attributes) {
            if (!isProvType(attribute)) {
                continue;
            }
            String value = attributeValueAsString(attribute);
            if (value == null) {
                continue;
            }
            if (value.equals(ProvConstants.CPM_BACKWARD_CONNECTOR)
                    || value.equals(ProvConstants.CPM_FORWARD_CONNECTOR)
                    || value.equals(ProvConstants.CPM_MAIN_ACTIVITY)
                    || value.equals(ProvConstants.CPM_RECEIVER_AGENT)
                    || value.equals(ProvConstants.CPM_SENDER_AGENT)
                    || value.equals(ProvConstants.CPM_ID)) {
                return true;
            }
        }
        return false;
    }

    private static boolean containsNonBackboneAttribute(Statement record) {
        List<Attribute> attributes = getAttributes(record);
        if (attributes.isEmpty()) {
            return false;
        }
        for (Attribute attribute : attributes) {
            QualifiedName name = attribute.getElementName();
            if (name == null) {
                continue;
            }
            String ns = name.getNamespaceURI();
            String local = name.getLocalPart();

            if (PROV_NS.equals(ns) && ("startTime".equals(local) || "endTime".equals(local))) {
                continue;
            }

            if (PROV_NS.equals(ns) && "type".equals(local)) {
                String value = attributeValueAsString(attribute);
                if (value != null && !value.startsWith(CPM_NS)) {
                    return true;
                }
                continue;
            }

            if (!CPM_NS.equals(ns)) {
                if (DCT_NS.equals(ns) && "hasPart".equals(local)) {
                    continue;
                }
                return true;
            }
        }
        return false;
    }

    private static boolean relationBelongsToBb(Set<QualifiedName> bbIds, Statement relation) {
        if (relation instanceof WasInfluencedBy rel) {
            return inBb(bbIds, rel.getInfluencee()) && inBb(bbIds, rel.getInfluencer());
        }
        if (relation instanceof WasInformedBy rel) {
            return inBb(bbIds, rel.getInformed()) && inBb(bbIds, rel.getInformant());
        }
        if (relation instanceof AlternateOf rel) {
            return inBb(bbIds, rel.getAlternate1()) && inBb(bbIds, rel.getAlternate2());
        }
        if (relation instanceof WasGeneratedBy rel) {
            return inBb(bbIds, rel.getEntity()) && inBb(bbIds, rel.getActivity());
        }
        if (relation instanceof Used rel) {
            return inBb(bbIds, rel.getActivity()) && inBb(bbIds, rel.getEntity());
        }
        if (relation instanceof WasStartedBy rel) {
            return inBb(bbIds, rel.getActivity());
        }
        if (relation instanceof WasEndedBy rel) {
            return inBb(bbIds, rel.getActivity());
        }
        if (relation instanceof WasInvalidatedBy rel) {
            return inBb(bbIds, rel.getEntity()) && inBb(bbIds, rel.getActivity());
        }
        if (relation instanceof WasDerivedFrom rel) {
            return inBb(bbIds, rel.getGeneratedEntity()) && inBb(bbIds, rel.getUsedEntity());
        }
        if (relation instanceof WasAttributedTo rel) {
            return inBb(bbIds, rel.getEntity()) && inBb(bbIds, rel.getAgent());
        }
        if (relation instanceof WasAssociatedWith rel) {
            return inBb(bbIds, rel.getActivity()) && inBb(bbIds, rel.getAgent());
        }
        if (relation instanceof ActedOnBehalfOf rel) {
            return inBb(bbIds, rel.getDelegate()) && inBb(bbIds, rel.getResponsible());
        }
        if (relation instanceof SpecializationOf rel) {
            return inBb(bbIds, rel.getSpecificEntity()) && inBb(bbIds, rel.getGeneralEntity());
        }
        if (relation instanceof MentionOf rel) {
            return inBb(bbIds, rel.getSpecificEntity()) && inBb(bbIds, rel.getGeneralEntity());
        }
        if (relation instanceof HadMember rel) {
            return inBb(bbIds, rel.getCollection()) && inBbAll(bbIds, rel.getEntity());
        }
        return false;
    }

    private static boolean inBb(Set<QualifiedName> bbIds, QualifiedName id) {
        return id != null && bbIds.contains(id);
    }

    private static boolean inBbAll(Set<QualifiedName> bbIds, List<QualifiedName> ids) {
        if (ids == null || ids.isEmpty()) {
            return false;
        }
        for (QualifiedName id : ids) {
            if (!inBb(bbIds, id)) {
                return false;
            }
        }
        return true;
    }

    private static boolean isProvType(Attribute attribute) {
        QualifiedName name = attribute.getElementName();
        return name != null && PROV_NS.equals(name.getNamespaceURI()) && "type".equals(name.getLocalPart());
    }

    private static String firstProvType(Statement record) {
        List<Attribute> attributes = getAttributes(record);
        if (attributes.isEmpty()) {
            return null;
        }
        for (Attribute attribute : attributes) {
            if (isProvType(attribute)) {
                return attributeValueAsString(attribute);
            }
        }
        return null;
    }

    private static List<Attribute> getAttributes(Statement record) {
        List<Attribute> result = new ArrayList<>();
        if (record instanceof HasType hasType && hasType.getType() != null) {
            result.addAll(hasType.getType());
        }
        if (record instanceof HasOther hasOther && hasOther.getOther() != null) {
            result.addAll(hasOther.getOther());
        }
        return result;
    }

    private static String attributeValueAsString(Attribute attribute) {
        Object value = attribute.getValue();
        if (value instanceof QualifiedName qn) {
            return qn.getNamespaceURI() + qn.getLocalPart();
        }
        return value != null ? value.toString() : null;
    }
}

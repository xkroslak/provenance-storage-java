package cz.muni.fi.distributed_prov_system.utils.prov;

import cz.muni.fi.distributed_prov_system.utils.ProvConstants;
import org.openprovenance.prov.model.*;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class ProvToolboxValidationUtils {

    private ProvToolboxValidationUtils() {
    }

    public static Set<String> getOtherAttributeNames(HasOther hasOther) {
        Set<String> names = new HashSet<>();
        if (hasOther == null || hasOther.getOther() == null) {
            return names;
        }
        for (Other other : hasOther.getOther()) {
            if (other.getElementName() != null) {
                names.add(qnameKey(other.getElementName()));
            }
        }
        return names;
    }

    public static Object getOtherAttributeValue(HasOther hasOther, String attributeKey) {
        if (hasOther == null || hasOther.getOther() == null) {
            return null;
        }
        for (Other other : hasOther.getOther()) {
            if (other.getElementName() == null) {
                continue;
            }
            if (matchesQualifiedName(other.getElementName(), attributeKey)) {
                return other.getValue();
            }
        }
        return null;
    }

    public static String qnameKey(QualifiedName qn) {
        if (qn == null) {
            return null;
        }
        if (qn.getUri() != null) {
            return qn.getUri();
        }
        return qn.toString();
    }

    public static boolean matchesQualifiedName(QualifiedName qn, String expected) {
        if (qn == null || expected == null) {
            return false;
        }
        String uri = qn.getUri() != null ? qn.getUri() : null;
        if (expected.startsWith("http")) {
            return expected.equals(uri) || expected.equals(qn.toString());
        }
        String prefixed = qn.getPrefix() + ":" + qn.getLocalPart();
        return expected.equals(prefixed) || expected.equals(qn.toString()) || expected.equals(uri);
    }

    public static List<WasGeneratedBy> filterGenerations(List<WasGeneratedBy> generations, String entityId) {
        List<WasGeneratedBy> result = new ArrayList<>();
        for (WasGeneratedBy generation : generations) {
            if (entityId.equals(qnameKey(generation.getEntity()))) {
                result.add(generation);
            }
        }
        return result;
    }

    public static List<WasGeneratedBy> filterGenerationsByActivity(List<WasGeneratedBy> generations, String activityId) {
        List<WasGeneratedBy> result = new ArrayList<>();
        for (WasGeneratedBy generation : generations) {
            if (activityId.equals(qnameKey(generation.getActivity()))) {
                result.add(generation);
            }
        }
        return result;
    }

    public static List<Used> filterUsages(List<Used> usages, String entityId) {
        List<Used> result = new ArrayList<>();
        for (Used usage : usages) {
            if (entityId.equals(qnameKey(usage.getEntity()))) {
                result.add(usage);
            }
        }
        return result;
    }

    public static List<Used> filterUsagesByActivity(List<Used> usages, String activityId) {
        List<Used> result = new ArrayList<>();
        for (Used usage : usages) {
            if (activityId.equals(qnameKey(usage.getActivity()))) {
                result.add(usage);
            }
        }
        return result;
    }

    public static List<WasDerivedFrom> filterDerivationsByGenerated(List<WasDerivedFrom> derivations, String generatedEntityId) {
        List<WasDerivedFrom> result = new ArrayList<>();
        for (WasDerivedFrom derivation : derivations) {
            if (generatedEntityId.equals(qnameKey(derivation.getGeneratedEntity()))) {
                result.add(derivation);
            }
        }
        return result;
    }

    public static List<WasDerivedFrom> filterDerivationsByUsed(List<WasDerivedFrom> derivations, String usedEntityId) {
        List<WasDerivedFrom> result = new ArrayList<>();
        for (WasDerivedFrom derivation : derivations) {
            if (usedEntityId.equals(qnameKey(derivation.getUsedEntity()))) {
                result.add(derivation);
            }
        }
        return result;
    }

    public static List<SpecializationOf> filterSpecializationsBySpecific(List<SpecializationOf> specializations, String specificEntityId) {
        List<SpecializationOf> result = new ArrayList<>();
        for (SpecializationOf specialization : specializations) {
            if (specificEntityId.equals(qnameKey(specialization.getSpecificEntity()))) {
                result.add(specialization);
            }
        }
        return result;
    }

    public static String getMetaBundleId(Activity mainActivity) {
        Object value = getOtherAttributeValue(mainActivity, ProvConstants.CPM_REFERENCED_META_BUNDLE_ID);
        if (value == null) {
            return null;
        }
        String str = value.toString();
        int idx = str.lastIndexOf("/api/v1/documents/meta/");
        if (idx >= 0) {
            String tail = str.substring(idx + "/api/v1/documents/meta/".length());
            int slash = tail.indexOf('/');
            return slash == -1 ? tail : tail.substring(0, slash);
        }
        int hash = str.lastIndexOf('/');
        return hash == -1 ? str : str.substring(hash + 1);
    }

    public static boolean checkConnectorDoesNotReferenceSelf(Entity connector, String bundleId, String metaBundleId) {
        Object bundleRef = getOtherAttributeValue(connector, ProvConstants.CPM_REFERENCED_BUNDLE_ID);
        if (bundleRef != null && bundleId != null && bundleRef.toString().contains(bundleId)) {
            return false;
        }
        Object metaRef = getOtherAttributeValue(connector, ProvConstants.CPM_REFERENCED_META_BUNDLE_ID);
        return metaRef == null || metaBundleId == null || !metaRef.toString().contains(metaBundleId);
    }
}

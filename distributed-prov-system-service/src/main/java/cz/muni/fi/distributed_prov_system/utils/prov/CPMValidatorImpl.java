package cz.muni.fi.distributed_prov_system.utils.prov;

import cz.muni.fi.distributed_prov_system.utils.ProvConstants;
import org.openprovenance.prov.model.*;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static cz.muni.fi.distributed_prov_system.utils.prov.ProvToolboxValidationUtils.*;

public class CPMValidatorImpl implements CPMValidator {

    private static final Set<String> BACKWARD_MANDATORY_ATTRIBUTES = Set.of(
            ProvConstants.CPM_REFERENCED_BUNDLE_ID,
            ProvConstants.CPM_REFERENCED_META_BUNDLE_ID,
            ProvConstants.CPM_REFERENCED_BUNDLE_HASH_VALUE,
            ProvConstants.CPM_HASH_ALG
    );

    @Override
    public boolean checkBackwardConnectorsAttributes(List<Entity> connectors) {
        for (Entity entity : connectors) {
            Set<String> attributeIds = getOtherAttributeNames(entity);
            for (String attribute : BACKWARD_MANDATORY_ATTRIBUTES) {
                if (!attributeIds.contains(attribute)) {
                    return false;
                }
            }
        }
        return true;
    }

    @Override
    public boolean checkForwardConnectorsAttributes(List<Entity> connectors) {
        for (Entity entity : connectors) {
            Set<String> attributeIds = getOtherAttributeNames(entity);
            boolean hasAny = false;
            boolean missingAny = false;
            for (String attribute : BACKWARD_MANDATORY_ATTRIBUTES) {
                if (attributeIds.contains(attribute)) {
                    hasAny = true;
                } else {
                    missingAny = true;
                }
            }
            if (hasAny && missingAny) {
                return false;
            }
        }
        return true;
    }

    @Override
    public ValidationResult checkCpmConstraints(Bundle bundle,
                                                List<Entity> forwardConnectors,
                                                List<Entity> backwardConnectors,
                                                Activity mainActivity) {
        List<WasGeneratedBy> generations = new ArrayList<>();
        List<Used> usages = new ArrayList<>();
        List<WasAttributedTo> attributions = new ArrayList<>();
        List<SpecializationOf> specializations = new ArrayList<>();
        List<WasDerivedFrom> derivations = new ArrayList<>();

        for (Statement statement : bundle.getStatement()) {
            if (statement instanceof WasGeneratedBy g) {
                generations.add(g);
            } else if (statement instanceof Used u) {
                usages.add(u);
            } else if (statement instanceof WasAttributedTo a) {
                attributions.add(a);
            } else if (statement instanceof SpecializationOf s) {
                specializations.add(s);
            } else if (statement instanceof WasDerivedFrom d) {
                derivations.add(d);
            }
        }

        Set<String> backwardIds = new HashSet<>();
        Set<String> forwardIds = new HashSet<>();
        for (Entity entity : forwardConnectors) {
            forwardIds.add(qnameKey(entity.getId()));
        }
        for (Entity entity : backwardConnectors) {
            backwardIds.add(qnameKey(entity.getId()));
        }

        String mainActivityId = qnameKey(mainActivity.getId());

        for (Entity connector : forwardConnectors) {
            String connectorId = qnameKey(connector.getId());
            List<WasGeneratedBy> connectorGenerations = filterGenerations(generations, connectorId);
            List<WasDerivedFrom> connectorDerivations = filterDerivationsByGenerated(derivations, connectorId);
            List<SpecializationOf> connectorSpecializations = filterSpecializationsBySpecific(specializations, connectorId);

            boolean derivedFromForward = false;

            if (connectorGenerations.isEmpty() && connectorDerivations.isEmpty() &&
                    getOtherAttributeValue(connector, ProvConstants.CPM_REFERENCED_BUNDLE_ID) != null) {
                if (connectorSpecializations.size() != 1 ||
                        !forwardIds.contains(qnameKey(connectorSpecializations.getFirst().getGeneralEntity()))) {
                    return new ValidationResult(false,
                            "Forward connector [" + connectorId + "] not specialized from other forward connector.");
                }
                continue;
            }

            if (connectorGenerations.size() != 1 && connectorDerivations.isEmpty()) {
                return new ValidationResult(false,
                        "Forward connector [" + connectorId + "] missing generation or derivation.");
            }

            if (connectorGenerations.size() == 1 &&
                    !mainActivityId.equals(qnameKey(connectorGenerations.getFirst().getActivity()))) {
                return new ValidationResult(false,
                        "Forward connector [" + connectorId + "] generated by non-main activity.");
            }

            for (WasDerivedFrom derivation : connectorDerivations) {
                String usedId = qnameKey(derivation.getUsedEntity());
                if (!forwardIds.contains(usedId) && !backwardIds.contains(usedId)) {
                    return new ValidationResult(false,
                            "Forward connector [" + connectorId + "] derived from non-connector.");
                }
                if (forwardIds.contains(usedId)) {
                    derivedFromForward = true;
                }
            }

            if (!derivedFromForward && connectorGenerations.size() != 1) {
                return new ValidationResult(false,
                        "Forward connector [" + connectorId + "] not generated by main activity.");
            }
        }

        for (Entity connector : backwardConnectors) {
            String connectorId = qnameKey(connector.getId());
            List<Used> connectorUsages = filterUsages(usages, connectorId);
            List<WasDerivedFrom> connectorDerivations = filterDerivationsByUsed(derivations, connectorId);

            boolean derivedFromBackward = false;

            if (connectorUsages.size() != 1 && connectorDerivations.isEmpty()) {
                return new ValidationResult(false,
                        "Backward connector [" + connectorId + "] missing usage or derivation.");
            }

            if (connectorUsages.size() == 1 &&
                    !mainActivityId.equals(qnameKey(connectorUsages.getFirst().getActivity()))) {
                return new ValidationResult(false,
                        "Backward connector [" + connectorId + "] used by non-main activity.");
            }

            for (WasDerivedFrom derivation : connectorDerivations) {
                String generatedId = qnameKey(derivation.getGeneratedEntity());
                if (!forwardIds.contains(generatedId) && !backwardIds.contains(generatedId)) {
                    return new ValidationResult(false,
                            "Backward connector [" + connectorId + "] derived with non-connector.");
                }
                if (backwardIds.contains(generatedId)) {
                    derivedFromBackward = true;
                }
            }

            if (!derivedFromBackward && connectorUsages.size() != 1) {
                return new ValidationResult(false,
                        "Backward connector [" + connectorId + "] not used by main activity.");
            }
        }

        List<Used> mainActivityUsages = filterUsagesByActivity(usages, mainActivityId);
        for (Used usage : mainActivityUsages) {
            if (!backwardIds.contains(qnameKey(usage.getEntity()))) {
                return new ValidationResult(false, "Main activity used non-backward connector.");
            }
        }

        List<WasGeneratedBy> mainActivityGenerations = filterGenerationsByActivity(generations, mainActivityId);
        for (WasGeneratedBy generation : mainActivityGenerations) {
            if (!forwardIds.contains(qnameKey(generation.getEntity()))) {
                return new ValidationResult(false, "Main activity generated non-forward connector.");
            }
        }

        // Connector reference checks
        String bundleId = qnameKey(bundle.getId());
        String metaBundleId = getMetaBundleId(mainActivity);
        for (Entity connector : forwardConnectors) {
            if (!checkConnectorDoesNotReferenceSelf(connector, bundleId, metaBundleId)) {
                return new ValidationResult(false, "Forward connector references this bundle or meta bundle.");
            }
        }
        for (Entity connector : backwardConnectors) {
            if (!checkConnectorDoesNotReferenceSelf(connector, bundleId, metaBundleId)) {
                return new ValidationResult(false, "Backward connector references this bundle or meta bundle.");
            }
        }

        return new ValidationResult(true, "ok");
    }
}

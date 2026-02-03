package cz.muni.fi.distributed_prov_system.utils.prov;

import org.openprovenance.prov.model.Activity;
import org.openprovenance.prov.model.Bundle;
import org.openprovenance.prov.model.Entity;

import java.util.List;

public interface CPMValidator {
    boolean checkBackwardConnectorsAttributes(List<Entity> connectors);

    boolean checkForwardConnectorsAttributes(List<Entity> connectors);

    ValidationResult checkCpmConstraints(Bundle bundle,
                                         List<Entity> forwardConnectors,
                                         List<Entity> backwardConnectors,
                                         Activity mainActivity);

    record ValidationResult(boolean ok, String message) {
    }
}

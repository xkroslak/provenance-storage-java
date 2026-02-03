package cz.muni.fi.distributed_prov_system.utils.prov;

import org.openprovenance.prov.model.Document;

public interface ProvDocumentValidator {
    boolean isValid(Document document);
}

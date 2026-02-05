package cz.muni.fi.distributed_prov_system.utils.prov;

import org.openprovenance.prov.model.Document;
import org.openprovenance.prov.validation.Config;
import org.openprovenance.prov.validation.Validate;
import org.openprovenance.prov.validation.report.ValidationReport;
import org.openprovenance.prov.vanilla.ProvFactory;

public class ProvDocumentValidatorImpl implements ProvDocumentValidator {

    @Override
    public boolean isValid(Document document) {
        try {
            var factory = ProvFactory.getFactory();
            Config config = Config.newYesToAllConfig(factory, new ValidationObjectMaker());
            Validate validator = new Validate(config);
            ValidationReport report = validator.validate(document);
            return report != null && isReportClean(report);
        } catch (Exception ex) {
            return false;
        }
    }

    private boolean isReportClean(ValidationReport report) {
        if (report == null) {
            return false;
        }
        if (report.getMalformedStatements() != null) {
            return false;
        }
        if (report.getSpecializationReport() != null) {
            return false;
        }
        if (report.getTypeReport() != null) {
            return false;
        }
        if (!report.getCycle().isEmpty()) {
            return false;
        }
        if (!report.getNonStrictCycle().isEmpty()) {
            return false;
        }
        if (!report.getFailedMerge().isEmpty()) {
            return false;
        }
        if (!report.getQualifiedNameMismatch().isEmpty()) {
            return false;
        }
        if (!report.getTypeOverlap().isEmpty()) {
            return false;
        }
        if (report.getValidationReport() != null) {
            for (ValidationReport sub : report.getValidationReport()) {
                if (!isReportClean(sub)) {
                    return false;
                }
            }
        }
        return true;
    }
}

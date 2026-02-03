package cz.muni.fi.trusted_party.data.records;

import cz.muni.fi.trusted_party.data.model.Certificate;
import cz.muni.fi.trusted_party.data.model.Organization;

import java.util.List;

public record OrganizationAndCertificateWithRevoked(
        Organization organization,
        Certificate activeCertificate,
        List<Certificate> revokedCertificates
) {
}

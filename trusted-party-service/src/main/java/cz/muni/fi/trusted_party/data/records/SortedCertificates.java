package cz.muni.fi.trusted_party.data.records;

import cz.muni.fi.trusted_party.data.model.Certificate;

import java.util.List;

public record SortedCertificates(
        Certificate activeCertificate,
        List<Certificate> revokedCertificates
) {}

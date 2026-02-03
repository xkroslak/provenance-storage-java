package cz.muni.fi.trusted_party.data.repository;

import cz.muni.fi.trusted_party.data.enums.CertificateType;
import cz.muni.fi.trusted_party.data.model.Certificate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CertificateRepository extends JpaRepository<Certificate, String> {

    List<Certificate> findByOrganizationOrgNameAndCertificateTypeAndIsRevoked(
            String orgName,
            CertificateType certificateType,
            boolean isRevoked);

    Certificate findFirstByOrganizationOrgNameAndCertificateTypeAndIsRevoked(
            String orgName,
            CertificateType certificateType,
            boolean isRevoked
    );

    Certificate findFirstByOrganizationOrgNameAndIsRevoked(
            String orgName,
            boolean isRevoked
    );

    Optional<Certificate> findByCertDigest(String digest);
}

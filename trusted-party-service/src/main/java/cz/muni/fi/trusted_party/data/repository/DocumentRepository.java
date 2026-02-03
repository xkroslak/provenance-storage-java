package cz.muni.fi.trusted_party.data.repository;

import cz.muni.fi.trusted_party.data.enums.DocumentType;
import cz.muni.fi.trusted_party.data.model.Document;
import cz.muni.fi.trusted_party.data.model.Organization;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DocumentRepository extends JpaRepository<Document, Long> {

    Optional<Document> findByIdentifierAndDocFormatAndOrganization(
            String identifier,
            String docFormat,
            Organization organization
    );

    Optional<Document> findByIdentifierAndDocFormatAndDocumentTypeAndOrganization(
            String identifier,
            String docFormat,
            DocumentType documentType,
            Organization organization
    );

    List<Document> findByOrganization(Organization organization);
}

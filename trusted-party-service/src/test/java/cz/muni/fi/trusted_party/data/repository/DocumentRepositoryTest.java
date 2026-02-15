package cz.muni.fi.trusted_party.data.repository;

import cz.muni.fi.trusted_party.data.enums.CertificateType;
import cz.muni.fi.trusted_party.data.enums.DocumentType;
import cz.muni.fi.trusted_party.data.model.Certificate;
import cz.muni.fi.trusted_party.data.model.Document;
import cz.muni.fi.trusted_party.data.model.Organization;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class DocumentRepositoryTest {

	@Autowired
	private DocumentRepository documentRepository;

	@Autowired
	private TestEntityManager entityManager;

	@Test
	void save_validDocument_persistsAndLoads() {
		Organization organization = saveOrganization("org-save");
		Certificate certificate = saveCertificate(organization, "cert-save");

		Document document = buildDocument("doc-save", "json", DocumentType.GRAPH, organization, certificate);

		documentRepository.save(document);

		Optional<Document> reloaded = documentRepository
				.findByIdentifierAndDocFormatAndOrganization("doc-save", "json", organization);

		assertThat(reloaded).isPresent();
		assertThat(reloaded.get().getDocumentType()).isEqualTo(DocumentType.GRAPH);
	}

	@Test
	void findByIdentifierAndDocFormatAndOrganization_existingDocument_returnsDocument() {
		Organization organization = saveOrganization("org-a");
		Certificate certificate = saveCertificate(organization, "cert-a");
		saveDocument("doc-a", "json", DocumentType.GRAPH, organization, certificate);

		Optional<Document> result = documentRepository
				.findByIdentifierAndDocFormatAndOrganization("doc-a", "json", organization);

		assertThat(result).isPresent();
		assertThat(result.get().getIdentifier()).isEqualTo("doc-a");
	}

	@Test
	void findByIdentifierAndDocFormatAndOrganization_nonExistingDocument_returnsEmpty() {
		Organization organization = saveOrganization("org-missing");

		Optional<Document> result = documentRepository
				.findByIdentifierAndDocFormatAndOrganization("doc-missing", "json", organization);

		assertThat(result).isEmpty();
	}

	@Test
	void findByIdentifierAndDocFormatAndDocumentTypeAndOrganization_existingDocument_returnsDocument() {
		Organization organization = saveOrganization("org-b");
		Certificate certificate = saveCertificate(organization, "cert-b");
		saveDocument("doc-b", "xml", DocumentType.META, organization, certificate);

		Optional<Document> result = documentRepository
				.findByIdentifierAndDocFormatAndDocumentTypeAndOrganization(
						"doc-b",
						"xml",
						DocumentType.META,
						organization);

		assertThat(result).isPresent();
		assertThat(result.get().getDocFormat()).isEqualTo("xml");
	}

	@Test
	void findByIdentifierAndDocFormatAndDocumentTypeAndOrganization_wrongType_returnsEmpty() {
		Organization organization = saveOrganization("org-c");
		Certificate certificate = saveCertificate(organization, "cert-c");
		saveDocument("doc-c", "xml", DocumentType.GRAPH, organization, certificate);

		Optional<Document> result = documentRepository
				.findByIdentifierAndDocFormatAndDocumentTypeAndOrganization(
						"doc-c",
						"xml",
						DocumentType.META,
						organization);

		assertThat(result).isEmpty();
	}

	@Test
	void findByIdentifierAndDocFormatAndDocumentTypeAndOrganization_wrongFormat_returnsEmpty() {
		Organization organization = saveOrganization("org-format");
		Certificate certificate = saveCertificate(organization, "cert-format");
		saveDocument("doc-format", "json", DocumentType.META, organization, certificate);

		Optional<Document> result = documentRepository
				.findByIdentifierAndDocFormatAndDocumentTypeAndOrganization(
						"doc-format",
						"xml",
						DocumentType.META,
						organization);

		assertThat(result).isEmpty();
	}

	@Test
	void findByIdentifierAndDocFormatAndDocumentTypeAndOrganization_wrongOrganization_returnsEmpty() {
		Organization organization = saveOrganization("org-right");
		Organization otherOrganization = saveOrganization("org-wrong");
		Certificate certificate = saveCertificate(organization, "cert-org");
		saveDocument("doc-org", "xml", DocumentType.GRAPH, organization, certificate);

		Optional<Document> result = documentRepository
				.findByIdentifierAndDocFormatAndDocumentTypeAndOrganization(
						"doc-org",
						"xml",
						DocumentType.GRAPH,
						otherOrganization);

		assertThat(result).isEmpty();
	}

	@Test
	void findByIdentifierAndDocFormatAndDocumentTypeAndOrganization_wrongIdentifier_returnsEmpty() {
		Organization organization = saveOrganization("org-id");
		Certificate certificate = saveCertificate(organization, "cert-id");
		saveDocument("doc-id", "json", DocumentType.META, organization, certificate);

		Optional<Document> result = documentRepository
				.findByIdentifierAndDocFormatAndDocumentTypeAndOrganization(
						"doc-id-wrong",
						"json",
						DocumentType.META,
						organization);

		assertThat(result).isEmpty();
	}

	@Test
	void findByOrganization_existingOrganization_returnsDocuments() {
		Organization organization = saveOrganization("org-list");
		Organization otherOrganization = saveOrganization("org-other");
		Certificate certificate = saveCertificate(organization, "cert-list");
		Certificate otherCertificate = saveCertificate(otherOrganization, "cert-other");

		saveDocument("doc-1", "json", DocumentType.GRAPH, organization, certificate);
		saveDocument("doc-2", "json", DocumentType.META, organization, certificate);
		saveDocument("doc-3", "json", DocumentType.META, otherOrganization, otherCertificate);

		List<Document> result = documentRepository.findByOrganization(organization);

		assertThat(result)
				.extracting(Document::getIdentifier)
				.containsExactlyInAnyOrder("doc-1", "doc-2");
	}

	@Test
	void findByOrganization_noDocuments_returnsEmptyList() {
		Organization organization = saveOrganization("org-empty");

		List<Document> result = documentRepository.findByOrganization(organization);

		assertThat(result).isEmpty();
	}

	@Test
	void findByOrganization_nonExistingOrganization_returnsEmptyList() {
		Organization organization = new Organization();
		organization.setOrgName("org-missing");

		List<Document> result = documentRepository.findByOrganization(organization);

		assertThat(result).isEmpty();
	}

	private Organization saveOrganization(String orgName) {
		Organization organization = new Organization();
		organization.setOrgName(orgName);
		entityManager.persist(organization);
		return organization;
	}

	private Certificate saveCertificate(Organization organization, String digest) {
		Certificate certificate = new Certificate();
		certificate.setCertDigest(digest);
		certificate.setCert("cert-body");
		certificate.setCertificateType(CertificateType.CLIENT);
		certificate.setIsRevoked(false);
		certificate.setReceived_on(LocalDateTime.now().minusDays(1));
		certificate.setOrganization(organization);
		entityManager.persist(certificate);
		return certificate;
	}

	private Document saveDocument(
			String identifier,
			String docFormat,
			DocumentType documentType,
			Organization organization,
			Certificate certificate) {
		Document document = buildDocument(identifier, docFormat, documentType, organization, certificate);
		entityManager.persist(document);
		entityManager.flush();
		return document;
	}

	private Document buildDocument(
			String identifier,
			String docFormat,
			DocumentType documentType,
			Organization organization,
			Certificate certificate) {
		Document document = new Document();
		document.setIdentifier(identifier);
		document.setDocFormat(docFormat);
		document.setCertificate(certificate);
		document.setOrganization(organization);
		document.setDocumentType(documentType);
		document.setDocumentText("{}");
		document.setCreatedOn(LocalDateTime.now());
		document.setSignature("sig-" + identifier);
		return document;
	}
}

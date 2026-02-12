package cz.muni.fi.trusted_party.data.repository;

import cz.muni.fi.trusted_party.data.enums.CertificateType;
import cz.muni.fi.trusted_party.data.enums.DocumentType;
import cz.muni.fi.trusted_party.data.enums.HashFunction;
import cz.muni.fi.trusted_party.data.model.Certificate;
import cz.muni.fi.trusted_party.data.model.Document;
import cz.muni.fi.trusted_party.data.model.Organization;
import cz.muni.fi.trusted_party.data.model.Token;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class TokenRepositoryTest {

	@Autowired
	private TokenRepository tokenRepository;

	@Autowired
	private TestEntityManager entityManager;

	@Test
	void findByDocument_existingDocument_returnsTokens() {
		Document documentA = saveDocument("doc-a");
		Document documentB = saveDocument("doc-b");

		saveToken(documentA, "hash-1");
		saveToken(documentA, "hash-2");
		saveToken(documentB, "hash-3");

		List<Token> result = tokenRepository.findByDocument(documentA);

		assertThat(result)
				.extracting(Token::getHash)
				.containsExactlyInAnyOrder("hash-1", "hash-2");
	}

	@Test
	void findByDocument_existingDocumentWithoutTokens_returnsEmptyList() {
		Document document = saveDocument("doc-empty");

		List<Token> result = tokenRepository.findByDocument(document);

		assertThat(result).isEmpty();
	}

	@Test
	void findByDocument_nonExistingDocument_returnsEmptyList() {
		Document document = new Document();
		document.setIdentifier("missing-doc");

		List<Token> result = tokenRepository.findByDocument(document);

		assertThat(result).isEmpty();
	}

	@Test
	void save_validToken_persistsAndLoads() {
		Document document = saveDocument("doc-save");

		Token token = new Token();
		token.setDocument(document);
		token.setHash("hash-save");
		token.setHashFunction(HashFunction.SHA512);
		token.setCreatedOn(LocalDateTime.now());
		token.setSignature("sig-save");

		Token saved = tokenRepository.save(token);
		entityManager.flush();
		entityManager.clear();

		Token reloaded = tokenRepository.findById(saved.getId()).orElseThrow();

		assertThat(reloaded.getHash()).isEqualTo("hash-save");
		assertThat(reloaded.getHashFunction()).isEqualTo(HashFunction.SHA512);
		assertThat(reloaded.getDocument().getIdentifier()).isEqualTo("doc-save");
	}

	private Document saveDocument(String identifier) {
		Organization organization = new Organization();
		organization.setOrgName("org-" + identifier);
		entityManager.persist(organization);

		Certificate certificate = new Certificate();
		certificate.setCertDigest("cert-" + identifier);
		certificate.setCert("cert-body");
		certificate.setCertificateType(CertificateType.CLIENT);
		certificate.setIsRevoked(false);
		certificate.setReceived_on(LocalDateTime.now().minusDays(1));
		certificate.setOrganization(organization);
		entityManager.persist(certificate);

		Document document = new Document();
		document.setIdentifier(identifier);
		document.setDocFormat("json");
		document.setCertificate(certificate);
		document.setOrganization(organization);
		document.setDocumentType(DocumentType.GRAPH);
		document.setDocumentText("{}");
		document.setCreatedOn(LocalDateTime.now());
		document.setSignature("sig-" + identifier);
		entityManager.persist(document);

		entityManager.flush();
		return document;
	}

	private Token saveToken(Document document, String hash) {
		Token token = new Token();
		token.setDocument(document);
		token.setHash(hash);
		token.setHashFunction(HashFunction.SHA256);
		token.setCreatedOn(LocalDateTime.now());
		token.setSignature("sig-" + hash);
		entityManager.persist(token);
		entityManager.flush();
		return token;
	}
}

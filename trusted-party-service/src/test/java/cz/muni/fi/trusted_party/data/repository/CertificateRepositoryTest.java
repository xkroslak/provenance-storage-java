package cz.muni.fi.trusted_party.data.repository;

import cz.muni.fi.trusted_party.data.enums.CertificateType;
import cz.muni.fi.trusted_party.data.model.Certificate;
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
class CertificateRepositoryTest {

	@Autowired
	private CertificateRepository certificateRepository;

	@Autowired
	private TestEntityManager entityManager;

	@Test
	void save_validCertificate_persistsAndLoads() {
		Organization organization = saveOrganization("org-save");
		Certificate certificate = buildCertificate("cert-save", organization, CertificateType.CLIENT, false);

		certificateRepository.save(certificate);

		Optional<Certificate> reloaded = certificateRepository.findByCertDigest("cert-save");

		assertThat(reloaded).isPresent();
		assertThat(reloaded.get().getCertificateType()).isEqualTo(CertificateType.CLIENT);
	}

	@Test
	void findByCertDigest_existingDigest_returnsCertificate() {
		Organization organization = saveOrganization("org-digest");
		saveCertificate("cert-digest", organization, CertificateType.ROOT, false);

		Optional<Certificate> result = certificateRepository.findByCertDigest("cert-digest");

		assertThat(result).isPresent();
		assertThat(result.get().getCertDigest()).isEqualTo("cert-digest");
	}

	@Test
	void findByCertDigest_nonExistingDigest_returnsEmpty() {
		Optional<Certificate> result = certificateRepository.findByCertDigest("cert-missing");

		assertThat(result).isEmpty();
	}

	@Test
	void findByOrganizationOrgNameAndCertificateTypeAndIsRevoked_existingMatches_returnsList() {
		Organization organization = saveOrganization("org-list");
		saveCertificate("cert-1", organization, CertificateType.CLIENT, false);
		saveCertificate("cert-2", organization, CertificateType.CLIENT, false);
		saveCertificate("cert-3", organization, CertificateType.CLIENT, true);
		saveCertificate("cert-4", organization, CertificateType.ROOT, false);

		List<Certificate> result = certificateRepository
				.findByOrganizationOrgNameAndCertificateTypeAndIsRevoked(
						"org-list",
						CertificateType.CLIENT,
						false);

		assertThat(result)
				.extracting(Certificate::getCertDigest)
				.containsExactlyInAnyOrder("cert-1", "cert-2");
	}

	@Test
	void findByOrganizationOrgNameAndCertificateTypeAndIsRevoked_noMatches_returnsEmptyList() {
		Organization organization = saveOrganization("org-empty");
		saveCertificate("cert-1", organization, CertificateType.CLIENT, true);

		List<Certificate> result = certificateRepository
				.findByOrganizationOrgNameAndCertificateTypeAndIsRevoked(
						"org-empty",
						CertificateType.CLIENT,
						false);

		assertThat(result).isEmpty();
	}

	@Test
	void findByOrganizationOrgNameAndCertificateTypeAndIsRevoked_nonExistingOrganization_returnsEmptyList() {
		List<Certificate> result = certificateRepository
				.findByOrganizationOrgNameAndCertificateTypeAndIsRevoked(
						"org-missing",
						CertificateType.CLIENT,
						false);

		assertThat(result).isEmpty();
	}

	@Test
	void findByOrganizationOrgNameAndCertificateTypeAndIsRevoked_revokedMatches_returnsList() {
		Organization organization = saveOrganization("org-revoked");
		saveCertificate("cert-revoked-1", organization, CertificateType.CLIENT, true);
		saveCertificate("cert-revoked-2", organization, CertificateType.CLIENT, false);

		List<Certificate> result = certificateRepository
				.findByOrganizationOrgNameAndCertificateTypeAndIsRevoked(
						"org-revoked",
						CertificateType.CLIENT,
						true);

		assertThat(result)
				.extracting(Certificate::getCertDigest)
				.containsExactly("cert-revoked-1");
	}

	@Test
	void findFirstByOrganizationOrgNameAndCertificateTypeAndIsRevoked_existingMatches_returnsOne() {
		Organization organization = saveOrganization("org-first");
		saveCertificate("cert-first-1", organization, CertificateType.INTERMEDIATE, false);
		saveCertificate("cert-first-2", organization, CertificateType.INTERMEDIATE, false);

		Certificate result = certificateRepository
				.findFirstByOrganizationOrgNameAndCertificateTypeAndIsRevoked(
						"org-first",
						CertificateType.INTERMEDIATE,
						false);

		assertThat(result).isNotNull();
		assertThat(result.getCertificateType()).isEqualTo(CertificateType.INTERMEDIATE);
	}

	@Test
	void findFirstByOrganizationOrgNameAndCertificateTypeAndIsRevoked_noMatches_returnsNull() {
		Organization organization = saveOrganization("org-none-type");
		saveCertificate("cert-none-type", organization, CertificateType.CLIENT, false);

		Certificate result = certificateRepository
				.findFirstByOrganizationOrgNameAndCertificateTypeAndIsRevoked(
						"org-none-type",
						CertificateType.ROOT,
						false);

		assertThat(result).isNull();
	}

	@Test
	void findFirstByOrganizationOrgNameAndIsRevoked_existingMatches_returnsOne() {
		Organization organization = saveOrganization("org-first-any");
		saveCertificate("cert-any-1", organization, CertificateType.CLIENT, false);
		saveCertificate("cert-any-2", organization, CertificateType.ROOT, false);

		Certificate result = certificateRepository
				.findFirstByOrganizationOrgNameAndIsRevoked("org-first-any", false);

		assertThat(result).isNotNull();
		assertThat(result.getIsRevoked()).isFalse();
	}

	@Test
	void findFirstByOrganizationOrgNameAndIsRevoked_noMatches_returnsNull() {
		Organization organization = saveOrganization("org-none");
		saveCertificate("cert-none", organization, CertificateType.ROOT, true);

		Certificate result = certificateRepository
				.findFirstByOrganizationOrgNameAndIsRevoked("org-none", false);

		assertThat(result).isNull();
	}

	@Test
	void findFirstByOrganizationOrgNameAndIsRevoked_nonExistingOrganization_returnsNull() {
		Certificate result = certificateRepository
				.findFirstByOrganizationOrgNameAndIsRevoked("org-missing", false);

		assertThat(result).isNull();
	}

	private Organization saveOrganization(String orgName) {
		Organization organization = new Organization();
		organization.setOrgName(orgName);
		entityManager.persist(organization);
		return organization;
	}

	private Certificate saveCertificate(
			String digest,
			Organization organization,
			CertificateType certificateType,
			boolean revoked) {
		Certificate certificate = buildCertificate(digest, organization, certificateType, revoked);
		entityManager.persist(certificate);
		entityManager.flush();
		return certificate;
	}

	private Certificate buildCertificate(
			String digest,
			Organization organization,
			CertificateType certificateType,
			boolean revoked) {
		Certificate certificate = new Certificate();
		certificate.setCertDigest(digest);
		certificate.setCert("cert-body");
		certificate.setCertificateType(certificateType);
		certificate.setIsRevoked(revoked);
		certificate.setReceived_on(LocalDateTime.now().minusDays(1));
		certificate.setOrganization(organization);
		return certificate;
	}
}

package cz.muni.fi.trusted_party.data.repository;

import cz.muni.fi.trusted_party.data.model.Organization;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class OrganizationRepositoryTest {

	@Autowired
	private OrganizationRepository organizationRepository;

	@Test
	void save_validOrganization_persistsAndLoads() {
		Organization saved = saveOrganization("org-save");

		Optional<Organization> reloaded = organizationRepository.findById(saved.getOrgName());

		assertThat(reloaded).isPresent();
		assertThat(reloaded.get().getOrgName()).isEqualTo("org-save");
	}

	@Test
	void findById_existingOrganization_returnsOrganization() {
		saveOrganization("org-existing");

		Optional<Organization> result = organizationRepository.findById("org-existing");

		assertThat(result).isPresent();
		assertThat(result.get().getOrgName()).isEqualTo("org-existing");
	}

	@Test
	void findById_nonExistingOrganization_returnsEmpty() {
		Optional<Organization> result = organizationRepository.findById("missing-org");

		assertThat(result).isEmpty();
	}

	@Test
	void existsById_existingOrganization_returnsTrue() {
		saveOrganization("org-present");

		boolean result = organizationRepository.existsById("org-present");

		assertThat(result).isTrue();
	}

	@Test
	void existsById_nonExistingOrganization_returnsFalse() {
		boolean result = organizationRepository.existsById("org-absent");

		assertThat(result).isFalse();
	}

	private Organization saveOrganization(String orgName) {
		Organization organization = new Organization();
		organization.setOrgName(orgName);
		return organizationRepository.save(organization);
	}
}

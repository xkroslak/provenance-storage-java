package cz.muni.fi.distributed_prov_system.data.repository;

import cz.muni.fi.distributed_prov_system.data.model.nonprovmodels.Organization;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.neo4j.DataNeo4jTest;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataNeo4jTest
class OrganizationRepositoryTest extends AbstractNeo4jRepositoryTest {

    @Autowired
    private OrganizationRepository organizationRepository;

    @Test
    void save_WhenOrganizationIsValid_ReturnsPersistedOrganization() {
        Organization organization = new Organization();
        organization.setIdentifier("org-save");
        organization.setClientCert("client-cert");

        Organization saved = organizationRepository.save(organization);

        assertThat(saved.getIdentifier()).isEqualTo("org-save");
        assertThat(saved.getClientCert()).isEqualTo("client-cert");
    }

    @Test
    void findById_WhenOrganizationExists_ReturnsOrganization() {
        Organization organization = new Organization();
        organization.setIdentifier("org-1");
        organization.setClientCert("client-cert");

        organizationRepository.save(organization);

        Optional<Organization> result = organizationRepository.findById("org-1");

        assertThat(result).isPresent();
        assertThat(result.get().getIdentifier()).isEqualTo("org-1");
        assertThat(result.get().getClientCert()).isEqualTo("client-cert");
    }

    @Test
    void findById_WhenOrganizationDoesNotExist_ReturnsEmptyOptional() {
        Optional<Organization> result = organizationRepository.findById("missing-org");

        assertThat(result).isEmpty();
    }

    @Test
    void existsById_WhenOrganizationExists_ReturnsTrue() {
        Organization organization = new Organization();
        organization.setIdentifier("org-exists");
        organization.setClientCert("client-cert");

        organizationRepository.save(organization);

        boolean exists = organizationRepository.existsById("org-exists");

        assertThat(exists).isTrue();
    }

    @Test
    void existsById_WhenOrganizationDoesNotExist_ReturnsFalse() {
        boolean exists = organizationRepository.existsById("missing-org");

        assertThat(exists).isFalse();
    }
}

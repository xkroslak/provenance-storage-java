package cz.muni.fi.distributed_prov_system.data.repository;

import cz.muni.fi.distributed_prov_system.data.model.nonprovmodels.DefaultTrustedParty;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.neo4j.DataNeo4jTest;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataNeo4jTest
class DefaultTrustedPartyRepositoryTest extends AbstractNeo4jRepositoryTest {

    @Autowired
    private DefaultTrustedPartyRepository defaultTrustedPartyRepository;

    @Test
    void save_WhenDefaultTrustedPartyIsValid_ReturnsPersistedDefaultTrustedParty() {
        DefaultTrustedParty defaultTrustedParty = new DefaultTrustedParty();
        defaultTrustedParty.setIdentifier("dtp-save");
        defaultTrustedParty.setUrl("default-tp.local");
        defaultTrustedParty.setCertificate("default-cert");
        defaultTrustedParty.setChecked(false);
        defaultTrustedParty.setValid(false);

        DefaultTrustedParty saved = defaultTrustedPartyRepository.save(defaultTrustedParty);

        assertThat(saved.getIdentifier()).isEqualTo("dtp-save");
        assertThat(saved.getUrl()).isEqualTo("default-tp.local");
    }

    @Test
    void findById_WhenDefaultTrustedPartyExists_ReturnsDefaultTrustedParty() {
        DefaultTrustedParty defaultTrustedParty = new DefaultTrustedParty();
        defaultTrustedParty.setIdentifier("dtp-1");
        defaultTrustedParty.setUrl("default-tp.local");
        defaultTrustedParty.setCertificate("default-cert");

        defaultTrustedPartyRepository.save(defaultTrustedParty);

        Optional<DefaultTrustedParty> result = defaultTrustedPartyRepository.findById("dtp-1");

        assertThat(result).isPresent();
        assertThat(result.get().getIdentifier()).isEqualTo("dtp-1");
    }

    @Test
    void findById_WhenDefaultTrustedPartyDoesNotExist_ReturnsEmptyOptional() {
        Optional<DefaultTrustedParty> result = defaultTrustedPartyRepository.findById("missing-dtp");

        assertThat(result).isEmpty();
    }

    @Test
    void existsById_WhenDefaultTrustedPartyExists_ReturnsTrue() {
        DefaultTrustedParty defaultTrustedParty = new DefaultTrustedParty();
        defaultTrustedParty.setIdentifier("dtp-exists");

        defaultTrustedPartyRepository.save(defaultTrustedParty);

        boolean exists = defaultTrustedPartyRepository.existsById("dtp-exists");

        assertThat(exists).isTrue();
    }

    @Test
    void existsById_WhenDefaultTrustedPartyDoesNotExist_ReturnsFalse() {
        boolean exists = defaultTrustedPartyRepository.existsById("missing-dtp");

        assertThat(exists).isFalse();
    }
}
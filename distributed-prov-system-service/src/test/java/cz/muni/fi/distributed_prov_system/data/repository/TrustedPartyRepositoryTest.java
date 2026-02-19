package cz.muni.fi.distributed_prov_system.data.repository;

import cz.muni.fi.distributed_prov_system.data.model.nonprovmodels.TrustedParty;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.neo4j.DataNeo4jTest;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataNeo4jTest
class TrustedPartyRepositoryTest extends AbstractNeo4jRepositoryTest {

    @Autowired
    private TrustedPartyRepository trustedPartyRepository;

    @Test
    void save_WhenTrustedPartyIsValid_ReturnsPersistedTrustedParty() {
        TrustedParty trustedParty = new TrustedParty();
        trustedParty.setIdentifier("tp-save");
        trustedParty.setUrl("tp.local");
        trustedParty.setCertificate("cert");
        trustedParty.setChecked(false);
        trustedParty.setValid(false);

        TrustedParty saved = trustedPartyRepository.save(trustedParty);

        assertThat(saved.getIdentifier()).isEqualTo("tp-save");
        assertThat(saved.getUrl()).isEqualTo("tp.local");
    }

    @Test
    void findById_WhenTrustedPartyExists_ReturnsTrustedParty() {
        TrustedParty trustedParty = new TrustedParty();
        trustedParty.setIdentifier("tp-1");
        trustedParty.setUrl("tp.local");
        trustedParty.setCertificate("cert");

        trustedPartyRepository.save(trustedParty);

        Optional<TrustedParty> result = trustedPartyRepository.findById("tp-1");

        assertThat(result).isPresent();
        assertThat(result.get().getIdentifier()).isEqualTo("tp-1");
    }

    @Test
    void findById_WhenTrustedPartyDoesNotExist_ReturnsEmptyOptional() {
        Optional<TrustedParty> result = trustedPartyRepository.findById("missing-tp");

        assertThat(result).isEmpty();
    }

    @Test
    void existsById_WhenTrustedPartyExists_ReturnsTrue() {
        TrustedParty trustedParty = new TrustedParty();
        trustedParty.setIdentifier("tp-exists");

        trustedPartyRepository.save(trustedParty);

        boolean exists = trustedPartyRepository.existsById("tp-exists");

        assertThat(exists).isTrue();
    }

    @Test
    void existsById_WhenTrustedPartyDoesNotExist_ReturnsFalse() {
        boolean exists = trustedPartyRepository.existsById("missing-tp");

        assertThat(exists).isFalse();
    }
}
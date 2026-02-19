package cz.muni.fi.distributed_prov_system.data.repository;

import cz.muni.fi.distributed_prov_system.data.model.nodes.Entity;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.neo4j.DataNeo4jTest;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataNeo4jTest
class EntityRepositoryTest extends AbstractNeo4jRepositoryTest {

    @Autowired
    private EntityRepository entityRepository;

    @Test
    void save_WhenEntityIsValid_ReturnsPersistedEntity() {
        Entity entity = new Entity();
        entity.setIdentifier("entity-save");

        Entity saved = entityRepository.save(entity);

        assertThat(saved.getIdentifier()).isEqualTo("entity-save");
    }

    @Test
    void findById_WhenEntityExists_ReturnsEntity() {
        Entity entity = new Entity();
        entity.setIdentifier("entity-1");

        entityRepository.save(entity);

        Optional<Entity> result = entityRepository.findById("entity-1");

        assertThat(result).isPresent();
        assertThat(result.get().getIdentifier()).isEqualTo("entity-1");
    }

    @Test
    void findById_WhenEntityDoesNotExist_ReturnsEmptyOptional() {
        Optional<Entity> result = entityRepository.findById("missing-entity");

        assertThat(result).isEmpty();
    }

    @Test
    void existsById_WhenEntityExists_ReturnsTrue() {
        Entity entity = new Entity();
        entity.setIdentifier("entity-exists");

        entityRepository.save(entity);

        boolean exists = entityRepository.existsById("entity-exists");

        assertThat(exists).isTrue();
    }

    @Test
    void existsById_WhenEntityDoesNotExist_ReturnsFalse() {
        boolean exists = entityRepository.existsById("missing-entity");

        assertThat(exists).isFalse();
    }
}
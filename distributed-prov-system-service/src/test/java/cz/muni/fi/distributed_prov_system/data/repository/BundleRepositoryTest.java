package cz.muni.fi.distributed_prov_system.data.repository;

import cz.muni.fi.distributed_prov_system.data.model.nodes.Bundle;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.neo4j.DataNeo4jTest;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataNeo4jTest
class BundleRepositoryTest extends AbstractNeo4jRepositoryTest {

    @Autowired
    private BundleRepository bundleRepository;

    @Test
    void save_WhenBundleIsValid_ReturnsPersistedBundle() {
        Bundle bundle = new Bundle();
        bundle.setIdentifier("bundle-save");

        Bundle saved = bundleRepository.save(bundle);

        assertThat(saved.getIdentifier()).isEqualTo("bundle-save");
    }

    @Test
    void findById_WhenBundleExists_ReturnsBundle() {
        Bundle bundle = new Bundle();
        bundle.setIdentifier("bundle-1");

        bundleRepository.save(bundle);

        Optional<Bundle> result = bundleRepository.findById("bundle-1");

        assertThat(result).isPresent();
        assertThat(result.get().getIdentifier()).isEqualTo("bundle-1");
    }

    @Test
    void findById_WhenBundleDoesNotExist_ReturnsEmptyOptional() {
        Optional<Bundle> result = bundleRepository.findById("missing-bundle");

        assertThat(result).isEmpty();
    }

    @Test
    void existsById_WhenBundleExists_ReturnsTrue() {
        Bundle bundle = new Bundle();
        bundle.setIdentifier("bundle-exists");

        bundleRepository.save(bundle);

        boolean exists = bundleRepository.existsById("bundle-exists");

        assertThat(exists).isTrue();
    }

    @Test
    void existsById_WhenBundleDoesNotExist_ReturnsFalse() {
        boolean exists = bundleRepository.existsById("missing-bundle");

        assertThat(exists).isFalse();
    }
}
package cz.muni.fi.distributed_prov_system.data.repository;

import cz.muni.fi.distributed_prov_system.data.model.nodes.Activity;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.neo4j.DataNeo4jTest;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataNeo4jTest
class ActivityRepositoryTest extends AbstractNeo4jRepositoryTest {

    @Autowired
    private ActivityRepository activityRepository;

    @Test
    void save_WhenActivityIsValid_ReturnsPersistedActivity() {
        Activity activity = new Activity();
        activity.setIdentifier("activity-save");

        Activity saved = activityRepository.save(activity);

        assertThat(saved.getIdentifier()).isEqualTo("activity-save");
    }

    @Test
    void findById_WhenActivityExists_ReturnsActivity() {
        Activity activity = new Activity();
        activity.setIdentifier("activity-1");

        activityRepository.save(activity);

        Optional<Activity> result = activityRepository.findById("activity-1");

        assertThat(result).isPresent();
        assertThat(result.get().getIdentifier()).isEqualTo("activity-1");
    }

    @Test
    void findById_WhenActivityDoesNotExist_ReturnsEmptyOptional() {
        Optional<Activity> result = activityRepository.findById("missing-activity");

        assertThat(result).isEmpty();
    }

    @Test
    void existsById_WhenActivityExists_ReturnsTrue() {
        Activity activity = new Activity();
        activity.setIdentifier("activity-exists");

        activityRepository.save(activity);

        boolean exists = activityRepository.existsById("activity-exists");

        assertThat(exists).isTrue();
    }

    @Test
    void existsById_WhenActivityDoesNotExist_ReturnsFalse() {
        boolean exists = activityRepository.existsById("missing-activity");

        assertThat(exists).isFalse();
    }
}
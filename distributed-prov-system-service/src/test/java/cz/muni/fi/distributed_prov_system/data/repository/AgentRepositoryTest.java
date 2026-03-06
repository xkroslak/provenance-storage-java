package cz.muni.fi.distributed_prov_system.data.repository;

import cz.muni.fi.distributed_prov_system.data.model.nodes.Agent;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.neo4j.DataNeo4jTest;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataNeo4jTest
class AgentRepositoryTest extends AbstractNeo4jRepositoryTest {

    @Autowired
    private AgentRepository agentRepository;

    @Test
    void save_WhenAgentIsValid_ReturnsPersistedAgent() {
        Agent agent = new Agent();
        agent.setIdentifier("agent-save");

        Agent saved = agentRepository.save(agent);

        assertThat(saved.getIdentifier()).isEqualTo("agent-save");
    }

    @Test
    void findById_WhenAgentExists_ReturnsAgent() {
        Agent agent = new Agent();
        agent.setIdentifier("agent-1");

        agentRepository.save(agent);

        Optional<Agent> result = agentRepository.findById("agent-1");

        assertThat(result).isPresent();
        assertThat(result.get().getIdentifier()).isEqualTo("agent-1");
    }

    @Test
    void findById_WhenAgentDoesNotExist_ReturnsEmptyOptional() {
        Optional<Agent> result = agentRepository.findById("missing-agent");

        assertThat(result).isEmpty();
    }

    @Test
    void existsById_WhenAgentExists_ReturnsTrue() {
        Agent agent = new Agent();
        agent.setIdentifier("agent-exists");

        agentRepository.save(agent);

        boolean exists = agentRepository.existsById("agent-exists");

        assertThat(exists).isTrue();
    }

    @Test
    void existsById_WhenAgentDoesNotExist_ReturnsFalse() {
        boolean exists = agentRepository.existsById("missing-agent");

        assertThat(exists).isFalse();
    }
}
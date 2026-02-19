package cz.muni.fi.distributed_prov_system.data.repository;

import org.junit.jupiter.api.BeforeEach;
import org.neo4j.harness.Neo4j;
import org.neo4j.harness.Neo4jBuilders;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.neo4j.core.Neo4jClient;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

abstract class AbstractNeo4jRepositoryTest {

        private static final Neo4j NEO4J = Neo4jBuilders.newInProcessBuilder()
            .withDisabledServer()
            .build();

    @DynamicPropertySource
    static void neo4jProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.neo4j.uri", () -> NEO4J.boltURI().toString());
    }

    @Autowired
    private Neo4jClient neo4jClient;

    @BeforeEach
    void cleanDb() {
        neo4jClient.query("MATCH (n) DETACH DELETE n").run();
    }
}

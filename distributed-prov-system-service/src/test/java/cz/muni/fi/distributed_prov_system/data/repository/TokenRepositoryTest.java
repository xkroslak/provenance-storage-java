package cz.muni.fi.distributed_prov_system.data.repository;

import cz.muni.fi.distributed_prov_system.data.model.nonprovmodels.Token;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.neo4j.DataNeo4jTest;
import org.springframework.data.neo4j.core.Neo4jClient;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataNeo4jTest
class TokenRepositoryTest extends AbstractNeo4jRepositoryTest {

    @Autowired
    private TokenRepository tokenRepository;

    @Autowired
    private Neo4jClient neo4jClient;

    @Test
    void save_WhenTokenIsValid_ReturnsPersistedToken() {
        Token token = new Token();
        token.setId(100L);
        token.setSignature("sig-save");
        token.setTokenTimestamp(123456L);

        Token saved = tokenRepository.save(token);
        Optional<Token> loaded = tokenRepository.findById(100L);

        assertThat(saved.getId()).isEqualTo(100L);
        assertThat(saved.getSignature()).isEqualTo("sig-save");
        assertThat(saved.getTokenTimestamp()).isEqualTo(123456L);
        assertThat(loaded).isPresent();
    }

    @Test
    void findLatestByDocumentIdentifier_WhenTokensExist_ReturnsNewestToken() {
        createTokenFixture();

        Optional<Token> result = tokenRepository.findLatestByDocumentIdentifier("doc-1");

        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(3L);
        assertThat(result.get().getTokenTimestamp()).isEqualTo(300L);
    }

    @Test
    void findLatestByDocumentIdentifierAndTpId_WhenTokensExistForTrustedParty_ReturnsNewestTokenForTrustedParty() {
        createTokenFixture();

        Optional<Token> result = tokenRepository.findLatestByDocumentIdentifierAndTpId("doc-1", "tp-1");

        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(2L);
        assertThat(result.get().getTokenTimestamp()).isEqualTo(200L);
    }

    @Test
    void findLatestByDocumentIdentifierAndDefaultTp_WhenDefaultTrustedPartyTokenExists_ReturnsNewestDefaultTrustedPartyToken() {
        createTokenFixture();

        Optional<Token> result = tokenRepository.findLatestByDocumentIdentifierAndDefaultTp("doc-1");

        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(3L);
        assertThat(result.get().getTokenTimestamp()).isEqualTo(300L);
    }

    @Test
    void findLatestByDocumentIdentifierAndTpId_WhenTrustedPartyDoesNotExist_ReturnsEmptyOptional() {
        createTokenFixture();

        Optional<Token> result = tokenRepository.findLatestByDocumentIdentifierAndTpId("doc-1", "tp-missing");

        assertThat(result).isEmpty();
    }

    @Test
    void findLatestByDocumentIdentifierAndDefaultTp_WhenDefaultTrustedPartyTokenDoesNotExist_ReturnsEmptyOptional() {
        createTokenFixtureWithoutDefaultTrustedPartyToken();

        Optional<Token> result = tokenRepository.findLatestByDocumentIdentifierAndDefaultTp("doc-1");

        assertThat(result).isEmpty();
    }

    @Test
    void findLatestByDocumentIdentifier_WhenNoTokensExist_ReturnsEmptyOptional() {
        neo4jClient.query("CREATE (:Document {identifier: 'doc-empty', format: 'json', graph: '{}'})").run();

        Optional<Token> result = tokenRepository.findLatestByDocumentIdentifier("doc-empty");

        assertThat(result).isEmpty();
    }

    private void createTokenFixture() {
        String cypher = """
            CREATE (d:Document {identifier: 'doc-1', format: 'json', graph: '{}'})
            CREATE (tp:TrustedParty {identifier: 'tp-1'})
            CREATE (dtp:DefaultTrustedParty:TrustedParty {identifier: 'tp-default'})
            CREATE (t1:Token {id: 1, token_timestamp: 100, signature: 'sig-1'})
            CREATE (t2:Token {id: 2, token_timestamp: 200, signature: 'sig-2'})
            CREATE (t3:Token {id: 3, token_timestamp: 300, signature: 'sig-3'})
            CREATE (t1)-[:belongs_to]->(d)
            CREATE (t2)-[:belongs_to]->(d)
            CREATE (t3)-[:belongs_to]->(d)
            CREATE (t1)-[:was_issued_by]->(tp)
            CREATE (t2)-[:was_issued_by]->(tp)
            CREATE (t3)-[:was_issued_by]->(dtp)
            """;

        neo4jClient.query(cypher).run();
    }

    private void createTokenFixtureWithoutDefaultTrustedPartyToken() {
        String cypher = """
            CREATE (d:Document {identifier: 'doc-1', format: 'json', graph: '{}'})
            CREATE (tp:TrustedParty {identifier: 'tp-1'})
            CREATE (t1:Token {id: 1, token_timestamp: 100, signature: 'sig-1'})
            CREATE (t2:Token {id: 2, token_timestamp: 200, signature: 'sig-2'})
            CREATE (t1)-[:belongs_to]->(d)
            CREATE (t2)-[:belongs_to]->(d)
            CREATE (t1)-[:was_issued_by]->(tp)
            CREATE (t2)-[:was_issued_by]->(tp)
            """;

        neo4jClient.query(cypher).run();
    }
}

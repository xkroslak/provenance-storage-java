package cz.muni.fi.distributed_prov_system.data.repository;

import cz.muni.fi.distributed_prov_system.data.model.nonprovmodels.Token;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TokenRepository extends Neo4jRepository<Token, Long> {

	@Query("MATCH (t:Token)-[:belongs_to]->(d:Document {identifier: $identifier}) " +
		   "RETURN t ORDER BY t.token_timestamp DESC LIMIT 1")
	Optional<Token> findLatestByDocumentIdentifier(String identifier);

	    @Query("MATCH (t:Token)-[:belongs_to]->(d:Document {identifier: $identifier}) " +
		    "MATCH (tp:TrustedParty {identifier: $tpId})<-[:was_issued_by]-(t) " +
		    "RETURN t ORDER BY t.token_timestamp DESC LIMIT 1")
	    Optional<Token> findLatestByDocumentIdentifierAndTpId(String identifier, String tpId);

	    @Query("MATCH (t:Token)-[:belongs_to]->(d:Document {identifier: $identifier}) " +
		    "MATCH (tp:DefaultTrustedParty)<-[:was_issued_by]-(t) " +
		    "RETURN t ORDER BY t.token_timestamp DESC LIMIT 1")
	    Optional<Token> findLatestByDocumentIdentifierAndDefaultTp(String identifier);
}

package cz.muni.fi.distributed_prov_system.data.repository;

import cz.muni.fi.distributed_prov_system.data.model.nonprovmodels.ConnectorTable;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.stereotype.Repository;

//TODO: Do I need this?

@Repository
public interface ConnectorTableRepository extends Neo4jRepository<ConnectorTable, Long> {
}

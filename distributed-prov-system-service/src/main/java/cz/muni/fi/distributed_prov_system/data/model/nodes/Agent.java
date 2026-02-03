package cz.muni.fi.distributed_prov_system.data.model.nodes;

import cz.muni.fi.distributed_prov_system.data.model.relationships.ActedOnBehalfOf;
import org.springframework.data.neo4j.core.schema.*;
import java.util.List;

@Node
public class Agent extends BaseProv {

    @Relationship(type = "acted_on_behalf_of")
    private List<ActedOnBehalfOf> actedOnBehalfOf;

    public List<ActedOnBehalfOf> getActedOnBehalfOf() {
        return actedOnBehalfOf;
    }

    public void setActedOnBehalfOf(List<ActedOnBehalfOf> actedOnBehalfOf) {
        this.actedOnBehalfOf = actedOnBehalfOf;
    }
}

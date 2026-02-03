package cz.muni.fi.distributed_prov_system.data.model.relationships;

import cz.muni.fi.distributed_prov_system.data.model.nodes.Agent;
import org.springframework.data.neo4j.core.schema.*;

@RelationshipProperties
public class ActedOnBehalfOf extends BaseProvRel {
    @TargetNode
    private Agent agent;

    @Property
    private String activity;

    public Agent getAgent() {
        return agent;
    }

    public void setAgent(Agent agent) {
        this.agent = agent;
    }

    public String getActivity() {
        return activity;
    }

    public void setActivity(String activity) {
        this.activity = activity;
    }
}

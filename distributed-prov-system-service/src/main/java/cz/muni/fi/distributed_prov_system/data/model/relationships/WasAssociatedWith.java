package cz.muni.fi.distributed_prov_system.data.model.relationships;

import cz.muni.fi.distributed_prov_system.data.model.nodes.Agent;
import org.springframework.data.neo4j.core.schema.*;

@RelationshipProperties
public class WasAssociatedWith extends BaseProvRel {
    @TargetNode
    private Agent agent;

    @Property
    private String plan;

    public Agent getAgent() {
        return agent;
    }

    public void setAgent(Agent agent) {
        this.agent = agent;
    }

    public String getPlan() {
        return plan;
    }

    public void setPlan(String plan) {
        this.plan = plan;
    }
}

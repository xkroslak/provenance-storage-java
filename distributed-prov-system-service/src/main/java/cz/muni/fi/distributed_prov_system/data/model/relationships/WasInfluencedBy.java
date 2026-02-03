package cz.muni.fi.distributed_prov_system.data.model.relationships;

import cz.muni.fi.distributed_prov_system.data.model.nodes.BaseProv;
import org.springframework.data.neo4j.core.schema.RelationshipProperties;
import org.springframework.data.neo4j.core.schema.TargetNode;

@RelationshipProperties
public class WasInfluencedBy extends BaseProvRel {
    @TargetNode
    private BaseProv target;

    public BaseProv getTarget() {
        return target;
    }

    public void setTarget(BaseProv target) {
        this.target = target;
    }
}

package cz.muni.fi.distributed_prov_system.data.model.relationships;

import cz.muni.fi.distributed_prov_system.data.model.nodes.Entity;
import org.springframework.data.neo4j.core.schema.*;

@RelationshipProperties
public class WasDerivedFrom extends BaseProvRel {
    @TargetNode
    private Entity entity;

    @Property
    private String activity;

    @Property
    private String generation;

    @Property
    private String usage;

    public Entity getEntity() {
        return entity;
    }

    public void setEntity(Entity entity) {
        this.entity = entity;
    }

    public String getActivity() {
        return activity;
    }

    public void setActivity(String activity) {
        this.activity = activity;
    }

    public String getGeneration() {
        return generation;
    }

    public void setGeneration(String generation) {
        this.generation = generation;
    }

    public String getUsage() {
        return usage;
    }

    public void setUsage(String usage) {
        this.usage = usage;
    }
}

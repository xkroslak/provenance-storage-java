package cz.muni.fi.distributed_prov_system.data.model.relationships;

import cz.muni.fi.distributed_prov_system.data.model.nodes.Entity;
import org.springframework.data.neo4j.core.schema.*;

import java.time.LocalDateTime;

@RelationshipProperties
public class WasRevisionOf extends BaseProvRel {
    @TargetNode
    private Entity entity;

    @Property
    private LocalDateTime time;

    public Entity getEntity() {
        return entity;
    }

    public void setEntity(Entity entity) {
        this.entity = entity;
    }

    public LocalDateTime getTime() {
        return time;
    }

    public void setTime(LocalDateTime time) {
        this.time = time;
    }
}
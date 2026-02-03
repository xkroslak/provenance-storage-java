package cz.muni.fi.distributed_prov_system.data.model.relationships;

import cz.muni.fi.distributed_prov_system.data.model.nodes.Activity;
import org.springframework.data.neo4j.core.schema.*;

import java.time.LocalDateTime;

@RelationshipProperties
public class WasGeneratedBy extends BaseProvRel {
    @TargetNode
    private Activity activity;

    @Property
    private LocalDateTime time;

    public Activity getActivity() {
        return activity;
    }

    public void setActivity(Activity activity) {
        this.activity = activity;
    }

    public LocalDateTime getTime() {
        return time;
    }

    public void setTime(LocalDateTime time) {
        this.time = time;
    }
}
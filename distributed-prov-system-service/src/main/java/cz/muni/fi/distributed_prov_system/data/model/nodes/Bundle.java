package cz.muni.fi.distributed_prov_system.data.model.nodes;

import org.springframework.data.neo4j.core.schema.*;

import java.util.List;

@Node
public class Bundle extends BaseProv {

    @Relationship(type = "contains", direction = Relationship.Direction.OUTGOING)
    private List<BaseProv> contains;

    public List<BaseProv> getContainsProvs() {
        return contains;
    }

    public void setContainsProvs(List<BaseProv> contains) {
        this.contains = contains;
    }
}

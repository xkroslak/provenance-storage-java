package cz.muni.fi.distributed_prov_system.data.model.nodes;

import cz.muni.fi.distributed_prov_system.data.model.relationships.WasInfluencedBy;
import org.springframework.data.neo4j.core.schema.*;

import java.util.Map;
import java.util.List;

@Node
public class BaseProv {
    @Id
    private String identifier;

    @CompositeProperty(prefix = "attr_")
    private Map<String, Object> attributes;

    @Relationship(type = "contains", direction = Relationship.Direction.INCOMING)
    private List<Bundle> contains;

    @Relationship(type = "was_influenced_by")
    private List<WasInfluencedBy> wasInfluencedBy;

    public String getIdentifier() {
        return identifier;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    public List<WasInfluencedBy> getWasInfluencedBy() {
        return wasInfluencedBy;
    }

    public void setWasInfluencedBy(List<WasInfluencedBy> wasInfluencedBy) {
        this.wasInfluencedBy = wasInfluencedBy;
    }

    public List<Bundle> getContains() {
        return contains;
    }

    public void setContains(List<Bundle> contains) {
        this.contains = contains;
    }

    public Map<String, Object> getAttributes() {
        return attributes;
    }

    public void setAttributes(Map<String, Object> attributes) {
        this.attributes = attributes;
    }
}

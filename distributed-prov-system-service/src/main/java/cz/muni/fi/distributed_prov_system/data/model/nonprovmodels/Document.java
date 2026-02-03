package cz.muni.fi.distributed_prov_system.data.model.nonprovmodels;

import org.springframework.data.neo4j.core.schema.*;

@Node
public class Document {
    @Id
    private String identifier;

    @Property
    private String graph;

    @Property
    private String format;

    @Relationship(type = "belongs_to", direction = Relationship.Direction.INCOMING)
    private Token belongsTo;

    public String getIdentifier() {
        return identifier;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    public String getGraph() {
        return graph;
    }

    public void setGraph(String graph) {
        this.graph = graph;
    }

    public String getFormat() {
        return format;
    }

    public void setFormat(String format) {
        this.format = format;
    }

    public Token getBelongsTo() {
        return belongsTo;
    }

    public void setBelongsTo(Token belongsTo) {
        this.belongsTo = belongsTo;
    }
}

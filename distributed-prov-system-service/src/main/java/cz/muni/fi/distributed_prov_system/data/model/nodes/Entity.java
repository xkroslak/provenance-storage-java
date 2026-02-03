package cz.muni.fi.distributed_prov_system.data.model.nodes;

import cz.muni.fi.distributed_prov_system.data.model.relationships.*;
import org.springframework.data.neo4j.core.schema.*;

import java.util.List;

@Node
public class Entity extends BaseProv {

    @Relationship(type = "was_generated_by")
    private List<WasGeneratedBy> wasGeneratedBy;

    @Relationship(type = "was_generated_by")
    private List<WasGeneratedBy> wasGeneratedByFake;

    @Relationship(type = "was_derived_from")
    private List<WasDerivedFrom> wasDerivedFrom;

    @Relationship(type = "was_invalidated_by")
    private List<WasInvalidatedBy> wasInvalidatedBy;

    @Relationship(type = "was_invalidated_by")
    private List<WasInvalidatedBy> wasInvalidatedByFake;

    @Relationship(type = "was_revision_of")
    private List<WasRevisionOf> wasRevisionOf;

    @Relationship(type = "was_attributed_to")
    private List<WasAttributedTo> wasAttributedTo;

    @Relationship(type = "specialization_of")
    private List<Entity> specializationOf;

    @Relationship(type = "alternate_of")
    private List<Entity> alternateOf;

    @Relationship(type = "had_member")
    private List<Entity> hadMember;

    @Relationship(type = "used", direction = Relationship.Direction.INCOMING)
    private List<Used> used;

    public List<WasGeneratedBy> getWasGeneratedBy() {
        return wasGeneratedBy;
    }

    public void setWasGeneratedBy(List<WasGeneratedBy> wasGeneratedBy) {
        this.wasGeneratedBy = wasGeneratedBy;
    }

    public List<Used> getUsed() {
        return used;
    }

    public void setUsed(List<Used> used) {
        this.used = used;
    }

    public List<Entity> getHadMember() {
        return hadMember;
    }

    public void setHadMember(List<Entity> hadMember) {
        this.hadMember = hadMember;
    }

    public List<Entity> getAlternateOf() {
        return alternateOf;
    }

    public void setAlternateOf(List<Entity> alternateOf) {
        this.alternateOf = alternateOf;
    }

    public List<Entity> getSpecializationOf() {
        return specializationOf;
    }

    public void setSpecializationOf(List<Entity> specializationOf) {
        this.specializationOf = specializationOf;
    }

    public List<WasAttributedTo> getWasAttributedTo() {
        return wasAttributedTo;
    }

    public void setWasAttributedTo(List<WasAttributedTo> wasAttributedTo) {
        this.wasAttributedTo = wasAttributedTo;
    }

    public List<WasRevisionOf> getWasRevisionOf() {
        return wasRevisionOf;
    }

    public void setWasRevisionOf(List<WasRevisionOf> wasRevisionOf) {
        this.wasRevisionOf = wasRevisionOf;
    }

    public List<WasInvalidatedBy> getWasInvalidatedByFake() {
        return wasInvalidatedByFake;
    }

    public void setWasInvalidatedByFake(List<WasInvalidatedBy> wasInvalidatedByFake) {
        this.wasInvalidatedByFake = wasInvalidatedByFake;
    }

    public List<WasInvalidatedBy> getWasInvalidatedBy() {
        return wasInvalidatedBy;
    }

    public void setWasInvalidatedBy(List<WasInvalidatedBy> wasInvalidatedBy) {
        this.wasInvalidatedBy = wasInvalidatedBy;
    }

    public List<WasGeneratedBy> getWasGeneratedByFake() {
        return wasGeneratedByFake;
    }

    public void setWasGeneratedByFake(List<WasGeneratedBy> wasGeneratedByFake) {
        this.wasGeneratedByFake = wasGeneratedByFake;
    }

    public List<WasDerivedFrom> getWasDerivedFrom() {
        return wasDerivedFrom;
    }

    public void setWasDerivedFrom(List<WasDerivedFrom> wasDerivedFrom) {
        this.wasDerivedFrom = wasDerivedFrom;
    }
}

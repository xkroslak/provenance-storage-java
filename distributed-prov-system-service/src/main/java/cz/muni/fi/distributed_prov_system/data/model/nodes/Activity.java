package cz.muni.fi.distributed_prov_system.data.model.nodes;

import cz.muni.fi.distributed_prov_system.data.model.relationships.*;
import org.springframework.data.neo4j.core.schema.*;
import java.time.LocalDateTime;
import java.util.List;

@Node
public class Activity extends BaseProv {

    @Property
    private LocalDateTime startTime;

    @Property
    private LocalDateTime endTime;

    @Relationship(type = "used")
    private List<Used> used;

    @Relationship(type = "used")
    private List<Used> usedFake;

    @Relationship(type = "was_informed_by")
    private List<WasInformedBy> wasInformedBy;

    @Relationship(type = "was_associated_with")
    private List<WasAssociatedWith> wasAssociatedWith;

    @Relationship(type = "was_associated_with")
    private List<WasAssociatedWith> wasAssociatedWithFake;

    @Relationship(type = "was_started_by")
    private List<WasStartedBy> wasStartedBy;

    @Relationship(type = "was_started_by")
    private List<WasStartedBy> wasStartedByFake;

    @Relationship(type = "was_ended_by")
    private List<WasEndedBy> wasEndedBy;

    @Relationship(type = "was_ended_by")
    private List<WasEndedBy> wasEndedByFake;

    @Relationship(type = "was_generated_by", direction = Relationship.Direction.INCOMING)
    private List<WasGeneratedBy> wasGeneratedBy;

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }

    public List<Used> getUsed() {
        return used;
    }

    public void setUsed(List<Used> used) {
        this.used = used;
    }

    public List<WasGeneratedBy> getWasGeneratedBy() {
        return wasGeneratedBy;
    }

    public void setWasGeneratedBy(List<WasGeneratedBy> wasGeneratedBy) {
        this.wasGeneratedBy = wasGeneratedBy;
    }

    public List<WasEndedBy> getWasEndedByFake() {
        return wasEndedByFake;
    }

    public void setWasEndedByFake(List<WasEndedBy> wasEndedByFake) {
        this.wasEndedByFake = wasEndedByFake;
    }

    public List<WasEndedBy> getWasEndedBy() {
        return wasEndedBy;
    }

    public void setWasEndedBy(List<WasEndedBy> wasEndedBy) {
        this.wasEndedBy = wasEndedBy;
    }

    public List<WasStartedBy> getWasStartedByFake() {
        return wasStartedByFake;
    }

    public void setWasStartedByFake(List<WasStartedBy> wasStartedByFake) {
        this.wasStartedByFake = wasStartedByFake;
    }

    public List<WasStartedBy> getWasStartedBy() {
        return wasStartedBy;
    }

    public void setWasStartedBy(List<WasStartedBy> wasStartedBy) {
        this.wasStartedBy = wasStartedBy;
    }

    public List<WasAssociatedWith> getWasAssociatedWithFake() {
        return wasAssociatedWithFake;
    }

    public void setWasAssociatedWithFake(List<WasAssociatedWith> wasAssociatedWithFake) {
        this.wasAssociatedWithFake = wasAssociatedWithFake;
    }

    public List<WasAssociatedWith> getWasAssociatedWith() {
        return wasAssociatedWith;
    }

    public void setWasAssociatedWith(List<WasAssociatedWith> wasAssociatedWith) {
        this.wasAssociatedWith = wasAssociatedWith;
    }

    public List<Used> getUsedFake() {
        return usedFake;
    }

    public void setUsedFake(List<Used> usedFake) {
        this.usedFake = usedFake;
    }

    public List<WasInformedBy> getWasInformedBy() {
        return wasInformedBy;
    }

    public void setWasInformedBy(List<WasInformedBy> wasInformedBy) {
        this.wasInformedBy = wasInformedBy;
    }
}

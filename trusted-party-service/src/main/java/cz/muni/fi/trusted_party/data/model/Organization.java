package cz.muni.fi.trusted_party.data.model;

import jakarta.persistence.*;

import java.util.Objects;

@Entity
public class Organization {

    @Id
    @Column(length = 40)
    private String orgName;

    public String getOrgName() {
        return orgName;
    }

    public void setOrgName(String orgName) {
        this.orgName = orgName;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Organization that)) return false;
        return Objects.equals(orgName, that.orgName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(orgName);
    }

    @Override
    public String toString() {
        return "Organization{" +
                "name=" + orgName + '}';
    }
}

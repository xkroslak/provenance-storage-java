package cz.muni.fi.distributed_prov_system.data.model.nonprovmodels;

import org.springframework.data.neo4j.core.schema.*;

import java.util.List;

@Node
public class TrustedParty {
    @Id
    private String identifier;

    @Property
    private String certificate;

    @Property
    private String url;

    @Property
    private boolean checked;

    @Property
    private boolean valid;

    @Relationship(type = "trusts", direction = Relationship.Direction.INCOMING)
    private List<Organization> trusts;

    @Relationship(type = "was_issued_by", direction = Relationship.Direction.INCOMING)
    private List<Token> wasIssuedBy;

    public String getIdentifier() {
        return identifier;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    public String getCertificate() {
        return certificate;
    }

    public void setCertificate(String certificate) {
        this.certificate = certificate;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public boolean isChecked() {
        return checked;
    }

    public void setChecked(boolean checked) {
        this.checked = checked;
    }

    public boolean isValid() {
        return valid;
    }

    public void setValid(boolean valid) {
        this.valid = valid;
    }

    public List<Organization> getTrusts() {
        return trusts;
    }

    public void setTrusts(List<Organization> trusts) {
        this.trusts = trusts;
    }

    public List<Token> getWasIssuedBy() {
        return wasIssuedBy;
    }

    public void setWasIssuedBy(List<Token> wasIssuedBy) {
        this.wasIssuedBy = wasIssuedBy;
    }
}

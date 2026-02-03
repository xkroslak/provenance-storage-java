package cz.muni.fi.distributed_prov_system.data.model.nonprovmodels;

import org.springframework.data.neo4j.core.schema.*;

import java.util.List;

@Node
public class Organization {
    @Id
    private String identifier;

    @Property(name = "client_cert")
    private String clientCert;

    @Property(name = "intermediate_certs")
    private List<String> intermediateCerts;

    @Relationship(type = "trusts")
    private List<TrustedParty> trusts;

    public String getIdentifier() {
        return identifier;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    public String getClientCert() {
        return clientCert;
    }

    public void setClientCert(String clientCert) {
        this.clientCert = clientCert;
    }

    public List<String> getIntermediateCerts() {
        return intermediateCerts;
    }

    public void setIntermediateCerts(List<String> intermediateCerts) {
        this.intermediateCerts = intermediateCerts;
    }

    public List<TrustedParty> getTrusts() {
        return trusts;
    }

    public void setTrusts(List<TrustedParty> trusts) {
        this.trusts = trusts;
    }
}

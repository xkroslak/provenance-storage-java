package cz.muni.fi.distributed_prov_system.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app")
public class AppProperties {
    private String id;
    private String fqdn;
    private String tpFqdn;
    private boolean disableTrustedParty;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getFqdn() {
        return fqdn;
    }

    public void setFqdn(String fqdn) {
        this.fqdn = fqdn;
    }

    public String getTpFqdn() {
        return tpFqdn;
    }

    public void setTpFqdn(String tpFqdn) {
        this.tpFqdn = tpFqdn;
    }

    public boolean isDisableTrustedParty() {
        return disableTrustedParty;
    }

    public void setDisableTrustedParty(boolean disableTrustedParty) {
        this.disableTrustedParty = disableTrustedParty;
    }
}
package cz.muni.fi.trusted_party.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

@Component
@ConfigurationProperties(prefix = "app")
public class AppProperties {
    private String id;
    private String fqdn;
    private String trustedCertsDirPath;
    private String publicCertPath;
    private String privateKeyPath;
    private String certificate; // If you want to expose the PEM as a string

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getFqdn() { return fqdn; }
    public void setFqdn(String fqdn) { this.fqdn = fqdn; }

    public String getTrustedCertsDirPath() { return trustedCertsDirPath; }
    public void setTrustedCertsDirPath(String trustedCertsDirPath) { this.trustedCertsDirPath = trustedCertsDirPath; }

    public String getPublicCertPath() { return publicCertPath; }
    public void setPublicCertPath(String publicCertPath) { this.publicCertPath = publicCertPath; }

    public String getPrivateKeyPath() { return privateKeyPath; }
    public void setPrivateKeyPath(String privateKeyPath) { this.privateKeyPath = privateKeyPath; }

    public String getCertificate() { return certificate; }
    public void setCertificate(String certificate) { this.certificate = certificate; }

    @PostConstruct
    public void initCertificate() {
        if (certificate == null || certificate.isBlank()) {
            try {
                if (publicCertPath != null && !publicCertPath.isBlank()) {
                    certificate = Files.readString(new File(publicCertPath).toPath());
                }
            } catch (IOException e) {
                throw new RuntimeException("Failed to load public certificate from " + publicCertPath, e);
            }
        }
    }

    public List<String> loadTrustedCertificates() {
        try {
            File dir = new File(trustedCertsDirPath);
            File[] pemFiles = dir.listFiles((d, name) -> name.endsWith(".pem") || name.endsWith(".crt"));

            List<String> result = new ArrayList<>();

            if (pemFiles != null) {
                for (File file : pemFiles) {
                    String pem = Files.readString(file.toPath());
                    result.add(pem);
                }
            }

            return result;
        } catch (IOException e) {
            throw new RuntimeException("Failed to load trusted certificates from " + trustedCertsDirPath, e);
        }
    }
}
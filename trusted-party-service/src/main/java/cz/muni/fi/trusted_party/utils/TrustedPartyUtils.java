package cz.muni.fi.trusted_party.utils;

import cz.muni.fi.trusted_party.config.AppProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.cert.CertPath;
import java.security.cert.CertPathValidator;
import java.security.cert.CertStore;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.cert.CollectionCertStoreParameters;
import java.security.cert.PKIXParameters;
import java.security.cert.TrustAnchor;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Component
public final class TrustedPartyUtils {

    private final AppProperties appProperties;

    @Autowired
    private TrustedPartyUtils(AppProperties appProperties) {
        this.appProperties = appProperties;
    }

    public static boolean verifyChainOfTrust(String clientCertPem,
                                             List<String> intermediateCertificates,
                                             List<String> trustedCertificates) {
        try {
            if (trustedCertificates == null || trustedCertificates.isEmpty()) {
                return false;
            }

            X509Certificate clientCert = parseX509Certificate(clientCertPem);

            List<X509Certificate> intermediateCerts = new ArrayList<>();
            if (intermediateCertificates != null) {
                for (String pem : intermediateCertificates) {
                    if (pem != null && !pem.isBlank()) {
                        intermediateCerts.add(parseX509Certificate(pem));
                    }
                }
            }

            Set<TrustAnchor> trustAnchors = new HashSet<>();
            for (String pem : trustedCertificates) {
                if (pem != null && !pem.isBlank()) {
                    X509Certificate root = parseX509Certificate(pem);
                    trustAnchors.add(new TrustAnchor(root, null));
                }
            }

            if (trustAnchors.isEmpty()) {
                return false;
            }

            List<Certificate> certChain = new ArrayList<>();
            certChain.add(clientCert);
            certChain.addAll(intermediateCerts);

            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            CertPath certPath = cf.generateCertPath(certChain);

            PKIXParameters params = new PKIXParameters(trustAnchors);
            params.setRevocationEnabled(false);

            if (!intermediateCerts.isEmpty()) {
                CertStore store = CertStore.getInstance(
                        "Collection",
                        new CollectionCertStoreParameters(intermediateCerts)
                );
                params.addCertStore(store);
            }

            CertPathValidator validator = CertPathValidator.getInstance("PKIX");
            validator.validate(certPath, params);

            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public static String computeCertificateDigest(String certPem) {
        try {
            X509Certificate cert = parseX509Certificate(certPem);
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(cert.getEncoded());
            return toHex(hashed);
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to compute certificate digest", e);
        }
    }

    private static X509Certificate parseX509Certificate(String pem) throws Exception {
        CertificateFactory cf = CertificateFactory.getInstance("X.509");
        return (X509Certificate) cf.generateCertificate(
                new ByteArrayInputStream(pem.getBytes(StandardCharsets.UTF_8))
        );
    }

    private static String toHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(String.format("%02X", b));
        }
        return sb.toString();
    }
}

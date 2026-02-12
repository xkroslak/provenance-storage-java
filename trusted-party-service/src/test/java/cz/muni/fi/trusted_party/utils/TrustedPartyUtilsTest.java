package cz.muni.fi.trusted_party.utils;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Disabled("Requires local test certificates; keep disabled unless certs are available")
class TrustedPartyUtilsTest {

    @Test
    void verifyChainOfTrust_validChain_returnsTrue() {
        String clientCert = readResource("/certs/uni_graz/client.pem");
        List<String> intermediates = List.of(
                readResource("/certs/uni_graz/intermediate_1.pem"),
                readResource("/certs/uni_graz/intermediate_2.pem"));
        List<String> trusted = List.of(readResource("/certs/uni_graz/ca.pem"));

        boolean result = TrustedPartyUtils.verifyChainOfTrust(clientCert, intermediates, trusted);

        assertThat(result).isTrue();
    }

    @Test
    void verifyChainOfTrust_noTrustedCertificates_returnsFalse() {
        String clientCert = readResource("/certs/uni_graz/client.pem");
        List<String> intermediates = List.of(readResource("/certs/uni_graz/intermediate_1.pem"));

        boolean result = TrustedPartyUtils.verifyChainOfTrust(clientCert, intermediates, List.of());

        assertThat(result).isFalse();
    }

    @Test
    void computeCertificateDigest_validPem_matchesReferenceDigest() throws Exception {
        String clientCert = readResource("/certs/uni_graz/client.pem");

        String expected = computeDigestReference(clientCert);
        String actual = TrustedPartyUtils.computeCertificateDigest(clientCert);

        assertThat(actual).isEqualTo(expected);
    }

    @Test
    void computeCertificateDigest_invalidPem_throwsIllegalArgumentException() {
        assertThatThrownBy(() -> TrustedPartyUtils.computeCertificateDigest("not-a-cert"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Failed to compute certificate digest");
    }

    private String readResource(String path) {
        try (InputStream input = TrustedPartyUtilsTest.class.getResourceAsStream(path)) {
            if (input == null) {
                throw new IllegalStateException("Missing test resource: " + path);
            }
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to read test resource: " + path, e);
        }
    }

    private String computeDigestReference(String pem) throws Exception {
        CertificateFactory cf = CertificateFactory.getInstance("X.509");
        try (InputStream input = new java.io.ByteArrayInputStream(pem.getBytes(StandardCharsets.UTF_8))) {
            X509Certificate cert = (X509Certificate) cf.generateCertificate(input);
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(cert.getEncoded());
            StringBuilder sb = new StringBuilder(hashed.length * 2);
            for (byte b : hashed) {
                sb.append(String.format("%02X", b));
            }
            return sb.toString();
        }
    }
}

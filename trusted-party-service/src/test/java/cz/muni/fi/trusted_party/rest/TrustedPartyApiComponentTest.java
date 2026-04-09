package cz.muni.fi.trusted_party.rest;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import cz.muni.fi.trusted_party.utils.prov.ProvToolboxUtils;
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.PEMKeyPair;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Assumptions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Security;
import java.security.Signature;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.io.ByteArrayInputStream;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertNull;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("componenttest")
class TrustedPartyApiComponentTest {

    private static final Path CLIENT_CERT_PATH = Path.of("../certs/cert-dev/cert.pem");
    private static final Path CLIENT_PRIVATE_KEY_PATH = Path.of("../certs/cert-dev/ec_key.pem");
    private static final Path TRUSTED_CERTS_DIR_PATH = Path.of("../certs/trusted_certs");
        private static final String PROV_N_DOCUMENT = """
            document
            prefix ex <http://example.org/>
            bundle ex:sample_bundle
            entity(ex:e1)
            endBundle
            endDocument
            """;

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate http;

    @Autowired
    private ObjectMapper objectMapper;

    private record GraphScenario(String orgId, byte[] documentBytes, String signature, String bundleId) {}

        @BeforeAll
        static void requireCertificateMaterial() {
        boolean certsPresent = Files.exists(CLIENT_CERT_PATH)
            && Files.exists(CLIENT_PRIVATE_KEY_PATH)
            && Files.isDirectory(TRUSTED_CERTS_DIR_PATH);

        Assumptions.assumeTrue(
            certsPresent,
            "Skipping TrustedPartyApiComponentTest because required certificate files are missing."
        );
        }

    @Test
    void infoEndpoint_returnsTrustedPartyInfo() {
        ResponseEntity<String> response = http.getForEntity(url("/api/v1/info"), String.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());

        JsonNode body = parseJson(response.getBody());
        assertEquals("Trusted_Party", body.path("id").asText());
        assertFalse(body.path("certificate").asText().isBlank());
    }

    @Test
    void organizationEndpoints_registerGetAndUpdateCertificates() throws IOException {
        String orgId = "ORG-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        String clientCert = Files.readString(CLIENT_CERT_PATH);

        ResponseEntity<Void> register = http.exchange(
                url("/api/v1/organizations/" + orgId),
                HttpMethod.POST,
                jsonEntity(storeOrganizationBody(orgId, clientCert, List.of())),
                Void.class);

        assertEquals(HttpStatus.CREATED, register.getStatusCode());

        ResponseEntity<String> getOne = http.getForEntity(url("/api/v1/organizations/" + orgId), String.class);
        assertEquals(HttpStatus.OK, getOne.getStatusCode());
        JsonNode orgBody = parseJson(getOne.getBody());
        assertEquals(orgId, orgBody.path("organizationId").asText());
        assertFalse(orgBody.path("clientCertificate").asText().isBlank());

        ResponseEntity<String> getAll = http.getForEntity(url("/api/v1/organizations"), String.class);
        assertEquals(HttpStatus.OK, getAll.getStatusCode());
        JsonNode allBody = parseJson(getAll.getBody());
        assertTrue(allBody.isArray());
        assertTrue(containsOrganization(allBody, orgId));

        ResponseEntity<Void> update = http.exchange(
                url("/api/v1/organizations/" + orgId + "/certs"),
                HttpMethod.PUT,
                jsonEntity(storeOrganizationBody(orgId, clientCert, List.of())),
                Void.class);

        assertEquals(HttpStatus.CREATED, update.getStatusCode());

        ResponseEntity<String> getCerts = http.getForEntity(url("/api/v1/organizations/" + orgId + "/certs"), String.class);
        assertEquals(HttpStatus.OK, getCerts.getStatusCode());
        JsonNode certBody = parseJson(getCerts.getBody());
        assertEquals(orgId, certBody.path("organizationId").asText());
        assertTrue(certBody.path("revokedCertificates").isArray());
        assertEquals(0, certBody.path("revokedCertificates").size());
    }

    @Test
    void tokenEndpoints_issueGraphTokenVerifySignatureAndRetrieveTokens() throws Exception {
        String orgId = "ORG-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        String clientCert = Files.readString(CLIENT_CERT_PATH);

        ResponseEntity<Void> register = http.exchange(
                url("/api/v1/organizations/" + orgId),
                HttpMethod.POST,
                jsonEntity(storeOrganizationBody(orgId, clientCert, List.of())),
                Void.class);

        assertEquals(HttpStatus.CREATED, register.getStatusCode());

        byte[] graphBytes = toJavaStandardProvJsonBytes(PROV_N_DOCUMENT);
        String signature = sign(graphBytes);
        Map<String, Object> tokenRequest = buildGraphTokenRequest(orgId, graphBytes, signature);

        ResponseEntity<String> issue = http.postForEntity(url("/api/v1/issueToken"), jsonEntity(tokenRequest), String.class);
        assertEquals(HttpStatus.CREATED, issue.getStatusCode());

        JsonNode issuedTokens = parseJson(issue.getBody());
        assertTrue(issuedTokens.isArray());
        assertEquals(1, issuedTokens.size());
        JsonNode token = issuedTokens.get(0);
        assertEquals(orgId, token.path("data").path("originatorId").asText());
        assertEquals("Trusted_Party", token.path("data").path("authorityId").asText());
        assertNotNull(token.path("data").path("additionalData").path("bundle").asText(null));

        ResponseEntity<Void> verifyOk = http.postForEntity(url("/api/v1/verifySignature"), jsonEntity(tokenRequest), Void.class);
        assertEquals(HttpStatus.OK, verifyOk.getStatusCode());

        Map<String, Object> invalidSignatureRequest = new LinkedHashMap<>(tokenRequest);
        invalidSignatureRequest.put("signature", sign("different-document".getBytes(StandardCharsets.UTF_8)));

        ResponseEntity<Void> verifyNok = http.postForEntity(
                url("/api/v1/verifySignature"),
                jsonEntity(invalidSignatureRequest),
                Void.class);
        assertEquals(HttpStatus.BAD_REQUEST, verifyNok.getStatusCode());

        ResponseEntity<String> allTokens = http.getForEntity(url("/api/v1/organizations/" + orgId + "/tokens"), String.class);
        assertEquals(HttpStatus.OK, allTokens.getStatusCode());
        JsonNode tokenList = parseJson(allTokens.getBody());
        assertTrue(tokenList.isArray());
        assertTrue(tokenList.size() >= 1);
    }

    @Test
    void tokenEndpoint_issueTokenMissingSignature_returnsBadRequest() throws Exception {
        String orgId = "ORG-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        String clientCert = Files.readString(CLIENT_CERT_PATH);

        ResponseEntity<Void> register = http.exchange(
                url("/api/v1/organizations/" + orgId),
                HttpMethod.POST,
                jsonEntity(storeOrganizationBody(orgId, clientCert, List.of())),
                Void.class);
        assertEquals(HttpStatus.CREATED, register.getStatusCode());

        byte[] graphBytes = toJavaStandardProvJsonBytes(PROV_N_DOCUMENT);
        Map<String, Object> tokenRequest = buildGraphTokenRequest(orgId, graphBytes, null);
        tokenRequest.remove("signature");

        ResponseEntity<String> issue = http.postForEntity(url("/api/v1/issueToken"), jsonEntity(tokenRequest), String.class);
        assertEquals(HttpStatus.BAD_REQUEST, issue.getStatusCode());
    }

    @Test
    void tokenEndpoint_issueTokenUnknownOrganization_returnsNotFound() throws Exception {
        String orgId = "ORG-NOT-EXISTS-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        byte[] graphBytes = toJavaStandardProvJsonBytes(PROV_N_DOCUMENT);
        String signature = sign(graphBytes);
        Map<String, Object> tokenRequest = buildGraphTokenRequest(orgId, graphBytes, signature);

        ResponseEntity<String> issue = http.postForEntity(url("/api/v1/issueToken"), jsonEntity(tokenRequest), String.class);
        assertEquals(HttpStatus.NOT_FOUND, issue.getStatusCode());
    }

    @Test
    void tokenEndpoint_issueTokenMeta_allowsUnknownOrganization() throws Exception {
        String unknownOrgId = "META-UNKNOWN-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        byte[] graphBytes = toJavaStandardProvJsonBytes(PROV_N_DOCUMENT);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("organizationId", unknownOrgId);
        body.put("document", Base64.getEncoder().encodeToString(graphBytes));
        body.put("documentFormat", "json");
        body.put("type", "meta");
        body.put("createdOn", String.valueOf(Instant.now().minusSeconds(60).getEpochSecond()));

        ResponseEntity<String> issue = http.postForEntity(url("/api/v1/issueToken"), jsonEntity(body), String.class);
        assertEquals(HttpStatus.CREATED, issue.getStatusCode());

        JsonNode token = parseJson(issue.getBody()).get(0);
        assertEquals(unknownOrgId, token.path("data").path("originatorId").asText());
        assertEquals("Trusted_Party", token.path("data").path("authorityId").asText());
    }

    @Test
    void tokenEndpoint_issueTokenBackbone_andWrongOrgPath() throws Exception {
        String orgId = "ORG-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        String clientCert = Files.readString(CLIENT_CERT_PATH);

        ResponseEntity<Void> register = http.exchange(
                url("/api/v1/organizations/" + orgId),
                HttpMethod.POST,
                jsonEntity(storeOrganizationBody(orgId, clientCert, List.of())),
                Void.class);
        assertEquals(HttpStatus.CREATED, register.getStatusCode());

        byte[] graphBytes = toJavaStandardProvJsonBytes(PROV_N_DOCUMENT);

        Map<String, Object> valid = new LinkedHashMap<>();
        valid.put("organizationId", orgId);
        valid.put("document", Base64.getEncoder().encodeToString(graphBytes));
        valid.put("documentFormat", "json");
        valid.put("type", "backbone");
        valid.put("createdOn", String.valueOf(Instant.now().minusSeconds(60).getEpochSecond()));

        ResponseEntity<String> ok = http.postForEntity(url("/api/v1/issueToken"), jsonEntity(valid), String.class);
        assertEquals(HttpStatus.CREATED, ok.getStatusCode());

        Map<String, Object> wrongOrg = new LinkedHashMap<>(valid);
        wrongOrg.put("organizationId", orgId + "-WRONG");

        ResponseEntity<String> nok = http.postForEntity(url("/api/v1/issueToken"), jsonEntity(wrongOrg), String.class);
        assertEquals(HttpStatus.NOT_FOUND, nok.getStatusCode());
    }

    @Test
    void tokenAndDocumentRetrieval_endToEndGraphFlow() throws Exception {
        GraphScenario scenario = issueGraphForNewOrganization();

        ResponseEntity<String> documentRes = http.getForEntity(
                url("/api/v1/organizations/" + scenario.orgId() + "/documents/" + scenario.bundleId() + "/json"),
                String.class);
        assertEquals(HttpStatus.OK, documentRes.getStatusCode());

        JsonNode documentBody = parseJson(documentRes.getBody());
        byte[] returnedDoc = Base64.getDecoder().decode(documentBody.path("documentText").asText());
        assertEquals(new String(scenario.documentBytes(), StandardCharsets.UTF_8), new String(returnedDoc, StandardCharsets.UTF_8));
        verifySignatureWithClientCertificate(returnedDoc, documentBody.path("signature").asText());

        ResponseEntity<String> tokenRes = http.getForEntity(
                url("/api/v1/organizations/" + scenario.orgId() + "/tokens/" + scenario.bundleId() + "/json"),
                String.class);
        assertEquals(HttpStatus.OK, tokenRes.getStatusCode());
        JsonNode tokens = parseJson(tokenRes.getBody());
        assertTrue(tokens.isArray());
        assertTrue(tokens.size() >= 1);
        assertEquals(scenario.bundleId(), tokens.get(0).path("data").path("additionalData").path("bundle").asText());

        ResponseEntity<String> wrongIdRes = http.getForEntity(
                url("/api/v1/organizations/" + scenario.orgId() + "/tokens/wrong_bundle_id/json"),
                String.class);
        assertEquals(HttpStatus.NOT_FOUND, wrongIdRes.getStatusCode());
    }

    @Test
    void verifySignature_endpointWrongOrganization_returnsNotFound() throws Exception {
        byte[] graphBytes = toJavaStandardProvJsonBytes(PROV_N_DOCUMENT);
        String signature = sign(graphBytes);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("organizationId", "ORG-VERIFY-WRONG-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase());
        body.put("document", Base64.getEncoder().encodeToString(graphBytes));
        body.put("signature", signature);

        ResponseEntity<String> res = http.postForEntity(url("/api/v1/verifySignature"), jsonEntity(body), String.class);
        assertEquals(HttpStatus.NOT_FOUND, res.getStatusCode());
    }

    @Test
    void organizations_duplicateRegistration_returnsConflict() throws Exception {
        String orgId = "ORG-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        String clientCert = Files.readString(CLIENT_CERT_PATH);
        Map<String, Object> payload = storeOrganizationBody(orgId, clientCert, List.of());

        ResponseEntity<Void> first = http.exchange(
                url("/api/v1/organizations/" + orgId),
                HttpMethod.POST,
                jsonEntity(payload),
                Void.class);
        assertEquals(HttpStatus.CREATED, first.getStatusCode());

        ResponseEntity<String> second = http.postForEntity(url("/api/v1/organizations/" + orgId), jsonEntity(payload), String.class);
        assertEquals(HttpStatus.CONFLICT, second.getStatusCode());
    }

    @Test
    void organizations_updateWithMismatchedId_returnsBadRequest() throws Exception {
        String orgId = "ORG-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        String clientCert = Files.readString(CLIENT_CERT_PATH);

        ResponseEntity<Void> register = http.exchange(
                url("/api/v1/organizations/" + orgId),
                HttpMethod.POST,
                jsonEntity(storeOrganizationBody(orgId, clientCert, List.of())),
                Void.class);
        assertEquals(HttpStatus.CREATED, register.getStatusCode());

        Map<String, Object> wrongBody = storeOrganizationBody(orgId + "-WRONG", clientCert, List.of());
        ResponseEntity<String> update = http.exchange(
                url("/api/v1/organizations/" + orgId + "/certs"),
                HttpMethod.PUT,
                jsonEntity(wrongBody),
                String.class);
        assertEquals(HttpStatus.BAD_REQUEST, update.getStatusCode());
    }

    @Test
    void tokensForOrganization_withoutDocuments_returnsEmptyArray() throws Exception {
        String orgId = "ORG-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        String clientCert = Files.readString(CLIENT_CERT_PATH);

        ResponseEntity<Void> register = http.exchange(
                url("/api/v1/organizations/" + orgId),
                HttpMethod.POST,
                jsonEntity(storeOrganizationBody(orgId, clientCert, List.of())),
                Void.class);
        assertEquals(HttpStatus.CREATED, register.getStatusCode());

        ResponseEntity<String> tokens = http.getForEntity(url("/api/v1/organizations/" + orgId + "/tokens"), String.class);
        assertEquals(HttpStatus.OK, tokens.getStatusCode());
        JsonNode body = parseJson(tokens.getBody());
        assertTrue(body.isArray());
        assertEquals(0, body.size());
    }

        @Test
        void unknownOrganization_readEndpoints_returnNotFound() {
        String orgId = "ORG-NOT-FOUND-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();

        ResponseEntity<String> orgRes = http.getForEntity(url("/api/v1/organizations/" + orgId), String.class);
        assertEquals(HttpStatus.NOT_FOUND, orgRes.getStatusCode());

        ResponseEntity<String> certsRes = http.getForEntity(url("/api/v1/organizations/" + orgId + "/certs"), String.class);
        assertEquals(HttpStatus.NOT_FOUND, certsRes.getStatusCode());

        ResponseEntity<String> tokensRes = http.getForEntity(url("/api/v1/organizations/" + orgId + "/tokens"), String.class);
        assertEquals(HttpStatus.NOT_FOUND, tokensRes.getStatusCode());

        ResponseEntity<String> docRes = http.getForEntity(
            url("/api/v1/organizations/" + orgId + "/documents/non-existing/json"),
            String.class);
        assertEquals(HttpStatus.NOT_FOUND, docRes.getStatusCode());
        }

        @Test
        void tokenAndDocumentRetrieval_wrongDocumentFormat_returnsNotFound() throws Exception {
        GraphScenario scenario = issueGraphForNewOrganization();

        ResponseEntity<String> documentWrongFormat = http.getForEntity(
            url("/api/v1/organizations/" + scenario.orgId() + "/documents/" + scenario.bundleId() + "/xml"),
            String.class);
        assertEquals(HttpStatus.NOT_FOUND, documentWrongFormat.getStatusCode());

        ResponseEntity<String> tokenWrongFormat = http.getForEntity(
            url("/api/v1/organizations/" + scenario.orgId() + "/tokens/" + scenario.bundleId() + "/xml"),
            String.class);
        assertEquals(HttpStatus.NOT_FOUND, tokenWrongFormat.getStatusCode());
        }

        @Test
        void tokenEndpoint_issueTokenFutureTimestamp_returnsBadRequest() throws Exception {
        String orgId = "ORG-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        String clientCert = Files.readString(CLIENT_CERT_PATH);

        ResponseEntity<Void> register = http.exchange(
            url("/api/v1/organizations/" + orgId),
            HttpMethod.POST,
            jsonEntity(storeOrganizationBody(orgId, clientCert, List.of())),
            Void.class);
        assertEquals(HttpStatus.CREATED, register.getStatusCode());

        byte[] graphBytes = toJavaStandardProvJsonBytes(PROV_N_DOCUMENT);
        String signature = sign(graphBytes);
        Map<String, Object> tokenRequest = buildGraphTokenRequest(orgId, graphBytes, signature);
        tokenRequest.put("createdOn", String.valueOf(Instant.now().plusSeconds(86_400).getEpochSecond()));

        ResponseEntity<String> issue = http.postForEntity(url("/api/v1/issueToken"), jsonEntity(tokenRequest), String.class);
        assertEquals(HttpStatus.BAD_REQUEST, issue.getStatusCode());
        }

        @Test
        void tokenEndpoint_issueSameGraphTwice_returnsTokenForSameBundleAndDigest() throws Exception {
        String orgId = "ORG-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        String clientCert = Files.readString(CLIENT_CERT_PATH);

        ResponseEntity<Void> register = http.exchange(
            url("/api/v1/organizations/" + orgId),
            HttpMethod.POST,
            jsonEntity(storeOrganizationBody(orgId, clientCert, List.of())),
            Void.class);
        assertEquals(HttpStatus.CREATED, register.getStatusCode());

        byte[] graphBytes = toJavaStandardProvJsonBytes(PROV_N_DOCUMENT);
        String signature = sign(graphBytes);
        Map<String, Object> tokenRequest = buildGraphTokenRequest(orgId, graphBytes, signature);

        ResponseEntity<String> first = http.postForEntity(url("/api/v1/issueToken"), jsonEntity(tokenRequest), String.class);
        assertEquals(HttpStatus.CREATED, first.getStatusCode());

        ResponseEntity<String> second = http.postForEntity(url("/api/v1/issueToken"), jsonEntity(tokenRequest), String.class);
        assertEquals(HttpStatus.CREATED, second.getStatusCode());

        JsonNode firstToken = parseJson(first.getBody()).get(0);
        JsonNode secondToken = parseJson(second.getBody()).get(0);

        assertEquals(
            firstToken.path("data").path("additionalData").path("bundle").asText(),
            secondToken.path("data").path("additionalData").path("bundle").asText()
        );
        assertEquals(
            firstToken.path("data").path("documentDigest").asText(),
            secondToken.path("data").path("documentDigest").asText()
        );
        assertFalse(firstToken.path("signature").asText().isBlank());
        assertFalse(secondToken.path("signature").asText().isBlank());
        }

        @Test
        void tokenEndpoint_issueTokenMalformedMetaDocument_returnsBadRequest() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("organizationId", "META-MALFORMED-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase());
        body.put("document", "@@@not-base64@@@");
        body.put("documentFormat", "json");
        body.put("type", "meta");
        body.put("createdOn", String.valueOf(Instant.now().minusSeconds(60).getEpochSecond()));

        ResponseEntity<String> issue = http.postForEntity(url("/api/v1/issueToken"), jsonEntity(body), String.class);
        assertEquals(HttpStatus.BAD_REQUEST, issue.getStatusCode());
        }

    private String url(String path) {
        return "http://localhost:" + port + path;
    }

    private HttpEntity<Object> jsonEntity(Object body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return new HttpEntity<>(body, headers);
    }

    private JsonNode parseJson(String content) {
        try {
            return objectMapper.readTree(content);
        } catch (Exception e) {
            throw new IllegalStateException("Invalid JSON response", e);
        }
    }

    private Map<String, Object> storeOrganizationBody(String orgId,
                                                      String clientCertificate,
                                                      List<String> intermediateCertificates) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("organizationId", orgId);
        body.put("clientCertificate", clientCertificate);
        body.put("intermediateCertificates", intermediateCertificates);
        return body;
    }

    private Map<String, Object> buildGraphTokenRequest(String orgId, byte[] graphBytes, String signature) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("organizationId", orgId);
        body.put("document", Base64.getEncoder().encodeToString(graphBytes));
        body.put("documentFormat", "json");
        body.put("type", "graph");
        body.put("createdOn", String.valueOf(Instant.now().minusSeconds(60).getEpochSecond()));
        body.put("signature", signature);
        return body;
    }

    private byte[] toJavaStandardProvJsonBytes(String provnDocument) {
        String provnBase64 = Base64.getEncoder().encodeToString(provnDocument.getBytes(StandardCharsets.UTF_8));
        org.openprovenance.prov.model.Document provDocument = ProvToolboxUtils.parseDocument(provnBase64, "provn");
        String jsonBase64 = ProvToolboxUtils.serializeDocumentToBase64(provDocument, "json");
        return Base64.getDecoder().decode(jsonBase64);
    }

    private GraphScenario issueGraphForNewOrganization() throws Exception {
        String orgId = "ORG-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        String clientCert = Files.readString(CLIENT_CERT_PATH);
        ResponseEntity<Void> register = http.exchange(
                url("/api/v1/organizations/" + orgId),
                HttpMethod.POST,
                jsonEntity(storeOrganizationBody(orgId, clientCert, List.of())),
                Void.class);
        assertEquals(HttpStatus.CREATED, register.getStatusCode());

        byte[] graphBytes = toJavaStandardProvJsonBytes(PROV_N_DOCUMENT);
        String signature = sign(graphBytes);
        Map<String, Object> tokenRequest = buildGraphTokenRequest(orgId, graphBytes, signature);

        ResponseEntity<String> issue = http.postForEntity(url("/api/v1/issueToken"), jsonEntity(tokenRequest), String.class);
        assertEquals(HttpStatus.CREATED, issue.getStatusCode());

        JsonNode issued = parseJson(issue.getBody());
        String bundleId = issued.get(0).path("data").path("additionalData").path("bundle").asText();
        return new GraphScenario(orgId, graphBytes, signature, bundleId);
    }

    private void verifySignatureWithClientCertificate(byte[] content, String signatureBase64) throws Exception {
        String certPem = Files.readString(CLIENT_CERT_PATH);
        CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");
        X509Certificate certificate = (X509Certificate) certificateFactory.generateCertificate(
                new ByteArrayInputStream(certPem.getBytes(StandardCharsets.UTF_8)));
        PublicKey publicKey = certificate.getPublicKey();

        Signature verifier = Signature.getInstance("SHA256withECDSA");
        verifier.initVerify(publicKey);
        verifier.update(content);
        assertTrue(verifier.verify(Base64.getDecoder().decode(signatureBase64)));
    }

    private boolean containsOrganization(JsonNode allOrganizations, String orgId) {
        List<String> ids = new ArrayList<>();
        for (JsonNode org : allOrganizations) {
            ids.add(org.path("organizationId").asText());
        }
        return ids.contains(orgId);
    }

    private String sign(byte[] content) {
        try {
            Security.addProvider(new BouncyCastleProvider());

            String privateKeyPem = Files.readString(CLIENT_PRIVATE_KEY_PATH);
            PrivateKey privateKey = parsePrivateKey(privateKeyPem);

            Signature signer = Signature.getInstance("SHA256withECDSA");
            signer.initSign(privateKey);
            signer.update(content);
            return Base64.getEncoder().encodeToString(signer.sign());
        } catch (Exception e) {
            throw new IllegalStateException("Could not sign content for test request", e);
        }
    }

    private PrivateKey parsePrivateKey(String privateKeyPem) throws IOException {
        try (PEMParser parser = new PEMParser(new StringReader(privateKeyPem))) {
            Object object = parser.readObject();
            if (object == null) {
                throw new IllegalStateException("Private key PEM is empty");
            }

            JcaPEMKeyConverter converter = new JcaPEMKeyConverter().setProvider("BC");
            if (object instanceof PrivateKeyInfo privateKeyInfo) {
                return converter.getPrivateKey(privateKeyInfo);
            }
            if (object instanceof PEMKeyPair pemKeyPair) {
                return converter.getKeyPair(pemKeyPair).getPrivate();
            }

            throw new IllegalStateException("Unsupported private key PEM type: " + object.getClass());
        }
    }
}
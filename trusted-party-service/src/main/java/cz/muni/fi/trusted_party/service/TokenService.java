package cz.muni.fi.trusted_party.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import cz.muni.fi.trusted_party.api.Token.TokenRequestDTO;
import cz.muni.fi.trusted_party.config.AppProperties;
import cz.muni.fi.trusted_party.data.enums.CertificateType;
import cz.muni.fi.trusted_party.data.enums.DocumentType;
import cz.muni.fi.trusted_party.data.enums.HashFunction;
import cz.muni.fi.trusted_party.data.model.Certificate;
import cz.muni.fi.trusted_party.data.model.Document;
import cz.muni.fi.trusted_party.data.model.Organization;
import cz.muni.fi.trusted_party.data.model.Token;
import cz.muni.fi.trusted_party.data.repository.CertificateRepository;
import cz.muni.fi.trusted_party.data.repository.DocumentRepository;
import cz.muni.fi.trusted_party.data.repository.OrganizationRepository;
import cz.muni.fi.trusted_party.data.repository.TokenRepository;
import cz.muni.fi.trusted_party.exceptions.CertificateNotFoundException;
import cz.muni.fi.trusted_party.exceptions.DocumentNotFoundException;
import cz.muni.fi.trusted_party.exceptions.InvalidRequestException;
import cz.muni.fi.trusted_party.exceptions.InvalidTimestampException;
import cz.muni.fi.trusted_party.exceptions.MissingSignatureException;
import cz.muni.fi.trusted_party.exceptions.OrganizationNotFoundException;
import cz.muni.fi.trusted_party.exceptions.SignatureVerificationException;
import cz.muni.fi.trusted_party.utils.prov.ProvToolboxUtils;
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.PEMKeyPair;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.openprovenance.prov.model.Bundle;
import org.openprovenance.prov.model.StatementOrBundle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyFactory;
import java.security.MessageDigest;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.TemporalAccessor;
import java.time.temporal.TemporalQueries;
import java.io.StringReader;
import java.security.Security;
import java.util.Base64;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class TokenService {

    private final TokenRepository tokenRepository;
    private final OrganizationRepository organizationRepository;
    private final DocumentRepository documentRepository;
    private final CertificateRepository certificateRepository;
    private final AppProperties appProperties;

    @Autowired
    public TokenService(TokenRepository tokenRepository,
                        OrganizationRepository organizationRepository,
                        DocumentRepository documentRepository,
                        CertificateRepository certificateRepository,
                        AppProperties appProperties) {
        this.tokenRepository = tokenRepository;
        this.organizationRepository = organizationRepository;
        this.documentRepository = documentRepository;
        this.certificateRepository = certificateRepository;
        this.appProperties = appProperties;
    }

    public List<Token> getToken(String organizationName, String documentId, String documentFormat) {
        Organization organization = organizationRepository
                .findById(organizationName)
                .orElseThrow(() -> new OrganizationNotFoundException(organizationName));

        Document doc = documentRepository
                .findByIdentifierAndDocFormatAndDocumentTypeAndOrganization(documentId, documentFormat, DocumentType.GRAPH, organization)
                .orElseThrow(() -> new DocumentNotFoundException(
                        "No document with id " + documentId
                                + " in format " + documentFormat
                                + "exists for organization " + organizationName));

        return tokenRepository.findByDocument(doc);
    }

    public Map<Document, List<Token>> getAllTokens(String organizationName) {
        Organization organization = organizationRepository
                .findById(organizationName)
                .orElseThrow(() -> new OrganizationNotFoundException(organizationName));
        List<Document> docs = documentRepository.findByOrganization(organization);

        Map<Document, List<Token>> tokensByDocument = new HashMap<>();
        for (Document document : docs) {
            List<Token> tokens = tokenRepository.findByDocument(document);
            tokensByDocument.put(document, tokens);
        }

        return tokensByDocument;
    }

    @Transactional
    public List<Token> issueToken(TokenRequestDTO body) {
        //Fields are checked in DTO using Jakarta Validation
        if (body.getOrganizationId() == null || body.getOrganizationId().isBlank()) {
            String inferred = inferOrganizationId(body);
            if (inferred != null && !inferred.isBlank()) {
                body.setOrganizationId(inferred);
            } else {
                throw new InvalidRequestException("Missing organizationId");
            }
        }

        if (body.getDocumentType() == DocumentType.GRAPH &&
                (body.getSignature() == null || body.getSignature().isBlank())) {
            throw new MissingSignatureException("Mandatory field [\"signature\"] not present in request!");
        }

        LocalDateTime createdOn = parseCreatedOn(body.getCreatedOn());
        if (createdOn.isAfter(LocalDateTime.now())) {
            throw new InvalidTimestampException("Incorrect timestamp for the document");
        }

        if (body.getDocumentType() == DocumentType.GRAPH ||
                body.getDocumentType() == DocumentType.BACKBONE ||
                body.getDocumentType() == DocumentType.DOMAIN_SPECIFIC) {
            Organization org = organizationRepository
                    .findById(body.getOrganizationId())
                    .orElseThrow(() -> new OrganizationNotFoundException(body.getOrganizationId()));
        }

        if (body.getDocumentType() == DocumentType.GRAPH) {
            boolean verified = verifySignature(body);
            if (!verified) {
                throw new SignatureVerificationException("Invalid signature to the graph!");
            }
        }

        return issueTokenAndStoreDoc(body);
    }

    protected List<Token> issueTokenAndStoreDoc(TokenRequestDTO body) {
        if (body.getOrganizationId() == null || body.getOrganizationId().isBlank()) {
            String inferred = inferOrganizationId(body);
            if (inferred != null && !inferred.isBlank()) {
                body.setOrganizationId(inferred);
            } else {
                throw new InvalidRequestException("Missing organizationId");
            }
        }
        // Use fully-qualified name to avoid clash with cz.muni.fi.trusted_party.data.model.Document
        org.openprovenance.prov.model.Document provDocument = ProvToolboxUtils.parseDocument(
                body.getDocument(), body.getDocumentFormat());

        Bundle bundle = extractSingleBundle(provDocument);
        String bundleId = resolveBundleId(bundle);

        if (body.getDocumentType() == DocumentType.DOMAIN_SPECIFIC
                || body.getDocumentType() == DocumentType.BACKBONE) {
            // TODO: retrieve original bundle and implement subgraph check - was not implemented in Python version
            checkIsSubgraph(bundle, null);
        }

        if (body.getDocumentType() == DocumentType.META) {
            return List.of(buildUnsignedMetaToken(body, bundleId));
        }

        Organization org = organizationRepository.findById(body.getOrganizationId())
                .orElseThrow(() -> new OrganizationNotFoundException(body.getOrganizationId()));

        Optional<Document> existingDoc = documentRepository
                .findByIdentifierAndDocFormatAndDocumentTypeAndOrganization(
                        bundleId,
                        body.getDocumentFormat(),
                        body.getDocumentType(),
                        org);

        if (existingDoc.isPresent()) {
            List<Token> tokens = tokenRepository.findByDocument(existingDoc.get());
            if (!tokens.isEmpty()) {
                return tokens;
            }
        }

        Certificate cert = certificateRepository
                .findFirstByOrganizationOrgNameAndIsRevoked(
                        body.getOrganizationId(),
                        false);

        if (cert == null) {
            throw new CertificateNotFoundException(body.getOrganizationId());
        }

        Document doc = new Document();
        doc.setIdentifier(bundleId);
        doc.setDocFormat(body.getDocumentFormat());
        doc.setOrganization(org);
        doc.setCertificate(cert);
        doc.setDocumentType(body.getDocumentType());
        doc.setDocumentText(body.getDocument());
        doc.setCreatedOn(parseCreatedOn(body.getCreatedOn()));
        doc.setSignature(body.getDocumentType() == DocumentType.GRAPH ? body.getSignature() : null);
        documentRepository.save(doc);

        Token token = buildSignedToken(body, doc, bundleId);
        tokenRepository.save(token);
        return List.of(token);
    }

    public boolean verifySignature(TokenRequestDTO body) {
        Organization org = organizationRepository
                .findById(body.getOrganizationId())
                .orElseThrow(() -> new OrganizationNotFoundException(body.getOrganizationId()));

        Certificate cert = certificateRepository
                .findFirstByOrganizationOrgNameAndCertificateTypeAndIsRevoked(
                        body.getOrganizationId(),
                        CertificateType.CLIENT,
                        false);

        if (cert == null) {
            throw new CertificateNotFoundException(body.getOrganizationId());
        }

        String graph = body.getDocument();

        try {
            CertificateFactory cf = CertificateFactory.getInstance("X.509");

            X509Certificate x509Cert = (X509Certificate) cf.generateCertificate(
                    new ByteArrayInputStream(cert.getCert().getBytes(StandardCharsets.UTF_8))
            );

            PublicKey publicKey = x509Cert.getPublicKey();

            Signature signature = Signature.getInstance("SHA256withECDSA");
            signature.initVerify(publicKey);

            signature.update(Base64.getDecoder().decode(graph));

            return signature.verify(Base64.getDecoder().decode(body.getSignature()));
        } catch (Exception e) {
            throw new SignatureVerificationException("Invalid signature", e);
        }
    }

    private LocalDateTime parseCreatedOn(String createdOn) {
        if (createdOn == null || createdOn.isBlank()) {
            return LocalDateTime.now();
        }
        String text = createdOn.trim();
        if ((text.startsWith("\"") && text.endsWith("\"")) || (text.startsWith("'") && text.endsWith("'"))) {
            text = text.substring(1, text.length() - 1).trim();
        }
        if (text.matches("^-?\\d+$")) {
            long value = Long.parseLong(text);
            if (Math.abs(value) >= 1_000_000_000_000L) {
                return LocalDateTime.ofInstant(Instant.ofEpochMilli(value), ZoneOffset.UTC);
            }
            return LocalDateTime.ofInstant(Instant.ofEpochSecond(value), ZoneOffset.UTC);
        }
        try {
            DateTimeFormatter fmt = new DateTimeFormatterBuilder()
                    .append(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                    .optionalStart()
                    .appendOffsetId()
                    .optionalEnd()
                    .toFormatter();

            TemporalAccessor ta = fmt.parse(text);
            ZoneOffset offset = TemporalQueries.offset().queryFrom(ta);
            if (offset != null) {
                return OffsetDateTime.from(ta).toLocalDateTime();
            }
            return LocalDateTime.from(ta);
        } catch (Exception ignored) {
            return LocalDateTime.ofInstant(Instant.parse(text), ZoneOffset.UTC);
        }
    }

    private String inferOrganizationId(TokenRequestDTO body) {
        String document = body.getDocument();
        if (document == null || document.isBlank()) {
            return null;
        }
        try {
            byte[] decoded = Base64.getDecoder().decode(document);
            String content = new String(decoded, StandardCharsets.UTF_8);
            Matcher matcher = Pattern
                    .compile("/organizations/([^/]+)/documents/")
                    .matcher(content);
            if (matcher.find()) {
                return matcher.group(1);
            }
        } catch (Exception ignored) {
            return null;
        }
        return null;
    }

    private Bundle extractSingleBundle(org.openprovenance.prov.model.Document document) {
        Bundle first = null;
        for (StatementOrBundle sb : document.getStatementOrBundle()) {
            if (sb instanceof Bundle b) {
                if (first != null) {
                    throw new IllegalArgumentException("Only one bundle expected in document!");
                }
                first = b;
            }
        }
        if (first == null) {
            throw new IllegalArgumentException("There are no bundles inside the document!");
        }
        return first;
    }

    private String resolveBundleId(Bundle bundle) {
        if (bundle.getId() == null) {
            throw new IllegalArgumentException("Bundle identifier is missing.");
        }
        if (bundle.getId().getUri() != null) {
            return bundle.getId().getUri();
        }
        return bundle.getId().toString();
    }

    private void checkIsSubgraph(Bundle provBundle, Bundle originalBundle) {
        // TODO: Implement real subgraph validation - was not implemented in Python version
    }

    private Token buildUnsignedMetaToken(TokenRequestDTO body, String bundleId) {
        Document doc = new Document();
        doc.setIdentifier(bundleId);
        doc.setDocFormat(body.getDocumentFormat());
        doc.setDocumentType(body.getDocumentType());
        doc.setDocumentText(body.getDocument());
        doc.setCreatedOn(parseCreatedOn(body.getCreatedOn()));
        doc.setSignature(null);

        Organization org = new Organization();
        org.setOrgName(body.getOrganizationId());
        doc.setOrganization(org);

        Token token = buildSignedToken(body, doc, bundleId);
        token.setDocument(doc);
        return token;
    }

    private Token buildSignedToken(TokenRequestDTO body, Document doc, String bundleId) {
        LocalDateTime tokenTimestamp = LocalDateTime.now();
        String documentDigest = sha256Hex(Base64.getDecoder().decode(body.getDocument()));

        String signature = signTokenData(
                body.getOrganizationId(),
                tokenTimestamp,
                parseCreatedOn(body.getCreatedOn()),
                documentDigest,
                bundleId);

        Token token = new Token();
        token.setDocument(doc);
        token.setHash(documentDigest);
        token.setHashFunction(HashFunction.SHA256);
        token.setCreatedOn(tokenTimestamp);
        token.setSignature(signature);
        return token;
    }

    private String signTokenData(String originatorId,
                                 LocalDateTime tokenTimestamp,
                                 LocalDateTime documentCreationTimestamp,
                                 String documentDigest,
                                 String bundleId) {
        try {
            Map<String, Object> additionalData = new LinkedHashMap<>();
            additionalData.put("bundle", bundleId);
            additionalData.put("hashFunction", "SHA256");
            additionalData.put("trustedPartyUri", appProperties.getFqdn());
            additionalData.put("trustedPartyCertificate", appProperties.getCertificate());

            Map<String, Object> data = new LinkedHashMap<>();
            data.put("originatorId", originatorId);
            data.put("authorityId", appProperties.getId());
            data.put("tokenTimestamp", tokenTimestamp);
            data.put("documentCreationTimestamp", documentCreationTimestamp);
            data.put("documentDigest", documentDigest);
            data.put("additionalData", additionalData);

            byte[] canonicalBytes = canonicalizeJson(data);

            ensureBouncyCastleProvider();
            PrivateKey privateKey = loadPrivateKey();
            Signature signer = Security.getProvider("BC") != null
                    ? Signature.getInstance("SHA256withECDSA", "BC")
                    : Signature.getInstance("SHA256withECDSA");
            signer.initSign(privateKey);
            signer.update(canonicalBytes);
            return Base64.getEncoder().encodeToString(signer.sign());
        } catch (Exception e) {
            throw new SignatureVerificationException("Failed to sign token data", e);
        }
    }

    private byte[] canonicalizeJson(Map<String, Object> data) throws Exception {
        ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();
        Object sorted = sortRecursively(data);
        String json = mapper.writeValueAsString(sorted);
        return json.getBytes(StandardCharsets.UTF_8);
    }

    private Object sortRecursively(Object value) {
        if (value instanceof Map<?, ?> map) {
            TreeMap<String, Object> sorted = new TreeMap<>();
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                String key = String.valueOf(entry.getKey());
                sorted.put(key, sortRecursively(entry.getValue()));
            }
            return sorted;
        }
        return value;
    }

    private PrivateKey loadPrivateKey() throws Exception {
        String pem = Files.readString(Path.of(appProperties.getPrivateKeyPath()));

        ensureBouncyCastleProvider();

        try (PEMParser parser = new PEMParser(new StringReader(pem))) {
            Object obj = parser.readObject();
            if (obj == null) {
                throw new IllegalArgumentException("Empty private key PEM");
            }

            JcaPEMKeyConverter converter =
                    new JcaPEMKeyConverter().setProvider("BC");

            if (obj instanceof PrivateKeyInfo pkInfo) {
                return converter.getPrivateKey(pkInfo);
            }
            if (obj instanceof PEMKeyPair kp) {
                return converter.getKeyPair(kp).getPrivate();
            }
        }

        String cleaned = pem
                .replaceAll("-----BEGIN ([A-Z ]*)-----", "")
                .replaceAll("-----END ([A-Z ]*)-----", "")
                .replaceAll("\\s", "");

        byte[] keyBytes = Base64.getDecoder().decode(cleaned);
        KeyFactory keyFactory = KeyFactory.getInstance("EC");
        return keyFactory.generatePrivate(new PKCS8EncodedKeySpec(keyBytes));
    }

    private void ensureBouncyCastleProvider() {
        if (Security.getProvider("BC") == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
    }

    private String sha256Hex(byte[] data) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(data);
            StringBuilder sb = new StringBuilder(hashed.length * 2);
            for (byte b : hashed) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to compute SHA-256 digest", e);
        }
    }

}
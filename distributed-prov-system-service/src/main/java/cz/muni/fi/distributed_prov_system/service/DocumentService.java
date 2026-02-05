package cz.muni.fi.distributed_prov_system.service;

import cz.muni.fi.distributed_prov_system.api.StoreGraphRequestDTO;
import cz.muni.fi.distributed_prov_system.api.StoreGraphResponseDTO;
import cz.muni.fi.distributed_prov_system.api.SubgraphResponseDTO;
import cz.muni.fi.distributed_prov_system.client.TrustedPartyClient;
import cz.muni.fi.distributed_prov_system.config.AppProperties;
import cz.muni.fi.distributed_prov_system.data.model.nodes.Bundle;
import cz.muni.fi.distributed_prov_system.data.model.nodes.Entity;
import cz.muni.fi.distributed_prov_system.data.model.nonprovmodels.Document;
import cz.muni.fi.distributed_prov_system.data.model.nonprovmodels.Token;
import cz.muni.fi.distributed_prov_system.data.model.nonprovmodels.TrustedParty;
import cz.muni.fi.distributed_prov_system.data.repository.BundleRepository;
import cz.muni.fi.distributed_prov_system.data.repository.DefaultTrustedPartyRepository;
import cz.muni.fi.distributed_prov_system.data.repository.DocumentRepository;
import cz.muni.fi.distributed_prov_system.data.repository.EntityRepository;
import cz.muni.fi.distributed_prov_system.data.repository.TokenRepository;
import cz.muni.fi.distributed_prov_system.data.repository.TrustedPartyRepository;
import cz.muni.fi.distributed_prov_system.exceptions.BadRequestException;
import cz.muni.fi.distributed_prov_system.exceptions.ConflictException;
import cz.muni.fi.distributed_prov_system.exceptions.NotFoundException;
import cz.muni.fi.distributed_prov_system.exceptions.UnauthorizedException;
import cz.muni.fi.distributed_prov_system.utils.TokenUtils;
import cz.muni.fi.distributed_prov_system.utils.prov.CPMValidatorImpl;
import cz.muni.fi.distributed_prov_system.utils.prov.InputGraphChecker;
import cz.muni.fi.distributed_prov_system.utils.prov.ProvDocumentValidatorImpl;
import cz.muni.fi.distributed_prov_system.utils.prov.SubgraphUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Service
public class DocumentService {

    private final AppProperties appProperties;
    private final TrustedPartyClient tpClient;
    private final OrganizationService organizationService;
    private final ImportGraphService importGraphService;
    private final DocumentRepository documentRepository;
    private final TokenRepository tokenRepository;
    private final EntityRepository entityRepository;
    private final BundleRepository bundleRepository;
    private final TrustedPartyRepository trustedPartyRepository;
    private final DefaultTrustedPartyRepository defaultTrustedPartyRepository;

    @Autowired
    public DocumentService(AppProperties appProperties,
                           TrustedPartyClient tpClient,
                           OrganizationService organizationService,
                           ImportGraphService importGraphService,
                           DocumentRepository documentRepository,
                           TokenRepository tokenRepository,
                           EntityRepository entityRepository,
                           BundleRepository bundleRepository,
                           TrustedPartyRepository trustedPartyRepository,
                           DefaultTrustedPartyRepository defaultTrustedPartyRepository) {
        this.appProperties = appProperties;
        this.tpClient = tpClient;
        this.organizationService = organizationService;
        this.importGraphService = importGraphService;
        this.documentRepository = documentRepository;
        this.tokenRepository = tokenRepository;
        this.entityRepository = entityRepository;
        this.bundleRepository = bundleRepository;
        this.trustedPartyRepository = trustedPartyRepository;
        this.defaultTrustedPartyRepository = defaultTrustedPartyRepository;
    }

    public StoreGraphResponseDTO storeDocument(String organizationId, String documentId, StoreGraphRequestDTO body) {
        requireDocumentFields(body);

        String requestPath = "/api/v1/organizations/" + organizationId + "/documents/" + documentId;
        InputGraphChecker checker = new InputGraphChecker(
            body.getDocument(),
            body.getDocumentFormat(),
            requestPath,
            appProperties,
            new CPMValidatorImpl(),
            new ProvDocumentValidatorImpl()
        );
        try {
            checker.parseGraph();
            checker.checkIdsMatch(documentId);
            checker.validateGraph();
        } catch (IllegalArgumentException ex) {
            throw new BadRequestException(ex.getMessage(), ex);
        }

        String bundleIdentifier = organizationId + "_" + checker.getBundleId();
        if (documentRepository.existsById(bundleIdentifier)) {
            throw new ConflictException("Document with id [" + checker.getBundleId() + "] already exists under organization [" + organizationId + "].");
        }

        if (!appProperties.isDisableTrustedParty()) {
            if (!organizationService.isRegistered(organizationId)) {
                throw new NotFoundException("Organization with id [" + organizationId + "] is not registered!");
            }
            if (body.getSignature() == null || body.getCreatedOn() == null) {
                throw new BadRequestException("Missing signature or createdOn.");
            }

            Map<String, Object> payload = TokenUtils.buildTokenPayload(body, organizationId, documentId);

            String tpUrl = organizationService.getTpUrlByOrganization(organizationId);
            ResponseEntity<String> verifyResp = tpClient.verifySignature(payload, tpUrl);
            if (!verifyResp.getStatusCode().is2xxSuccessful()) {
                throw new UnauthorizedException("Unverifiable signature.");
            }

            ResponseEntity<String> tokenResp = tpClient.issueToken(payload, tpUrl);
            if (!tokenResp.getStatusCode().is2xxSuccessful()) {
                throw new IllegalStateException("Could not issue token.");
            }

            Map<String, Object> tokenEnvelope = TokenUtils.parseTokenResponse(tokenResp.getBody());
                importGraphService.importGraph(body, tokenEnvelope, organizationId, documentId,
                    checker.getMetaProvenanceId(), false);
            storeTokenIntoDb(tokenEnvelope, organizationId, documentId);
            JsonNode tokenNode = toJsonNode(tokenEnvelope);
            return new StoreGraphResponseDTO(tokenNode, null);
        }

        Map<String, Object> dummyToken = TokenUtils.createDummyToken(organizationId);
        importGraphService.importGraph(body, dummyToken, organizationId, documentId,
            checker.getMetaProvenanceId(), false);
        return new StoreGraphResponseDTO(null,
                "Trusted party is disabled therefore no token has been issued, however graph has been stored.");
    }

    private JsonNode toJsonNode(Object value) {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.valueToTree(value);
    }

    private void storeTokenIntoDb(Map<String, Object> tokenEnvelope, String organizationId, String documentId) {
        if (tokenEnvelope == null) {
            return;
        }

        Map<String, Object> tokenData = TokenUtils.normalizeTokenData(tokenEnvelope);

        Token token = new Token();
        token.setSignature(tokenData.get("signature") != null ? tokenData.get("signature").toString() : null);
        token.setOriginatorId(stringOrNull(tokenData.get("originatorId")));
        token.setAuthorityId(stringOrNull(tokenData.get("authorityId")));
        token.setTokenTimestamp(longOrNull(tokenData.get("tokenTimestamp")));
        token.setDocumentCreationTimestamp(longOrNull(tokenData.get("documentCreationTimestamp")));
        token.setDocumentDigest(stringOrNull(tokenData.get("documentDigest")));
        token.setMessageTimestamp(longOrNull(tokenData.get("messageTimestamp")));
        if (tokenData.get("additionalData") instanceof Map<?, ?> additionalData) {
            Map<String, String> converted = new java.util.HashMap<>();
            for (Map.Entry<?, ?> entry : additionalData.entrySet()) {
                if (entry.getKey() != null && entry.getValue() != null) {
                    converted.put(entry.getKey().toString(), entry.getValue().toString());
                }
            }
            token.setAdditionalData(converted);
        }

        String identifier = organizationId + "_" + documentId;
        Document doc = documentRepository.findById(identifier)
                .orElseThrow(() -> new NotFoundException("Document with id [" + identifier + "] not found."));
        token.setBelongsTo(doc);

        String authorityId = stringOrNull(tokenData.get("authorityId"));
        TrustedParty tp = resolveTrustedParty(authorityId, organizationId);
        if (tp != null) {
            token.setWasIssuedBy(tp);
        }

        tokenRepository.save(token);
    }

    private String stringOrNull(Object value) {
        return value == null ? null : value.toString();
    }

    private Long longOrNull(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        String text = value.toString().trim();
        if ((text.startsWith("\"") && text.endsWith("\"")) || (text.startsWith("'") && text.endsWith("'"))) {
            text = text.substring(1, text.length() - 1).trim();
        }
        try {
            return Long.parseLong(text);
        } catch (NumberFormatException ex) {
            // Try ISO-8601 date-time with optional offset or fraction
            try {
                java.time.format.DateTimeFormatter fmt = new java.time.format.DateTimeFormatterBuilder()
                        .append(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                        .optionalStart()
                        .appendOffsetId()
                        .optionalEnd()
                        .toFormatter();

                java.time.temporal.TemporalAccessor ta = fmt.parse(text);
                java.time.ZoneOffset offset = java.time.temporal.TemporalQueries.offset().queryFrom(ta);
                if (offset != null) {
                    return java.time.OffsetDateTime.from(ta).toEpochSecond();
                }
                return java.time.LocalDateTime.from(ta).toEpochSecond(java.time.ZoneOffset.UTC);
            } catch (Exception ignored) {
                // Try Instant
                try {
                    return java.time.Instant.parse(text).getEpochSecond();
                } catch (Exception ignored2) {
                    throw ex;
                }
            }
        }
    }

    private void requireDocumentFields(StoreGraphRequestDTO body) {
        if (body.getDocument() == null || body.getDocumentFormat() == null) {
            throw new IllegalArgumentException("Missing document or documentFormat.");
        }
    }

    public StoreGraphResponseDTO updateDocument(String organizationId, String documentId, StoreGraphRequestDTO body) {
        requireDocumentFields(body);

        String requestPath = "/api/v1/organizations/" + organizationId + "/documents/" + documentId;
        InputGraphChecker checker = new InputGraphChecker(
                body.getDocument(),
                body.getDocumentFormat(),
                requestPath,
                appProperties,
                new CPMValidatorImpl(),
                new ProvDocumentValidatorImpl()
        );

        try {
            checker.parseGraph();
            checker.validateGraph();
        } catch (IllegalArgumentException ex) {
            throw new BadRequestException(ex.getMessage(), ex);
        }

        validateUpdateConditions(checker.getMetaProvenanceId(), documentId, organizationId);

        if (!appProperties.isDisableTrustedParty()) {
            if (!organizationService.isRegistered(organizationId)) {
                throw new NotFoundException("Organization with id [" + organizationId + "] is not registered!");
            }
            if (body.getSignature() == null || body.getCreatedOn() == null) {
                throw new BadRequestException("Missing signature or createdOn.");
            }

            Map<String, Object> payload = TokenUtils.buildTokenPayload(body, organizationId, documentId);
            String tpUrl = organizationService.getTpUrlByOrganization(organizationId);

            ResponseEntity<String> verifyResp = tpClient.verifySignature(payload, tpUrl);
            if (!verifyResp.getStatusCode().is2xxSuccessful()) {
                throw new UnauthorizedException("Unverifiable signature.");
            }

            ResponseEntity<String> tokenResp = tpClient.issueToken(payload, tpUrl);
            if (!tokenResp.getStatusCode().is2xxSuccessful()) {
                throw new IllegalStateException("Could not issue token.");
            }

            Map<String, Object> tokenEnvelope = TokenUtils.parseTokenResponse(tokenResp.getBody());
            importGraphService.importGraph(body, tokenEnvelope, organizationId, documentId,
                    checker.getMetaProvenanceId(), true);
            storeTokenIntoDb(tokenEnvelope, organizationId, documentId);

            JsonNode tokenNode = toJsonNode(tokenEnvelope);
            return new StoreGraphResponseDTO(tokenNode, null);
        }

        Map<String, Object> dummyToken = TokenUtils.createDummyToken(organizationId);
        importGraphService.importGraph(body, dummyToken, organizationId, documentId,
                checker.getMetaProvenanceId(), true);

        return new StoreGraphResponseDTO(null,
                "Trusted party is disabled therefore no token has been issued, however graph has been stored.");
    }

    private void validateUpdateConditions(String metaBundleId, String documentId, String organizationId) {
        String entityId = organizationId + "_" + documentId;
        Entity entity = entityRepository.findById(entityId)
                .orElseThrow(() -> new NotFoundException(
                        "Document with id [" + documentId + "] does not exist under organization [" + organizationId + "]."));

        if (!bundleRepository.existsById(metaBundleId)) {
            throw new BadRequestException("Meta provenance with id [" + metaBundleId + "] does not exist!");
        }

        var metaBundles = entity.getContains();
        if (metaBundles == null || metaBundles.size() != 1) {
            throw new BadRequestException("Entity cannot be part of more than one meta bundles");
        }
        Bundle metaBundle = metaBundles.getFirst();
        if (!metaBundleId.equals(metaBundle.getIdentifier())) {
            throw new BadRequestException(
                    "Graph with id [" + documentId + "] is part of meta bundle with id [" + metaBundle.getIdentifier() + "]," +
                            " however main_activity from given bundle is resolvable to different id [" + metaBundleId + "]");
        }

        String documentIdentifier = organizationId + "_" + documentId;
        if (!documentRepository.existsById(documentIdentifier)) {
            throw new NotFoundException(
                    "Document with id [" + documentId + "] does not exist." +
                            "Please check whether the ID you have given is correct.");
        }
    }

    public Object getDocument(String organizationId, String documentId) {
        String identifier = organizationId + "_" + documentId;
        Document document = documentRepository.findById(identifier)
                .orElseThrow(() -> new NotFoundException(
                        "Document with id [" + documentId + "] does not exist under organization [" + organizationId + "]."));

        Map<String, Object> response = new HashMap<>();
        response.put("document", document.getGraph());

        if (appProperties.isDisableTrustedParty()) {
            return response;
        }

        boolean registered = organizationService.isRegistered(organizationId);
        var tokenOpt = registered
                ? findTokenForRegisteredOrg(identifier, organizationId)
                : tokenRepository.findLatestByDocumentIdentifierAndDefaultTp(identifier);
        if (tokenOpt.isPresent()) {
            response.put("token", toTokenEnvelope(tokenOpt.get()));
            return response;
        }

        Map<String, Object> payload = new HashMap<>();
        payload.put("graph", document.getGraph());

        String tpUrl = registered
                ? organizationService.getTpUrlByOrganization(organizationId)
                : appProperties.getTpFqdn();
        ResponseEntity<String> tokenResp = tpClient.issueToken(payload, tpUrl);
        if (!tokenResp.getStatusCode().is2xxSuccessful()) {
            throw new IllegalStateException("Could not issue token.");
        }

        Map<String, Object> tokenEnvelope = TokenUtils.parseTokenResponse(tokenResp.getBody());
        storeTokenIntoDb(tokenEnvelope, organizationId, documentId);
        response.put("token", tokenEnvelope);
        return response;
    }

    private java.util.Optional<Token> findTokenForRegisteredOrg(String documentIdentifier, String organizationId) {
        TrustedParty tp = organizationService.getTrustedPartyForOrganization(organizationId);
        if (tp == null || tp.getIdentifier() == null) {
            return java.util.Optional.empty();
        }
        return tokenRepository.findLatestByDocumentIdentifierAndTpId(documentIdentifier, tp.getIdentifier());
    }

    private TrustedParty resolveTrustedParty(String authorityId, String organizationId) {
        if (authorityId != null) {
            TrustedParty tp = trustedPartyRepository.findById(authorityId).orElse(null);
            if (tp != null) {
                return tp;
            }
            return getOrCreateDefaultTrustedParty(authorityId);
        }

        TrustedParty orgTp = organizationService.getTrustedPartyForOrganization(organizationId);
        if (orgTp != null) {
            return orgTp;
        }
        return getOrCreateDefaultTrustedParty(null);
    }

    private TrustedParty getOrCreateDefaultTrustedParty(String authorityId) {
        String id = (authorityId == null || authorityId.isBlank()) ? "DefaultTrustedParty" : authorityId;
        return defaultTrustedPartyRepository.findById(id)
                .map(tp -> (TrustedParty) tp)
                .orElseGet(() -> {
                    var tp = new cz.muni.fi.distributed_prov_system.data.model.nonprovmodels.DefaultTrustedParty();
                    tp.setIdentifier(id);
                    tp.setUrl(appProperties.getTpFqdn());
                    return defaultTrustedPartyRepository.save(tp);
                });
    }

    private Map<String, Object> toTokenEnvelope(Token token) {
        Map<String, Object> data = new HashMap<>();
        data.put("originatorId", token.getOriginatorId());
        data.put("authorityId", token.getAuthorityId());
        data.put("tokenTimestamp", token.getTokenTimestamp());
        data.put("documentCreationTimestamp", token.getMessageTimestamp());
        data.put("documentDigest", token.getDocumentDigest());
        data.put("additionalData", token.getAdditionalData());

        Map<String, Object> envelope = new HashMap<>();
        envelope.put("data", data);
        envelope.put("signature", token.getSignature());
        return envelope;
    }

    public boolean documentExists(String organizationId, String documentId) {
        String identifier = organizationId + "_" + documentId;
        return documentRepository.existsById(identifier);
    }

    public SubgraphResponseDTO getDomainSpecificSubgraph(String organizationId, String documentId, String format) {
        return getSubgraph(organizationId, documentId, format, "_domain", "domain_specific", true);
    }

    public SubgraphResponseDTO getBackboneSubgraph(String organizationId, String documentId, String format) {
        return getSubgraph(organizationId, documentId, format, "_backbone", "backbone", false);
    }

    private SubgraphResponseDTO getSubgraph(String organizationId,
                                            String documentId,
                                            String format,
                                            String suffix,
                                            String type,
                                            boolean domainSpecific) {
        String requestedFormat = normalizeSubgraphFormat(format);
        String identifier = organizationId + "_" + documentId + suffix;

        Document subgraphDoc = documentRepository.findByIdentifierAndFormat(identifier, requestedFormat)
                .orElseGet(() -> createSubgraph(organizationId, documentId, requestedFormat, suffix, domainSpecific));

        if (appProperties.isDisableTrustedParty()) {
            return new SubgraphResponseDTO(subgraphDoc.getGraph(), null);
        }

        var tokenOpt = tokenRepository.findLatestByDocumentIdentifier(identifier);
        if (tokenOpt.isPresent()) {
            return new SubgraphResponseDTO(subgraphDoc.getGraph(), toJsonNode(toTokenEnvelope(tokenOpt.get())));
        }

        Map<String, Object> payload = new HashMap<>();
        payload.put("document", subgraphDoc.getGraph());
        payload.put("createdOn", Instant.now().getEpochSecond());
        payload.put("type", type);
        payload.put("organizationId", organizationId);
        payload.put("documentFormat", requestedFormat);
        payload.put("doc_format", requestedFormat);
        payload.put("graphId", documentId);

        String tpUrl = organizationService.getTpUrlByOrganization(organizationId);
        ResponseEntity<String> tokenResp = tpClient.issueToken(payload, tpUrl);
        if (!tokenResp.getStatusCode().is2xxSuccessful()) {
            throw new IllegalStateException("Could not issue token.");
        }

        Map<String, Object> tokenEnvelope = TokenUtils.parseTokenResponse(tokenResp.getBody());
        storeTokenIntoDb(tokenEnvelope, organizationId, documentId + suffix);
        return new SubgraphResponseDTO(subgraphDoc.getGraph(), toJsonNode(tokenEnvelope));
    }

    private String normalizeSubgraphFormat(String format) {
        String requestedFormat = (format == null || format.isBlank()) ? "rdf" : format.toLowerCase();
        if (!requestedFormat.equals("rdf") && !requestedFormat.equals("json")
                && !requestedFormat.equals("xml") && !requestedFormat.equals("provn")) {
            throw new BadRequestException("Requested format [" + format + "] is not supported!");
        }
        return requestedFormat;
    }

    private Document createSubgraph(String organizationId,
                                    String documentId,
                                    String requestedFormat,
                                    String suffix,
                                    boolean domainSpecific) {
        String mainId = organizationId + "_" + documentId;
        Document mainDocument = documentRepository.findById(mainId)
                .orElseThrow(() -> new NotFoundException(
                        "Document with id [" + documentId + "] does not exist under organization [" + organizationId + "]."));

        String subgraphBase64 = SubgraphUtils.buildSubgraphBase64(
                mainDocument.getGraph(),
                mainDocument.getFormat(),
                requestedFormat,
                domainSpecific
        );

        Document subgraphDoc = new Document();
        subgraphDoc.setIdentifier(organizationId + "_" + documentId + suffix);
        subgraphDoc.setFormat(requestedFormat);
        subgraphDoc.setGraph(subgraphBase64);

        return documentRepository.save(subgraphDoc);
    }
}
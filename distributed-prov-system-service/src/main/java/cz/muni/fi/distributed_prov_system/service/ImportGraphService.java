package cz.muni.fi.distributed_prov_system.service;

import cz.muni.fi.distributed_prov_system.api.StoreGraphRequestDTO;
import cz.muni.fi.distributed_prov_system.config.AppProperties;
import cz.muni.fi.distributed_prov_system.data.model.nodes.*;
import cz.muni.fi.distributed_prov_system.data.model.nonprovmodels.Document;
import cz.muni.fi.distributed_prov_system.data.model.relationships.*;
import cz.muni.fi.distributed_prov_system.data.repository.*;
import cz.muni.fi.distributed_prov_system.exceptions.NotFoundException;
import cz.muni.fi.distributed_prov_system.utils.MetaProvenanceUtils;
import cz.muni.fi.distributed_prov_system.utils.ProvConstants;
import cz.muni.fi.distributed_prov_system.utils.TokenUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ImportGraphService {

    private final DocumentRepository documentRepository;
    private final BundleRepository bundleRepository;
    private final EntityRepository entityRepository;
    private final ActivityRepository activityRepository;
    private final AgentRepository agentRepository;
    private final AppProperties appProperties;

    @Autowired
    public ImportGraphService(DocumentRepository documentRepository,
                              BundleRepository bundleRepository,
                              EntityRepository entityRepository,
                              ActivityRepository activityRepository,
                              AgentRepository agentRepository,
                              AppProperties appProperties) {
        this.documentRepository = documentRepository;
        this.bundleRepository = bundleRepository;
        this.entityRepository = entityRepository;
        this.activityRepository = activityRepository;
        this.agentRepository = agentRepository;
        this.appProperties = appProperties;
    }

    @Transactional
    public void importGraph(StoreGraphRequestDTO body,
                            Map<String, Object> tokenEnvelope,
                            String organizationId,
                            String documentId,
                            String metaBundleId,
                            boolean isUpdate) {
        String identifier = organizationId + "_" + documentId;

        Document neoDocument = new Document();
        neoDocument.setIdentifier(identifier);
        neoDocument.setGraph(body.getDocument());
        neoDocument.setFormat(body.getDocumentFormat());

        Map<String, Object> tokenData = TokenUtils.normalizeTokenData(tokenEnvelope);

        if (isUpdate) {
            updateMetaProvenance(identifier, metaBundleId, tokenData, documentId, neoDocument);
        } else {
            createMetaProvenance(identifier, metaBundleId, tokenData, neoDocument);
        }
    }

    private void createMetaProvenance(String identifier,
                                      String metaBundleId,
                                      Map<String, Object> tokenData,
                                      Document document) {
        Bundle metaBundle = bundleRepository.findById(metaBundleId).orElse(null);
        boolean metaBundleNew = false;
        if (metaBundle == null) {
            metaBundle = new Bundle();
            metaBundle.setIdentifier(metaBundleId);
            metaBundleNew = true;
        }

        Entity genEntity = new Entity();
        genEntity.setIdentifier(MetaProvenanceUtils.computeGenEntityId(identifier));
        Map<String, Object> genAttributes = new HashMap<>();
        genAttributes.put(ProvConstants.PROV_TYPE, ProvConstants.PROV_ATTR_BUNDLE);
        genEntity.setAttributes(genAttributes);

        Entity firstVersion = new Entity();
        firstVersion.setIdentifier(identifier);
        Map<String, Object> firstAttrs = new HashMap<>();
        firstAttrs.put(ProvConstants.PROV_TYPE, ProvConstants.PROV_ATTR_BUNDLE);
        firstAttrs.put(ProvConstants.PAV_VERSION, 1);
        firstVersion.setAttributes(firstAttrs);
        firstVersion.setSpecializationOf(List.of(genEntity));

        TokenMetaResult tokenMeta = storeTokenIntoMeta(metaBundle, firstVersion, tokenData);

        documentRepository.save(document);
        entityRepository.save(genEntity);
        entityRepository.save(firstVersion);
        if (metaBundleNew) {
            bundleRepository.save(metaBundle);
        }
        addToBundle(metaBundle, genEntity, firstVersion, tokenMeta.tokenEntity, tokenMeta.activity, tokenMeta.agent);
        bundleRepository.save(metaBundle);

        if (tokenMeta.newAgent) {
            agentRepository.save(tokenMeta.agent);
        }

        activityRepository.save(tokenMeta.activity);
        entityRepository.save(tokenMeta.tokenEntity);
        entityRepository.save(firstVersion);
    }

    private void updateMetaProvenance(String identifier,
                                      String metaBundleId,
                                      Map<String, Object> tokenData,
                                      String documentId,
                                      Document document) {
        Bundle metaBundle = bundleRepository.findById(metaBundleId)
                .orElseThrow(() -> new NotFoundException("Meta bundle [" + metaBundleId + "] not found."));

        String targetEntityId = tokenData.get("originatorId") + "_" + documentId;
        Entity entityToUpdate = entityRepository.findById(targetEntityId)
                .orElseThrow(() -> new NotFoundException("Entity [" + targetEntityId + "] not found."));

        List<Entity> genEntities = entityToUpdate.getSpecializationOf();
        if (genEntities == null || genEntities.size() != 1) {
            throw new IllegalStateException("Expected exactly one generator entity for version chain.");
        }
        Entity genEntity = genEntities.get(0);

        Object versionObj = entityToUpdate.getAttributes() != null
                ? entityToUpdate.getAttributes().get(ProvConstants.PAV_VERSION)
                : null;
        int lastVersion = versionObj == null ? 0 : Integer.parseInt(versionObj.toString());

        Entity newVersion = new Entity();
        newVersion.setIdentifier(identifier);
        Map<String, Object> newAttrs = new HashMap<>(entityToUpdate.getAttributes());
        newAttrs.put(ProvConstants.PAV_VERSION, lastVersion + 1);
        newVersion.setAttributes(newAttrs);
        newVersion.setSpecializationOf(List.of(genEntity));

        WasRevisionOf wasRevisionOf = new WasRevisionOf();
        wasRevisionOf.setEntity(entityToUpdate);
        newVersion.setWasRevisionOf(List.of(wasRevisionOf));

        TokenMetaResult tokenMeta = storeTokenIntoMeta(metaBundle, newVersion, tokenData);

        documentRepository.save(document);
        entityRepository.save(newVersion);
        activityRepository.save(tokenMeta.activity);
        entityRepository.save(tokenMeta.tokenEntity);
        if (tokenMeta.newAgent) {
            agentRepository.save(tokenMeta.agent);
        }

        addToBundle(metaBundle, newVersion, tokenMeta.tokenEntity, tokenMeta.activity, tokenMeta.agent);
        bundleRepository.save(metaBundle);
    }

    private TokenMetaResult storeTokenIntoMeta(Bundle metaBundle, Entity entity, Map<String, Object> tokenData) {
        Map<String, Object> tokenAttributes = new HashMap<>();
        for (Map.Entry<String, Object> entry : tokenData.entrySet()) {
            if ("additionalData".equals(entry.getKey()) && entry.getValue() instanceof Map<?, ?> additionalData) {
                for (Map.Entry<?, ?> ad : additionalData.entrySet()) {
                    tokenAttributes.put(ProvConstants.cpm(ad.getKey().toString()), ad.getValue());
                }
                continue;
            }
            tokenAttributes.put(ProvConstants.cpm(entry.getKey()), entry.getValue());
        }
        tokenAttributes.put(ProvConstants.PROV_TYPE, ProvConstants.CPM_TOKEN);

        Entity tokenEntity = new Entity();
        tokenEntity.setIdentifier(entity.getIdentifier() + "_token");
        tokenEntity.setAttributes(tokenAttributes);

        AgentResult agentResult = getTpAgent(metaBundle, tokenData);

        Activity activity = new Activity();
        activity.setIdentifier(entity.getIdentifier() + "_tokenGeneration");
        LocalDateTime time = MetaProvenanceUtils.toLocalDateTimeFromEpochSeconds(tokenData.get("tokenTimestamp"));
        activity.setStartTime(time);
        activity.setEndTime(time);
        Map<String, Object> activityAttrs = new HashMap<>();
        activityAttrs.put(ProvConstants.PROV_TYPE, ProvConstants.CPM_TOKEN_GENERATION);
        activity.setAttributes(activityAttrs);

        Used used = new Used();
        used.setEntity(entity);
        activity.setUsed(List.of(used));

        WasAssociatedWith associatedWith = new WasAssociatedWith();
        associatedWith.setAgent(agentResult.agent);
        activity.setWasAssociatedWith(List.of(associatedWith));

        WasGeneratedBy wasGeneratedBy = new WasGeneratedBy();
        wasGeneratedBy.setActivity(activity);
        tokenEntity.setWasGeneratedBy(List.of(wasGeneratedBy));

        WasAttributedTo wasAttributedTo = new WasAttributedTo();
        wasAttributedTo.setAgent(agentResult.agent);
        tokenEntity.setWasAttributedTo(List.of(wasAttributedTo));

        return new TokenMetaResult(activity, agentResult.agent, agentResult.newAgent, tokenEntity);
    }

    private AgentResult getTpAgent(Bundle metaBundle, Map<String, Object> tokenData) {
        String authorityId = tokenData.get("authorityId").toString();
        Agent agent = agentRepository.findById(authorityId).orElse(null);
        boolean newAgent = false;

        if (agent == null) {
            agent = new Agent();
            agent.setIdentifier(authorityId);

            Map<String, Object> attrs = new HashMap<>();
            attrs.put(ProvConstants.PROV_TYPE, ProvConstants.CPM_TRUSTED_PARTY);

            if (!appProperties.isDisableTrustedParty()) {
                Object additionalDataObj = tokenData.get("additionalData");
                if (additionalDataObj instanceof Map<?, ?> additionalData) {
                    Object uri = additionalData.get("trustedPartyUri");
                    if (uri != null) {
                        attrs.put(ProvConstants.CPM_TRUSTED_PARTY_URI, uri);
                    }
                    Object cert = additionalData.get("trustedPartyCertificate");
                    if (cert != null) {
                        attrs.put(ProvConstants.CPM_TRUSTED_PARTY_CERTIFICATE, cert);
                    }
                }
            }

            agent.setAttributes(attrs);
            newAgent = true;
        }

        return new AgentResult(agent, newAgent);
    }

    private void addToBundle(Bundle metaBundle, BaseProv... provs) {
        List<BaseProv> contains = metaBundle.getContainsProvs();
        if (contains == null) {
            contains = new ArrayList<>();
            metaBundle.setContainsProvs(contains);
        }
        for (BaseProv prov : provs) {
            if (prov != null && !contains.contains(prov)) {
                contains.add(prov);
            }
        }
    }

    private record TokenMetaResult(Activity activity, Agent agent, boolean newAgent, Entity tokenEntity) {
    }

    private record AgentResult(Agent agent, boolean newAgent) {
    }
}

package cz.muni.fi.distributed_prov_system.facade;

import cz.muni.fi.distributed_prov_system.api.MetaResponseDTO;
import cz.muni.fi.distributed_prov_system.service.MetaService;
import cz.muni.fi.distributed_prov_system.exceptions.MetaNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class MetaFacadeImpl implements MetaFacade {

    private final MetaService metaService;

    @Autowired
    public MetaFacadeImpl(MetaService metaService) {
        this.metaService = metaService;
    }

    @Override
    public boolean metaBundleExists(String metaId) {
        return metaService.metaBundleExists(metaId);
    }

    @Override
    public MetaResponseDTO getMeta(String metaId, String format, String organizationId) {
        // Validate format
        if (!format.equals("rdf") && !format.equals("json") && !format.equals("xml") && !format.equals("provn")) {
            throw new IllegalArgumentException("Requested format [" + format + "] is not supported!");
        }

        // Retrieve meta provenance graph (base64-encoded)
        String graph;
        try {
            graph = metaService.getB64EncodedMetaProvenance(metaId, format);
        } catch (MetaNotFoundException ex) {
            throw ex;
        }

        // If Trusted Party is enabled, get token
        if (!metaService.isTrustedPartyDisabled()) {
            String tpUrl = organizationId != null
                    ? metaService.getTpUrlByOrganization(organizationId)
                    : null;

            Object payload = metaService.buildMetaTokenPayload(graph, metaId, format, organizationId);
            Object token = metaService.sendTokenRequestToTp(payload, tpUrl);

            return new MetaResponseDTO(graph, token);
        } else {
            return new MetaResponseDTO(graph, null);
        }
    }
}
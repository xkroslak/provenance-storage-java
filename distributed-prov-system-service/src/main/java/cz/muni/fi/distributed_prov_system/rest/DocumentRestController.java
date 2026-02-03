package cz.muni.fi.distributed_prov_system.rest;

import cz.muni.fi.distributed_prov_system.api.StoreGraphRequestDTO;
import cz.muni.fi.distributed_prov_system.api.StoreGraphResponseDTO;
import cz.muni.fi.distributed_prov_system.api.SubgraphResponseDTO;
import cz.muni.fi.distributed_prov_system.facade.DocumentFacade;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;

import org.springframework.web.bind.annotation.*;

@Tag(name = "Documents", description = "Documents actions")
@RestController
@RequestMapping("/api/v1/organizations/{organizationId}/documents")
public class DocumentRestController {

    private final DocumentFacade documentFacade;

    @Autowired
    public DocumentRestController(DocumentFacade documentFacade) {
        this.documentFacade = documentFacade;
    }

    @PostMapping("/{documentId}")
    public ResponseEntity<StoreGraphResponseDTO> storeDocument(@PathVariable String organizationId,
                                                               @PathVariable String documentId,
                                                               @Valid @RequestBody StoreGraphRequestDTO body) {
        var result = documentFacade.storeDocument(organizationId, documentId, body);
        return ResponseEntity.status(201).body(result);
    }

    @PutMapping("/{documentId}")
    public ResponseEntity<StoreGraphResponseDTO> updateDocument(@PathVariable String organizationId,
                                                                @PathVariable String documentId,
                                                                @Valid @RequestBody StoreGraphRequestDTO body) {
        var result = documentFacade.updateDocument(organizationId, documentId, body);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/{documentId}")
    public ResponseEntity<?> getDocument(@PathVariable String organizationId,
                                         @PathVariable String documentId) {
        var result = documentFacade.getDocument(organizationId, documentId);
        return ResponseEntity.ok(result);
    }

    @RequestMapping(path = "/{documentId}", method = RequestMethod.HEAD)
    public ResponseEntity<Void> headDocument(@PathVariable String organizationId,
                                             @PathVariable String documentId) {
        boolean exists = documentFacade.documentExists(organizationId, documentId);
        return exists ? ResponseEntity.ok().build() : ResponseEntity.notFound().build();
    }

    @GetMapping("/{documentId}/domain-specific")
    public ResponseEntity<SubgraphResponseDTO> getDomainSpecificSubgraph(@PathVariable String organizationId,
                                                                         @PathVariable String documentId,
                                                                         @RequestParam(name = "format", defaultValue = "rdf") String format) {
        var result = documentFacade.getDomainSpecificSubgraph(organizationId, documentId, format);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/{documentId}/backbone")
    public ResponseEntity<SubgraphResponseDTO> getBackboneSubgraph(@PathVariable String organizationId,
                                                                   @PathVariable String documentId,
                                                                   @RequestParam(name = "format", defaultValue = "rdf") String format) {
        var result = documentFacade.getBackboneSubgraph(organizationId, documentId, format);
        return ResponseEntity.ok(result);
    }
}
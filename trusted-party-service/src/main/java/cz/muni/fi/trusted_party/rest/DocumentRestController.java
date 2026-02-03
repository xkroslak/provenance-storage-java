package cz.muni.fi.trusted_party.rest;

import cz.muni.fi.trusted_party.api.Document.DocumentDTO;
import cz.muni.fi.trusted_party.facade.DocumentFacade;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Documents", description = "Actions for documents")
@RestController
@RequestMapping("/api/v1")
public class DocumentRestController {

    private final DocumentFacade documentFacade;

    @Autowired
    public DocumentRestController(DocumentFacade documentFacade) {
        this.documentFacade = documentFacade;
    }

    @GetMapping("/organizations/{orgId}/documents/{docId}/{docFormat}")
    public ResponseEntity<DocumentDTO> getDocument(
            @PathVariable String orgId,
            @PathVariable String docId,
            @PathVariable String docFormat) {
        DocumentDTO response = documentFacade.getDocument(orgId, docId, docFormat);
        return ResponseEntity.ok(response);
    }
}
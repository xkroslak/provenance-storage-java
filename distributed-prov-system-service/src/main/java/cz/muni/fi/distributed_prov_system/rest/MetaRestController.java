package cz.muni.fi.distributed_prov_system.rest;

import cz.muni.fi.distributed_prov_system.exceptions.MetaNotFoundException;
import cz.muni.fi.distributed_prov_system.facade.MetaFacade;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Meta", description = "Meta data")
@RestController
@RequestMapping("/api/v1/documents/meta")
public class MetaRestController {

    private final MetaFacade metaFacade;

    @Autowired
    public MetaRestController(MetaFacade metaFacade) {
        this.metaFacade = metaFacade;
    }

    @RequestMapping(value = "/{metaId}", method = RequestMethod.HEAD)
    public ResponseEntity<Void> headMeta(@PathVariable String metaId) {
        boolean exists = metaFacade.metaBundleExists(metaId);
        return exists ? ResponseEntity.ok().build() : ResponseEntity.notFound().build();
    }

    @GetMapping("/{metaId}")
    public ResponseEntity<?> getMeta(@PathVariable String metaId,
                                     @RequestParam(name = "format", defaultValue = "rdf") String format,
                                     @RequestParam(name = "organizationId", required = false) String organizationId) {
        // Validate format
        if (!format.equals("rdf") && !format.equals("json") && !format.equals("xml") && !format.equals("provn")) {
            return ResponseEntity.badRequest().body(
                    new ErrorResponse("Requested format [" + format + "] is not supported!")
            );
        }

        try {
            var result = metaFacade.getMeta(metaId, format, organizationId);
            return ResponseEntity.ok(result);
        } catch (MetaNotFoundException ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ErrorResponse("The meta-provenance with id [" + metaId + "] does not exist."));
        }
    }

    // DTO for error responses
    public static class ErrorResponse {
        public String error;
        public ErrorResponse(String error) { this.error = error; }
    }
}
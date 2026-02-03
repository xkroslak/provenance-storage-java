package cz.muni.fi.distributed_prov_system.rest;

import cz.muni.fi.distributed_prov_system.api.RegisterOrganizationRequestDTO;
import cz.muni.fi.distributed_prov_system.facade.OrganizationFacade;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Organizations", description = "Organization registration and update")
@RestController
@RequestMapping("/api/v1/organizations")
public class OrganizationRestController {

    private final OrganizationFacade organizationFacade;

    @Autowired
    public OrganizationRestController(OrganizationFacade organizationFacade) {
        this.organizationFacade = organizationFacade;
    }

    @PostMapping("/{organizationId}")
    public ResponseEntity<Void> register(@PathVariable String organizationId,
                                         @Valid @RequestBody RegisterOrganizationRequestDTO body) {
        organizationFacade.register(organizationId, body);
        return ResponseEntity.status(201).build();
    }

    @PutMapping("/{organizationId}")
    public ResponseEntity<Void> modify(@PathVariable String organizationId,
                                       @Valid @RequestBody RegisterOrganizationRequestDTO body) {
        organizationFacade.modify(organizationId, body);
        return ResponseEntity.ok().build();
    }
}
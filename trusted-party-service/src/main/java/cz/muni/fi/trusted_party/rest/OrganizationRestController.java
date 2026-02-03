package cz.muni.fi.trusted_party.rest;

import cz.muni.fi.trusted_party.api.Organization.OrganizationDTO;
import cz.muni.fi.trusted_party.api.Organization.StoreCertOrganizationDTO;
import cz.muni.fi.trusted_party.facade.OrganizationFacade;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Organizations", description = "")
@RestController
@RequestMapping("/api/v1/organizations")
public class OrganizationRestController {

    private final OrganizationFacade organizationFacade;

    @Autowired
    public OrganizationRestController(OrganizationFacade organizationFacade) {
        this.organizationFacade = organizationFacade;
    }

    @GetMapping
    public ResponseEntity<List<OrganizationDTO>> getAllOrganizations() {
        List<OrganizationDTO> organizations = organizationFacade.getAllOrganizations();
        return ResponseEntity.ok(organizations);
    }

    @GetMapping("/{organizationId}")
    public ResponseEntity<OrganizationDTO> getOrganization(@PathVariable String organizationId) {
        OrganizationDTO response = organizationFacade.getOrganization(organizationId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{organizationId}")
    public ResponseEntity<Void> registerOrganization(@PathVariable String organizationId,
                                                  @RequestBody @Valid StoreCertOrganizationDTO body) {
        organizationFacade.registerOrganization(organizationId, body);
        return ResponseEntity.status(201).build();
    }

    @GetMapping("/{organizationId}/certs")
    public ResponseEntity<OrganizationDTO> retrieveCertificates(@PathVariable String organizationId) {
        OrganizationDTO orgWithAllCerts = organizationFacade.retrieveCertificates(organizationId);
        return ResponseEntity.ok(orgWithAllCerts);
    }

    @PutMapping("/{organizationId}/certs")
    public ResponseEntity<?> updateCertificates(@PathVariable String organizationId,
                                                @RequestBody @Valid StoreCertOrganizationDTO body) {
        organizationFacade.updateCertificates(organizationId, body);
        return ResponseEntity.status(201).build();
    }
}
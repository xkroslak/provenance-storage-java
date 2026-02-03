package cz.muni.fi.trusted_party.rest;

import cz.muni.fi.trusted_party.api.InfoResponseDTO;
import cz.muni.fi.trusted_party.facade.InfoFacade;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Info", description = "")
@RestController
@RequestMapping("/api/v1/info")
public class InfoRestController {
    private final InfoFacade infoFacade;

    @Autowired
    public InfoRestController(InfoFacade infoFacade) {
        this.infoFacade = infoFacade;
    }

    @GetMapping()
    public ResponseEntity<InfoResponseDTO> getInfo() {
        return ResponseEntity.ok(infoFacade.getInfo());
    }
}
package cz.muni.fi.trusted_party.rest;

import cz.muni.fi.trusted_party.api.Token.TokenDTO;
import cz.muni.fi.trusted_party.api.Token.TokenRequestDTO;
import cz.muni.fi.trusted_party.api.TokenResponseDTO;
import cz.muni.fi.trusted_party.facade.TokenFacade;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Tokens", description = "Actions for tokens")
@RestController
@RequestMapping("/api/v1")
public class TokenRestController {

    private final TokenFacade tokenFacade;

    @Autowired
    public TokenRestController(TokenFacade tokenFacade) {
        this.tokenFacade = tokenFacade;
    }

    @GetMapping("/organizations/{orgId}/tokens/{docId}/{docFormat}")
    public ResponseEntity<List<TokenDTO>> getToken(
            @PathVariable String orgId,
            @PathVariable String docId,
            @PathVariable String docFormat) {
        List<TokenDTO> response = tokenFacade.getToken(orgId, docId, docFormat);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/organizations/{orgId}/tokens")
    public ResponseEntity<List<TokenDTO>> getAllTokens(@PathVariable String orgId) {
        return ResponseEntity.ok(tokenFacade.getAllTokens(orgId));
    }

    @PostMapping("/issueToken")
    public ResponseEntity<List<TokenDTO>> issueToken(@RequestBody @Valid TokenRequestDTO body) {
        List<TokenDTO> response = tokenFacade.issueToken(body);
        return ResponseEntity.status(201).body(response);
    }

    @PostMapping("/verifySignature")
    public ResponseEntity<Void> verifySignature(
            @RequestBody TokenRequestDTO body) {
        if (tokenFacade.verifySignature(body)) {
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.badRequest().build();
    }
}
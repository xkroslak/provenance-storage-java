package cz.muni.fi.trusted_party.facade;

import cz.muni.fi.trusted_party.api.Token.TokenDTO;
import cz.muni.fi.trusted_party.api.Token.TokenRequestDTO;

import java.util.List;

public interface TokenFacade {
    List<TokenDTO> getToken(String organizationId, String documentId, String documentFormat);
    List<TokenDTO> getAllTokens(String organizationId);
    List<TokenDTO> issueToken(TokenRequestDTO body);
    boolean verifySignature(TokenRequestDTO body);
}
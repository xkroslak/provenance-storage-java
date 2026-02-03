package cz.muni.fi.trusted_party.facade;

import cz.muni.fi.trusted_party.api.Token.TokenDTO;
import cz.muni.fi.trusted_party.api.Token.TokenRequestDTO;
import cz.muni.fi.trusted_party.config.AppProperties;
import cz.muni.fi.trusted_party.mappers.TokenMapper;
import cz.muni.fi.trusted_party.data.model.Document;
import cz.muni.fi.trusted_party.data.model.Token;
import cz.muni.fi.trusted_party.service.TokenService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class TokenFacadeImpl implements TokenFacade {

    private final TokenService tokenService;
    private final AppProperties appProperties;
    private final TokenMapper tokenMapper;

    @Autowired
    public TokenFacadeImpl(TokenService tokenService, AppProperties appProperties, TokenMapper tokenMapper) {
        this.tokenService = tokenService;
        this.appProperties = appProperties;
        this.tokenMapper = tokenMapper;
    }

    @Override
    public List<TokenDTO> getToken(String organizationId, String documentId, String documentFormat) {
        List<Token> tokens = tokenService.getToken(organizationId, documentId, documentFormat);

        return tokenMapper.mapToList(tokens, appProperties);
    }

    @Override
    public List<TokenDTO> getAllTokens(String organizationId) {
        Map<Document, List<Token>> tokensByDoc = tokenService.getAllTokens(organizationId);

        List<TokenDTO> result = new ArrayList<>();

        //Todo: check if I need document
        for (Map.Entry<Document, List<Token>> entry : tokensByDoc.entrySet()) {
            Document document = entry.getKey();
            List<Token> tokens = entry.getValue();

            for (Token token : tokens) {
                TokenDTO dto = tokenMapper.mapToDTO(token, appProperties);
                result.add(dto);
            }
        }
        return result;
    }

    @Override
    public List<TokenDTO> issueToken(TokenRequestDTO body) {
        return tokenMapper.mapToList(tokenService.issueToken(body), appProperties);
    }

    @Override
    public boolean verifySignature(TokenRequestDTO body) {
        return tokenService.verifySignature(body);
    }
}
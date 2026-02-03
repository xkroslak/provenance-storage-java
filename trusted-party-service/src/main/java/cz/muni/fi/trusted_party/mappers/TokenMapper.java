package cz.muni.fi.trusted_party.mappers;

import cz.muni.fi.trusted_party.api.Token.TokenDTO;
import cz.muni.fi.trusted_party.config.AppProperties;
import cz.muni.fi.trusted_party.data.model.Token;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class TokenMapper {

    public TokenDTO mapToDTO(Token token, AppProperties appProperties) {
        TokenDTO.Data.AdditionalData additionalData = new TokenDTO.Data.AdditionalData();
        additionalData.setBundle(token.getDocument().getIdentifier());
        additionalData.setHashFunction("SHA256");
        additionalData.setTrustedPartyUri(appProperties.getFqdn());
        additionalData.setTrustedPartyCertificate(appProperties.getCertificate());

        TokenDTO.Data data = new TokenDTO.Data();
        data.setOriginatorId(token.getDocument().getOrganization().getOrgName());
        data.setAuthorityId(appProperties.getId());
        data.setTokenTimestamp(token.getCreatedOn());
        data.setDocumentCreationTimestamp(token.getDocument().getCreatedOn());
        data.setDocumentDigest(token.getHash());
        data.setAdditionalData(additionalData);

        TokenDTO tokenDTO = new TokenDTO();
        tokenDTO.setData(data);
        tokenDTO.setSignature(token.getSignature());
        return tokenDTO;
    }

    public List<TokenDTO> mapToList(List<Token> tokens, AppProperties appProperties) {
        return tokens
                .stream()
                .map(token -> mapToDTO(token, appProperties))
                .toList();
    }
}

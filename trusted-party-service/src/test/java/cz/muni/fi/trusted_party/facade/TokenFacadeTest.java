package cz.muni.fi.trusted_party.facade;

import cz.muni.fi.trusted_party.api.Token.TokenDTO;
import cz.muni.fi.trusted_party.api.Token.TokenRequestDTO;
import cz.muni.fi.trusted_party.config.AppProperties;
import cz.muni.fi.trusted_party.data.model.Document;
import cz.muni.fi.trusted_party.data.model.Token;
import cz.muni.fi.trusted_party.mappers.TokenMapper;
import cz.muni.fi.trusted_party.service.TokenService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TokenFacadeTest {

    @Mock
    private TokenService tokenService;

    @Mock
    private AppProperties appProperties;

    @Mock
    private TokenMapper tokenMapper;

    @InjectMocks
    private TokenFacadeImpl tokenFacade;

    @Test
    void getToken_returnsMappedList() {
        Token token = new Token();
        TokenDTO dto = new TokenDTO();

        when(tokenService.getToken("org-1", "doc-1", "json")).thenReturn(List.of(token));
        when(tokenMapper.mapToList(List.of(token), appProperties)).thenReturn(List.of(dto));

        List<TokenDTO> result = tokenFacade.getToken("org-1", "doc-1", "json");

        assertThat(result).containsExactly(dto);
    }

    @Test
    void getAllTokens_returnsFlattenedList() {
        Document document = new Document();
        Token tokenA = new Token();
        tokenA.setHash("hash-a");
        Token tokenB = new Token();
        tokenB.setHash("hash-b");
        TokenDTO dtoA = new TokenDTO();
        TokenDTO dtoB = new TokenDTO();

        when(tokenService.getAllTokens("org-1"))
                .thenReturn(Map.of(document, List.of(tokenA, tokenB)));
        when(tokenMapper.mapToDTO(tokenA, appProperties)).thenReturn(dtoA);
        when(tokenMapper.mapToDTO(tokenB, appProperties)).thenReturn(dtoB);

        List<TokenDTO> result = tokenFacade.getAllTokens("org-1");

        assertThat(result).containsExactlyInAnyOrder(dtoA, dtoB);
    }

    @Test
    void issueToken_returnsMappedList() {
        TokenRequestDTO body = new TokenRequestDTO();
        Token token = new Token();
        TokenDTO dto = new TokenDTO();

        when(tokenService.issueToken(body)).thenReturn(List.of(token));
        when(tokenMapper.mapToList(List.of(token), appProperties)).thenReturn(List.of(dto));

        List<TokenDTO> result = tokenFacade.issueToken(body);

        assertThat(result).containsExactly(dto);
    }

    @Test
    void verifySignature_delegatesToService() {
        TokenRequestDTO body = new TokenRequestDTO();
        when(tokenService.verifySignature(body)).thenReturn(true);

        boolean result = tokenFacade.verifySignature(body);

        assertThat(result).isTrue();
        verify(tokenService).verifySignature(body);
    }
}

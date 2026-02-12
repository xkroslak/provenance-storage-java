package cz.muni.fi.trusted_party.service;

import cz.muni.fi.trusted_party.api.Token.TokenRequestDTO;
import cz.muni.fi.trusted_party.config.AppProperties;
import cz.muni.fi.trusted_party.data.enums.DocumentType;
import cz.muni.fi.trusted_party.data.model.Document;
import cz.muni.fi.trusted_party.data.model.Organization;
import cz.muni.fi.trusted_party.data.model.Token;
import cz.muni.fi.trusted_party.data.repository.CertificateRepository;
import cz.muni.fi.trusted_party.data.repository.DocumentRepository;
import cz.muni.fi.trusted_party.data.repository.OrganizationRepository;
import cz.muni.fi.trusted_party.data.repository.TokenRepository;
import cz.muni.fi.trusted_party.exceptions.CertificateNotFoundException;
import cz.muni.fi.trusted_party.exceptions.DocumentNotFoundException;
import cz.muni.fi.trusted_party.exceptions.InvalidRequestException;
import cz.muni.fi.trusted_party.exceptions.InvalidTimestampException;
import cz.muni.fi.trusted_party.exceptions.MissingSignatureException;
import cz.muni.fi.trusted_party.exceptions.OrganizationNotFoundException;
import cz.muni.fi.trusted_party.exceptions.SignatureVerificationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TokenServiceTest {

    @Mock
    private TokenRepository tokenRepository;

    @Mock
    private OrganizationRepository organizationRepository;

    @Mock
    private DocumentRepository documentRepository;

    @Mock
    private CertificateRepository certificateRepository;

    @Mock
    private AppProperties appProperties;

    private TokenService tokenService;

    @BeforeEach
    void setUp() {
        tokenService = new TokenService(
                tokenRepository,
                organizationRepository,
                documentRepository,
                certificateRepository,
                appProperties);
    }

    @Test
    void getToken_existingDocument_returnsTokens() {
        Organization organization = new Organization();
        organization.setOrgName("org-1");
        Document document = new Document();
        document.setIdentifier("doc-1");

        List<Token> tokens = List.of(new Token());

        when(organizationRepository.findById("org-1")).thenReturn(Optional.of(organization));
        when(documentRepository.findByIdentifierAndDocFormatAndDocumentTypeAndOrganization(
                "doc-1",
                "json",
                DocumentType.GRAPH,
                organization)).thenReturn(Optional.of(document));
        when(tokenRepository.findByDocument(document)).thenReturn(tokens);

        List<Token> result = tokenService.getToken("org-1", "doc-1", "json");

        assertThat(result).isSameAs(tokens);
    }

    @Test
    void getToken_missingOrganization_throwsOrganizationNotFoundException() {
        when(organizationRepository.findById("missing-org")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> tokenService.getToken("missing-org", "doc-1", "json"))
                .isInstanceOf(OrganizationNotFoundException.class)
                .hasMessageContaining("missing-org");
    }

    @Test
    void getToken_missingDocument_throwsDocumentNotFoundException() {
        Organization organization = new Organization();
        organization.setOrgName("org-1");

        when(organizationRepository.findById("org-1")).thenReturn(Optional.of(organization));
        when(documentRepository.findByIdentifierAndDocFormatAndDocumentTypeAndOrganization(
                "doc-404",
                "json",
                DocumentType.GRAPH,
                organization)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> tokenService.getToken("org-1", "doc-404", "json"))
                .isInstanceOf(DocumentNotFoundException.class)
                .hasMessageContaining("doc-404");
    }

    @Test
    void getAllTokens_existingOrganization_returnsMap() {
        Organization organization = new Organization();
        organization.setOrgName("org-1");
        Document documentA = new Document();
        documentA.setIdentifier("doc-a");
        Document documentB = new Document();
        documentB.setIdentifier("doc-b");
        List<Document> documents = List.of(documentA, documentB);
        List<Token> tokensA = List.of(new Token());
        List<Token> tokensB = List.of(new Token(), new Token());

        when(organizationRepository.findById("org-1")).thenReturn(Optional.of(organization));
        when(documentRepository.findByOrganization(organization)).thenReturn(documents);
        when(tokenRepository.findByDocument(documentA)).thenReturn(tokensA);
        when(tokenRepository.findByDocument(documentB)).thenReturn(tokensB);

        Map<Document, List<Token>> result = tokenService.getAllTokens("org-1");

        assertThat(result)
                .hasSize(2)
                .containsEntry(documentA, tokensA)
                .containsEntry(documentB, tokensB);
    }

    @Test
    void getAllTokens_missingOrganization_throwsOrganizationNotFoundException() {
        when(organizationRepository.findById("missing-org")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> tokenService.getAllTokens("missing-org"))
                .isInstanceOf(OrganizationNotFoundException.class)
                .hasMessageContaining("missing-org");
    }

    @Test
    void issueToken_missingOrganizationId_throwsInvalidRequestException() {
        TokenRequestDTO body = buildRequest(DocumentType.META);
        body.setOrganizationId(" ");
        body.setDocument(null);

        assertThatThrownBy(() -> tokenService.issueToken(body))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("Missing organizationId");
    }

    @Test
    void issueToken_graphMissingSignature_throwsMissingSignatureException() {
        TokenRequestDTO body = buildRequest(DocumentType.GRAPH);
        body.setSignature(" ");

        assertThatThrownBy(() -> tokenService.issueToken(body))
                .isInstanceOf(MissingSignatureException.class);
    }

    @Test
    void issueToken_futureTimestamp_throwsInvalidTimestampException() {
        TokenRequestDTO body = buildRequest(DocumentType.GRAPH);
        body.setCreatedOn(LocalDateTime.now().plusHours(1).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));

        assertThatThrownBy(() -> tokenService.issueToken(body))
                .isInstanceOf(InvalidTimestampException.class);
    }

    @Test
    void issueToken_missingOrganization_throwsOrganizationNotFoundException() {
        TokenRequestDTO body = buildRequest(DocumentType.GRAPH);

        when(organizationRepository.findById("org-1")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> tokenService.issueToken(body))
                .isInstanceOf(OrganizationNotFoundException.class)
                .hasMessageContaining("org-1");
    }

    @Test
    void issueToken_invalidSignature_throwsSignatureVerificationException() {
        TokenRequestDTO body = buildRequest(DocumentType.GRAPH);
        Organization organization = new Organization();
        organization.setOrgName("org-1");

        when(organizationRepository.findById("org-1")).thenReturn(Optional.of(organization));

        TokenService serviceSpy = spy(tokenService);
        doReturn(false).when(serviceSpy).verifySignature(body);

        assertThatThrownBy(() -> serviceSpy.issueToken(body))
                .isInstanceOf(SignatureVerificationException.class);
    }

    @Test
    void issueToken_validRequest_callsIssueTokenAndStoreDoc() {
        TokenRequestDTO body = buildRequest(DocumentType.GRAPH);
        Organization organization = new Organization();
        organization.setOrgName("org-1");
        List<Token> tokens = List.of(new Token());

        when(organizationRepository.findById("org-1")).thenReturn(Optional.of(organization));

        TokenService serviceSpy = spy(tokenService);
        doReturn(true).when(serviceSpy).verifySignature(body);
        doReturn(tokens).when(serviceSpy).issueTokenAndStoreDoc(body);

        List<Token> result = serviceSpy.issueToken(body);

        assertThat(result).isSameAs(tokens);
    }

        @Test
        void issueToken_missingOrganizationId_infersFromDocument() {
        TokenRequestDTO body = buildRequest(DocumentType.META);
        body.setOrganizationId(" ");
        body.setDocument(base64Of("/organizations/org-inferred/documents/doc-x"));
        List<Token> tokens = List.of(new Token());

        TokenService serviceSpy = spy(tokenService);
        doReturn(tokens).when(serviceSpy).issueTokenAndStoreDoc(body);

        List<Token> result = serviceSpy.issueToken(body);

        assertThat(result).isSameAs(tokens);
        assertThat(body.getOrganizationId()).isEqualTo("org-inferred");
        }

        @Test
        void issueTokenAndStoreDoc_existingDocumentWithTokens_returnsTokens() {
            TokenRequestDTO body = buildRequestWithBundleId(DocumentType.GRAPH, "b1");
        Organization organization = new Organization();
        organization.setOrgName("org-1");
        Document document = new Document();
            document.setIdentifier("ex:b1");
        List<Token> tokens = List.of(new Token());

        when(organizationRepository.findById("org-1")).thenReturn(Optional.of(organization));
        when(documentRepository.findByIdentifierAndDocFormatAndDocumentTypeAndOrganization(
            anyString(),
            eq("provn"),
            eq(DocumentType.GRAPH),
            eq(organization))).thenReturn(Optional.of(document));
        when(tokenRepository.findByDocument(document)).thenReturn(tokens);

        List<Token> result = tokenService.issueTokenAndStoreDoc(body);

        assertThat(result).isSameAs(tokens);
        }

        @Test
        void issueTokenAndStoreDoc_missingCertificate_throwsCertificateNotFoundException() {
            TokenRequestDTO body = buildRequestWithBundleId(DocumentType.GRAPH, "b2");
        Organization organization = new Organization();
        organization.setOrgName("org-1");

        when(organizationRepository.findById("org-1")).thenReturn(Optional.of(organization));
        when(documentRepository.findByIdentifierAndDocFormatAndDocumentTypeAndOrganization(
            anyString(),
            eq("provn"),
            eq(DocumentType.GRAPH),
            eq(organization))).thenReturn(Optional.empty());
        when(certificateRepository.findFirstByOrganizationOrgNameAndIsRevoked("org-1", false))
            .thenReturn(null);

        assertThatThrownBy(() -> tokenService.issueTokenAndStoreDoc(body))
            .isInstanceOf(CertificateNotFoundException.class)
            .hasMessageContaining("org-1");
        }

        @Test
        void verifySignature_missingOrganization_throwsOrganizationNotFoundException() {
        TokenRequestDTO body = buildRequest(DocumentType.GRAPH);
        when(organizationRepository.findById("org-1")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> tokenService.verifySignature(body))
            .isInstanceOf(OrganizationNotFoundException.class)
            .hasMessageContaining("org-1");
        }

        @Test
        void verifySignature_missingCertificate_throwsCertificateNotFoundException() {
        TokenRequestDTO body = buildRequest(DocumentType.GRAPH);
        Organization organization = new Organization();
        organization.setOrgName("org-1");

        when(organizationRepository.findById("org-1")).thenReturn(Optional.of(organization));
        when(certificateRepository.findFirstByOrganizationOrgNameAndCertificateTypeAndIsRevoked(
            "org-1",
            cz.muni.fi.trusted_party.data.enums.CertificateType.CLIENT,
            false)).thenReturn(null);

        assertThatThrownBy(() -> tokenService.verifySignature(body))
            .isInstanceOf(CertificateNotFoundException.class)
            .hasMessageContaining("org-1");
        }

        @Test
        void verifySignature_invalidCertificate_throwsSignatureVerificationException() {
        TokenRequestDTO body = buildRequest(DocumentType.GRAPH);
        Organization organization = new Organization();
        organization.setOrgName("org-1");
        cz.muni.fi.trusted_party.data.model.Certificate certificate =
            new cz.muni.fi.trusted_party.data.model.Certificate();
        certificate.setCert("not-a-certificate");

        when(organizationRepository.findById("org-1")).thenReturn(Optional.of(organization));
        when(certificateRepository.findFirstByOrganizationOrgNameAndCertificateTypeAndIsRevoked(
            "org-1",
            cz.muni.fi.trusted_party.data.enums.CertificateType.CLIENT,
            false)).thenReturn(certificate);

        assertThatThrownBy(() -> tokenService.verifySignature(body))
            .isInstanceOf(SignatureVerificationException.class)
            .hasMessageContaining("Invalid signature");
        }

    private TokenRequestDTO buildRequest(DocumentType documentType) {
        TokenRequestDTO body = new TokenRequestDTO();
        body.setOrganizationId("org-1");
        body.setDocument("ZHVtbXktZG9j");
        body.setDocumentFormat("json");
        body.setDocumentType(documentType);
        body.setCreatedOn(LocalDateTime.now().minusMinutes(1).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        body.setSignature("sig");
        return body;
    }

    private TokenRequestDTO buildRequestWithBundleId(DocumentType documentType, String bundleId) {
        TokenRequestDTO body = new TokenRequestDTO();
        body.setOrganizationId("org-1");
        body.setDocument(base64Of("document\nprefix ex <http://example.org/>\n"
                + "bundle ex:" + bundleId + "\nentity(ex:e1)\nendBundle\nendDocument"));
        body.setDocumentFormat("provn");
        body.setDocumentType(documentType);
        body.setCreatedOn(LocalDateTime.now().minusMinutes(1).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        body.setSignature("sig");
        return body;
    }

    private String base64Of(String content) {
        return Base64.getEncoder().encodeToString(content.getBytes(StandardCharsets.UTF_8));
    }
}

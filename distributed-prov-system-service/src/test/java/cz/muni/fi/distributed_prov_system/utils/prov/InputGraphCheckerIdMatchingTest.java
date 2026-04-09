package cz.muni.fi.distributed_prov_system.utils.prov;

import cz.muni.fi.distributed_prov_system.config.AppProperties;
import cz.muni.fi.distributed_prov_system.utils.TestDataFactory;
import cz.muni.fi.distributed_prov_system.api.StoreGraphRequestDTO;
import org.junit.jupiter.api.Test;
import org.openprovenance.prov.model.Bundle;
import org.openprovenance.prov.model.Document;
import org.openprovenance.prov.model.QualifiedName;

import java.lang.reflect.Field;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class InputGraphCheckerIdMatchingTest {

    @Test
    void checkIdsMatch_WhenBundleLocalPartContainsAbsoluteUri_UsesPathTailForComparison() throws Exception {
        InputGraphChecker checker = checkerWithBundle(
                "//prov-storage-pathology:8000/api/v1/organizations/org-1/documents/doc-1",
                "http://prov-storage-pathology:8000/api/v1/organizations/org-1/documents/doc-1",
                "/api/v1/organizations/org-1/documents/doc-1"
        );

        assertThatCode(() -> checker.checkIdsMatch("doc-1")).doesNotThrowAnyException();
        assertThat(checker.getBundleId()).isEqualTo("doc-1");
    }

    @Test
    void checkIdsMatch_WhenBundleIdTailDiffers_ThrowsIdMismatchWithNormalizedId() throws Exception {
        InputGraphChecker checker = checkerWithBundle(
                "//prov-storage-pathology:8000/api/v1/organizations/org-1/documents/another-doc-id",
                "http://prov-storage-pathology:8000/api/v1/organizations/org-1/documents/another-doc-id",
                "/api/v1/organizations/org-1/documents/doc-1"
        );

        assertThatThrownBy(() -> checker.checkIdsMatch("doc-1"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("The bundle id [another-doc-id] does not match requested id [doc-1].");
    }

    @Test
    void checkIdsMatch_WhenUriPathDiffers_ThrowsRequestPathMismatch() throws Exception {
        InputGraphChecker checker = checkerWithBundle(
                "doc-1",
                "http://prov-storage-pathology:8000/api/v1/organizations/org-1/documents/doc-1",
                "/api/v1/organizations/org-1/documents/doc-2"
        );

        assertThatThrownBy(() -> checker.checkIdsMatch("doc-1"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("The bundle identifier does not end with request path [/api/v1/organizations/org-1/documents/doc-2].");
    }

    @Test
    void checkIdsMatch_WhenUriContainsNamespacePath_AppendsLocalPartBeforePathComparison() throws Exception {
        InputGraphChecker checker = checkerWithBundle(
                "doc-1",
                "http://prov-storage-pathology:8000/api/v1/organizations/org-1/documents/",
                null,
                "/api/v1/organizations/org-1/documents/doc-1"
        );

        assertThatCode(() -> checker.checkIdsMatch("doc-1")).doesNotThrowAnyException();
    }

    @Test
    void checkIdsMatch_WithRealComponentPayload_UsesParsedBundleIdentifierShape() {
        StoreGraphRequestDTO request = TestDataFactory.componentStoreGraphRequest(
                "http://prov-storage-pathology:8000",
                "org-1",
                "doc-1"
        );

        AppProperties appProperties = mock(AppProperties.class);
        InputGraphChecker checker = new InputGraphChecker(
                request.getDocument(),
                request.getDocumentFormat(),
                "/api/v1/organizations/org-1/documents/doc-1",
                appProperties,
                mock(CPMValidator.class),
                mock(ProvDocumentValidator.class)
        );

        checker.parseGraph();
        assertThatCode(() -> checker.checkIdsMatch("doc-1")).doesNotThrowAnyException();
    }

    @Test
    void checkIdsMatch_WhenBundleUriIsRelativeToken_DoesNotFailPathValidation() throws Exception {
        InputGraphChecker checker = checkerWithBundle(
                "doc-1",
                "nulldoc-1",
                "/api/v1/organizations/org-1/documents/doc-1"
        );

        assertThatCode(() -> checker.checkIdsMatch("doc-1")).doesNotThrowAnyException();
    }

    @Test
    void checkIdsMatch_WhenBundleLocalPartContainsQueryAndFragment_ResolvesIdTail() throws Exception {
        InputGraphChecker checker = checkerWithBundle(
                "storage:doc-1?version=2#fragment",
                "http://prov-storage-pathology:8000/api/v1/organizations/org-1/documents/doc-1",
                "/api/v1/organizations/org-1/documents/doc-1"
        );

        assertThatCode(() -> checker.checkIdsMatch("doc-1")).doesNotThrowAnyException();
        assertThat(checker.getBundleId()).isEqualTo("doc-1");
    }

    private InputGraphChecker checkerWithBundle(String localPart, String uri, String requestPath) throws Exception {
        return checkerWithBundle(localPart, uri, null, requestPath);
    }

    private InputGraphChecker checkerWithBundle(String localPart, String uri, String namespaceUri, String requestPath) throws Exception {
        InputGraphChecker checker = new InputGraphChecker(
                "",
                "json",
                requestPath,
                mock(AppProperties.class),
                mock(CPMValidator.class),
                mock(ProvDocumentValidator.class)
        );

        Bundle bundle = mock(Bundle.class);
        QualifiedName id = mock(QualifiedName.class);
        when(id.getLocalPart()).thenReturn(localPart);
        when(id.getUri()).thenReturn(uri);
        when(id.getNamespaceURI()).thenReturn(namespaceUri);
        when(bundle.getId()).thenReturn(id);

        setField(checker, "document", mock(Document.class));
        setField(checker, "bundle", bundle);
        return checker;
    }

    private void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
}

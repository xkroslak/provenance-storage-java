package cz.muni.fi.distributed_prov_system.facade;

import cz.muni.fi.distributed_prov_system.api.StoreGraphRequestDTO;
import cz.muni.fi.distributed_prov_system.api.StoreGraphResponseDTO;
import cz.muni.fi.distributed_prov_system.api.SubgraphResponseDTO;
import cz.muni.fi.distributed_prov_system.service.DocumentService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DocumentFacadeImplTest {

    @Mock
    private DocumentService documentService;

    @InjectMocks
    private DocumentFacadeImpl documentFacade;

    @Test
    void storeDocument_WhenInputIsValid_ReturnsServiceResponse() {
        StoreGraphRequestDTO request = new StoreGraphRequestDTO();
        StoreGraphResponseDTO expected = new StoreGraphResponseDTO();

        when(documentService.storeDocument("org-1", "doc-1", request)).thenReturn(expected);

        StoreGraphResponseDTO result = documentFacade.storeDocument("org-1", "doc-1", request);

        assertThat(result).isSameAs(expected);
        verify(documentService).storeDocument("org-1", "doc-1", request);
    }

    @Test
    void updateDocument_WhenInputIsValid_ReturnsServiceResponse() {
        StoreGraphRequestDTO request = new StoreGraphRequestDTO();
        StoreGraphResponseDTO expected = new StoreGraphResponseDTO();

        when(documentService.updateDocument("org-1", "doc-1", request)).thenReturn(expected);

        StoreGraphResponseDTO result = documentFacade.updateDocument("org-1", "doc-1", request);

        assertThat(result).isSameAs(expected);
        verify(documentService).updateDocument("org-1", "doc-1", request);
    }

    @Test
    void getDocument_WhenDocumentExists_ReturnsServiceObject() {
        Object expected = new Object();

        when(documentService.getDocument("org-1", "doc-1")).thenReturn(expected);

        Object result = documentFacade.getDocument("org-1", "doc-1");

        assertThat(result).isSameAs(expected);
        verify(documentService).getDocument("org-1", "doc-1");
    }

    @Test
    void documentExists_WhenServiceReturnsTrue_ReturnsTrue() {
        when(documentService.documentExists("org-1", "doc-1")).thenReturn(true);

        boolean result = documentFacade.documentExists("org-1", "doc-1");

        assertThat(result).isTrue();
        verify(documentService).documentExists("org-1", "doc-1");
    }

    @Test
    void getDomainSpecificSubgraph_WhenInputIsValid_ReturnsServiceResponse() {
        SubgraphResponseDTO expected = new SubgraphResponseDTO();

        when(documentService.getDomainSpecificSubgraph("org-1", "doc-1", "json")).thenReturn(expected);

        SubgraphResponseDTO result = documentFacade.getDomainSpecificSubgraph("org-1", "doc-1", "json");

        assertThat(result).isSameAs(expected);
        verify(documentService).getDomainSpecificSubgraph("org-1", "doc-1", "json");
    }

    @Test
    void getBackboneSubgraph_WhenInputIsValid_ReturnsServiceResponse() {
        SubgraphResponseDTO expected = new SubgraphResponseDTO();

        when(documentService.getBackboneSubgraph("org-1", "doc-1", "json")).thenReturn(expected);

        SubgraphResponseDTO result = documentFacade.getBackboneSubgraph("org-1", "doc-1", "json");

        assertThat(result).isSameAs(expected);
        verify(documentService).getBackboneSubgraph("org-1", "doc-1", "json");
    }
}
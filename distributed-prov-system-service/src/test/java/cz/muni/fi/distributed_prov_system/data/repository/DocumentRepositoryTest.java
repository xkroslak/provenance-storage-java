package cz.muni.fi.distributed_prov_system.data.repository;

import cz.muni.fi.distributed_prov_system.data.model.nonprovmodels.Document;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.neo4j.DataNeo4jTest;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataNeo4jTest
class DocumentRepositoryTest extends AbstractNeo4jRepositoryTest {

    @Autowired
    private DocumentRepository documentRepository;

    @Test
    void save_WhenDocumentIsValid_ReturnsPersistedDocument() {
        Document document = new Document();
        document.setIdentifier("doc-save");
        document.setFormat("json");
        document.setGraph("{\"saved\":true}");

        Document saved = documentRepository.save(document);

        assertThat(saved.getIdentifier()).isEqualTo("doc-save");
        assertThat(saved.getFormat()).isEqualTo("json");
        assertThat(saved.getGraph()).isEqualTo("{\"saved\":true}");
    }

    @Test
    void findById_WhenDocumentExists_ReturnsDocument() {
        Document document = new Document();
        document.setIdentifier("doc-find-id");
        document.setFormat("json");
        document.setGraph("{\"graph\":true}");

        documentRepository.save(document);

        Optional<Document> result = documentRepository.findById("doc-find-id");

        assertThat(result).isPresent();
        assertThat(result.get().getIdentifier()).isEqualTo("doc-find-id");
        assertThat(result.get().getFormat()).isEqualTo("json");
    }

    @Test
    void findById_WhenDocumentDoesNotExist_ReturnsEmptyOptional() {
        Optional<Document> result = documentRepository.findById("missing-doc");

        assertThat(result).isEmpty();
    }

    @Test
    void existsById_WhenDocumentExists_ReturnsTrue() {
        Document document = new Document();
        document.setIdentifier("doc-exists");
        document.setFormat("json");
        document.setGraph("{}");

        documentRepository.save(document);

        boolean exists = documentRepository.existsById("doc-exists");

        assertThat(exists).isTrue();
    }

    @Test
    void existsById_WhenDocumentDoesNotExist_ReturnsFalse() {
        boolean exists = documentRepository.existsById("missing-doc");

        assertThat(exists).isFalse();
    }

    @Test
    void findByIdentifierAndFormat_WhenDocumentExists_ReturnsDocument() {
        Document document = new Document();
        document.setIdentifier("doc-1");
        document.setFormat("json");
        document.setGraph("{\"graph\":true}");

        documentRepository.save(document);

        Optional<Document> result = documentRepository.findByIdentifierAndFormat("doc-1", "json");

        assertThat(result).isPresent();
        assertThat(result.get().getIdentifier()).isEqualTo("doc-1");
        assertThat(result.get().getFormat()).isEqualTo("json");
    }

    @Test
    void findByIdentifierAndFormat_WhenDocumentDoesNotExist_ReturnsEmptyOptional() {
        Optional<Document> result = documentRepository.findByIdentifierAndFormat("missing", "json");

        assertThat(result).isEmpty();
    }

    @Test
    void findByIdentifierAndFormat_WhenFormatDoesNotMatch_ReturnsEmptyOptional() {
        Document document = new Document();
        document.setIdentifier("doc-2");
        document.setFormat("xml");
        document.setGraph("<graph>true</graph>");

        documentRepository.save(document);

        Optional<Document> result = documentRepository.findByIdentifierAndFormat("doc-2", "json");

        assertThat(result).isEmpty();
    }
}

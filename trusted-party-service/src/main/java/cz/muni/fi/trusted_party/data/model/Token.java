package cz.muni.fi.trusted_party.data.model;

import cz.muni.fi.trusted_party.data.enums.HashFunction;
import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.Objects;

@Entity
public class Token {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "document_id")
    private Document document;

    @Column(length = 128)
    private String hash;

    @Enumerated(EnumType.STRING)
    private HashFunction hashFunction;

    private LocalDateTime createdOn;

    @Lob
    private String signature;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getSignature() {
        return signature;
    }

    public void setSignature(String signature) {
        this.signature = signature;
    }

    public LocalDateTime getCreatedOn() {
        return createdOn;
    }

    public void setCreatedOn(LocalDateTime createdOn) {
        this.createdOn = createdOn;
    }

    public HashFunction getHashFunction() {
        return hashFunction;
    }

    public void setHashFunction(HashFunction hashFunction) {
        this.hashFunction = hashFunction;
    }

    public String getHash() {
        return hash;
    }

    public void setHash(String hash) {
        this.hash = hash;
    }

    public Document getDocument() {
        return document;
    }

    public void setDocument(Document document) {
        this.document = document;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Token token)) return false;
        return Objects.equals(id, token.id)
                && Objects.equals(document, token.document)
                && Objects.equals(hash, token.hash)
                && hashFunction == token.hashFunction
                && Objects.equals(createdOn, token.createdOn)
                && Objects.equals(signature, token.signature);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, document, hash, hashFunction, createdOn, signature);
    }

    @Override
    public String toString() {
        return "Token{" +
                "id=" + id +
                ", document=" + document +
                ", hash='" + hash + '\'' +
                ", hashFunction=" + hashFunction +
                ", createdOn=" + createdOn +
                ", signature='" + signature + '\'' +
                '}';
    }
}

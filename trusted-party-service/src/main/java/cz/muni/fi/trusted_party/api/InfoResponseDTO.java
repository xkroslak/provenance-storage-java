package cz.muni.fi.trusted_party.api;

public class InfoResponseDTO {
    private String id;
    private String certificate;

    public InfoResponseDTO(String id, String certificate) {
        this.id = id;
        this.certificate = certificate;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getCertificate() {
        return certificate;
    }

    public void setCertificate(String certificate) {
        this.certificate = certificate;
    }
}
package cz.muni.fi.trusted_party.service;

import cz.muni.fi.trusted_party.api.InfoResponseDTO;
import cz.muni.fi.trusted_party.config.AppProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class InfoService {
    private final AppProperties appProperties;

    @Autowired
    public InfoService(AppProperties appProperties) {
        this.appProperties = appProperties;
    }

    public InfoResponseDTO getInfo() {
        return new InfoResponseDTO(appProperties.getId(), appProperties.getCertificate());
    }
}

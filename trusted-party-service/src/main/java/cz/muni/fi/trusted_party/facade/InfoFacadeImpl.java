package cz.muni.fi.trusted_party.facade;

import cz.muni.fi.trusted_party.api.InfoResponseDTO;
import cz.muni.fi.trusted_party.service.InfoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class InfoFacadeImpl implements InfoFacade {
    private final InfoService infoService;

    @Autowired
    public InfoFacadeImpl(InfoService infoService) {
        this.infoService = infoService;
    }

    @Override
    public InfoResponseDTO getInfo() {
        return infoService.getInfo();
    }
}

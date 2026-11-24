package mg.working.tantana.service;

import lombok.RequiredArgsConstructor;
import mg.working.tantana.dto.RapportVentes;
import mg.working.tantana.exception.RegleMetierException;
import mg.working.tantana.repository.RapportVentesRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RapportVentesService {

    private final RapportVentesRepository rapportRepository;

    public RapportVentes.Tableau tableau(LocalDate du, LocalDate au) {
        if (du.isAfter(au)) {
            throw new RegleMetierException("La date de début doit précéder la date de fin.");
        }
        return new RapportVentes.Tableau(du, au,
                rapportRepository.synthese(du, au),
                rapportRepository.parArticle(du, au),
                rapportRepository.parClient(du, au),
                rapportRepository.encaissements(du, au),
                rapportRepository.creances());
    }
}

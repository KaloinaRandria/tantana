package mg.working.tantana.service;

import lombok.RequiredArgsConstructor;
import mg.working.tantana.repository.NumerotationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

/** Genere les numeros de documents : CA-2026-0001 (commande d'achat), etc. */
@Service
@RequiredArgsConstructor
public class NumerotationService {

    public static final String COMMANDE_ACHAT = "CA";
    public static final String RECEPTION = "BR";

    private final NumerotationRepository numerotationRepository;

    /** Doit etre appele dans la transaction qui enregistre le document. */
    @Transactional(propagation = Propagation.MANDATORY)
    public String prochainNumero(String prefixe, LocalDate date) {
        int annee = date.getYear();
        int numero = numerotationRepository.incrementer(prefixe, annee);
        return "%s-%d-%04d".formatted(prefixe, annee, numero);
    }
}

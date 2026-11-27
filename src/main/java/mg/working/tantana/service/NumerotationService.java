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
    /** Factures de vente : suite continue, attribuee a la validation seulement. */
    public static final String FACTURE = "FV";
    public static final String AVOIR = "AV";
    /** Articles du catalogue : compteur unique, sans annee (ART-0001). */
    public static final String ARTICLE = "ART";
    /** Valeur de la colonne annee pour les compteurs non annuels. */
    private static final int SANS_ANNEE = 0;

    private final NumerotationRepository numerotationRepository;

    /** Doit etre appele dans la transaction qui enregistre le document. */
    @Transactional(propagation = Propagation.MANDATORY)
    public String prochainNumero(String prefixe, LocalDate date) {
        int annee = date.getYear();
        int numero = numerotationRepository.incrementer(prefixe, annee);
        return "%s-%d-%04d".formatted(prefixe, annee, numero);
    }

    /** Prochaine reference article (ART-0001, ART-0002, ...). Meme contrainte de transaction. */
    @Transactional(propagation = Propagation.MANDATORY)
    public String prochaineReferenceArticle() {
        int numero = numerotationRepository.incrementer(ARTICLE, SANS_ANNEE);
        return "%s-%04d".formatted(ARTICLE, numero);
    }
}

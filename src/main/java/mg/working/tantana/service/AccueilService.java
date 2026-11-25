package mg.working.tantana.service;

import lombok.RequiredArgsConstructor;
import mg.working.tantana.dto.RapportVentes;
import mg.working.tantana.dto.TableauAccueil;
import mg.working.tantana.entity.StatutReception;
import mg.working.tantana.entity.StatutVente;
import mg.working.tantana.repository.CommandeAchatRepository;
import mg.working.tantana.repository.CommandeAchatSpecifications;
import mg.working.tantana.repository.RapportVentesRepository;
import mg.working.tantana.repository.ReceptionRepository;
import mg.working.tantana.repository.StockRepository;
import mg.working.tantana.repository.VenteRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Indicateurs et series de la page d'accueil. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AccueilService {

    private static final int NB_MOIS = 12;
    private static final Locale FR = Locale.FRANCE;

    private final ReceptionRepository receptionRepository;
    private final VenteRepository venteRepository;
    private final CommandeAchatRepository commandeRepository;
    private final StockRepository stockRepository;
    private final RapportVentesRepository rapportRepository;
    private final JdbcTemplate jdbc;

    /** @param financier vrai pour ADMIN / CHECKER : chiffres d'affaires, marges et graphiques. */
    public TableauAccueil tableau(boolean financier) {
        long receptions = receptionRepository.countByStatut(StatutReception.SAISIE);
        long ventes = venteRepository.countByStatut(StatutVente.A_VALIDER);
        long retards = commandeRepository.count(CommandeAchatSpecifications.enRetard(true));
        Long sansStock = jdbc.queryForObject("""
                SELECT count(*) FROM article a LEFT JOIN stock s ON s.article_id = a.id
                WHERE a.actif AND a.prix_vente_ht IS NOT NULL AND coalesce(s.quantite, 0) <= 0
                """, Long.class);

        if (!financier) {
            return new TableauAccueil(receptions, ventes, retards, sansStock == null ? 0 : sansStock,
                    null, null, null, null, null, null, List.of(), List.of(), List.of(), List.of(), List.of(), List.of());
        }

        LocalDate aujourdhui = LocalDate.now();
        LocalDate debutMois = aujourdhui.withDayOfMonth(1);
        RapportVentes.Synthese mois = rapportRepository.synthese(debutMois, aujourdhui);
        // Mois en cours (du 1er a aujourd'hui) compare a la MEME periode du mois precedent :
        // comparer a un mois complet ferait toujours apparaitre une baisse en debut de mois.
        LocalDate finComparaison = aujourdhui.minusMonths(1); // 31 mars -> 28/29 fevrier
        RapportVentes.Synthese precedent = rapportRepository.synthese(debutMois.minusMonths(1), finComparaison);
        BigDecimal evolutionCa = evolution(mois.getCaNetHt(), precedent.getCaNetHt());
        String periodeComparaison = "1er–" + finComparaison.getDayOfMonth() + " "
                + finComparaison.getMonth().getDisplayName(TextStyle.SHORT, FR);
        BigDecimal encaisse = rapportRepository.encaissements(debutMois, aujourdhui).stream()
                .map(RapportVentes.Encaissement::montant).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal creances = rapportRepository.creances().stream()
                .map(RapportVentes.Creance::reste).reduce(BigDecimal.ZERO, BigDecimal::add);

        // 12 derniers mois, mois sans vente compris (valeur 0), du plus ancien au plus recent
        YearMonth courant = YearMonth.from(aujourdhui);
        YearMonth premier = courant.minusMonths(NB_MOIS - 1L);
        // un mois de plus en arriere pour calculer l'evolution du premier mois affiche
        Map<YearMonth, BigDecimal[]> donnees = rapportRepository.parMois(premier.minusMonths(1).atDay(1));
        List<String> libelles = new ArrayList<>();
        List<BigDecimal> ca = new ArrayList<>();
        List<BigDecimal> marge = new ArrayList<>();
        List<BigDecimal> evolutions = new ArrayList<>();
        BigDecimal caPrecedent = donnees.getOrDefault(premier.minusMonths(1), new BigDecimal[]{BigDecimal.ZERO})[0];
        for (YearMonth m = premier; !m.isAfter(courant); m = m.plusMonths(1)) {
            BigDecimal[] v = donnees.getOrDefault(m, new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO});
            libelles.add(m.getMonth().getDisplayName(TextStyle.SHORT, FR) + " " + String.valueOf(m.getYear()).substring(2));
            ca.add(v[0]);
            marge.add(v[0].subtract(v[1]));
            evolutions.add(m.equals(courant) ? evolutionCa : evolution(v[0], caPrecedent));
            caPrecedent = v[0];
        }

        List<RapportVentes.LigneMarge> top = rapportRepository.parArticle(premier.atDay(1), aujourdhui).stream()
                .limit(5).toList();

        return new TableauAccueil(receptions, ventes, retards, sansStock == null ? 0 : sansStock,
                mois, encaisse, creances, stockRepository.valeurTotaleRevient(), evolutionCa, periodeComparaison,
                libelles, ca, marge, evolutions, top, rapportRepository.encaissements(debutMois, aujourdhui));
    }

    /** Variation en % de « actuel » par rapport a « reference » ; null si la reference est nulle. */
    private static BigDecimal evolution(BigDecimal actuel, BigDecimal reference) {
        if (reference == null || reference.signum() <= 0) {
            return null;
        }
        return actuel.subtract(reference).multiply(BigDecimal.valueOf(100)).divide(reference, 1, RoundingMode.HALF_UP);
    }
}

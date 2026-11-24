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
                    null, null, null, null, List.of(), List.of(), List.of(), List.of(), List.of());
        }

        LocalDate aujourdhui = LocalDate.now();
        LocalDate debutMois = aujourdhui.withDayOfMonth(1);
        RapportVentes.Synthese mois = rapportRepository.synthese(debutMois, aujourdhui);
        BigDecimal encaisse = rapportRepository.encaissements(debutMois, aujourdhui).stream()
                .map(RapportVentes.Encaissement::montant).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal creances = rapportRepository.creances().stream()
                .map(RapportVentes.Creance::reste).reduce(BigDecimal.ZERO, BigDecimal::add);

        // 12 derniers mois, mois sans vente compris (valeur 0), du plus ancien au plus recent
        YearMonth courant = YearMonth.from(aujourdhui);
        YearMonth premier = courant.minusMonths(NB_MOIS - 1L);
        Map<YearMonth, BigDecimal[]> donnees = rapportRepository.parMois(premier.atDay(1));
        List<String> libelles = new ArrayList<>();
        List<BigDecimal> ca = new ArrayList<>();
        List<BigDecimal> marge = new ArrayList<>();
        for (YearMonth m = premier; !m.isAfter(courant); m = m.plusMonths(1)) {
            BigDecimal[] v = donnees.getOrDefault(m, new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO});
            libelles.add(m.getMonth().getDisplayName(TextStyle.SHORT, FR) + " " + String.valueOf(m.getYear()).substring(2));
            ca.add(v[0]);
            marge.add(v[0].subtract(v[1]));
        }

        List<RapportVentes.LigneMarge> top = rapportRepository.parArticle(premier.atDay(1), aujourdhui).stream()
                .limit(5).toList();

        return new TableauAccueil(receptions, ventes, retards, sansStock == null ? 0 : sansStock,
                mois, encaisse, creances, stockRepository.valeurTotaleRevient(),
                libelles, ca, marge, top, rapportRepository.encaissements(debutMois, aujourdhui));
    }
}

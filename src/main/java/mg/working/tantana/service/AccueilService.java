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
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Indicateurs, tableaux et series de la page d'accueil. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AccueilService {

    private static final int NB_MOIS = 12;
    /** Au-dela, la courbe du CA passe d'un point par jour a un point par mois. */
    private static final int MAX_JOURS_COURBE = 92;
    private static final Locale FR = Locale.FRANCE;
    private static final DateTimeFormatter JOUR = DateTimeFormatter.ofPattern("dd/MM", FR);

    private final ReceptionRepository receptionRepository;
    private final VenteRepository venteRepository;
    private final CommandeAchatRepository commandeRepository;
    private final StockRepository stockRepository;
    private final RapportVentesRepository rapportRepository;
    private final RapportVentesService rapportService;
    private final JdbcTemplate jdbc;

    /**
     * @param financier vrai pour ADMIN / CHECKER : chiffres d'affaires, marges et graphiques.
     * @param du        debut de la periode analysee (ignore si non financier)
     * @param au        fin de la periode analysee (ignore si non financier)
     */
    public TableauAccueil tableau(boolean financier, LocalDate du, LocalDate au) {
        long receptions = receptionRepository.countByStatut(StatutReception.SAISIE);
        long ventes = venteRepository.countByStatut(StatutVente.A_VALIDER);
        long retards = commandeRepository.count(CommandeAchatSpecifications.enRetard(true));
        Long sansStock = jdbc.queryForObject("""
                SELECT count(*) FROM article a LEFT JOIN stock s ON s.article_id = a.id
                WHERE a.actif AND a.prix_vente_ht IS NOT NULL AND coalesce(s.quantite, 0) <= 0
                """, Long.class);
        long ruptures = sansStock == null ? 0 : sansStock;

        if (!financier) {
            return new TableauAccueil(receptions, ventes, retards, ruptures,
                    null, null, null, null, null, List.of(), List.of(), List.of(), List.of());
        }

        // controle de la periode (debut <= fin) : leve RegleMetierException
        RapportVentes.Tableau rapport = rapportService.tableau(du, au);

        // Comparaison a la periode precedente equivalente (ex. 1er-28 nov. vs 1er-28 oct.)
        LocalDate[] precedente = periodePrecedente(du, au);
        RapportVentes.Synthese synthesePrecedente = rapportRepository.synthese(precedente[0], precedente[1]);
        BigDecimal evolutionCa = evolution(rapport.synthese().getCaNetHt(), synthesePrecedente.getCaNetHt());
        String libellePrecedente = libelle(precedente[0], precedente[1]);

        TableauAccueil.Courbe courbe = courbe(du, au, precedente[0], precedente[1], libellePrecedente);

        // ----- 12 derniers mois, mois sans vente compris (valeur 0), du plus ancien au plus recent -----
        LocalDate aujourdhui = LocalDate.now();
        YearMonth courant = YearMonth.from(aujourdhui);
        YearMonth premier = courant.minusMonths(NB_MOIS - 1L);
        // le mois en cours est compare a la MEME periode du mois precedent :
        // comparer a un mois complet ferait toujours apparaitre une baisse en debut de mois.
        LocalDate debutMois = aujourdhui.withDayOfMonth(1);
        BigDecimal evolutionMoisEnCours = evolution(
                rapportRepository.synthese(debutMois, aujourdhui).getCaNetHt(),
                rapportRepository.synthese(debutMois.minusMonths(1), aujourdhui.minusMonths(1)).getCaNetHt());
        // un mois de plus en arriere pour calculer l'evolution du premier mois affiche
        Map<YearMonth, BigDecimal[]> donnees = rapportRepository.parMois(premier.minusMonths(1).atDay(1));
        List<String> libelles = new ArrayList<>();
        List<BigDecimal> ca = new ArrayList<>();
        List<BigDecimal> marge = new ArrayList<>();
        List<BigDecimal> evolutions = new ArrayList<>();
        BigDecimal caPrecedent = donnees.getOrDefault(premier.minusMonths(1), new BigDecimal[]{BigDecimal.ZERO})[0];
        for (YearMonth m = premier; !m.isAfter(courant); m = m.plusMonths(1)) {
            BigDecimal[] v = donnees.getOrDefault(m, new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO});
            libelles.add(libelleMois(m));
            ca.add(v[0]);
            marge.add(v[0].subtract(v[1]));
            evolutions.add(m.equals(courant) ? evolutionMoisEnCours : evolution(v[0], caPrecedent));
            caPrecedent = v[0];
        }

        return new TableauAccueil(receptions, ventes, retards, ruptures,
                rapport, stockRepository.valeurTotaleRevient(), evolutionCa, libellePrecedente, courbe,
                libelles, ca, marge, evolutions);
    }

    /**
     * Courbe du CA net : un point par jour si la periode est courte, sinon par mois.
     * La periode precedente est alignee rang par rang (1er jour avec 1er jour, etc.).
     */
    private TableauAccueil.Courbe courbe(LocalDate du, LocalDate au, LocalDate duPrec, LocalDate auPrec,
                                         String libellePrecedente) {
        Map<LocalDate, BigDecimal> jours = rapportRepository.caParJour(du, au);
        Map<LocalDate, BigDecimal> joursPrec = rapportRepository.caParJour(duPrec, auPrec);
        List<String> libelles = new ArrayList<>();
        List<BigDecimal> ca = new ArrayList<>();
        List<BigDecimal> caPrec = new ArrayList<>();
        boolean parJour = ChronoUnit.DAYS.between(du, au) + 1 <= MAX_JOURS_COURBE;

        if (parJour) {
            long n = ChronoUnit.DAYS.between(du, au) + 1;
            for (int i = 0; i < n; i++) {
                LocalDate j = du.plusDays(i);
                LocalDate jp = duPrec.plusDays(i);
                libelles.add(j.format(JOUR));
                ca.add(jours.getOrDefault(j, BigDecimal.ZERO));
                caPrec.add(jp.isAfter(auPrec) ? null : joursPrec.getOrDefault(jp, BigDecimal.ZERO));
            }
        } else {
            Map<YearMonth, BigDecimal> mois = parMois(jours, du, au);
            Map<YearMonth, BigDecimal> moisPrec = parMois(joursPrec, duPrec, auPrec);
            List<BigDecimal> valeursPrec = new ArrayList<>(moisPrec.values());
            int i = 0;
            for (Map.Entry<YearMonth, BigDecimal> e : mois.entrySet()) {
                libelles.add(libelleMois(e.getKey()));
                ca.add(e.getValue());
                caPrec.add(i < valeursPrec.size() ? valeursPrec.get(i) : null);
                i++;
            }
        }
        return new TableauAccueil.Courbe(parJour, libelles, ca, caPrec, libelle(du, au), libellePrecedente);
    }

    /** Cumule les CA journaliers par mois, chaque mois de la periode present (0 si aucune vente). */
    private static Map<YearMonth, BigDecimal> parMois(Map<LocalDate, BigDecimal> jours, LocalDate du, LocalDate au) {
        Map<YearMonth, BigDecimal> resultat = new LinkedHashMap<>();
        for (YearMonth m = YearMonth.from(du); !m.isAfter(YearMonth.from(au)); m = m.plusMonths(1)) {
            resultat.put(m, BigDecimal.ZERO);
        }
        jours.forEach((j, v) -> resultat.merge(YearMonth.from(j), v, BigDecimal::add));
        return resultat;
    }

    /**
     * Periode precedente « equivalente » :
     * <ul>
     *   <li>depuis le 1er janvier (meme annee) : memes dates l'annee precedente ;</li>
     *   <li>depuis le 1er d'un trimestre, sur plusieurs mois du trimestre : memes jours du trimestre precedent ;</li>
     *   <li>depuis le 1er d'un mois : decalee d'autant de mois que la periode en couvre
     *       (1er-28 nov. -> 1er-28 oct. ; octobre entier -> septembre entier) ;</li>
     *   <li>sinon : le meme nombre de jours, juste avant.</li>
     * </ul>
     */
    static LocalDate[] periodePrecedente(LocalDate du, LocalDate au) {
        if (du.getDayOfYear() == 1 && au.getYear() == du.getYear()
                && YearMonth.from(au).isAfter(YearMonth.from(du).plusMonths(2))) {
            return new LocalDate[]{du.minusYears(1), au.minusYears(1)};
        }
        if (du.getDayOfMonth() == 1) {
            long mois = ChronoUnit.MONTHS.between(YearMonth.from(du), YearMonth.from(au)) + 1;
            boolean debutTrimestre = (du.getMonthValue() - 1) % 3 == 0;
            boolean memeTrimestre = (du.getMonthValue() - 1) / 3 == (au.getMonthValue() - 1) / 3
                    && du.getYear() == au.getYear();
            if (debutTrimestre && memeTrimestre && mois > 1) {
                mois = 3;
            }
            // une fin de mois reste une fin de mois (30 sept. -> 31 août)
            LocalDate fin = au.equals(YearMonth.from(au).atEndOfMonth())
                    ? YearMonth.from(au).minusMonths(mois).atEndOfMonth()
                    : au.minusMonths(mois);
            return new LocalDate[]{du.minusMonths(mois), fin};
        }
        long jours = ChronoUnit.DAYS.between(du, au) + 1;
        return new LocalDate[]{du.minusDays(jours), au.minusDays(jours)};
    }

    /** « 1er–28 nov. 2026 », « 3 nov. – 2 déc. 2026 », « 15 déc. 2025 – 14 janv. 2026 ». */
    static String libelle(LocalDate du, LocalDate au) {
        if (du.equals(au)) {
            return jour(du) + " " + mois(du) + " " + du.getYear();
        }
        if (du.getYear() == au.getYear() && du.getMonth() == au.getMonth()) {
            return jour(du) + "–" + au.getDayOfMonth() + " " + mois(au) + " " + au.getYear();
        }
        if (du.getYear() == au.getYear()) {
            return jour(du) + " " + mois(du) + " – " + jour(au) + " " + mois(au) + " " + au.getYear();
        }
        return jour(du) + " " + mois(du) + " " + du.getYear() + " – " + jour(au) + " " + mois(au) + " " + au.getYear();
    }

    private static String jour(LocalDate d) {
        return d.getDayOfMonth() == 1 ? "1er" : String.valueOf(d.getDayOfMonth());
    }

    private static String mois(LocalDate d) {
        return d.getMonth().getDisplayName(TextStyle.SHORT, FR);
    }

    private static String libelleMois(YearMonth m) {
        return m.getMonth().getDisplayName(TextStyle.SHORT, FR) + " " + String.valueOf(m.getYear()).substring(2);
    }

    /** Variation en % de « actuel » par rapport a « reference » ; null si la reference est nulle. */
    private static BigDecimal evolution(BigDecimal actuel, BigDecimal reference) {
        if (reference == null || reference.signum() <= 0) {
            return null;
        }
        return actuel.subtract(reference).multiply(BigDecimal.valueOf(100)).divide(reference, 1, RoundingMode.HALF_UP);
    }
}

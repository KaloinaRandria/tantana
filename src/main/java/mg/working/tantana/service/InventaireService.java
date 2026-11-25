package mg.working.tantana.service;

import lombok.RequiredArgsConstructor;
import mg.working.tantana.dto.ArticleAInventorier;
import mg.working.tantana.dto.ComptageForm;
import mg.working.tantana.dto.InventaireForm;
import mg.working.tantana.entity.Article;
import mg.working.tantana.entity.Inventaire;
import mg.working.tantana.entity.LigneInventaire;
import mg.working.tantana.entity.MotifEcart;
import mg.working.tantana.entity.StatutInventaire;
import mg.working.tantana.entity.Stock;
import mg.working.tantana.entity.TypeInventaire;
import mg.working.tantana.entity.Utilisateur;
import mg.working.tantana.exception.ElementIntrouvableException;
import mg.working.tantana.exception.RegleMetierException;
import mg.working.tantana.repository.ArticleRepository;
import mg.working.tantana.repository.InventaireRepository;
import mg.working.tantana.repository.StockRepository;
import mg.working.tantana.repository.UtilisateurRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Inventaires physiques, selon la pratique standard :
 * <ol>
 *   <li>ouverture : photo du stock theorique et des couts, articles geles ;</li>
 *   <li>comptage a l'aveugle (le compteur ne voit pas le theorique), en une ou plusieurs fois ;</li>
 *   <li>fin du comptage : les ecarts apparaissent et doivent etre justifies ;</li>
 *   <li>validation par un CHECKER qui n'a ni cree ni compte : ajustements de stock au CMUP ;
 *       ou rejet avec motif : retour en comptage (recomptage).</li>
 * </ol>
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InventaireService {

    public static final String PREFIXE = "INV";
    /** Seuil de la classe A : articles qui portent 80 % de la valeur du stock. */
    private static final BigDecimal SEUIL_A = BigDecimal.valueOf(0.80);
    private static final BigDecimal SEUIL_B = BigDecimal.valueOf(0.95);

    private final InventaireRepository inventaireRepository;
    private final ArticleRepository articleRepository;
    private final StockRepository stockRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final NumerotationService numerotationService;
    private final StockService stockService;

    // ===================== Lecture =====================

    public Page<Inventaire> rechercher(String texte, StatutInventaire statut, Pageable pageable) {
        String t = texte == null ? "" : texte.trim();
        return statut == null ? inventaireRepository.rechercher(t, pageable)
                : inventaireRepository.rechercher(t, statut, pageable);
    }

    public Inventaire trouverAvecDetails(Long id) {
        return inventaireRepository.findAvecDetails(id)
                .orElseThrow(() -> new ElementIntrouvableException("Inventaire", id));
    }

    public long nombreAValider() {
        return inventaireRepository.countByStatut(StatutInventaire.A_VALIDER);
    }

    /**
     * Articles pouvant etre inventories (actifs, ou inactifs avec du stock), tries par valeur
     * decroissante, avec leur classe ABC : la bonne pratique est de compter la classe A
     * souvent (chaque mois), la B chaque trimestre, la C une ou deux fois par an.
     */
    public List<ArticleAInventorier> articlesInventoriables() {
        Map<Long, Stock> stocks = stockRepository.findAll().stream()
                .collect(Collectors.toMap(s -> s.getArticle().getId(), Function.identity()));
        Map<Long, String> geles = stockService.articlesGeles();

        record Candidat(Article article, BigDecimal quantite, BigDecimal valeur) {
        }
        List<Candidat> candidats = articleRepository.findAll().stream()
                .map(a -> {
                    Stock s = stocks.get(a.getId());
                    BigDecimal q = s == null ? BigDecimal.ZERO : s.getQuantite();
                    BigDecimal v = s == null ? BigDecimal.ZERO : s.getValeurRevient();
                    return new Candidat(a, q, v);
                })
                .filter(c -> c.article().isActif() || c.quantite().signum() != 0)
                .sorted(Comparator.comparing(Candidat::valeur).reversed()
                        .thenComparing(c -> c.article().getReference()))
                .toList();

        BigDecimal total = candidats.stream().map(Candidat::valeur).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal cumul = BigDecimal.ZERO;
        List<ArticleAInventorier> resultat = new ArrayList<>();
        for (Candidat c : candidats) {
            // la classe se decide sur le cumul AVANT l'article : le premier article est toujours A
            String classe = "C";
            if (total.signum() > 0 && c.valeur().signum() > 0) {
                BigDecimal part = cumul.divide(total, 4, RoundingMode.HALF_UP);
                classe = part.compareTo(SEUIL_A) < 0 ? "A" : part.compareTo(SEUIL_B) < 0 ? "B" : "C";
            }
            cumul = cumul.add(c.valeur());
            Article a = c.article();
            resultat.add(new ArticleAInventorier(a.getId(), a.getReference(), a.getDesignation(), a.getUnite().getLibelle(),
                    c.quantite(), c.valeur(), classe, a.isActif(), geles.get(a.getId())));
        }
        return resultat;
    }

    // ===================== Ouverture =====================

    @Transactional
    public Inventaire creer(InventaireForm form, String username) {
        Utilisateur createur = utilisateur(username);
        Map<Long, ArticleAInventorier> disponibles = articlesInventoriables().stream()
                .collect(Collectors.toMap(ArticleAInventorier::id, Function.identity(), (x, y) -> x, LinkedHashMap::new));

        List<Long> ids;
        if (form.getType() == TypeInventaire.COMPLET) {
            ids = new ArrayList<>(disponibles.keySet());
            List<String> geles = disponibles.values().stream().filter(ArticleAInventorier::isGele)
                    .map(ArticleAInventorier::inventaireGel).distinct().toList();
            if (!geles.isEmpty()) {
                throw new RegleMetierException("Un inventaire est déjà ouvert (" + String.join(", ", geles)
                        + ") : validez-le ou annulez-le avant de lancer un inventaire complet.");
            }
        } else {
            ids = form.getArticleIds() == null ? List.of() : form.getArticleIds().stream().distinct().toList();
            if (ids.isEmpty()) {
                throw new RegleMetierException("articleIds", "Cochez au moins un article à compter.");
            }
            for (Long id : ids) {
                ArticleAInventorier a = disponibles.get(id);
                if (a == null) {
                    throw new RegleMetierException("articleIds", "Article inconnu ou inactif sans stock.");
                }
                if (a.isGele()) {
                    throw new RegleMetierException("articleIds", a.reference() + " est déjà dans l'inventaire "
                            + a.inventaireGel() + " en cours.");
                }
            }
        }
        if (ids.isEmpty()) {
            throw new RegleMetierException("Aucun article à inventorier.");
        }

        Inventaire inventaire = new Inventaire();
        inventaire.setType(form.getType());
        inventaire.setDateInventaire(form.getDateInventaire());
        inventaire.setObservations(texte(form.getObservations()));
        inventaire.setCreePar(createur);

        // Photo du stock : chaque stock est verrouille le temps de la creation, une vente en cours
        // de validation sur le meme article passe avant ou apres, jamais au milieu.
        Map<Long, Article> articles = articleRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(Article::getId, Function.identity()));
        ids.stream().map(articles::get)
                .sorted(Comparator.comparing(Article::getReference))
                .forEach(article -> {
                    Stock stock = stockRepository.findPourMiseAJour(article.getId()).orElse(null);
                    LigneInventaire ligne = new LigneInventaire();
                    ligne.setArticle(article);
                    ligne.setQuantiteTheorique(stock == null ? BigDecimal.ZERO : stock.getQuantite());
                    ligne.setCmup(stock == null ? BigDecimal.ZERO : stock.getCmup());
                    ligne.setCmupRevient(stock == null ? BigDecimal.ZERO : stock.getCmupRevient());
                    inventaire.ajouterLigne(ligne);
                });

        inventaire.setNumero(numerotationService.prochainNumero(PREFIXE, inventaire.getDateInventaire()));
        return inventaireRepository.save(inventaire);
    }

    // ===================== Comptage =====================

    /** Enregistre les quantites comptees ; si demande, termine le comptage (envoi en validation). */
    @Transactional
    public Inventaire enregistrerComptage(Long id, ComptageForm form, String username) {
        Inventaire inventaire = trouverAvecDetails(id);
        verifierVersion(inventaire, form);
        if (!inventaire.isComptageModifiable()) {
            throw new RegleMetierException("Cet inventaire est " + inventaire.getStatut().getLibelle().toLowerCase()
                    + " : le comptage ne peut plus être modifié.");
        }
        Map<Long, LigneInventaire> lignes = lignesParId(inventaire);
        for (ComptageForm.Ligne saisie : form.getLignes()) {
            LigneInventaire ligne = lignes.get(saisie.getId());
            if (ligne == null) {
                throw new RegleMetierException("Ligne d'inventaire inconnue.");
            }
            ligne.setQuantiteComptee(saisie.getQuantiteComptee());
            ligne.setCommentaire(texte(saisie.getCommentaire()));
        }
        inventaire.setComptePar(utilisateur(username));

        if (form.isSoumettre()) {
            long restants = inventaire.getNombreLignes() - inventaire.getNombreComptees();
            if (restants > 0) {
                throw new RegleMetierException(restants + " article(s) pas encore compté(s). Saisissez 0 pour un article absent.");
            }
            inventaire.setStatut(StatutInventaire.A_VALIDER);
            inventaire.setDateSoumission(LocalDateTime.now());
        }
        return inventaire;
    }

    /** Justification des ecarts (motif + commentaire), apres la fin du comptage. */
    @Transactional
    public Inventaire justifier(Long id, ComptageForm form) {
        Inventaire inventaire = trouverAvecDetails(id);
        verifierVersion(inventaire, form);
        if (inventaire.getStatut() != StatutInventaire.A_VALIDER) {
            throw new RegleMetierException("Les écarts se justifient une fois le comptage terminé, avant la validation.");
        }
        Map<Long, LigneInventaire> lignes = lignesParId(inventaire);
        for (ComptageForm.Ligne saisie : form.getLignes()) {
            LigneInventaire ligne = lignes.get(saisie.getId());
            if (ligne == null) {
                throw new RegleMetierException("Ligne d'inventaire inconnue.");
            }
            ligne.setMotifEcart(ligne.isAvecEcart() ? saisie.getMotifEcart() : null);
            ligne.setCommentaire(texte(saisie.getCommentaire()));
        }
        return inventaire;
    }

    // ===================== Controle (CHECKER) =====================

    @Transactional
    public Inventaire valider(Long id, String username) {
        Inventaire inventaire = trouverAvecDetails(id);
        Utilisateur validateur = verifierControle(inventaire, username);
        long nonJustifies = inventaire.getNombreNonJustifies();
        if (nonJustifies > 0) {
            throw new RegleMetierException(nonJustifies + " écart(s) sans motif : chaque écart doit être justifié "
                    + "(motif, et commentaire pour « Autre ») avant la validation.");
        }
        for (LigneInventaire ligne : inventaire.getLignes()) {
            if (ligne.isAvecEcart()) {
                stockService.ajusterInventaire(ligne.getArticle(), ligne.getEcart(), inventaire, validateur);
            }
        }
        inventaire.setStatut(StatutInventaire.VALIDE);
        inventaire.setValidePar(validateur);
        inventaire.setDateValidation(LocalDateTime.now());
        return inventaire;
    }

    /** Rejet : retour en comptage pour recompter (les quantites deja saisies sont conservees). */
    @Transactional
    public Inventaire rejeter(Long id, String motif, String username) {
        Inventaire inventaire = trouverAvecDetails(id);
        Utilisateur controleur = verifierControle(inventaire, username);
        if (motif == null || motif.isBlank()) {
            throw new RegleMetierException("Indiquez ce qu'il faut recompter et pourquoi.");
        }
        inventaire.setStatut(StatutInventaire.EN_COMPTAGE);
        inventaire.setMotifRejet(motif.trim());
        inventaire.setRejetePar(controleur);
        inventaire.setDateRejet(LocalDateTime.now());
        inventaire.setDateSoumission(null);
        return inventaire;
    }

    /** Annulation : aucun effet sur le stock, les articles sont degeles. */
    @Transactional
    public Inventaire annuler(Long id, String motif, String username) {
        Inventaire inventaire = trouverAvecDetails(id);
        if (!inventaire.getStatut().estOuvert()) {
            throw new RegleMetierException("Seul un inventaire en cours peut être annulé.");
        }
        if (motif == null || motif.isBlank()) {
            throw new RegleMetierException("Indiquez le motif de l'annulation.");
        }
        inventaire.setStatut(StatutInventaire.ANNULE);
        inventaire.setMotifAnnulation(motif.trim());
        inventaire.setAnnulePar(utilisateur(username));
        inventaire.setDateAnnulation(LocalDateTime.now());
        return inventaire;
    }

    // ===================== Regles =====================

    /** Principe des 4 yeux : le controleur n'a ni ouvert ni compte l'inventaire. */
    private Utilisateur verifierControle(Inventaire inventaire, String username) {
        if (inventaire.getStatut() != StatutInventaire.A_VALIDER) {
            throw new RegleMetierException("Cet inventaire est " + inventaire.getStatut().getLibelle().toLowerCase()
                    + " : il ne peut pas être contrôlé.");
        }
        Utilisateur controleur = utilisateur(username);
        Set<Long> intervenants = new HashSet<>();
        intervenants.add(inventaire.getCreePar().getId());
        if (inventaire.getComptePar() != null) {
            intervenants.add(inventaire.getComptePar().getId());
        }
        if (intervenants.contains(controleur.getId())) {
            throw new RegleMetierException("Vous avez ouvert ou compté cet inventaire : il doit être contrôlé par une autre personne.");
        }
        return controleur;
    }

    private void verifierVersion(Inventaire inventaire, ComptageForm form) {
        if (form.getVersion() == null || !form.getVersion().equals(inventaire.getVersion())) {
            throw new ObjectOptimisticLockingFailureException(Inventaire.class, inventaire.getId());
        }
    }

    private static Map<Long, LigneInventaire> lignesParId(Inventaire inventaire) {
        return inventaire.getLignes().stream().collect(Collectors.toMap(LigneInventaire::getId, Function.identity()));
    }

    /** Motifs proposes a la justification. */
    public MotifEcart[] motifs() {
        return MotifEcart.values();
    }

    private Utilisateur utilisateur(String username) {
        return utilisateurRepository.findByUsernameIgnoreCase(username)
                .orElseThrow(() -> new RegleMetierException("Utilisateur connecté introuvable"));
    }

    private static String texte(String valeur) {
        return valeur == null || valeur.isBlank() ? null : valeur.trim();
    }
}

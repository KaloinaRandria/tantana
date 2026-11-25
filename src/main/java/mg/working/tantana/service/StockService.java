package mg.working.tantana.service;

import lombok.RequiredArgsConstructor;
import mg.working.tantana.entity.Article;
import mg.working.tantana.entity.Avoir;
import mg.working.tantana.entity.Inventaire;
import mg.working.tantana.entity.MouvementStock;
import mg.working.tantana.entity.Reception;
import mg.working.tantana.entity.StatutInventaire;
import mg.working.tantana.entity.Stock;
import mg.working.tantana.entity.TypeMouvement;
import mg.working.tantana.entity.Utilisateur;
import mg.working.tantana.entity.Vente;
import mg.working.tantana.exception.RegleMetierException;
import mg.working.tantana.repository.InventaireRepository;
import mg.working.tantana.repository.MouvementStockRepository;
import mg.working.tantana.repository.StockRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Toute modification du stock passe par ce service : il met a jour la quantite
 * et le CMUP, et ecrit le mouvement correspondant (tracabilite).
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StockService {

    private final StockRepository stockRepository;
    private static final Set<StatutInventaire> INVENTAIRES_OUVERTS =
            EnumSet.of(StatutInventaire.EN_COMPTAGE, StatutInventaire.A_VALIDER);

    private final MouvementStockRepository mouvementRepository;
    private final InventaireRepository inventaireRepository;

    public Page<Stock> rechercher(String texte, Pageable pageable) {
        return stockRepository.rechercher(texte == null ? "" : texte.trim(), pageable);
    }

    public BigDecimal valeurTotale() {
        return stockRepository.valeurTotale();
    }

    public BigDecimal valeurTotaleRevient() {
        return stockRepository.valeurTotaleRevient();
    }

    public Optional<Stock> trouverParArticle(Long articleId) {
        return stockRepository.findByArticleId(articleId);
    }

    public Page<MouvementStock> mouvements(Long articleId, int page) {
        return mouvementRepository.findByArticleId(articleId, PageRequest.of(Math.max(page, 0), 30,
                Sort.by(Sort.Direction.DESC, "dateMouvement").and(Sort.by(Sort.Direction.DESC, "id"))));
    }

    /** Quantite disponible par article (0 si jamais recu). */
    public Map<Long, BigDecimal> disponibles(Collection<Long> articleIds) {
        Map<Long, BigDecimal> resultat = new HashMap<>();
        articleIds.forEach(id -> resultat.put(id, BigDecimal.ZERO));
        stockRepository.findByArticleIdIn(articleIds)
                .forEach(s -> resultat.put(s.getArticle().getId(), s.getQuantite()));
        return resultat;
    }

    /** Couts actuels (CMUP de revient) par article, pour detecter les ventes a perte. */
    public Map<Long, BigDecimal> coutsRevient(Collection<Long> articleIds) {
        Map<Long, BigDecimal> resultat = new HashMap<>();
        stockRepository.findByArticleIdIn(articleIds)
                .forEach(s -> resultat.put(s.getArticle().getId(), s.getCmupRevient()));
        return resultat;
    }

    /**
     * Sortie de stock pour une vente validee. Verrouille le stock, refuse si la quantite
     * disponible est insuffisante (le stock ne devient jamais negatif), et sort au CMUP
     * sans le modifier. Retourne le stock pour que la vente fige ses couts.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public Stock sortirVente(Article article, BigDecimal quantite, Vente vente, Utilisateur utilisateur) {
        Stock stock = stockRepository.findPourMiseAJour(article.getId())
                .orElseThrow(() -> new RegleMetierException("Article " + article.getReference()
                        + " : aucun stock (jamais reçu)."));
        verifierNonGele(article, null);
        if (stock.getQuantite().compareTo(quantite) < 0) {
            throw new RegleMetierException("Stock insuffisant pour " + article.getReference() + " : "
                    + stock.getQuantite().stripTrailingZeros().toPlainString() + " disponible(s), "
                    + quantite.stripTrailingZeros().toPlainString() + " demandé(s).");
        }
        stock.sortir(quantite);

        MouvementStock mouvement = new MouvementStock();
        mouvement.setArticle(article);
        mouvement.setType(TypeMouvement.SORTIE_VENTE);
        mouvement.setQuantite(quantite);
        mouvement.setCoutUnitaire(stock.getCmup());
        mouvement.setCoutRevientUnitaire(stock.getCmupRevient());
        mouvement.setQuantiteApres(stock.getQuantite());
        mouvement.setCmupApres(stock.getCmup());
        mouvement.setCmupRevientApres(stock.getCmupRevient());
        mouvement.setReference(vente.getNumero());
        mouvement.setVente(vente);
        mouvement.setUtilisateur(utilisateur);
        mouvementRepository.save(mouvement);
        return stock;
    }

    /**
     * Retour client (avoir avec retour en stock) : la marchandise rentre au cout auquel
     * elle etait sortie (couts figes sur la ligne de vente), ce qui preserve les CMUP.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public MouvementStock retourVente(Article article, BigDecimal quantite, BigDecimal coutAchat,
                                      BigDecimal coutRevient, Avoir avoir, Utilisateur utilisateur) {
        Stock stock = stockRepository.findPourMiseAJour(article.getId())
                .orElseGet(() -> stockRepository.save(new Stock(article)));
        verifierNonGele(article, null);
        stock.entrer(quantite, coutAchat, coutRevient);

        MouvementStock mouvement = new MouvementStock();
        mouvement.setArticle(article);
        mouvement.setType(TypeMouvement.RETOUR_VENTE);
        mouvement.setQuantite(quantite);
        mouvement.setCoutUnitaire(coutAchat);
        mouvement.setCoutRevientUnitaire(coutRevient);
        mouvement.setQuantiteApres(stock.getQuantite());
        mouvement.setCmupApres(stock.getCmup());
        mouvement.setCmupRevientApres(stock.getCmupRevient());
        mouvement.setReference(avoir.getNumero());
        mouvement.setAvoir(avoir);
        mouvement.setUtilisateur(utilisateur);
        return mouvementRepository.save(mouvement);
    }

    /**
     * Entree en stock suite a une reception validee.
     * Doit s'executer dans la transaction de validation : si une etape echoue, rien n'est enregistre.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public MouvementStock entrerAchat(Article article, BigDecimal quantite, BigDecimal coutAchat,
                                      BigDecimal coutRevient, Reception reception, Utilisateur utilisateur) {
        Stock stock = stockRepository.findPourMiseAJour(article.getId())
                .orElseGet(() -> stockRepository.save(new Stock(article)));
        verifierNonGele(article, null);
        stock.entrer(quantite, coutAchat, coutRevient);

        MouvementStock mouvement = new MouvementStock();
        mouvement.setArticle(article);
        mouvement.setType(TypeMouvement.ENTREE_ACHAT);
        mouvement.setQuantite(quantite);
        mouvement.setCoutUnitaire(coutAchat);
        mouvement.setCoutRevientUnitaire(coutRevient);
        mouvement.setQuantiteApres(stock.getQuantite());
        mouvement.setCmupApres(stock.getCmup());
        mouvement.setCmupRevientApres(stock.getCmupRevient());
        mouvement.setReference(reception.getNumero());
        mouvement.setReception(reception);
        mouvement.setUtilisateur(utilisateur);
        return mouvementRepository.save(mouvement);
    }

    // ===================== Inventaires =====================

    /** Articles geles par un inventaire ouvert : id article -> numero d'inventaire. */
    public Map<Long, String> articlesGeles() {
        Map<Long, String> resultat = new HashMap<>();
        for (Object[] ligne : inventaireRepository.articlesGeles(INVENTAIRES_OUVERTS)) {
            resultat.put((Long) ligne[0], (String) ligne[1]);
        }
        return resultat;
    }

    /**
     * Ajustement d'inventaire : met le stock au niveau compte. L'ecart entre ou sort au CMUP
     * actuel, sans le modifier (un ecart d'inventaire n'est pas un achat).
     *
     * @param ecart quantite comptee - quantite theorique (non nul)
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public MouvementStock ajusterInventaire(Article article, BigDecimal ecart, Inventaire inventaire, Utilisateur utilisateur) {
        Stock stock = stockRepository.findPourMiseAJour(article.getId())
                .orElseGet(() -> stockRepository.save(new Stock(article)));
        verifierNonGele(article, inventaire.getId());

        BigDecimal quantite = ecart.abs();
        TypeMouvement type;
        if (ecart.signum() > 0) {
            type = TypeMouvement.AJUSTEMENT_POSITIF;
            stock.entrer(quantite, stock.getCmup(), stock.getCmupRevient());
        } else {
            type = TypeMouvement.AJUSTEMENT_NEGATIF;
            if (stock.getQuantite().compareTo(quantite) < 0) {
                throw new RegleMetierException("Article " + article.getReference() + " : le stock actuel ("
                        + stock.getQuantite().stripTrailingZeros().toPlainString()
                        + ") ne correspond plus au stock figé de l'inventaire. Annulez et recommencez l'inventaire.");
            }
            stock.sortir(quantite);
        }

        MouvementStock mouvement = new MouvementStock();
        mouvement.setArticle(article);
        mouvement.setType(type);
        mouvement.setQuantite(quantite);
        mouvement.setCoutUnitaire(stock.getCmup());
        mouvement.setCoutRevientUnitaire(stock.getCmupRevient());
        mouvement.setQuantiteApres(stock.getQuantite());
        mouvement.setCmupApres(stock.getCmup());
        mouvement.setCmupRevientApres(stock.getCmupRevient());
        mouvement.setReference(inventaire.getNumero());
        mouvement.setInventaire(inventaire);
        mouvement.setUtilisateur(utilisateur);
        return mouvementRepository.save(mouvement);
    }

    /**
     * Pendant un inventaire, ses articles sont geles : aucune entree ni sortie, sinon le
     * stock compte ne correspondrait plus au stock fige. Appele APRES le verrou du stock.
     *
     * @param inventaireAutorise inventaire qui a le droit de bouger l'article (sa propre validation)
     */
    private void verifierNonGele(Article article, Long inventaireAutorise) {
        List<String> bloquants = inventaireRepository.findContenantArticle(article.getId(), INVENTAIRES_OUVERTS).stream()
                .filter(i -> !i.getId().equals(inventaireAutorise))
                .map(Inventaire::getNumero)
                .toList();
        if (!bloquants.isEmpty()) {
            throw new RegleMetierException("Article " + article.getReference() + " en cours d'inventaire ("
                    + String.join(", ", bloquants) + ") : aucun mouvement de stock possible avant la validation "
                    + "ou l'annulation de l'inventaire.");
        }
    }
}

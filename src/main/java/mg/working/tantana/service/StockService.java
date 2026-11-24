package mg.working.tantana.service;

import lombok.RequiredArgsConstructor;
import mg.working.tantana.entity.Article;
import mg.working.tantana.entity.MouvementStock;
import mg.working.tantana.entity.Reception;
import mg.working.tantana.entity.Stock;
import mg.working.tantana.entity.TypeMouvement;
import mg.working.tantana.entity.Utilisateur;
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
import java.util.Optional;

/**
 * Toute modification du stock passe par ce service : il met a jour la quantite
 * et le CMUP, et ecrit le mouvement correspondant (tracabilite).
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StockService {

    private final StockRepository stockRepository;
    private final MouvementStockRepository mouvementRepository;

    public Page<Stock> rechercher(String texte, Pageable pageable) {
        return stockRepository.rechercher(texte == null ? "" : texte.trim(), pageable);
    }

    public BigDecimal valeurTotale() {
        return stockRepository.valeurTotale();
    }

    public Optional<Stock> trouverParArticle(Long articleId) {
        return stockRepository.findByArticleId(articleId);
    }

    public Page<MouvementStock> mouvements(Long articleId, int page) {
        return mouvementRepository.findByArticleId(articleId, PageRequest.of(Math.max(page, 0), 30,
                Sort.by(Sort.Direction.DESC, "dateMouvement").and(Sort.by(Sort.Direction.DESC, "id"))));
    }

    /**
     * Entree en stock suite a une reception validee.
     * Doit s'executer dans la transaction de validation : si une etape echoue, rien n'est enregistre.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public MouvementStock entrerAchat(Article article, BigDecimal quantite, BigDecimal coutUnitaire,
                                      Reception reception, Utilisateur utilisateur) {
        Stock stock = stockRepository.findPourMiseAJour(article.getId())
                .orElseGet(() -> stockRepository.save(new Stock(article)));
        stock.entrer(quantite, coutUnitaire);

        MouvementStock mouvement = new MouvementStock();
        mouvement.setArticle(article);
        mouvement.setType(TypeMouvement.ENTREE_ACHAT);
        mouvement.setQuantite(quantite);
        mouvement.setCoutUnitaire(coutUnitaire);
        mouvement.setQuantiteApres(stock.getQuantite());
        mouvement.setCmupApres(stock.getCmup());
        mouvement.setReference(reception.getNumero());
        mouvement.setReception(reception);
        mouvement.setUtilisateur(utilisateur);
        return mouvementRepository.save(mouvement);
    }
}

package mg.working.tantana.service;

import lombok.RequiredArgsConstructor;
import mg.working.tantana.dto.LigneTarif;
import mg.working.tantana.entity.Article;
import mg.working.tantana.entity.HistoriquePrixVente;
import mg.working.tantana.entity.Stock;
import mg.working.tantana.entity.Utilisateur;
import mg.working.tantana.exception.ElementIntrouvableException;
import mg.working.tantana.exception.RegleMetierException;
import mg.working.tantana.repository.ArticleRepository;
import mg.working.tantana.repository.HistoriquePrixVenteRepository;
import mg.working.tantana.repository.StockRepository;
import mg.working.tantana.repository.UtilisateurRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Prix de vente catalogue : consultation avec marges, modification tracee (ADMIN uniquement). */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TarifService {

    private final ArticleRepository articleRepository;
    private final StockRepository stockRepository;
    private final HistoriquePrixVenteRepository historiqueRepository;
    private final UtilisateurRepository utilisateurRepository;

    /** Articles actifs avec leurs couts et leur marge. */
    public Page<LigneTarif> lister(String texte, Pageable pageable) {
        Page<Article> articles = articleRepository.rechercher(texte == null ? "" : texte.trim(), true, pageable);
        Map<Long, Stock> stocks = stockRepository.findByArticleIdIn(
                        articles.getContent().stream().map(Article::getId).toList()).stream()
                .collect(Collectors.toMap(s -> s.getArticle().getId(), Function.identity()));
        return articles.map(a -> new LigneTarif(a, stocks.get(a.getId())));
    }

    public List<HistoriquePrixVente> historique(Long articleId) {
        return historiqueRepository.findByArticleIdOrderByDateModificationDescIdDesc(articleId);
    }

    /**
     * Change le prix de vente HT et garde la trace (ancien prix, nouveau prix, cout de revient du moment).
     * Un prix sous le prix de revient est autorise (destockage, promotion) mais le motif devient obligatoire.
     */
    @Transactional
    public Article modifierPrix(Long articleId, BigDecimal nouveauPrix, String motif, String username) {
        Article article = articleRepository.findById(articleId)
                .orElseThrow(() -> new ElementIntrouvableException("Article", articleId));
        if (nouveauPrix == null || nouveauPrix.signum() < 0) {
            throw new RegleMetierException("Le prix de vente doit être positif ou nul.");
        }
        BigDecimal prix = nouveauPrix.setScale(2, RoundingMode.HALF_UP);
        if (article.getPrixVenteHt() != null && article.getPrixVenteHt().compareTo(prix) == 0) {
            throw new RegleMetierException("Le prix de " + article.getReference() + " est déjà de ce montant.");
        }

        BigDecimal revient = stockRepository.findByArticleId(articleId).map(Stock::getCmupRevient).orElse(null);
        String motifNettoye = motif == null || motif.isBlank() ? null : motif.trim();
        if (revient != null && prix.compareTo(revient) < 0 && motifNettoye == null) {
            throw new RegleMetierException("Ce prix est inférieur au prix de revient : indiquez le motif (destockage, promotion…).");
        }

        Utilisateur utilisateur = utilisateurRepository.findByUsernameIgnoreCase(username)
                .orElseThrow(() -> new RegleMetierException("Utilisateur connecté introuvable"));

        HistoriquePrixVente trace = new HistoriquePrixVente();
        trace.setArticle(article);
        trace.setAncienPrix(article.getPrixVenteHt());
        trace.setNouveauPrix(prix);
        trace.setCmupRevient(revient);
        trace.setMotif(motifNettoye);
        trace.setModifiePar(utilisateur);
        historiqueRepository.save(trace);

        article.setPrixVenteHt(prix);
        return article;
    }
}

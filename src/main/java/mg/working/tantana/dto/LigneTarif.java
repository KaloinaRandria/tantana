package mg.working.tantana.dto;

import mg.working.tantana.entity.Article;
import mg.working.tantana.entity.Stock;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Une ligne de la page Tarifs : prix d'achat, prix de revient, prix de vente et marge d'un article.
 * Marge = prix de vente HT - prix de revient ; taux de marque = marge / prix de vente HT.
 */
public record LigneTarif(Article article, Stock stock) {

    // getters classiques pour Thymeleaf (${t.article.reference})
    public Article getArticle() {
        return article;
    }

    public Stock getStock() {
        return stock;
    }

    public BigDecimal getQuantite() {
        return stock == null ? BigDecimal.ZERO : stock.getQuantite();
    }

    /** null si l'article n'a jamais ete recu. */
    public BigDecimal getPrixAchat() {
        return stock == null ? null : stock.getCmup();
    }

    public BigDecimal getPrixRevient() {
        return stock == null ? null : stock.getCmupRevient();
    }

    public BigDecimal getPrixVente() {
        return article.getPrixVenteHt();
    }

    public BigDecimal getMarge() {
        if (getPrixVente() == null || getPrixRevient() == null) {
            return null;
        }
        return getPrixVente().subtract(getPrixRevient());
    }

    /** Marge en % du prix de vente (taux de marque). */
    public BigDecimal getTauxMarque() {
        BigDecimal marge = getMarge();
        if (marge == null || getPrixVente().signum() == 0) {
            return null;
        }
        return marge.multiply(BigDecimal.valueOf(100)).divide(getPrixVente(), 1, RoundingMode.HALF_UP);
    }

    /** Vendre a ce prix ferait perdre de l'argent. */
    public boolean isSousRevient() {
        BigDecimal marge = getMarge();
        return marge != null && marge.signum() < 0;
    }
}

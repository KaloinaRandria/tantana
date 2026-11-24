package mg.working.tantana.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * Donnees de la page d'accueil. Les montants financiers (ca, marge...) ne sont remplis
 * que pour les roles ADMIN / CHECKER ; les autres voient les indicateurs operationnels.
 */
public record TableauAccueil(
        // operationnel
        long receptionsAValider,
        long ventesAControler,
        long commandesEnRetard,
        long articlesSansStock,
        // financier du mois en cours (null si non autorise)
        RapportVentes.Synthese moisEnCours,
        BigDecimal encaisseMois,
        BigDecimal creances,
        BigDecimal valeurStockRevient,
        // series des graphiques
        List<String> moisLibelles,
        List<BigDecimal> caParMois,
        List<BigDecimal> margeParMois,
        List<RapportVentes.LigneMarge> topArticles,
        List<RapportVentes.Encaissement> encaissements) {

    public boolean isFinancier() {
        return moisEnCours != null;
    }

    public long getReceptionsAValider() { return receptionsAValider; }
    public long getVentesAControler() { return ventesAControler; }
    public long getCommandesEnRetard() { return commandesEnRetard; }
    public long getArticlesSansStock() { return articlesSansStock; }
    public RapportVentes.Synthese getMoisEnCours() { return moisEnCours; }
    public BigDecimal getEncaisseMois() { return encaisseMois; }
    public BigDecimal getCreances() { return creances; }
    public BigDecimal getValeurStockRevient() { return valeurStockRevient; }
    public List<String> getMoisLibelles() { return moisLibelles; }
    public List<BigDecimal> getCaParMois() { return caParMois; }
    public List<BigDecimal> getMargeParMois() { return margeParMois; }
    public List<RapportVentes.LigneMarge> getTopArticles() { return topArticles; }
    public List<RapportVentes.Encaissement> getEncaissements() { return encaissements; }

    // ----- series a plat pour les graphiques (lues en JavaScript) -----
    public List<String> getEncaissementsLibelles() { return encaissements.stream().map(e -> e.mode().getLibelle()).toList(); }
    public List<BigDecimal> getEncaissementsMontants() { return encaissements.stream().map(RapportVentes.Encaissement::montant).toList(); }
    public List<String> getArticlesLibelles() { return topArticles.stream().map(a -> a.libelle() + " · " + a.detail()).toList(); }
    public List<BigDecimal> getArticlesMarges() { return topArticles.stream().map(RapportVentes.LigneMarge::getMarge).toList(); }
}

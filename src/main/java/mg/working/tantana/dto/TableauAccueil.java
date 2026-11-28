package mg.working.tantana.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * Donnees de la page d'accueil (accueil et tableau de bord des ventes fusionnes).
 * Les montants financiers (rapport, courbes...) ne sont remplis que pour les roles
 * ADMIN / CHECKER ; les autres voient les indicateurs operationnels.
 */
public record TableauAccueil(
        // operationnel
        long receptionsAValider,
        long ventesAControler,
        long commandesEnRetard,
        long articlesSansStock,
        // financier sur la periode choisie (null si non autorise)
        RapportVentes.Tableau rapport,
        BigDecimal valeurStockRevient,
        // evolution du CA HT net vs la periode precedente equivalente (null si rien a comparer)
        BigDecimal evolutionCaPct,
        String periodeComparaison,
        // courbe d'evolution du CA sur la periode, et celle de la periode precedente
        Courbe courbe,
        // 12 derniers mois
        List<String> moisLibelles,
        List<BigDecimal> caParMois,
        List<BigDecimal> margeParMois,
        // evolution du CA de chaque mois vs le mois precedent (le mois en cours : a periode egale)
        List<BigDecimal> evolutionCaParMois) {

    /**
     * Points de la courbe du CA. {@code caPrecedent} est aligne point par point sur {@code ca}
     * (meme rang dans la periode precedente) ; null quand la periode precedente est plus courte.
     */
    public record Courbe(boolean parJour, List<String> libelles, List<BigDecimal> ca, List<BigDecimal> caPrecedent,
                         String periodeLibelle, String periodePrecedenteLibelle) {
        public boolean isParJour() { return parJour; }
        public List<String> getLibelles() { return libelles; }
        public List<BigDecimal> getCa() { return ca; }
        public List<BigDecimal> getCaPrecedent() { return caPrecedent; }
        public String getPeriodeLibelle() { return periodeLibelle; }
        public String getPeriodePrecedenteLibelle() { return periodePrecedenteLibelle; }
    }

    public boolean isFinancier() {
        return rapport != null;
    }

    public long getReceptionsAValider() { return receptionsAValider; }
    public long getVentesAControler() { return ventesAControler; }
    public long getCommandesEnRetard() { return commandesEnRetard; }
    public long getArticlesSansStock() { return articlesSansStock; }
    public RapportVentes.Tableau getRapport() { return rapport; }
    public BigDecimal getValeurStockRevient() { return valeurStockRevient; }
    public BigDecimal getEvolutionCaPct() { return evolutionCaPct; }
    public String getPeriodeComparaison() { return periodeComparaison; }
    public Courbe getCourbe() { return courbe; }
    public List<String> getMoisLibelles() { return moisLibelles; }
    public List<BigDecimal> getCaParMois() { return caParMois; }
    public List<BigDecimal> getMargeParMois() { return margeParMois; }
    public List<BigDecimal> getEvolutionCaParMois() { return evolutionCaParMois; }

    // ----- series a plat pour les graphiques (lues en JavaScript) -----
    public List<String> getEncaissementsLibelles() {
        return rapport.encaissements().stream().map(e -> e.mode().getLibelle()).toList();
    }

    public List<BigDecimal> getEncaissementsMontants() {
        return rapport.encaissements().stream().map(RapportVentes.Encaissement::montant).toList();
    }
}

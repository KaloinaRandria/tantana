package mg.working.tantana.dto;

import mg.working.tantana.entity.ModePaiement;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;

/** Donnees du tableau de bord des ventes sur une periode. */
public final class RapportVentes {

    private RapportVentes() {
    }

    private static BigDecimal taux(BigDecimal marge, BigDecimal ca) {
        return ca.signum() == 0 ? null : marge.multiply(BigDecimal.valueOf(100)).divide(ca, 1, RoundingMode.HALF_UP);
    }

    /** Chiffres globaux : ventes brutes, avoirs, et net (= ventes - avoirs). */
    public record Synthese(long nbFactures, BigDecimal caHt, BigDecimal coutRevient, BigDecimal ttc,
                           long nbAvoirs, BigDecimal avoirsHt, BigDecimal avoirsCout, BigDecimal avoirsTtc) {
        public BigDecimal getNbFactures() { return BigDecimal.valueOf(nbFactures); }
        public BigDecimal getNbAvoirs() { return BigDecimal.valueOf(nbAvoirs); }
        public BigDecimal getCaHt() { return caHt; }
        public BigDecimal getAvoirsHt() { return avoirsHt; }
        public BigDecimal getAvoirsTtc() { return avoirsTtc; }
        public BigDecimal getCaNetHt() { return caHt.subtract(avoirsHt); }
        public BigDecimal getCoutNet() { return coutRevient.subtract(avoirsCout); }
        public BigDecimal getMargeNette() { return getCaNetHt().subtract(getCoutNet()); }
        public BigDecimal getTauxMarque() { return taux(getMargeNette(), getCaNetHt()); }
        public BigDecimal getTtcNet() { return ttc.subtract(avoirsTtc); }
    }

    /** Marge par article ou par client (avoirs deduits). */
    public record LigneMarge(Long id, String libelle, String detail, BigDecimal quantite, BigDecimal caHt, BigDecimal cout) {
        public Long getId() { return id; }
        public String getLibelle() { return libelle; }
        public String getDetail() { return detail; }
        public BigDecimal getQuantite() { return quantite; }
        public BigDecimal getCaHt() { return caHt; }
        public BigDecimal getCout() { return cout; }
        public BigDecimal getMarge() { return caHt.subtract(cout); }
        public BigDecimal getTauxMarque() { return taux(getMarge(), caHt); }
    }

    /** Mode de paiement tel qu'affiche (libelle et icone lus en base). */
    public record ModeAffiche(String libelle, String icone) {
        public String getLibelle() { return libelle; }
        public String getIcone() { return icone; }
    }

    public record Encaissement(ModeAffiche mode, BigDecimal montant, long nombre) {
        public ModeAffiche getMode() { return mode; }
        public BigDecimal getMontant() { return montant; }
        public long getNombre() { return nombre; }
    }

    /** Ce que chaque client doit encore (toutes periodes confondues). */
    public record Creance(Long clientId, String client, BigDecimal reste, long nbFactures, LocalDate plusAncienne,
                          BigDecimal plafond) {
        public Long getClientId() { return clientId; }
        public String getClient() { return client; }
        public BigDecimal getReste() { return reste; }
        public long getNbFactures() { return nbFactures; }
        public LocalDate getPlusAncienne() { return plusAncienne; }
        public BigDecimal getPlafond() { return plafond; }
        public long getJours() { return java.time.temporal.ChronoUnit.DAYS.between(plusAncienne, LocalDate.now()); }
        public boolean isAuDessusDuPlafond() { return plafond.signum() > 0 && reste.compareTo(plafond) > 0; }
    }

    public record Tableau(LocalDate du, LocalDate au, Synthese synthese, List<LigneMarge> parArticle,
                         List<LigneMarge> parClient, List<Encaissement> encaissements, List<Creance> creances) {
        public LocalDate getDu() { return du; }
        public LocalDate getAu() { return au; }
        public Synthese getSynthese() { return synthese; }
        public List<LigneMarge> getParArticle() { return parArticle; }
        public List<LigneMarge> getParClient() { return parClient; }
        public List<Encaissement> getEncaissements() { return encaissements; }
        public List<Creance> getCreances() { return creances; }
        public BigDecimal getTotalEncaisse() {
            return encaissements.stream().map(Encaissement::montant).reduce(BigDecimal.ZERO, BigDecimal::add);
        }
        public BigDecimal getTotalCreances() {
            return creances.stream().map(Creance::reste).reduce(BigDecimal.ZERO, BigDecimal::add);
        }
    }
}

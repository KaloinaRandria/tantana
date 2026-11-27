package mg.working.tantana.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Un article a compter : photo du stock a l'ouverture + quantite comptee + justification de l'ecart. */
@Entity
@Table(name = "ligne_inventaire")
@Getter
@Setter
@NoArgsConstructor
public class LigneInventaire {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "inventaire_id", updatable = false)
    private Inventaire inventaire;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "article_id", updatable = false)
    private Article article;

    /** Stock du systeme au moment de l'ouverture (fige). */
    @Column(name = "quantite_theorique", nullable = false, precision = 15, scale = 3, updatable = false)
    private BigDecimal quantiteTheorique;

    @Column(nullable = false, precision = 18, scale = 4, updatable = false)
    private BigDecimal cmup;

    @Column(name = "cmup_revient", nullable = false, precision = 18, scale = 4, updatable = false)
    private BigDecimal cmupRevient;

    /** NULL tant que l'article n'a pas ete compte. */
    @Column(name = "quantite_comptee", precision = 15, scale = 3)
    private BigDecimal quantiteComptee;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "motif_ecart_id")
    private MotifEcart motifEcart;

    @Column(length = 255)
    private String commentaire;

    public boolean isComptee() {
        return quantiteComptee != null;
    }

    /** Compte - theorique (positif = surplus, negatif = manquant) ; null si pas encore compte. */
    public BigDecimal getEcart() {
        return quantiteComptee == null ? null : quantiteComptee.subtract(quantiteTheorique);
    }

    public boolean isAvecEcart() {
        return quantiteComptee != null && quantiteComptee.compareTo(quantiteTheorique) != 0;
    }

    /** Valeur de l'ecart au cout de revient fige (Ariary). */
    public BigDecimal getEcartValeur() {
        BigDecimal ecart = getEcart();
        return ecart == null ? BigDecimal.ZERO : ecart.multiply(cmupRevient).setScale(0, RoundingMode.HALF_UP);
    }

    /** Ecart en % du theorique (null si theorique nul ou pas encore compte). */
    public BigDecimal getEcartPct() {
        BigDecimal ecart = getEcart();
        if (ecart == null || quantiteTheorique.signum() == 0) {
            return null;
        }
        return ecart.multiply(BigDecimal.valueOf(100)).divide(quantiteTheorique, 1, RoundingMode.HALF_UP);
    }

    public BigDecimal getValeurTheorique() {
        return quantiteTheorique.multiply(cmupRevient).setScale(0, RoundingMode.HALF_UP);
    }

    /** Un ecart doit avoir un motif (et un commentaire si le motif l'exige, ex. « Autre »). */
    public boolean isJustifiee() {
        if (!isAvecEcart()) {
            return true;
        }
        if (motifEcart == null) {
            return false;
        }
        return !motifEcart.isCommentaireObligatoire() || (commentaire != null && !commentaire.isBlank());
    }
}

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

/** Ligne de vente : le prix et les couts sont figes (snapshot), jamais relus du catalogue apres coup. */
@Entity
@Table(name = "ligne_vente")
@Getter
@Setter
@NoArgsConstructor
public class LigneVente {

    private static final BigDecimal CENT = BigDecimal.valueOf(100);

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "vente_id")
    private Vente vente;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "article_id")
    private Article article;

    @Column(nullable = false, precision = 15, scale = 3)
    private BigDecimal quantite;

    @Column(name = "prix_catalogue_ht", nullable = false, precision = 18, scale = 2)
    private BigDecimal prixCatalogueHt;

    @Column(name = "remise_pct", nullable = false, precision = 5, scale = 2)
    private BigDecimal remisePct = BigDecimal.ZERO;

    /** Prix net apres remise. */
    @Column(name = "prix_unitaire_ht", nullable = false, precision = 18, scale = 2)
    private BigDecimal prixUnitaireHt;

    @Column(name = "montant_ht", nullable = false, precision = 18, scale = 2)
    private BigDecimal montantHt;

    /** Quantite deja annulee par avoir. */
    @Column(name = "quantite_avoir", nullable = false, precision = 15, scale = 3)
    private BigDecimal quantiteAvoir = BigDecimal.ZERO;

    @Column(name = "cout_achat_unitaire", precision = 18, scale = 4)
    private BigDecimal coutAchatUnitaire;

    @Column(name = "cout_revient_unitaire", precision = 18, scale = 4)
    private BigDecimal coutRevientUnitaire;

    /** Applique le prix catalogue, la remise et la quantite, et calcule le prix net et le montant. */
    public void appliquer(BigDecimal prixCatalogue, BigDecimal remise, BigDecimal qte) {
        prixCatalogueHt = prixCatalogue;
        remisePct = remise;
        quantite = qte;
        prixUnitaireHt = prixCatalogue.multiply(CENT.subtract(remise)).divide(CENT, 2, RoundingMode.HALF_UP);
        montantHt = prixUnitaireHt.multiply(qte).setScale(2, RoundingMode.HALF_UP);
    }

    /** Quantite pouvant encore faire l'objet d'un avoir. */
    public BigDecimal getQuantiteCreditable() {
        return quantite.subtract(quantiteAvoir).max(BigDecimal.ZERO);
    }

    public BigDecimal getMontantBrutHt() {
        return prixCatalogueHt.multiply(quantite);
    }

    /** Marge de la ligne (connue apres validation). */
    public BigDecimal getMarge() {
        return coutRevientUnitaire == null ? null
                : montantHt.subtract(coutRevientUnitaire.multiply(quantite)).setScale(0, RoundingMode.HALF_UP);
    }
}

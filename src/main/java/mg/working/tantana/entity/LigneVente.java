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

    /**
     * Applique le prix catalogue, le prix negocie et la quantite, et calcule la remise et le montant.
     * Le prix net est exactement le prix negocie ; la remise en % en est deduite, arrondie au
     * centieme SUPERIEUR pour que le controle du plafond ne laisse jamais passer un depassement.
     *
     * @param prixNegocie null = vendu au prix catalogue (aucune remise)
     */
    public void appliquer(BigDecimal prixCatalogue, BigDecimal prixNegocie, BigDecimal qte) {
        prixCatalogueHt = prixCatalogue;
        quantite = qte;
        if (prixNegocie == null || prixNegocie.compareTo(prixCatalogue) >= 0) {
            prixUnitaireHt = prixCatalogue.setScale(2, RoundingMode.HALF_UP);
            remisePct = BigDecimal.ZERO;
        } else {
            prixUnitaireHt = prixNegocie.setScale(2, RoundingMode.HALF_UP);
            remisePct = prixCatalogue.subtract(prixUnitaireHt).multiply(CENT)
                    .divide(prixCatalogue, 2, RoundingMode.UP).min(CENT);
        }
        montantHt = prixUnitaireHt.multiply(qte).setScale(2, RoundingMode.HALF_UP);
    }

    /** Vendu sous le prix catalogue (prix negocie avec le client) ? */
    public boolean isPrixNegocie() {
        return prixUnitaireHt.compareTo(prixCatalogueHt) < 0;
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

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

@Entity
@Table(name = "ligne_commande_achat")
@Getter
@Setter
@NoArgsConstructor
public class LigneCommandeAchat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "commande_id")
    private CommandeAchat commande;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "article_id")
    private Article article;

    @Column(name = "quantite_commandee", nullable = false, precision = 15, scale = 3)
    private BigDecimal quantiteCommandee;

    /** Prix unitaire dans la devise de la commande. */
    @Column(name = "prix_unitaire", nullable = false, precision = 18, scale = 4)
    private BigDecimal prixUnitaire;

    /** Cumul des quantites recues sur les receptions validees. */
    @Column(name = "quantite_recue", nullable = false, precision = 15, scale = 3)
    private BigDecimal quantiteRecue = BigDecimal.ZERO;

    /** Montant de la ligne en devise. */
    public BigDecimal getMontant() {
        return quantiteCommandee.multiply(prixUnitaire);
    }

    public BigDecimal getQuantiteRestante() {
        return quantiteCommandee.subtract(quantiteRecue).max(BigDecimal.ZERO);
    }
}

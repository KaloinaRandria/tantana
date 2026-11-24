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

@Entity
@Table(name = "ligne_reception")
@Getter
@Setter
@NoArgsConstructor
public class LigneReception {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "reception_id")
    private Reception reception;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ligne_commande_id")
    private LigneCommandeAchat ligneCommande;

    /** Quantite arrivee en bon etat : c'est elle qui entre en stock. */
    @Column(name = "quantite_recue", nullable = false, precision = 15, scale = 3)
    private BigDecimal quantiteRecue = BigDecimal.ZERO;

    /** Quantite abimee ou refusee : tracee mais n'entre pas en stock. */
    @Column(name = "quantite_refusee", nullable = false, precision = 15, scale = 3)
    private BigDecimal quantiteRefusee = BigDecimal.ZERO;

    @Column(length = 255)
    private String commentaire;

    /** Part des frais du transitaire par unite recue, figee a la validation. */
    @Column(name = "frais_unitaire", nullable = false, precision = 18, scale = 4)
    private BigDecimal fraisUnitaire = BigDecimal.ZERO;

    /** Prix d'achat unitaire : prix fournisseur converti au taux du jour de reception. */
    public BigDecimal getCoutUnitaireMga() {
        return ligneCommande.getPrixUnitaire().multiply(reception.getTauxChange()).setScale(4, RoundingMode.HALF_UP);
    }

    /** Valeur d'achat de la quantite recue (base de la repartition des frais). */
    public BigDecimal getValeurAchatMga() {
        return getCoutUnitaireMga().multiply(quantiteRecue);
    }

    /** Prix de revient unitaire : prix d'achat + part des frais. */
    public BigDecimal getCoutRevientUnitaireMga() {
        return getCoutUnitaireMga().add(fraisUnitaire);
    }
}

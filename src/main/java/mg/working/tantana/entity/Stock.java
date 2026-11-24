package mg.working.tantana.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

/** Etat du stock d'un article : quantite disponible et cout moyen pondere (CMUP) en Ariary. */
@Entity
@Table(name = "stock")
@Getter
@Setter
@NoArgsConstructor
public class Stock {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "article_id", unique = true, updatable = false)
    private Article article;

    @Column(nullable = false, precision = 15, scale = 3)
    private BigDecimal quantite = BigDecimal.ZERO;

    /** CMUP d'achat : prix fournisseur x taux du jour, sans les frais. */
    @Column(nullable = false, precision = 18, scale = 4)
    private BigDecimal cmup = BigDecimal.ZERO;

    /** CMUP de revient : prix d'achat + part des frais du transitaire. Base du calcul des marges. */
    @Column(name = "cmup_revient", nullable = false, precision = 18, scale = 4)
    private BigDecimal cmupRevient = BigDecimal.ZERO;

    @Column(name = "date_maj", nullable = false)
    private LocalDateTime dateMaj = LocalDateTime.now();

    @Version
    private Long version;

    public Stock(Article article) {
        this.article = article;
    }

    /**
     * Entree en stock. Chaque CMUP est recalcule de la meme facon :
     * (quantite actuelle x CMUP actuel + quantite entree x cout) / (quantite actuelle + quantite entree)
     *
     * @param coutAchat   prix fournisseur x taux du jour
     * @param coutRevient coutAchat + part des frais par unite
     */
    public void entrer(BigDecimal quantiteEntree, BigDecimal coutAchat, BigDecimal coutRevient) {
        cmup = moyennePonderee(cmup, quantiteEntree, coutAchat);
        cmupRevient = moyennePonderee(cmupRevient, quantiteEntree, coutRevient);
        quantite = quantite.add(quantiteEntree);
        dateMaj = LocalDateTime.now();
    }

    /** Sortie de stock (vente) : la quantite baisse, les CMUP ne changent pas. */
    public void sortir(BigDecimal quantiteSortie) {
        quantite = quantite.subtract(quantiteSortie);
        dateMaj = LocalDateTime.now();
    }

    private BigDecimal moyennePonderee(BigDecimal coutActuel, BigDecimal quantiteEntree, BigDecimal coutEntree) {
        if (quantite.signum() <= 0) {
            // stock vide (ou negatif) : l'ancien cout n'a plus de sens
            return coutEntree.setScale(4, RoundingMode.HALF_UP);
        }
        BigDecimal valeur = quantite.multiply(coutActuel).add(quantiteEntree.multiply(coutEntree));
        return valeur.divide(quantite.add(quantiteEntree), 4, RoundingMode.HALF_UP);
    }

    /** Valeur du stock en Ariary au prix d'achat. */
    public BigDecimal getValeur() {
        return quantite.multiply(cmup).setScale(0, RoundingMode.HALF_UP);
    }

    /** Valeur du stock en Ariary au prix de revient (frais compris). */
    public BigDecimal getValeurRevient() {
        return quantite.multiply(cmupRevient).setScale(0, RoundingMode.HALF_UP);
    }
}

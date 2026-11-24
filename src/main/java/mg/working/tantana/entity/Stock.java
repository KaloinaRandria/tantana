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

    @Column(nullable = false, precision = 18, scale = 4)
    private BigDecimal cmup = BigDecimal.ZERO;

    @Column(name = "date_maj", nullable = false)
    private LocalDateTime dateMaj = LocalDateTime.now();

    @Version
    private Long version;

    public Stock(Article article) {
        this.article = article;
    }

    /**
     * Entree en stock au cout unitaire donne. Nouveau CMUP :
     * (quantite actuelle x CMUP actuel + quantite entree x cout) / (quantite actuelle + quantite entree)
     */
    public void entrer(BigDecimal quantiteEntree, BigDecimal coutUnitaire) {
        BigDecimal nouvelleQuantite = quantite.add(quantiteEntree);
        if (quantite.signum() <= 0) {
            // stock vide (ou negatif) : l'ancien cout n'a plus de sens
            cmup = coutUnitaire.setScale(4, RoundingMode.HALF_UP);
        } else {
            BigDecimal valeur = quantite.multiply(cmup).add(quantiteEntree.multiply(coutUnitaire));
            cmup = valeur.divide(nouvelleQuantite, 4, RoundingMode.HALF_UP);
        }
        quantite = nouvelleQuantite;
        dateMaj = LocalDateTime.now();
    }

    /** Valeur du stock en Ariary. */
    public BigDecimal getValeur() {
        return quantite.multiply(cmup).setScale(0, RoundingMode.HALF_UP);
    }
}

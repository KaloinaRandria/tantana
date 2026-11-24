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
@Table(name = "ligne_avoir")
@Getter
@Setter
@NoArgsConstructor
public class LigneAvoir {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "avoir_id")
    private Avoir avoir;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ligne_vente_id")
    private LigneVente ligneVente;

    @Column(nullable = false, precision = 15, scale = 3)
    private BigDecimal quantite;

    /** Quantite x prix net de la facture (meme prix que celui facture). */
    @Column(name = "montant_ht", nullable = false, precision = 18, scale = 2)
    private BigDecimal montantHt;

    /** Vrai si la marchandise revient en stock (retour client) ; faux pour un geste commercial ou un produit detruit. */
    @Column(name = "retour_stock", nullable = false)
    private boolean retourStock;
}

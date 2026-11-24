package mg.working.tantana.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** Avoir AV-AAAA-NNNN : annule tout ou partie d'une facture. Definitif une fois cree. */
@Entity
@Table(name = "avoir")
@Getter
@Setter
@NoArgsConstructor
public class Avoir {

    private static final BigDecimal CENT = BigDecimal.valueOf(100);

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 20, updatable = false)
    private String numero;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "vente_id", updatable = false)
    private Vente vente;

    @Column(name = "date_avoir", nullable = false, updatable = false)
    private LocalDate dateAvoir = LocalDate.now();

    @Column(nullable = false, length = 255, updatable = false)
    private String motif;

    @Column(name = "taux_tva", nullable = false, precision = 5, scale = 2, updatable = false)
    private BigDecimal tauxTva;

    @Column(name = "total_ht", nullable = false, precision = 18, scale = 2)
    private BigDecimal totalHt = BigDecimal.ZERO;

    @Column(name = "total_tva", nullable = false, precision = 18, scale = 2)
    private BigDecimal totalTva = BigDecimal.ZERO;

    @Column(name = "total_ttc", nullable = false, precision = 18, scale = 2)
    private BigDecimal totalTtc = BigDecimal.ZERO;

    @Column(name = "cout_revient_total", nullable = false, precision = 18, scale = 2)
    private BigDecimal coutRevientTotal = BigDecimal.ZERO;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "saisi_par_id", updatable = false)
    private Utilisateur saisiPar;

    @Column(name = "date_creation", nullable = false, insertable = false, updatable = false)
    private LocalDateTime dateCreation;

    @OneToMany(mappedBy = "avoir", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id")
    private List<LigneAvoir> lignes = new ArrayList<>();

    public void ajouterLigne(LigneAvoir ligne) {
        ligne.setAvoir(this);
        lignes.add(ligne);
    }

    /** Memes regles que la facture : TVA sur le total HT, arrondie a l'Ariary. */
    public void calculerTotaux() {
        totalHt = lignes.stream().map(LigneAvoir::getMontantHt)
                .reduce(BigDecimal.ZERO, BigDecimal::add).setScale(0, RoundingMode.HALF_UP);
        totalTva = totalHt.multiply(tauxTva).divide(CENT, 0, RoundingMode.HALF_UP);
        totalTtc = totalHt.add(totalTva);
        coutRevientTotal = lignes.stream()
                .map(l -> l.getLigneVente().getCoutRevientUnitaire().multiply(l.getQuantite()))
                .reduce(BigDecimal.ZERO, BigDecimal::add).setScale(0, RoundingMode.HALF_UP);
    }
}

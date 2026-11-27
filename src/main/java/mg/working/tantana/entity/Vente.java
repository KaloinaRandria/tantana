package mg.working.tantana.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** Vente a un client. Une fois VALIDEE, c'est une facture : elle ne change plus. */
@Entity
@Table(name = "vente")
@Getter
@Setter
@NoArgsConstructor
public class Vente {

    private static final BigDecimal CENT = BigDecimal.valueOf(100);

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Numero de facture FV-AAAA-NNNN, attribue a la validation uniquement. */
    @Column(unique = true, length = 20)
    private String numero;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "client_id")
    private Client client;

    @Column(name = "date_vente", nullable = false)
    private LocalDate dateVente = LocalDate.now();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatutVente statut = StatutVente.BROUILLON;

    /** Taux de TVA applique (copie du parametre au moment de la vente). */
    @Column(name = "taux_tva", nullable = false, precision = 5, scale = 2)
    private BigDecimal tauxTva;

    @Column(name = "total_brut_ht", nullable = false, precision = 18, scale = 2)
    private BigDecimal totalBrutHt = BigDecimal.ZERO;

    @Column(name = "total_remise", nullable = false, precision = 18, scale = 2)
    private BigDecimal totalRemise = BigDecimal.ZERO;

    @Column(name = "total_ht", nullable = false, precision = 18, scale = 2)
    private BigDecimal totalHt = BigDecimal.ZERO;

    @Column(name = "total_tva", nullable = false, precision = 18, scale = 2)
    private BigDecimal totalTva = BigDecimal.ZERO;

    @Column(name = "total_ttc", nullable = false, precision = 18, scale = 2)
    private BigDecimal totalTtc = BigDecimal.ZERO;

    @Column(name = "cout_revient_total", precision = 18, scale = 2)
    private BigDecimal coutRevientTotal;

    @Column(name = "marge_totale", precision = 18, scale = 2)
    private BigDecimal margeTotale;

    /** Cumul des paiements non annules. */
    @Column(name = "montant_paye", nullable = false, precision = 18, scale = 2)
    private BigDecimal montantPaye = BigDecimal.ZERO;

    /** Cumul TTC des avoirs emis sur cette facture. */
    @Column(name = "montant_avoirs", nullable = false, precision = 18, scale = 2)
    private BigDecimal montantAvoirs = BigDecimal.ZERO;

    @Column(columnDefinition = "TEXT")
    private String observations;

    @Column(name = "motif_controle", columnDefinition = "TEXT")
    private String motifControle;

    @Column(name = "commentaire_controle", columnDefinition = "TEXT")
    private String commentaireControle;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "vendeur_id", updatable = false)
    private Utilisateur vendeur;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "controleur_id")
    private Utilisateur controleur;

    @Column(name = "date_creation", nullable = false, insertable = false, updatable = false)
    private LocalDateTime dateCreation;

    @Column(name = "date_controle")
    private LocalDateTime dateControle;

    @Column(name = "date_validation")
    private LocalDateTime dateValidation;

    @Version
    private Long version;

    @OneToMany(mappedBy = "vente", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id")
    private List<LigneVente> lignes = new ArrayList<>();

    public void ajouterLigne(LigneVente ligne) {
        ligne.setVente(this);
        lignes.add(ligne);
    }

    /**
     * Totaux de la facture. La TVA est calculee une seule fois sur le total HT,
     * puis arrondie a l'Ariary (pas d'ecarts d'arrondi ligne par ligne).
     */
    public void recalculerTotaux() {
        totalBrutHt = lignes.stream().map(LigneVente::getMontantBrutHt)
                .reduce(BigDecimal.ZERO, BigDecimal::add).setScale(0, RoundingMode.HALF_UP);
        totalHt = lignes.stream().map(LigneVente::getMontantHt)
                .reduce(BigDecimal.ZERO, BigDecimal::add).setScale(0, RoundingMode.HALF_UP);
        totalRemise = totalBrutHt.subtract(totalHt).max(BigDecimal.ZERO);
        totalTva = totalHt.multiply(tauxTva).divide(CENT, 0, RoundingMode.HALF_UP);
        totalTtc = totalHt.add(totalTva);
    }

    /** Couts et marge figes a la validation (prix de revient du moment). */
    public void figerMarge() {
        coutRevientTotal = lignes.stream()
                .map(l -> l.getCoutRevientUnitaire().multiply(l.getQuantite()))
                .reduce(BigDecimal.ZERO, BigDecimal::add).setScale(0, RoundingMode.HALF_UP);
        margeTotale = totalHt.subtract(coutRevientTotal);
    }

    /** Marge en % du chiffre d'affaires HT (taux de marque). */
    public BigDecimal getTauxMarque() {
        if (margeTotale == null || totalHt.signum() == 0) {
            return null;
        }
        return margeTotale.multiply(CENT).divide(totalHt, 1, RoundingMode.HALF_UP);
    }

    /** Montant TTC du apres avoirs. */
    public BigDecimal getNetAPayer() {
        return totalTtc.subtract(montantAvoirs).max(BigDecimal.ZERO);
    }

    public BigDecimal getResteAPayer() {
        return getNetAPayer().subtract(montantPaye).max(BigDecimal.ZERO);
    }

    /** Trop-percu a rembourser au client (paye plus que le net, apres un avoir). */
    public BigDecimal getTropPercu() {
        return montantPaye.subtract(getNetAPayer()).max(BigDecimal.ZERO);
    }

    public StatutPaiement getStatutPaiement() {
        if (getNetAPayer().signum() == 0 && montantAvoirs.signum() > 0) {
            return StatutPaiement.SOLDEE_PAR_AVOIR;
        }
        if (getResteAPayer().signum() == 0) {
            return StatutPaiement.PAYEE;
        }
        return montantPaye.signum() > 0 ? StatutPaiement.PARTIELLE : StatutPaiement.NON_PAYEE;
    }

    /** Taux de TVA de CETTE vente (fige a sa creation) superieur a 0. */
    public boolean isTvaApplicable() {
        return tauxTva != null && tauxTva.signum() > 0;
    }

    public boolean isModifiable() {
        return statut == StatutVente.BROUILLON;
    }

    /** Libelle court : numero de facture, ou « Brouillon n° id ». */
    public String getLibelle() {
        return numero != null ? numero : "Brouillon n° " + id;
    }
}

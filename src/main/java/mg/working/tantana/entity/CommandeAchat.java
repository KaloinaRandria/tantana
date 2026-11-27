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
import jakarta.persistence.PreUpdate;
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

@Entity
@Table(name = "commande_achat")
@Getter
@Setter
@NoArgsConstructor
public class CommandeAchat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 20, updatable = false)
    private String numero;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "fournisseur_id")
    private Fournisseur fournisseur;

    @Column(name = "date_commande", nullable = false)
    private LocalDate dateCommande;

    @Column(name = "date_arrivee_prevue", nullable = false)
    private LocalDate dateArriveePrevue;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "devise_id", nullable = false)
    private Devise devise;

    /** Nombre d'Ariary pour 1 unite de la devise. */
    @Column(name = "taux_change", nullable = false, precision = 18, scale = 6)
    private BigDecimal tauxChange;

    /** Total des lignes en devise (stocke pour les listes, recalcule a chaque enregistrement). */
    @Column(name = "montant_total", nullable = false, precision = 20, scale = 2)
    private BigDecimal montantTotal = BigDecimal.ZERO;

    @Column(name = "statut_id", nullable = false)
    private StatutCommande statut = StatutCommande.BROUILLON;

    @Column(columnDefinition = "TEXT")
    private String observations;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cree_par_id", updatable = false)
    private Utilisateur creePar;

    @Column(name = "date_creation", nullable = false, insertable = false, updatable = false)
    private LocalDateTime dateCreation;

    @Column(name = "date_modification")
    private LocalDateTime dateModification;

    /** Verrouillage optimiste : empeche deux utilisateurs d'ecraser leurs modifications. */
    @Version
    private Long version;

    @OneToMany(mappedBy = "commande", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id")
    private List<LigneCommandeAchat> lignes = new ArrayList<>();

    @PreUpdate
    void avantMiseAJour() {
        dateModification = LocalDateTime.now();
    }

    public void ajouterLigne(LigneCommandeAchat ligne) {
        ligne.setCommande(this);
        lignes.add(ligne);
    }

    public void recalculerMontantTotal() {
        montantTotal = lignes.stream()
                .map(LigneCommandeAchat::getMontant)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
    }

    /** Estimation en Ariary au taux de la commande (le cout reel sera calcule a la reception). */
    public BigDecimal getMontantTotalMga() {
        return montantTotal.multiply(tauxChange).setScale(0, RoundingMode.HALF_UP);
    }

    /** Date d'arrivee depassee alors que la marchandise est encore attendue. */
    public boolean isEnRetard() {
        return statut.estEnAttente() && dateArriveePrevue.isBefore(LocalDate.now());
    }

    /** Modifiable tant que rien n'a ete recu. */
    public boolean isModifiable() {
        return (statut == StatutCommande.BROUILLON || statut == StatutCommande.COMMANDEE) && !aDejaRecu();
    }

    public boolean isAnnulable() {
        return isModifiable();
    }

    public boolean aDejaRecu() {
        return lignes.stream().anyMatch(l -> l.getQuantiteRecue().signum() > 0);
    }
}

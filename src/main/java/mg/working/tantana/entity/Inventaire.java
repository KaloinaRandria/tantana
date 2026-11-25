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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Inventaire physique : le stock theorique est fige a l'ouverture, les articles sont geles
 * jusqu'a la validation (ou l'annulation), les ecarts sont ajustes a la validation.
 */
@Entity
@Table(name = "inventaire")
@Getter
@Setter
@NoArgsConstructor
public class Inventaire {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 20, updatable = false)
    private String numero;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20, updatable = false)
    private TypeInventaire type;

    @Column(name = "date_inventaire", nullable = false)
    private LocalDate dateInventaire;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatutInventaire statut = StatutInventaire.EN_COMPTAGE;

    @Column(columnDefinition = "TEXT")
    private String observations;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cree_par_id", updatable = false)
    private Utilisateur creePar;

    @Column(name = "date_creation", nullable = false, updatable = false)
    private LocalDateTime dateCreation = LocalDateTime.now();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "compte_par_id")
    private Utilisateur comptePar;

    @Column(name = "date_soumission")
    private LocalDateTime dateSoumission;

    @Column(name = "motif_rejet", columnDefinition = "TEXT")
    private String motifRejet;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rejete_par_id")
    private Utilisateur rejetePar;

    @Column(name = "date_rejet")
    private LocalDateTime dateRejet;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "valide_par_id")
    private Utilisateur validePar;

    @Column(name = "date_validation")
    private LocalDateTime dateValidation;

    @Column(name = "motif_annulation", columnDefinition = "TEXT")
    private String motifAnnulation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "annule_par_id")
    private Utilisateur annulePar;

    @Column(name = "date_annulation")
    private LocalDateTime dateAnnulation;

    @Version
    private Long version;

    // Lignes creees dans l'ordre des references : l'ordre des id est l'ordre de la fiche de comptage
    @OneToMany(mappedBy = "inventaire", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id")
    private Set<LigneInventaire> lignes = new LinkedHashSet<>();

    public void ajouterLigne(LigneInventaire ligne) {
        ligne.setInventaire(this);
        lignes.add(ligne);
    }

    // ===================== Indicateurs =====================

    public int getNombreLignes() {
        return lignes.size();
    }

    public long getNombreComptees() {
        return compter(LigneInventaire::isComptee);
    }

    public long getNombreEcarts() {
        return compter(LigneInventaire::isAvecEcart);
    }

    public long getNombreNonJustifies() {
        return compter(l -> !l.isJustifiee());
    }

    public boolean isToutCompte() {
        return getNombreComptees() == lignes.size();
    }

    /** Avancement du comptage en % (barre de progression). */
    public int getAvancement() {
        return lignes.isEmpty() ? 0 : (int) (getNombreComptees() * 100 / lignes.size());
    }

    public BigDecimal getValeurTheorique() {
        return lignes.stream().map(LigneInventaire::getValeurTheorique).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /** Surplus trouves (valeur positive). */
    public BigDecimal getEcartsPositifs() {
        return lignes.stream().map(LigneInventaire::getEcartValeur).filter(v -> v.signum() > 0)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /** Manquants (valeur negative). */
    public BigDecimal getEcartsNegatifs() {
        return lignes.stream().map(LigneInventaire::getEcartValeur).filter(v -> v.signum() < 0)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal getEcartNet() {
        return getEcartsPositifs().add(getEcartsNegatifs());
    }

    /** Taux de fiabilite du stock : part des articles comptes sans ecart. */
    public Integer getTauxFiabilite() {
        long comptees = getNombreComptees();
        return comptees == 0 ? null : (int) Math.round((comptees - getNombreEcarts()) * 100.0 / comptees);
    }

    public boolean isComptageModifiable() {
        return statut == StatutInventaire.EN_COMPTAGE;
    }

    private long compter(Predicate<LigneInventaire> condition) {
        return lignes.stream().filter(condition).count();
    }
}

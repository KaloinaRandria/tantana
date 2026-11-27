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
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.Set;

/** Un arrivage d'une commande, recupere chez le transitaire a une date donnee. */
@Entity
@Table(name = "reception")
@Getter
@Setter
@NoArgsConstructor
public class Reception {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 20, updatable = false)
    private String numero;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "commande_id", updatable = false)
    private CommandeAchat commande;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "transitaire_id")
    private Transitaire transitaire;

    @Column(name = "date_reception", nullable = false)
    private LocalDate dateReception;

    /** Taux reel du jour (Ariary pour 1 unite de la devise de la commande). */
    @Column(name = "taux_change", nullable = false, precision = 18, scale = 6)
    private BigDecimal tauxChange;

    @Column(name = "statut_id", nullable = false)
    private StatutReception statut = StatutReception.SAISIE;

    @Column(columnDefinition = "TEXT")
    private String observations;

    @Column(name = "motif_rejet", columnDefinition = "TEXT")
    private String motifRejet;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "saisi_par_id", updatable = false)
    private Utilisateur saisiPar;

    @Column(name = "date_saisie", nullable = false)
    private LocalDateTime dateSaisie = LocalDateTime.now();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "valide_par_id")
    private Utilisateur validePar;

    @Column(name = "date_validation")
    private LocalDateTime dateValidation;

    @Version
    private Long version;

    // Set (et non List) : Hibernate ne peut pas charger deux List en une seule requete
    @OneToMany(mappedBy = "reception", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id")
    private Set<LigneReception> lignes = new LinkedHashSet<>();

    @OneToMany(mappedBy = "reception", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id")
    private Set<FraisReception> frais = new LinkedHashSet<>();

    public void ajouterLigne(LigneReception ligne) {
        ligne.setReception(this);
        lignes.add(ligne);
    }

    public void ajouterFrais(FraisReception f) {
        f.setReception(this);
        frais.add(f);
    }

    public BigDecimal getTotalFrais() {
        return frais.stream().map(FraisReception::getMontant).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /** Valeur des marchandises recues en Ariary (prix fournisseur x taux du jour), hors frais. */
    public BigDecimal getValeurMarchandisesMga() {
        return lignes.stream().map(l -> l.getCoutUnitaireMga().multiply(l.getQuantiteRecue()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /** Modifiable / supprimable tant qu'elle n'a pas ete validee. */
    public boolean isModifiable() {
        return statut != StatutReception.VALIDEE;
    }
}

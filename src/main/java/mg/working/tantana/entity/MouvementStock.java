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
import java.time.LocalDateTime;

/** Trace immuable d'une entree ou sortie de stock. */
@Entity
@Table(name = "mouvement_stock")
@Getter
@Setter
@NoArgsConstructor
public class MouvementStock {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "article_id", updatable = false)
    private Article article;

    @Column(name = "type_mouvement_id", nullable = false, updatable = false)
    private TypeMouvement type;

    /** Toujours positive ; le sens est donne par le type. */
    @Column(nullable = false, precision = 15, scale = 3, updatable = false)
    private BigDecimal quantite;

    @Column(name = "cout_unitaire", nullable = false, precision = 18, scale = 4, updatable = false)
    private BigDecimal coutUnitaire;

    @Column(name = "quantite_apres", nullable = false, precision = 15, scale = 3, updatable = false)
    private BigDecimal quantiteApres;

    @Column(name = "cmup_apres", nullable = false, precision = 18, scale = 4, updatable = false)
    private BigDecimal cmupApres;

    /** Cout de revient unitaire (achat + frais) du mouvement. */
    @Column(name = "cout_revient_unitaire", nullable = false, precision = 18, scale = 4, updatable = false)
    private BigDecimal coutRevientUnitaire;

    @Column(name = "cmup_revient_apres", nullable = false, precision = 18, scale = 4, updatable = false)
    private BigDecimal cmupRevientApres;

    /** Numero du document a l'origine du mouvement (BR-2026-0001...). */
    @Column(nullable = false, length = 50, updatable = false)
    private String reference;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reception_id", updatable = false)
    private Reception reception;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vente_id", updatable = false)
    private Vente vente;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "avoir_id", updatable = false)
    private Avoir avoir;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "inventaire_id", updatable = false)
    private Inventaire inventaire;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "utilisateur_id", updatable = false)
    private Utilisateur utilisateur;

    @Column(name = "date_mouvement", nullable = false, updatable = false)
    private LocalDateTime dateMouvement = LocalDateTime.now();
}

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

/** Trace de chaque changement du prix de vente catalogue (qui, quand, pourquoi, a quel cout). */
@Entity
@Table(name = "historique_prix_vente")
@Getter
@Setter
@NoArgsConstructor
public class HistoriquePrixVente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "article_id", updatable = false)
    private Article article;

    @Column(name = "ancien_prix", precision = 18, scale = 2, updatable = false)
    private BigDecimal ancienPrix;

    @Column(name = "nouveau_prix", nullable = false, precision = 18, scale = 2, updatable = false)
    private BigDecimal nouveauPrix;

    /** Cout de revient de l'article au moment du changement. */
    @Column(name = "cmup_revient", precision = 18, scale = 4, updatable = false)
    private BigDecimal cmupRevient;

    @Column(length = 255, updatable = false)
    private String motif;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "modifie_par_id", updatable = false)
    private Utilisateur modifiePar;

    @Column(name = "date_modification", nullable = false, updatable = false)
    private LocalDateTime dateModification = LocalDateTime.now();
}

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
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Produit du catalogue, partage par les modules Achats, Stock et Ventes. */
@Entity
@Table(name = "article")
@Getter
@Setter
@NoArgsConstructor
public class Article {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Generee automatiquement a la creation (ART-0001) par ArticleService, jamais saisie. */
    @Column(nullable = false, unique = true, length = 50, updatable = false)
    private String reference;

    @NotBlank(message = "La désignation est obligatoire")
    @Size(max = 200, message = "200 caractères maximum")
    @Column(nullable = false, length = 200)
    private String designation;

    /** Unite de stock (article.unite_id -> unite.id). */
    @NotNull(message = "L'unité est obligatoire")
    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "unite_id", nullable = false)
    private Unite unite;

    @Size(max = 2000, message = "2000 caractères maximum")
    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    private boolean actif = true;

    @Column(name = "date_creation", nullable = false, insertable = false, updatable = false)
    private LocalDateTime dateCreation;

    /**
     * Prix de vente catalogue hors taxe, en Ariary. null = pas encore fixe (article non vendable).
     * Modifie uniquement par l'ADMIN via TarifService (jamais par le formulaire article).
     */
    @Column(name = "prix_vente_ht", precision = 18, scale = 2)
    private BigDecimal prixVenteHt;
}

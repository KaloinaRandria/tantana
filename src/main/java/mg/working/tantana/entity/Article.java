package mg.working.tantana.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

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

    @NotBlank(message = "La référence est obligatoire")
    @Size(max = 50, message = "50 caractères maximum")
    @Pattern(regexp = "^[A-Za-z0-9._/-]*$", message = "Lettres, chiffres et . _ / - uniquement")
    @Column(nullable = false, unique = true, length = 50)
    private String reference;

    @NotBlank(message = "La désignation est obligatoire")
    @Size(max = 200, message = "200 caractères maximum")
    @Column(nullable = false, length = 200)
    private String designation;

    @NotNull(message = "L'unité est obligatoire")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Unite unite = Unite.PIECE;

    @Size(max = 2000, message = "2000 caractères maximum")
    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    private boolean actif = true;

    @Column(name = "date_creation", nullable = false, insertable = false, updatable = false)
    private LocalDateTime dateCreation;
}

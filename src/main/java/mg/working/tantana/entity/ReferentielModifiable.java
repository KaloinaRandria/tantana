package mg.working.tantana.entity;

import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Base des listes metier gerees par l'ADMIN (unites, modes de paiement, motifs d'ecart,
 * types de frais, devises) : identifiant numerique, code fige apres creation, libelle unique,
 * desactivation au lieu de la suppression (les documents existants gardent leur valeur).
 */
@MappedSuperclass
@Getter
@Setter
public abstract class ReferentielModifiable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Short id;

    @NotBlank(message = "Le code est obligatoire")
    @Size(max = 20, message = "20 caractères maximum")
    @Pattern(regexp = "^[A-Za-z0-9_]*$", message = "Lettres sans accent, chiffres et _ uniquement")
    @Column(nullable = false, unique = true, length = 20, updatable = false)
    private String code;

    @NotBlank(message = "Le libellé est obligatoire")
    @Size(max = 50, message = "50 caractères maximum")
    @Column(nullable = false, length = 60)
    private String libelle;

    @Column(nullable = false)
    private boolean actif = true;

    @Column(name = "date_creation", nullable = false, insertable = false, updatable = false)
    private LocalDateTime dateCreation;

    @Column(name = "date_modification")
    private LocalDateTime dateModification;
}

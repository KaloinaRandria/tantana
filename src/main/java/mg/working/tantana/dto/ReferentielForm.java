package mg.working.tantana.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Saisie commune aux listes metier (les champs propres a une liste sont ignores pour les autres). */
@Getter
@Setter
@NoArgsConstructor
public class ReferentielForm {

    private Short id;

    @NotBlank(message = "Le code est obligatoire")
    @Size(max = 20, message = "20 caractères maximum")
    @Pattern(regexp = "^[A-Za-z0-9_]*$", message = "Lettres sans accent, chiffres et _ uniquement")
    private String code;

    @NotBlank(message = "Le libellé est obligatoire")
    @Size(max = 50, message = "50 caractères maximum")
    private String libelle;

    /** Modes de paiement : icone Bootstrap Icons. */
    @Size(max = 40, message = "40 caractères maximum")
    @Pattern(regexp = "^(bi-[a-z0-9-]+)?$", message = "Nom d'icône Bootstrap Icons, ex. bi-cash")
    private String icone;

    /** Modes de paiement. */
    private boolean referenceObligatoire = true;

    /** Motifs d'ecart. */
    private boolean commentaireObligatoire;

    /** Devises (lecture seule). */
    private boolean monnaieLocale;
}

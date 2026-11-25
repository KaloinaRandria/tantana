package mg.working.tantana.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Creation / modification d'un utilisateur par l'ADMIN.
 * Pas de champ mot de passe : il est genere par le systeme (mot de passe temporaire).
 */
@Getter
@Setter
public class UtilisateurForm {

    private Long id;

    /** Identifiant de connexion : non modifiable apres la creation (il apparait dans l'historique). */
    @NotBlank(message = "Obligatoire")
    @Pattern(regexp = "[a-zA-Z0-9._-]{3,50}",
            message = "3 à 50 caractères : lettres sans accent, chiffres, point, tiret ou souligné")
    private String username;

    @NotBlank(message = "Obligatoire")
    @Size(max = 150, message = "150 caractères maximum")
    private String nomComplet;

    @Email(message = "Adresse e-mail invalide")
    @Size(max = 150, message = "150 caractères maximum")
    private String email;

    @NotEmpty(message = "Choisissez au moins un rôle")
    private Set<Long> roleIds = new LinkedHashSet<>();

    private boolean actif = true;
}

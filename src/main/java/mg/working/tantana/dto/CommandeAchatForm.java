package mg.working.tantana.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Donnees saisies dans l'ecran de commande. On n'expose pas l'entite directement :
 * le numero, le statut, le createur et les quantites recues ne sont pas modifiables par formulaire.
 */
@Getter
@Setter
public class CommandeAchatForm {

    /** null = nouvelle commande. */
    private Long id;

    /** Version lue a l'ouverture du formulaire (detection des modifications concurrentes). */
    private Long version;

    @NotNull(message = "Choisissez un fournisseur")
    private Long fournisseurId;

    @NotNull(message = "Date obligatoire")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate dateCommande = LocalDate.now();

    @NotNull(message = "Date obligatoire")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate dateArriveePrevue;

    @NotNull(message = "Choisissez une devise")
    private Short deviseId;

    @NotNull(message = "Taux obligatoire")
    @DecimalMin(value = "0", inclusive = false, message = "Doit être supérieur à 0")
    private BigDecimal tauxChange;

    @Size(max = 2000, message = "2000 caractères maximum")
    private String observations;

    @Valid
    @NotEmpty(message = "Ajoutez au moins un article")
    private List<LigneCommandeForm> lignes = new ArrayList<>();

    /** Retire les lignes ajoutees puis laissees vides, avant la validation. */
    public void retirerLignesVides() {
        lignes.removeIf(l -> l == null || l.estVide());
    }
}

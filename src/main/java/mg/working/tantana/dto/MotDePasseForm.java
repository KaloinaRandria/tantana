package mg.working.tantana.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

/** Changement de son propre mot de passe (« Mon compte »). */
@Getter
@Setter
public class MotDePasseForm {

    @NotBlank(message = "Obligatoire")
    private String actuel;

    @NotBlank(message = "Obligatoire")
    private String nouveau;

    @NotBlank(message = "Obligatoire")
    private String confirmation;
}

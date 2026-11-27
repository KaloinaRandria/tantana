package mg.working.tantana.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Saisie sur les lignes d'un inventaire :
 *  - en comptage : quantites comptees (+ commentaire) ;
 *  - a valider   : justification des ecarts (motif + commentaire).
 */
@Getter
@Setter
public class ComptageForm {

    @NotNull
    private Long version;

    /** true : terminer le comptage et l'envoyer en validation (bouton « Terminer le comptage »). */
    private boolean soumettre;

    @Valid
    private List<Ligne> lignes = new ArrayList<>();

    @Getter
    @Setter
    public static class Ligne {

        @NotNull
        private Long id;

        // vide = pas encore compte
        @DecimalMin(value = "0", message = "Ne peut pas être négative")
        @Digits(integer = 12, fraction = 3, message = "3 décimales maximum")
        private BigDecimal quantiteComptee;

        private Short motifEcartId;

        @Size(max = 255, message = "255 caractères maximum")
        private String commentaire;
    }
}

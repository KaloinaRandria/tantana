package mg.working.tantana.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * Une ligne de la commande dans l'ecran de reception.
 * Les champs « affichage » sont recalcules par le service : ils ne sont jamais crus tels quels.
 */
@Getter
@Setter
public class LigneReceptionForm {

    @NotNull
    private Long ligneCommandeId;

    @NotNull(message = "Obligatoire (0 si rien reçu)")
    @DecimalMin(value = "0", message = "Ne peut pas être négative")
    @Digits(integer = 12, fraction = 3, message = "3 décimales maximum")
    private BigDecimal quantiteRecue = BigDecimal.ZERO;

    @NotNull(message = "Obligatoire (0 si aucune)")
    @DecimalMin(value = "0", message = "Ne peut pas être négative")
    @Digits(integer = 12, fraction = 3, message = "3 décimales maximum")
    private BigDecimal quantiteRefusee = BigDecimal.ZERO;

    @Size(max = 255, message = "255 caractères maximum")
    private String commentaire;

    // ----- affichage -----
    private String reference;
    private String designation;
    private String unite;
    private BigDecimal quantiteCommandee;
    private BigDecimal quantiteRestante;
    private BigDecimal prixUnitaire;
}

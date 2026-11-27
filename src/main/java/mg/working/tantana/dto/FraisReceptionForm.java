package mg.working.tantana.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/** Un frais du transitaire saisi sur la reception (montant en Ariary). */
@Getter
@Setter
@NoArgsConstructor
public class FraisReceptionForm {

    @NotNull(message = "Choisissez le type")
    private Short typeId;

    @NotNull(message = "Montant obligatoire")
    @DecimalMin(value = "0", message = "Ne peut pas être négatif")
    @Digits(integer = 16, fraction = 2, message = "2 décimales maximum")
    private BigDecimal montant;

    @Size(max = 100, message = "100 caractères maximum")
    private String referenceFacture;

    @Size(max = 255, message = "255 caractères maximum")
    private String commentaire;

    public FraisReceptionForm(Short typeId, BigDecimal montant, String referenceFacture, String commentaire) {
        this.typeId = typeId;
        this.montant = montant;
        this.referenceFacture = referenceFacture;
        this.commentaire = commentaire;
    }

    public boolean estVide() {
        return typeId == null && montant == null
                && (referenceFacture == null || referenceFacture.isBlank())
                && (commentaire == null || commentaire.isBlank());
    }
}

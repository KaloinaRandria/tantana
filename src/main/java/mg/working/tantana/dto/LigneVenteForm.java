package mg.working.tantana.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * Ligne saisie par le vendeur. Le prix ne vient JAMAIS du formulaire :
 * il est relu dans le catalogue ; le vendeur ne choisit que la quantite et la remise.
 */
@Getter
@Setter
@NoArgsConstructor
public class LigneVenteForm {

    @NotNull(message = "Choisissez un article")
    private Long articleId;

    @NotNull(message = "Quantité obligatoire")
    @DecimalMin(value = "0.001", message = "Doit être supérieure à 0")
    @Digits(integer = 12, fraction = 3, message = "3 décimales maximum")
    private BigDecimal quantite;

    @NotNull(message = "Remise obligatoire (0 si aucune)")
    @DecimalMin(value = "0", message = "Entre 0 et 100")
    @DecimalMax(value = "100", message = "Entre 0 et 100")
    @Digits(integer = 3, fraction = 2, message = "2 décimales maximum")
    private BigDecimal remisePct = BigDecimal.ZERO;

    public LigneVenteForm(Long articleId, BigDecimal quantite, BigDecimal remisePct) {
        this.articleId = articleId;
        this.quantite = quantite;
        this.remisePct = remisePct;
    }

    public boolean estVide() {
        return articleId == null && quantite == null;
    }
}

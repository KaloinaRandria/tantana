package mg.working.tantana.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * Ligne saisie par le vendeur. Le prix officiel ne vient JAMAIS du formulaire :
 * il est relu dans le catalogue. Le vendeur choisit la quantite et, s'il a negocie
 * avec le client, un prix negocie ; la remise en est deduite. Sans prix negocie,
 * la ligne est vendue au prix officiel.
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

    /** Prix unitaire negocie avec le client ; vide = prix officiel du catalogue. */
    @DecimalMin(value = "0.01", message = "Doit être supérieur à 0")
    @Digits(integer = 16, fraction = 2, message = "2 décimales maximum")
    private BigDecimal prixNegocie;

    public LigneVenteForm(Long articleId, BigDecimal quantite, BigDecimal prixNegocie) {
        this.articleId = articleId;
        this.quantite = quantite;
        this.prixNegocie = prixNegocie;
    }

    public boolean estVide() {
        return articleId == null && quantite == null && prixNegocie == null;
    }
}

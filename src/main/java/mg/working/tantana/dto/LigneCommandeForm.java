package mg.working.tantana.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/** Une ligne saisie dans le formulaire de commande. */
@Getter
@Setter
@NoArgsConstructor
public class LigneCommandeForm {

    @NotNull(message = "Choisissez un article")
    private Long articleId;

    @NotNull(message = "Quantité obligatoire")
    @DecimalMin(value = "0.001", message = "Doit être supérieure à 0")
    @Digits(integer = 12, fraction = 3, message = "3 décimales maximum")
    private BigDecimal quantite;

    @NotNull(message = "Prix obligatoire")
    @DecimalMin(value = "0", message = "Ne peut pas être négatif")
    @Digits(integer = 14, fraction = 4, message = "4 décimales maximum")
    private BigDecimal prixUnitaire;

    public LigneCommandeForm(Long articleId, BigDecimal quantite, BigDecimal prixUnitaire) {
        this.articleId = articleId;
        this.quantite = quantite;
        this.prixUnitaire = prixUnitaire;
    }

    /** Ligne ajoutee dans l'ecran mais laissee totalement vide. */
    public boolean estVide() {
        return articleId == null && quantite == null && prixUnitaire == null;
    }
}

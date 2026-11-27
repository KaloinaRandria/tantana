package mg.working.tantana.service;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

/**
 * Affichage des quantites et taux dans les pages : au plus 2 decimales (12,125 -> « 12,13 », 5 -> « 5 »).
 * Les montants en Ariary, eux, s'affichent toujours avec 2 decimales (#numbers.formatDecimal ..., 2, 'COMMA').
 * Affichage seulement : les calculs et la base gardent leur precision.
 * Utilisation dans Thymeleaf : ${@nombres.qte(l.quantite)}
 */
@Component("nombres")
public class Nombres {

    public String qte(BigDecimal valeur) {
        if (valeur == null) {
            return "";
        }
        DecimalFormatSymbols symboles = new DecimalFormatSymbols(Locale.FRANCE);
        symboles.setGroupingSeparator(' ');
        symboles.setDecimalSeparator(',');
        return new DecimalFormat("#,##0.##", symboles).format(valeur.setScale(2, RoundingMode.HALF_UP));
    }

    /**
     * Valeur pre-remplie dans un champ de saisie : zeros inutiles retires (20.000 -> 20,
     * 615.920000 -> 615.92), SANS arrondi : un formulaire renvoye tel quel garde la valeur exacte.
     */
    public static BigDecimal sansZeros(BigDecimal valeur) {
        return valeur == null ? null : new BigDecimal(valeur.stripTrailingZeros().toPlainString());
    }
}

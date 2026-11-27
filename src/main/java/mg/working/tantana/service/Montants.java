package mg.working.tantana.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

/** Mise en forme des montants en Ariary dans les messages : 1234567 -> « 1 234 567,00 MGA ». */
public final class Montants {

    private Montants() {
    }

    public static String ariary(BigDecimal montant) {
        DecimalFormatSymbols symboles = new DecimalFormatSymbols(Locale.FRANCE);
        symboles.setGroupingSeparator(' ');
        return new DecimalFormat("#,##0.00", symboles).format(montant.setScale(2, RoundingMode.HALF_UP)) + " MGA";
    }
}

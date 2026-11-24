package mg.working.tantana.service;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Ecrit un montant entier en toutes lettres (francais), pour la mention
 * « Arretee la presente facture a la somme de ... Ariary ».
 * Regles : vingt et un, soixante et onze, quatre-vingts / quatre-vingt-un,
 * deux cents / deux cent un, mille invariable, million(s), milliard(s).
 */
public final class MontantEnLettres {

    private static final String[] UNITES = {
            "zéro", "un", "deux", "trois", "quatre", "cinq", "six", "sept", "huit", "neuf",
            "dix", "onze", "douze", "treize", "quatorze", "quinze", "seize",
            "dix-sept", "dix-huit", "dix-neuf"};
    private static final String[] DIZAINES = {
            "", "", "vingt", "trente", "quarante", "cinquante", "soixante", "soixante", "quatre-vingt", "quatre-vingt"};

    private MontantEnLettres() {
    }

    public static String ecrire(BigDecimal montant) {
        long n = montant.setScale(0, RoundingMode.HALF_UP).longValueExact();
        if (n == 0) {
            return "zéro";
        }
        if (n < 0) {
            return "moins " + ecrire(BigDecimal.valueOf(-n));
        }
        StringBuilder sb = new StringBuilder();
        long milliards = n / 1_000_000_000L;
        long millions = (n / 1_000_000L) % 1000;
        long milliers = (n / 1000L) % 1000;
        long reste = n % 1000;

        if (milliards > 0) {
            sb.append(centaines((int) milliards, false)).append(milliards > 1 ? " milliards " : " milliard ");
        }
        if (millions > 0) {
            sb.append(centaines((int) millions, false)).append(millions > 1 ? " millions " : " million ");
        }
        if (milliers > 0) {
            // « mille » et non « un mille » ; « cent » ne prend pas de s devant mille (deux cent mille)
            sb.append(milliers == 1 ? "" : centaines((int) milliers, true) + " ").append("mille ");
        }
        if (reste > 0) {
            sb.append(centaines((int) reste, false));
        }
        return sb.toString().trim().replaceAll(" +", " ");
    }

    /** 1 a 999. @param devantMille vrai si suivi de « mille » (pas de s a cent / vingt). */
    private static String centaines(int n, boolean devantMille) {
        int c = n / 100;
        int r = n % 100;
        StringBuilder sb = new StringBuilder();
        if (c > 0) {
            if (c > 1) {
                sb.append(UNITES[c]).append(' ');
            }
            sb.append("cent");
            if (c > 1 && r == 0 && !devantMille) {
                sb.append('s');
            }
            if (r > 0) {
                sb.append(' ');
            }
        }
        if (r > 0) {
            sb.append(dizaines(r, devantMille));
        }
        return sb.toString();
    }

    private static String dizaines(int n, boolean devantMille) {
        if (n < 20) {
            return UNITES[n];
        }
        int d = n / 10;
        int u = n % 10;
        if (d == 7 || d == 9) {
            // soixante-dix..., quatre-vingt-dix...
            int sous = 10 + u;
            String base = DIZAINES[d];
            if (d == 7 && u == 1) {
                return base + " et onze";
            }
            return base + "-" + UNITES[sous];
        }
        String base = DIZAINES[d];
        if (u == 0) {
            return d == 8 && !devantMille ? base + "s" : base;
        }
        if (u == 1 && d != 8) {
            return base + " et un";
        }
        return base + "-" + UNITES[u];
    }
}

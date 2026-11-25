package mg.working.tantana.service;

import java.security.SecureRandom;

/**
 * Politique des mots de passe et generation des mots de passe temporaires.
 * Regle : 8 caracteres minimum, au moins une lettre et un chiffre, different du login.
 */
public final class MotsDePasse {

    public static final int LONGUEUR_MIN = 8;
    public static final String REGLE = "8 caractères minimum, avec au moins une lettre et un chiffre, "
            + "et différent de l'identifiant.";

    // Sans caracteres ambigus a la lecture (0/O, 1/l/I) : le mot de passe est dicte ou recopie
    private static final String LETTRES = "abcdefghjkmnpqrstuvwxyzABCDEFGHJKLMNPQRSTUVWXYZ";
    private static final String CHIFFRES = "23456789";
    private static final String TOUS = LETTRES + CHIFFRES;
    private static final int LONGUEUR_TEMPORAIRE = 12;
    private static final SecureRandom ALEA = new SecureRandom();

    private MotsDePasse() {
    }

    /** @return le message d'erreur, ou null si le mot de passe respecte la politique */
    public static String verifier(String motDePasse, String username) {
        if (motDePasse == null || motDePasse.length() < LONGUEUR_MIN) {
            return "Au moins " + LONGUEUR_MIN + " caractères.";
        }
        if (motDePasse.chars().noneMatch(Character::isLetter) || motDePasse.chars().noneMatch(Character::isDigit)) {
            return "Au moins une lettre et un chiffre.";
        }
        if (username != null && motDePasse.equalsIgnoreCase(username)) {
            return "Le mot de passe ne peut pas être l'identifiant.";
        }
        return null;
    }

    /** Mot de passe temporaire de 12 caracteres, avec au moins une lettre et un chiffre. */
    public static String genererTemporaire() {
        char[] resultat = new char[LONGUEUR_TEMPORAIRE];
        resultat[0] = LETTRES.charAt(ALEA.nextInt(LETTRES.length()));
        resultat[1] = CHIFFRES.charAt(ALEA.nextInt(CHIFFRES.length()));
        for (int i = 2; i < resultat.length; i++) {
            resultat[i] = TOUS.charAt(ALEA.nextInt(TOUS.length()));
        }
        // melange : la lettre et le chiffre imposes ne restent pas en tete
        for (int i = resultat.length - 1; i > 0; i--) {
            int j = ALEA.nextInt(i + 1);
            char c = resultat[i];
            resultat[i] = resultat[j];
            resultat[j] = c;
        }
        return new String(resultat);
    }
}

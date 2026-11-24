package mg.working.tantana.exception;

import lombok.Getter;

/**
 * Valeur deja utilisee par un autre enregistrement (reference, nom...).
 * Le controleur l'affiche sous le champ concerne du formulaire.
 */
@Getter
public class DoublonException extends RuntimeException {

    private final String champ;

    public DoublonException(String champ, String message) {
        super(message);
        this.champ = champ;
    }
}

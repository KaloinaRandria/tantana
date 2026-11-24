package mg.working.tantana.exception;

import lombok.Getter;

/**
 * Operation refusee par une regle metier (statut incompatible, dates incoherentes...).
 * Si un champ est indique, le message s'affiche sous ce champ ; sinon en haut du formulaire.
 */
@Getter
public class RegleMetierException extends RuntimeException {

    private final String champ;

    public RegleMetierException(String message) {
        this(null, message);
    }

    public RegleMetierException(String champ, String message) {
        super(message);
        this.champ = champ;
    }
}

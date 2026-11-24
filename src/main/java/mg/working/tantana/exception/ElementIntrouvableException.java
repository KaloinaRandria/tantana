package mg.working.tantana.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/** Element demande (par son id) absent de la base : la page repond 404. */
@ResponseStatus(HttpStatus.NOT_FOUND)
public class ElementIntrouvableException extends RuntimeException {

    public ElementIntrouvableException(String type, Long id) {
        super(type + " introuvable (id = " + id + ")");
    }
}

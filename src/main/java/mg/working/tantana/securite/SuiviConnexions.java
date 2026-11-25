package mg.working.tantana.securite;

import lombok.RequiredArgsConstructor;
import mg.working.tantana.service.UtilisateurService;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.stereotype.Component;

/** Enregistre la date de derniere connexion (visible dans la liste des utilisateurs). */
@Component
@RequiredArgsConstructor
public class SuiviConnexions {

    private final UtilisateurService utilisateurService;

    @EventListener
    public void connexionReussie(AuthenticationSuccessEvent event) {
        utilisateurService.enregistrerConnexion(event.getAuthentication().getName());
    }
}

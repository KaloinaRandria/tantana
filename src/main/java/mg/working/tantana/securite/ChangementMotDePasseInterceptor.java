package mg.working.tantana.securite;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import mg.working.tantana.service.UtilisateurService;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.security.Principal;

/**
 * Mot de passe temporaire (cree ou reinitialise par l'ADMIN) : tant qu'il n'est pas change,
 * toute page renvoie vers « Changer mon mot de passe ». Les ressources statiques, la
 * deconnexion et la page de changement elle-meme restent accessibles (voir WebConfig).
 */
@Component
@RequiredArgsConstructor
public class ChangementMotDePasseInterceptor implements HandlerInterceptor {

    public static final String URL_CHANGEMENT = "/compte/mot-de-passe";

    private final UtilisateurService utilisateurService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        Principal principal = request.getUserPrincipal();
        if (principal != null && utilisateurService.doitChangerMotDePasse(principal.getName())) {
            response.sendRedirect(request.getContextPath() + URL_CHANGEMENT);
            return false;
        }
        return true;
    }
}

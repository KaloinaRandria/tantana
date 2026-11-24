package mg.working.tantana.controller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import mg.working.tantana.service.ReceptionService;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.security.Principal;

/** Donnees communes a toutes les pages (sidebar). */
@ControllerAdvice
@RequiredArgsConstructor
public class NavigationAdvice {

    private final ReceptionService receptionService;

    /** URL courante, pour surligner le lien actif du sidebar. */
    @ModelAttribute("urlCourante")
    public String urlCourante(HttpServletRequest request) {
        return request.getRequestURI().substring(request.getContextPath().length());
    }

    /** Nombre de receptions en attente de validation (pastille du sidebar). */
    @ModelAttribute("receptionsAValider")
    public long receptionsAValider(Principal principal) {
        return principal == null ? 0 : receptionService.nombreAValider();
    }
}

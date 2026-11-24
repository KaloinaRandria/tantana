package mg.working.tantana.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/** Expose l'URL courante a toutes les vues, pour surligner le lien actif du sidebar. */
@ControllerAdvice
public class NavigationAdvice {

    @ModelAttribute("urlCourante")
    public String urlCourante(HttpServletRequest request) {
        return request.getRequestURI().substring(request.getContextPath().length());
    }
}

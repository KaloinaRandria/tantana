package mg.working.tantana.controller;

import lombok.RequiredArgsConstructor;
import mg.working.tantana.service.AccueilService;
import mg.working.tantana.service.UtilisateurService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequiredArgsConstructor
public class AccueilController {

    private final UtilisateurService utilisateurService;
    private final AccueilService accueilService;

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @GetMapping("/")
    public String accueil(Authentication authentication, Model model) {
        utilisateurService.trouverParUsername(authentication.getName())
                .ifPresent(u -> model.addAttribute("utilisateur", u));
        // chiffres financiers (CA, marges) reserves a l'ADMIN et au CHECKER
        boolean financier = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN") || a.getAuthority().equals("ROLE_CHECKER"));
        model.addAttribute("tableau", accueilService.tableau(financier));
        return "index";
    }

    /**
     * Toutes methodes : Spring Security fait un « forward » vers cette page en conservant
     * la methode de la requete refusee (un POST refuse arrive donc ici en POST).
     */
    @RequestMapping("/acces-refuse")
    public String accesRefuse() {
        return "acces-refuse";
    }
}

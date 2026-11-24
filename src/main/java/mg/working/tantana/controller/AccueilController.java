package mg.working.tantana.controller;

import lombok.RequiredArgsConstructor;
import mg.working.tantana.service.UtilisateurService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.security.Principal;

@Controller
@RequiredArgsConstructor
public class AccueilController {

    private final UtilisateurService utilisateurService;

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @GetMapping("/")
    public String accueil(Principal principal, Model model) {
        utilisateurService.trouverParUsername(principal.getName())
                .ifPresent(u -> model.addAttribute("utilisateur", u));
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

package mg.working.tantana.controller;

import lombok.RequiredArgsConstructor;
import mg.working.tantana.service.UtilisateurService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

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

    @GetMapping("/acces-refuse")
    public String accesRefuse() {
        return "acces-refuse";
    }
}

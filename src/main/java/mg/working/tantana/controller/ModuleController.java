package mg.working.tantana.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Pages provisoires des modules, pour tester les droits d'acces.
 * Les regles d'acces sont dans SecurityConfig ; chaque page sera remplacee
 * par son vrai controleur (VenteController, AchatController, ...).
 */
@Controller
public class ModuleController {

    @GetMapping("/ventes")
    public String ventes(Model model) {
        return module(model, "Ventes", "ADMIN, VENDEUR");
    }

    /** Le module Achats s'ouvre sur la liste des commandes. */
    @GetMapping("/achats")
    public String achats() {
        return "redirect:/achats/commandes";
    }

    @GetMapping("/admin")
    public String admin(Model model) {
        return module(model, "Administration", "ADMIN");
    }

    private String module(Model model, String titre, String rolesAutorises) {
        model.addAttribute("titre", titre);
        model.addAttribute("rolesAutorises", rolesAutorises);
        return "module";
    }
}

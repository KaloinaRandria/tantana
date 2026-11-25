package mg.working.tantana.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import mg.working.tantana.entity.Parametre;
import mg.working.tantana.exception.RegleMetierException;
import mg.working.tantana.service.ParametreService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** Parametres generaux (acces ADMIN via /admin/**). */
@Controller
@RequestMapping("/admin/parametres")
@RequiredArgsConstructor
public class ParametreController {

    private final ParametreService parametreService;

    @GetMapping
    public String formulaire(Model model) {
        model.addAttribute("parametre", parametreService.obtenir());
        return "parametres/parametre-saisie";
    }

    @PostMapping
    public String enregistrer(@Valid @ModelAttribute("parametre") Parametre parametre,
                              BindingResult erreurs,
                              RedirectAttributes redirect) {
        if (erreurs.hasErrors()) {
            return "parametres/parametre-saisie";
        }
        try {
            parametreService.enregistrer(parametre);
            redirect.addFlashAttribute("succes", "Paramètres enregistrés.");
            return "redirect:/admin/parametres";
        } catch (RegleMetierException e) {
            erreurs.reject("regle", e.getMessage());
            return "parametres/parametre-saisie";
        }
    }
}

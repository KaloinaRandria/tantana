package mg.working.tantana.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import mg.working.tantana.entity.Transitaire;
import mg.working.tantana.exception.DoublonException;
import mg.working.tantana.service.TransitaireService;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/achats/transitaires")
@RequiredArgsConstructor
public class TransitaireController {

    private static final int TAILLE_PAGE = 20;

    private final TransitaireService transitaireService;

    @GetMapping
    public String liste(@RequestParam(defaultValue = "") String q,
                        @RequestParam(defaultValue = FiltreStatut.ACTIFS) String statut,
                        @RequestParam(defaultValue = "0") int page,
                        Model model) {
        var pageable = PageRequest.of(Math.max(page, 0), TAILLE_PAGE, Sort.by("nom"));
        model.addAttribute("page", transitaireService.rechercher(q, FiltreStatut.versBooleen(statut), pageable));
        model.addAttribute("q", q);
        model.addAttribute("statut", statut);
        return "transitaires/transitaire-liste";
    }

    @GetMapping("/nouveau")
    public String nouveau(Model model) {
        model.addAttribute("transitaire", new Transitaire());
        return "transitaires/transitaire-saisie";
    }

    @GetMapping("/{id}/modifier")
    public String modifier(@PathVariable Long id, Model model) {
        model.addAttribute("transitaire", transitaireService.trouver(id));
        return "transitaires/transitaire-saisie";
    }

    @PostMapping("/enregistrer")
    public String enregistrer(@Valid @ModelAttribute("transitaire") Transitaire transitaire,
                              BindingResult erreurs,
                              RedirectAttributes redirect) {
        if (erreurs.hasErrors()) {
            return "transitaires/transitaire-saisie";
        }
        try {
            Transitaire enregistre = transitaireService.enregistrer(transitaire);
            redirect.addFlashAttribute("succes", "Transitaire « " + enregistre.getNom() + " » enregistré.");
            return "redirect:/achats/transitaires";
        } catch (DoublonException e) {
            erreurs.rejectValue(e.getChamp(), "doublon", e.getMessage());
            return "transitaires/transitaire-saisie";
        }
    }

    @PostMapping("/{id}/statut")
    public String basculerStatut(@PathVariable Long id, RedirectAttributes redirect) {
        Transitaire transitaire = transitaireService.basculerActif(id);
        redirect.addFlashAttribute("succes", "Transitaire « " + transitaire.getNom() + " »"
                + (transitaire.isActif() ? " réactivé." : " désactivé."));
        return "redirect:/achats/transitaires?statut=tous";
    }
}

package mg.working.tantana.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import mg.working.tantana.entity.Devise;
import mg.working.tantana.entity.Fournisseur;
import mg.working.tantana.exception.DoublonException;
import mg.working.tantana.exception.RegleMetierException;
import mg.working.tantana.service.FournisseurService;
import mg.working.tantana.service.ReferentielService;
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
@RequestMapping("/achats/fournisseurs")
@RequiredArgsConstructor
public class FournisseurController {

    private static final int TAILLE_PAGE = 20;

    private final FournisseurService fournisseurService;
    private final ReferentielService referentielService;


    @GetMapping
    public String liste(@RequestParam(defaultValue = "") String q,
                        @RequestParam(defaultValue = FiltreStatut.ACTIFS) String statut,
                        @RequestParam(defaultValue = "0") int page,
                        Model model) {
        var pageable = PageRequest.of(Math.max(page, 0), TAILLE_PAGE, Sort.by("nom"));
        model.addAttribute("page", fournisseurService.rechercher(q, FiltreStatut.versBooleen(statut), pageable));
        model.addAttribute("q", q);
        model.addAttribute("statut", statut);
        return "fournisseurs/fournisseur-liste";
    }

    @GetMapping("/nouveau")
    public String nouveau(Model model) {
        Fournisseur fournisseur = new Fournisseur();
        fournisseur.setDevise(new Devise());
        return formulaire(fournisseur, model);
    }

    @GetMapping("/{id}/modifier")
    public String modifier(@PathVariable Long id, Model model) {
        return formulaire(fournisseurService.trouver(id), model);
    }

    @PostMapping("/enregistrer")
    public String enregistrer(@Valid @ModelAttribute("fournisseur") Fournisseur fournisseur,
                              BindingResult erreurs,
                              Model model,
                              RedirectAttributes redirect) {
        if (erreurs.hasErrors()) {
            return listes(fournisseur, model);
        }
        try {
            Fournisseur enregistre = fournisseurService.enregistrer(fournisseur);
            redirect.addFlashAttribute("succes", "Fournisseur « " + enregistre.getNom() + " » enregistré.");
            return "redirect:/achats/fournisseurs";
        } catch (DoublonException e) {
            erreurs.rejectValue(e.getChamp(), "doublon", e.getMessage());
            return listes(fournisseur, model);
        } catch (RegleMetierException e) {
            erreurs.rejectValue(e.getChamp(), "regle", e.getMessage());
            return listes(fournisseur, model);
        }
    }

    @PostMapping("/{id}/statut")
    public String basculerStatut(@PathVariable Long id, RedirectAttributes redirect) {
        Fournisseur fournisseur = fournisseurService.basculerActif(id);
        redirect.addFlashAttribute("succes", "Fournisseur « " + fournisseur.getNom() + " »"
                + (fournisseur.isActif() ? " réactivé." : " désactivé."));
        return "redirect:/achats/fournisseurs?statut=tous";
    }

    private String formulaire(Fournisseur fournisseur, Model model) {
        model.addAttribute("fournisseur", fournisseur);
        return listes(fournisseur, model);
    }

    /** Devises actives, plus la devise actuelle du fournisseur si elle a ete desactivee. */
    private String listes(Fournisseur fournisseur, Model model) {
        Devise actuelle = fournisseur.getId() == null ? null : fournisseurService.trouver(fournisseur.getId()).getDevise();
        model.addAttribute("devises", referentielService.devises(actuelle));
        return "fournisseurs/fournisseur-saisie";
    }
}

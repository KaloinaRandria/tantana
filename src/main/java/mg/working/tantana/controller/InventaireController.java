package mg.working.tantana.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import mg.working.tantana.dto.ComptageForm;
import mg.working.tantana.dto.InventaireForm;
import mg.working.tantana.entity.Inventaire;
import mg.working.tantana.entity.StatutInventaire;
import mg.working.tantana.entity.TypeInventaire;
import mg.working.tantana.exception.RegleMetierException;
import mg.working.tantana.service.InventaireService;
import mg.working.tantana.service.ParametreService;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.access.prepost.PreAuthorize;
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

import java.security.Principal;
import java.util.function.Supplier;

/**
 * Inventaires : ouverture et comptage par le gestionnaire de stock,
 * validation / rejet par un CHECKER qui n'a ni ouvert ni compte.
 */
@Controller
@RequestMapping("/stock/inventaires")
@RequiredArgsConstructor
public class InventaireController {

    private static final int TAILLE_PAGE = 20;
    private static final String GESTION = "hasAnyRole('ADMIN','GESTIONNAIRE_STOCK')";
    private static final String CONTROLE = "hasAnyRole('ADMIN','CHECKER')";

    private final InventaireService inventaireService;
    private final ParametreService parametreService;

    // ===================== Liste et detail =====================

    @GetMapping
    public String liste(@RequestParam(defaultValue = "") String q,
                        @RequestParam(required = false) StatutInventaire statut,
                        @RequestParam(defaultValue = "0") int page,
                        Model model) {
        var pageable = PageRequest.of(Math.max(page, 0), TAILLE_PAGE, Sort.by(Sort.Direction.DESC, "id"));
        model.addAttribute("page", inventaireService.rechercher(q, statut, pageable));
        model.addAttribute("q", q);
        model.addAttribute("statut", statut);
        model.addAttribute("statuts", StatutInventaire.values());
        return "inventaires/inventaire-liste";
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, Model model) {
        Inventaire inventaire = inventaireService.trouverAvecDetails(id);
        model.addAttribute("inventaire", inventaire);
        model.addAttribute("motifs", inventaireService.motifs());
        return "inventaires/inventaire-fiche";
    }

    /** Fiche de comptage imprimable (sans quantite theorique : comptage a l'aveugle). */
    @GetMapping("/{id}/fiche")
    public String fiche(@PathVariable Long id, Model model) {
        model.addAttribute("inventaire", inventaireService.trouverAvecDetails(id));
        model.addAttribute("parametre", parametreService.obtenir());
        return "inventaires/inventaire-impression";
    }

    // ===================== Ouverture =====================

    @GetMapping("/nouveau")
    @PreAuthorize(GESTION)
    public String nouveau(Model model) {
        return afficherFormulaire(new InventaireForm(), model);
    }

    @PostMapping("/creer")
    @PreAuthorize(GESTION)
    public String creer(@Valid @ModelAttribute("form") InventaireForm form, BindingResult erreurs,
                        Principal principal, Model model, RedirectAttributes redirect) {
        if (erreurs.hasErrors()) {
            return afficherFormulaire(form, model);
        }
        try {
            Inventaire inventaire = inventaireService.creer(form, principal.getName());
            redirect.addFlashAttribute("succes", "Inventaire " + inventaire.getNumero() + " ouvert : "
                    + inventaire.getNombreLignes() + " article(s) à compter. Ils sont gelés jusqu'à la validation.");
            return "redirect:/stock/inventaires/" + inventaire.getId();
        } catch (RegleMetierException e) {
            if (e.getChamp() != null) {
                erreurs.rejectValue(e.getChamp(), "regle", e.getMessage());
            } else {
                erreurs.reject("regle", e.getMessage());
            }
            return afficherFormulaire(form, model);
        }
    }

    // ===================== Comptage et justification =====================

    @PostMapping("/{id}/comptage")
    @PreAuthorize(GESTION)
    public String comptage(@PathVariable Long id, @Valid @ModelAttribute ComptageForm form, BindingResult erreurs,
                           Principal principal, RedirectAttributes redirect) {
        if (erreurs.hasErrors()) {
            redirect.addFlashAttribute("erreur", "Quantités invalides : " + premiereErreur(erreurs));
            return "redirect:/stock/inventaires/" + id;
        }
        return executer(id, redirect, () -> inventaireService.enregistrerComptage(id, form, principal.getName()),
                form.isSoumettre() ? "comptage terminé : justifiez les écarts, puis faites valider par une autre personne."
                        : "comptage enregistré.");
    }

    @PostMapping("/{id}/justifier")
    @PreAuthorize(GESTION)
    public String justifier(@PathVariable Long id, @Valid @ModelAttribute ComptageForm form, BindingResult erreurs,
                            RedirectAttributes redirect) {
        if (erreurs.hasErrors()) {
            redirect.addFlashAttribute("erreur", "Saisie invalide : " + premiereErreur(erreurs));
            return "redirect:/stock/inventaires/" + id;
        }
        return executer(id, redirect, () -> inventaireService.justifier(id, form), "justification des écarts enregistrée.");
    }

    @PostMapping("/{id}/annuler")
    @PreAuthorize(GESTION)
    public String annuler(@PathVariable Long id, @RequestParam(defaultValue = "") String motif,
                          Principal principal, RedirectAttributes redirect) {
        return executer(id, redirect, () -> inventaireService.annuler(id, motif, principal.getName()),
                "annulé : les articles sont dégelés, le stock n'a pas changé.");
    }

    // ===================== Controle (CHECKER) =====================

    @PostMapping("/{id}/valider")
    @PreAuthorize(CONTROLE)
    public String valider(@PathVariable Long id, Principal principal, RedirectAttributes redirect) {
        return executer(id, redirect, () -> inventaireService.valider(id, principal.getName()),
                "validé : les écarts ont été ajustés dans le stock.");
    }

    @PostMapping("/{id}/rejeter")
    @PreAuthorize(CONTROLE)
    public String rejeter(@PathVariable Long id, @RequestParam(defaultValue = "") String motif,
                          Principal principal, RedirectAttributes redirect) {
        return executer(id, redirect, () -> inventaireService.rejeter(id, motif, principal.getName()),
                "renvoyé en comptage : les articles indiqués doivent être recomptés.");
    }

    // ===================== Utilitaires =====================

    private String executer(Long id, RedirectAttributes redirect, Supplier<Inventaire> action, String message) {
        try {
            Inventaire inventaire = action.get();
            redirect.addFlashAttribute("succes", "Inventaire " + inventaire.getNumero() + " " + message);
        } catch (RegleMetierException e) {
            redirect.addFlashAttribute("erreur", e.getMessage());
        } catch (ObjectOptimisticLockingFailureException e) {
            redirect.addFlashAttribute("erreur", "L'inventaire a été modifié entre-temps par quelqu'un d'autre : "
                    + "rechargez la page et ressaisissez vos dernières modifications.");
        }
        return "redirect:/stock/inventaires/" + id;
    }

    private static String premiereErreur(BindingResult erreurs) {
        var erreur = erreurs.getAllErrors().get(0);
        return erreur.getDefaultMessage();
    }

    private String afficherFormulaire(InventaireForm form, Model model) {
        model.addAttribute("form", form);
        model.addAttribute("types", TypeInventaire.values());
        model.addAttribute("articles", inventaireService.articlesInventoriables());
        return "inventaires/inventaire-saisie";
    }
}

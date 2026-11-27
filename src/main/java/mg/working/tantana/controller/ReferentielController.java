package mg.working.tantana.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import mg.working.tantana.dto.ReferentielForm;
import mg.working.tantana.entity.ReferentielModifiable;
import mg.working.tantana.exception.DoublonException;
import mg.working.tantana.exception.RegleMetierException;
import mg.working.tantana.service.ListeReferentiel;
import mg.working.tantana.service.ReferentielService;
import mg.working.tantana.service.ReferentielSystemeService;
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

/**
 * Administration > Referentiels (acces ADMIN via /admin/**) : un seul ecran a onglets.
 * Onglets des listes metier : la liste et, a cote, le formulaire de creation ou de modification (?modifier=id).
 * Onglet « statuts » : libelles et couleurs des statuts et types systeme (valeurs fixes).
 */
@Controller
@RequestMapping("/admin/referentiels")
@RequiredArgsConstructor
public class ReferentielController {

    private static final String VUE = "referentiels/referentiel-liste";
    private static final String ONGLET_STATUTS = "statuts";

    private final ReferentielService referentielService;
    private final ReferentielSystemeService systemeService;

    @GetMapping
    public String ecran(@RequestParam(defaultValue = "unites") String onglet,
                        @RequestParam(required = false) Short modifier,
                        @RequestParam(required = false) String table,
                        Model model) {
        if (ONGLET_STATUTS.equals(onglet)) {
            if (table != null && modifier != null) {
                model.addAttribute("valeurSysteme", systemeService.trouver(table, modifier));
                model.addAttribute("tableSysteme", systemeService.table(table));
            }
            return afficherStatuts(model);
        }
        ListeReferentiel liste = liste(onglet);
        model.addAttribute("form", referentielService.versFormulaire(liste, modifier));
        return afficher(liste, model);
    }

    // ===================== Listes metier =====================

    @PostMapping("/{onglet}/enregistrer")
    public String enregistrer(@PathVariable String onglet,
                              @Valid @ModelAttribute("form") ReferentielForm form,
                              BindingResult erreurs,
                              Model model,
                              RedirectAttributes redirect) {
        ListeReferentiel liste = liste(onglet);
        if (erreurs.hasErrors()) {
            return afficher(liste, model);
        }
        try {
            boolean creation = form.getId() == null;
            ReferentielModifiable r = referentielService.enregistrer(liste, form);
            redirect.addFlashAttribute("succes", liste.getElement() + " « " + r.getLibelle() + " » (" + r.getCode() + ") "
                    + (creation ? "créé(e)." : "modifié(e)."));
            return "redirect:/admin/referentiels?onglet=" + liste.getOnglet();
        } catch (DoublonException e) {
            erreurs.rejectValue(e.getChamp(), "doublon", e.getMessage());
            return afficher(liste, model);
        }
    }

    @PostMapping("/{onglet}/{id}/statut")
    public String basculerStatut(@PathVariable String onglet, @PathVariable Short id, RedirectAttributes redirect) {
        ListeReferentiel liste = liste(onglet);
        try {
            ReferentielModifiable r = referentielService.basculerActif(liste, id);
            redirect.addFlashAttribute("succes", "« " + r.getLibelle() + " »" + (r.isActif() ? " réactivé(e)." : " désactivé(e)."));
        } catch (RegleMetierException e) {
            redirect.addFlashAttribute("erreur", e.getMessage());
        }
        return "redirect:/admin/referentiels?onglet=" + liste.getOnglet();
    }

    // ===================== Statuts et types systeme =====================

    @PostMapping("/statuts/enregistrer")
    public String enregistrerStatut(@RequestParam String table, @RequestParam short id,
                                    @RequestParam(defaultValue = "") String libelle,
                                    @RequestParam(defaultValue = "secondary") String couleur,
                                    @RequestParam(required = false) String description,
                                    RedirectAttributes redirect) {
        try {
            systemeService.modifier(table, id, libelle, couleur, description);
            redirect.addFlashAttribute("succes", "Libellé « " + libelle.trim() + " » enregistré.");
            return "redirect:/admin/referentiels?onglet=statuts";
        } catch (RegleMetierException e) {
            redirect.addFlashAttribute("erreur", e.getMessage());
            return "redirect:/admin/referentiels?onglet=statuts&table=" + table + "&modifier=" + id;
        }
    }

    // ===================== Commun =====================

    private String afficher(ListeReferentiel liste, Model model) {
        model.addAttribute("listes", ListeReferentiel.values());
        model.addAttribute("liste", liste);
        model.addAttribute("onglet", liste.getOnglet());
        model.addAttribute("valeurs", referentielService.lister(liste));
        model.addAttribute("utilisations", referentielService.utilisations(liste));
        return VUE;
    }

    private String afficherStatuts(Model model) {
        model.addAttribute("listes", ListeReferentiel.values());
        model.addAttribute("onglet", ONGLET_STATUTS);
        model.addAttribute("groupes", systemeService.lister());
        model.addAttribute("couleurs", ReferentielSystemeService.COULEURS);
        return VUE;
    }

    private static ListeReferentiel liste(String onglet) {
        ListeReferentiel liste = ListeReferentiel.depuisOnglet(onglet);
        if (liste == null) {
            throw new RegleMetierException("Onglet inconnu : " + onglet);
        }
        return liste;
    }
}

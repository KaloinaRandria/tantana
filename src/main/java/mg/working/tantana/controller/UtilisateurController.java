package mg.working.tantana.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import mg.working.tantana.dto.UtilisateurForm;
import mg.working.tantana.entity.Utilisateur;
import mg.working.tantana.exception.DoublonException;
import mg.working.tantana.exception.RegleMetierException;
import mg.working.tantana.service.UtilisateurService;
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

import java.security.Principal;

/** Gestion des comptes par l'ADMIN (acces restreint par SecurityConfig : /admin/**). */
@Controller
@RequestMapping("/admin/utilisateurs")
@RequiredArgsConstructor
public class UtilisateurController {

    private static final int TAILLE_PAGE = 20;

    private final UtilisateurService utilisateurService;

    @GetMapping
    public String liste(@RequestParam(defaultValue = "") String q,
                        @RequestParam(defaultValue = FiltreStatut.ACTIFS) String statut,
                        @RequestParam(defaultValue = "0") int page,
                        Model model) {
        var pageable = PageRequest.of(Math.max(page, 0), TAILLE_PAGE, Sort.by("nomComplet"));
        model.addAttribute("page", utilisateurService.rechercher(q, FiltreStatut.versBooleen(statut), pageable));
        model.addAttribute("q", q);
        model.addAttribute("statut", statut);
        return "utilisateurs/utilisateur-liste";
    }

    @GetMapping("/nouveau")
    public String nouveau(Model model) {
        return afficherFormulaire(new UtilisateurForm(), null, model);
    }

    @GetMapping("/{id}/modifier")
    public String modifier(@PathVariable Long id, Model model) {
        return afficherFormulaire(utilisateurService.versFormulaire(id), utilisateurService.trouver(id), model);
    }

    @PostMapping("/enregistrer")
    public String enregistrer(@Valid @ModelAttribute("form") UtilisateurForm form, BindingResult erreurs,
                              Principal principal, Model model, RedirectAttributes redirect) {
        Utilisateur existant = form.getId() == null ? null : utilisateurService.trouver(form.getId());
        if (existant != null) {
            form.setUsername(existant.getUsername()); // l'identifiant ne se modifie pas
        }
        if (erreurs.hasErrors()) {
            return afficherFormulaire(form, existant, model);
        }
        try {
            if (existant == null) {
                var cree = utilisateurService.creer(form);
                montrerMotDePasse(redirect, cree, "Compte « " + cree.utilisateur().getUsername() + " » créé.");
            } else {
                Utilisateur u = utilisateurService.modifier(form, principal.getName());
                redirect.addFlashAttribute("succes", "Compte « " + u.getUsername() + " » enregistré.");
            }
            return "redirect:/admin/utilisateurs";
        } catch (DoublonException e) {
            erreurs.rejectValue(e.getChamp(), "doublon", e.getMessage());
        } catch (RegleMetierException e) {
            if (e.getChamp() != null) {
                erreurs.rejectValue(e.getChamp(), "regle", e.getMessage());
            } else {
                erreurs.reject("regle", e.getMessage());
            }
        }
        return afficherFormulaire(form, existant, model);
    }

    @PostMapping("/{id}/statut")
    public String basculerStatut(@PathVariable Long id, Principal principal, RedirectAttributes redirect) {
        try {
            Utilisateur u = utilisateurService.basculerActif(id, principal.getName());
            redirect.addFlashAttribute("succes", "Compte « " + u.getUsername() + " »"
                    + (u.isActif() ? " réactivé." : " désactivé : la connexion est refusée."));
        } catch (RegleMetierException e) {
            redirect.addFlashAttribute("erreur", e.getMessage());
        }
        return "redirect:/admin/utilisateurs?statut=tous";
    }

    @PostMapping("/{id}/reinitialiser-mot-de-passe")
    public String reinitialiser(@PathVariable Long id, Principal principal, RedirectAttributes redirect) {
        try {
            var resultat = utilisateurService.reinitialiserMotDePasse(id, principal.getName());
            montrerMotDePasse(redirect, resultat, "Mot de passe de « " + resultat.utilisateur().getUsername()
                    + " » réinitialisé : l'ancien ne fonctionne plus.");
        } catch (RegleMetierException e) {
            redirect.addFlashAttribute("erreur", e.getMessage());
        }
        return "redirect:/admin/utilisateurs?statut=tous";
    }

    /** Le mot de passe temporaire ne transite que par l'attribut flash : affiche une seule fois. */
    private static void montrerMotDePasse(RedirectAttributes redirect, UtilisateurService.AvecMotDePasse resultat,
                                          String message) {
        redirect.addFlashAttribute("succes", message);
        redirect.addFlashAttribute("motDePasseTemporaire", resultat.motDePasseTemporaire());
        redirect.addFlashAttribute("compteTemporaire", resultat.utilisateur().getUsername());
        redirect.addFlashAttribute("nomTemporaire", resultat.utilisateur().getNomComplet());
    }

    private String afficherFormulaire(UtilisateurForm form, Utilisateur existant, Model model) {
        model.addAttribute("form", form);
        model.addAttribute("utilisateur", existant);
        model.addAttribute("roles", utilisateurService.roles());
        return "utilisateurs/utilisateur-saisie";
    }
}

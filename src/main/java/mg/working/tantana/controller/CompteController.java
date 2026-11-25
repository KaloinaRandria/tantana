package mg.working.tantana.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import mg.working.tantana.dto.MotDePasseForm;
import mg.working.tantana.exception.RegleMetierException;
import mg.working.tantana.securite.ChangementMotDePasseInterceptor;
import mg.working.tantana.service.MotsDePasse;
import mg.working.tantana.service.UtilisateurService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;

/** « Mon compte » : changement de son propre mot de passe (obligatoire apres une reinitialisation). */
@Controller
@RequiredArgsConstructor
public class CompteController {

    private final UtilisateurService utilisateurService;

    @GetMapping(ChangementMotDePasseInterceptor.URL_CHANGEMENT)
    public String formulaire(Principal principal, Model model) {
        return afficher(new MotDePasseForm(), principal, model);
    }

    @PostMapping(ChangementMotDePasseInterceptor.URL_CHANGEMENT)
    public String changer(@Valid @ModelAttribute("form") MotDePasseForm form, BindingResult erreurs,
                          Principal principal, Model model, RedirectAttributes redirect) {
        if (erreurs.hasErrors()) {
            return afficher(form, principal, model);
        }
        try {
            utilisateurService.changerMotDePasse(principal.getName(), form);
            redirect.addFlashAttribute("succes", "Mot de passe modifié.");
            return "redirect:/";
        } catch (RegleMetierException e) {
            if (e.getChamp() != null) {
                erreurs.rejectValue(e.getChamp(), "regle", e.getMessage());
            } else {
                erreurs.reject("regle", e.getMessage());
            }
            return afficher(form, principal, model);
        }
    }

    private String afficher(MotDePasseForm form, Principal principal, Model model) {
        // les mots de passe ne sont jamais renvoyes au navigateur
        form.setActuel(null);
        form.setNouveau(null);
        form.setConfirmation(null);
        model.addAttribute("form", form);
        model.addAttribute("obligatoire", utilisateurService.doitChangerMotDePasse(principal.getName()));
        model.addAttribute("regle", MotsDePasse.REGLE);
        return "compte/mot-de-passe-saisie";
    }
}

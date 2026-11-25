package mg.working.tantana.controller;

import lombok.RequiredArgsConstructor;
import mg.working.tantana.dto.ReceptionForm;
import mg.working.tantana.entity.CommandeAchat;
import mg.working.tantana.entity.LigneReception;
import mg.working.tantana.service.RepartitionFrais;
import mg.working.tantana.entity.Reception;
import mg.working.tantana.entity.StatutReception;
import mg.working.tantana.entity.Transitaire;
import mg.working.tantana.entity.TypeFrais;
import mg.working.tantana.exception.RegleMetierException;
import mg.working.tantana.service.ReceptionService;
import mg.working.tantana.service.TransitaireService;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.SmartValidator;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * Receptions : saisie par un MAKER, validation / rejet par un CHECKER different.
 * Les roles sont verifies ici (@PreAuthorize), la regle « pas soi-meme » dans le service.
 */
@Controller
@RequestMapping("/achats/receptions")
@RequiredArgsConstructor
public class ReceptionController {

    private static final int TAILLE_PAGE = 20;
    private static final String SAISIE = "hasAnyRole('ADMIN','MAKER')";
    private static final String CONTROLE = "hasAnyRole('ADMIN','CHECKER')";

    private final ReceptionService receptionService;
    private final TransitaireService transitaireService;
    private final SmartValidator validator;

    // ===================== Liste et detail =====================

    @GetMapping
    public String liste(@RequestParam(defaultValue = "") String q,
                        @RequestParam(required = false) StatutReception statut,
                        @RequestParam(defaultValue = "0") int page,
                        Model model) {
        var pageable = PageRequest.of(Math.max(page, 0), TAILLE_PAGE, Sort.by(Sort.Direction.DESC, "id"));
        model.addAttribute("page", receptionService.rechercher(q, statut, pageable));
        model.addAttribute("q", q);
        model.addAttribute("statut", statut);
        model.addAttribute("statuts", StatutReception.values());
        return "receptions/reception-liste";
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, Model model) {
        Reception reception = receptionService.trouverAvecDetails(id);
        if (reception.getStatut() != StatutReception.VALIDEE) {
            // Apercu des frais par unite. L'entite est detachee (transaction terminee) :
            // ces valeurs servent uniquement a l'affichage et ne sont pas enregistrees.
            RepartitionFrais.fraisUnitaires(reception).forEach(LigneReception::setFraisUnitaire);
        }
        model.addAttribute("reception", reception);
        return "receptions/reception-fiche";
    }

    // ===================== Saisie (MAKER) =====================

    @GetMapping("/nouvelle")
    @PreAuthorize(SAISIE)
    public String nouvelle(@RequestParam Long commandeId, Model model, RedirectAttributes redirect) {
        try {
            return afficherFormulaire(receptionService.preparerNouvelle(commandeId), null, model);
        } catch (RegleMetierException e) {
            redirect.addFlashAttribute("erreur", e.getMessage());
            return "redirect:/achats/commandes/" + commandeId;
        }
    }

    @GetMapping("/{id}/modifier")
    @PreAuthorize(SAISIE)
    public String modifier(@PathVariable Long id, Model model, RedirectAttributes redirect) {
        Reception reception = receptionService.trouverAvecDetails(id);
        if (!reception.isModifiable()) {
            redirect.addFlashAttribute("erreur", "Une réception validée ne peut plus être modifiée.");
            return "redirect:/achats/receptions/" + id;
        }
        return afficherFormulaire(receptionService.versFormulaire(id), reception, model);
    }

    @PostMapping("/enregistrer")
    @PreAuthorize(SAISIE)
    public String enregistrer(@ModelAttribute("form") ReceptionForm form,
                              BindingResult erreurs,
                              Principal principal,
                              Model model,
                              RedirectAttributes redirect) {
        form.retirerFraisVides();
        validator.validate(form, erreurs);

        Reception existante = form.getId() == null ? null : receptionService.trouverAvecDetails(form.getId());
        // commande rechargee avec ses lignes (open-in-view est desactive : pas de chargement paresseux ici)
        CommandeAchat commande = receptionService.commandeAvecDetails(
                existante != null ? existante.getCommande().getId() : form.getCommandeId());
        // les colonnes d'affichage viennent toujours de la base, jamais du navigateur
        receptionService.completerAffichage(form, commande);

        if (erreurs.hasErrors()) {
            return afficherFormulaire(form, existante, model);
        }
        try {
            Reception reception = receptionService.enregistrer(form, principal.getName());
            redirect.addFlashAttribute("succes", "Réception " + reception.getNumero()
                    + " enregistrée. Elle doit maintenant être validée par une autre personne.");
            return "redirect:/achats/receptions/" + reception.getId();
        } catch (RegleMetierException e) {
            if (e.getChamp() != null) {
                erreurs.rejectValue(e.getChamp(), "regle", e.getMessage());
            } else {
                erreurs.reject("regle", e.getMessage());
            }
            return afficherFormulaire(form, existante, model);
        } catch (ObjectOptimisticLockingFailureException e) {
            erreurs.reject("concurrence", "La réception a été modifiée entre-temps. Rechargez la page.");
            return afficherFormulaire(form, existante, model);
        }
    }

    @PostMapping("/{id}/supprimer")
    @PreAuthorize(SAISIE)
    public String supprimer(@PathVariable Long id, Principal principal, RedirectAttributes redirect) {
        Reception reception = receptionService.trouverAvecDetails(id);
        Long commandeId = reception.getCommande().getId();
        try {
            receptionService.supprimer(id, principal.getName());
            redirect.addFlashAttribute("succes", "Réception " + reception.getNumero() + " supprimée.");
            return "redirect:/achats/commandes/" + commandeId;
        } catch (RegleMetierException e) {
            redirect.addFlashAttribute("erreur", e.getMessage());
            return "redirect:/achats/receptions/" + id;
        }
    }

    // ===================== Controle (CHECKER) =====================

    @PostMapping("/{id}/valider")
    @PreAuthorize(CONTROLE)
    public String valider(@PathVariable Long id, Principal principal, RedirectAttributes redirect) {
        return controler(id, redirect, () -> receptionService.valider(id, principal.getName()),
                "validée : le stock a été mis à jour.");
    }

    @PostMapping("/{id}/rejeter")
    @PreAuthorize(CONTROLE)
    public String rejeter(@PathVariable Long id, @RequestParam(defaultValue = "") String motif,
                          Principal principal, RedirectAttributes redirect) {
        return controler(id, redirect, () -> receptionService.rejeter(id, motif, principal.getName()),
                "rejetée : l'auteur peut la corriger.");
    }

    private String controler(Long id, RedirectAttributes redirect, Supplier<Reception> action, String message) {
        try {
            Reception reception = action.get();
            redirect.addFlashAttribute("succes", "Réception " + reception.getNumero() + " " + message);
        } catch (RegleMetierException e) {
            redirect.addFlashAttribute("erreur", e.getMessage());
        } catch (ObjectOptimisticLockingFailureException e) {
            redirect.addFlashAttribute("erreur", "La réception a été traitée entre-temps par quelqu'un d'autre.");
        }
        return "redirect:/achats/receptions/" + id;
    }

    // ===================== Donnees du formulaire =====================

    private String afficherFormulaire(ReceptionForm form, Reception existante, Model model) {
        List<Transitaire> transitaires = new ArrayList<>(transitaireService.listerActifs());
        if (existante != null && !existante.getTransitaire().isActif()) {
            transitaires.add(existante.getTransitaire());
        }
        model.addAttribute("form", form);
        model.addAttribute("reception", existante);
        model.addAttribute("transitaires", transitaires);
        model.addAttribute("typesFrais", TypeFrais.values());
        return "receptions/reception-saisie";
    }
}

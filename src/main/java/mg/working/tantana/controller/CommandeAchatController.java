package mg.working.tantana.controller;

import lombok.RequiredArgsConstructor;
import mg.working.tantana.dto.CommandeAchatForm;
import mg.working.tantana.dto.FiltrePeriode;
import mg.working.tantana.dto.LigneCommandeForm;
import mg.working.tantana.dto.Periode;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDate;
import mg.working.tantana.entity.Article;
import mg.working.tantana.entity.CommandeAchat;
import mg.working.tantana.entity.Fournisseur;
import mg.working.tantana.entity.StatutCommande;
import mg.working.tantana.exception.RegleMetierException;
import mg.working.tantana.service.ArticleService;
import mg.working.tantana.service.CommandeAchatService;
import mg.working.tantana.service.FournisseurService;
import mg.working.tantana.service.ReceptionService;
import mg.working.tantana.service.ReferentielService;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
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
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Controller
@RequestMapping("/achats/commandes")
@RequiredArgsConstructor
public class CommandeAchatController {

    private static final int TAILLE_PAGE = 20;

    private final CommandeAchatService commandeService;
    private final FournisseurService fournisseurService;
    private final ArticleService articleService;
    private final ReceptionService receptionService;
    private final ReferentielService referentielService;
    private final SmartValidator validator;

    // ===================== Liste et detail =====================

    @GetMapping
    public String liste(@RequestParam(defaultValue = "") String q,
                        @RequestParam(required = false) StatutCommande statut,
                        @RequestParam(required = false) Long fournisseurId,
                        @RequestParam(defaultValue = "false") boolean retard,
                        @RequestParam(required = false) Periode periode,
                        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate du,
                        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate au,
                        @RequestParam(defaultValue = "0") int page,
                        Model model) {
        var pageable = PageRequest.of(Math.max(page, 0), TAILLE_PAGE,
                Sort.by(Sort.Direction.DESC, "dateCommande").and(Sort.by(Sort.Direction.DESC, "id")));
        // par defaut : toutes les dates (filtre sur la date de commande)
        FiltrePeriode filtre = FiltrePeriode.resoudre(periode, du, au, Periode.TOUT, FiltrePeriode.LISTE);
        model.addAttribute("page", commandeService.rechercher(q, statut, fournisseurId, retard, filtre, pageable));
        model.addAttribute("filtre", filtre);
        model.addAttribute("q", q);
        model.addAttribute("statut", statut);
        model.addAttribute("fournisseurId", fournisseurId);
        model.addAttribute("retard", retard);
        model.addAttribute("statuts", StatutCommande.values());
        model.addAttribute("fournisseurs", fournisseurService.listerActifs());
        return "commandes/commande-liste";
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, Model model) {
        CommandeAchat commande = commandeService.trouverAvecDetails(id);
        model.addAttribute("commande", commande);
        model.addAttribute("modifiable", commandeService.estModifiable(commande));
        model.addAttribute("receptions", receptionService.listerPourCommande(id));
        model.addAttribute("peutReceptionner",
                commande.getStatut().estEnAttente() && !receptionService.aUneReceptionEnAttente(id));
        return "commandes/commande-fiche";
    }

    // ===================== Saisie =====================

    @GetMapping("/nouvelle")
    public String nouvelle(Model model) {
        CommandeAchatForm form = new CommandeAchatForm();
        form.getLignes().add(new LigneCommandeForm());
        return afficherFormulaire(form, null, model);
    }

    @GetMapping("/{id}/modifier")
    public String modifier(@PathVariable Long id, Model model, RedirectAttributes redirect) {
        CommandeAchat commande = commandeService.trouverAvecDetails(id);
        if (!commandeService.estModifiable(commande)) {
            redirect.addFlashAttribute("erreur", "Cette commande n'est plus modifiable.");
            return "redirect:/achats/commandes/" + id;
        }
        return afficherFormulaire(commandeService.versFormulaire(commande), commande, model);
    }

    @PostMapping("/enregistrer")
    public String enregistrer(@ModelAttribute("form") CommandeAchatForm form,
                              BindingResult erreurs,
                              Principal principal,
                              Model model,
                              RedirectAttributes redirect) {
        // Les lignes vides ne sont pas des erreurs : on les ignore avant de valider
        form.retirerLignesVides();
        validator.validate(form, erreurs);

        CommandeAchat existante = form.getId() == null ? null : commandeService.trouverAvecDetails(form.getId());
        if (erreurs.hasErrors()) {
            return afficherFormulaire(form, existante, model);
        }
        try {
            CommandeAchat commande = form.getId() == null
                    ? commandeService.creer(form, principal.getName())
                    : commandeService.modifier(form.getId(), form);
            redirect.addFlashAttribute("succes", "Commande " + commande.getNumero() + " enregistrée.");
            return "redirect:/achats/commandes/" + commande.getId();
        } catch (RegleMetierException e) {
            if (e.getChamp() != null) {
                erreurs.rejectValue(e.getChamp(), "regle", e.getMessage());
            } else {
                erreurs.reject("regle", e.getMessage());
            }
            return afficherFormulaire(form, existante, model);
        } catch (ObjectOptimisticLockingFailureException e) {
            erreurs.reject("concurrence", "La commande a été modifiée par un autre utilisateur. Rechargez la page.");
            return afficherFormulaire(form, existante, model);
        }
    }

    // ===================== Changements de statut =====================

    @PostMapping("/{id}/passer")
    public String passer(@PathVariable Long id, RedirectAttributes redirect) {
        return changerStatut(id, redirect, () -> commandeService.passer(id), "passée au fournisseur");
    }

    @PostMapping("/{id}/annuler")
    public String annuler(@PathVariable Long id, RedirectAttributes redirect) {
        return changerStatut(id, redirect, () -> commandeService.annuler(id), "annulée");
    }

    @PostMapping("/{id}/cloturer")
    public String cloturer(@PathVariable Long id, RedirectAttributes redirect) {
        return changerStatut(id, redirect, () -> commandeService.cloturer(id), "clôturée (le reste ne sera pas livré)");
    }

    private String changerStatut(Long id, RedirectAttributes redirect,
                                 java.util.function.Supplier<CommandeAchat> action, String libelle) {
        try {
            CommandeAchat commande = action.get();
            redirect.addFlashAttribute("succes", "Commande " + commande.getNumero() + " " + libelle + ".");
        } catch (RegleMetierException e) {
            redirect.addFlashAttribute("erreur", e.getMessage());
        } catch (ObjectOptimisticLockingFailureException e) {
            redirect.addFlashAttribute("erreur", "La commande a été modifiée entre-temps. Réessayez.");
        }
        return "redirect:/achats/commandes/" + id;
    }

    // ===================== Donnees du formulaire =====================

    /**
     * Listes deroulantes : elements actifs + ceux deja utilises par la commande
     * (un fournisseur ou un article desactive depuis reste affiche sur ses anciennes commandes).
     */
    private String afficherFormulaire(CommandeAchatForm form, CommandeAchat existante, Model model) {
        List<Fournisseur> fournisseurs = new ArrayList<>(fournisseurService.listerActifs());
        List<Article> articles = new ArrayList<>(articleService.listerActifs());
        if (existante != null) {
            if (!existante.getFournisseur().isActif()) {
                fournisseurs.add(existante.getFournisseur());
            }
            existante.getLignes().stream().map(l -> l.getArticle()).filter(a -> !a.isActif()).forEach(articles::add);
        }
        articles.sort(Comparator.comparing(Article::getDesignation, String.CASE_INSENSITIVE_ORDER));

        model.addAttribute("form", form);
        model.addAttribute("commande", existante);
        model.addAttribute("fournisseurs", fournisseurs);
        model.addAttribute("articles", articles);
        model.addAttribute("devises", referentielService.devises(existante == null ? null : existante.getDevise()));
        return "commandes/commande-saisie";
    }
}

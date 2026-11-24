package mg.working.tantana.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import mg.working.tantana.entity.Client;
import mg.working.tantana.entity.TypeClient;
import mg.working.tantana.exception.DoublonException;
import mg.working.tantana.exception.RegleMetierException;
import mg.working.tantana.service.ClientService;
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

/** Fichier clients (acces ADMIN et VENDEUR, voir SecurityConfig). */
@Controller
@RequestMapping("/clients")
@RequiredArgsConstructor
public class ClientController {

    private static final int TAILLE_PAGE = 20;

    private final ClientService clientService;

    @ModelAttribute("types")
    public TypeClient[] types() {
        return TypeClient.values();
    }

    @GetMapping
    public String liste(@RequestParam(defaultValue = "") String q,
                        @RequestParam(defaultValue = FiltreStatut.ACTIFS) String statut,
                        @RequestParam(defaultValue = "0") int page,
                        Model model) {
        // le client comptoir toujours en tete, puis ordre alphabetique
        var pageable = PageRequest.of(Math.max(page, 0), TAILLE_PAGE,
                Sort.by(Sort.Direction.DESC, "comptoir").and(Sort.by("nom")));
        model.addAttribute("page", clientService.rechercher(q, FiltreStatut.versBooleen(statut), pageable));
        model.addAttribute("q", q);
        model.addAttribute("statut", statut);
        return "clients/liste";
    }

    @GetMapping("/nouveau")
    public String nouveau(Model model) {
        model.addAttribute("client", new Client());
        return "clients/formulaire";
    }

    @GetMapping("/{id}/modifier")
    public String modifier(@PathVariable Long id, Model model) {
        model.addAttribute("client", clientService.trouver(id));
        return "clients/formulaire";
    }

    @PostMapping("/enregistrer")
    public String enregistrer(@Valid @ModelAttribute("client") Client client,
                              BindingResult erreurs,
                              Model model,
                              RedirectAttributes redirect) {
        // le formulaire ne transmet pas « comptoir » : on le relit pour l'affichage en cas d'erreur
        if (client.getId() != null) {
            client.setComptoir(clientService.trouver(client.getId()).isComptoir());
        }
        if (erreurs.hasErrors()) {
            return "clients/formulaire";
        }
        try {
            Client enregistre = clientService.enregistrer(client);
            redirect.addFlashAttribute("succes", "Client « " + enregistre.getNom() + " » enregistré.");
            return "redirect:/clients";
        } catch (DoublonException e) {
            erreurs.rejectValue(e.getChamp(), "doublon", e.getMessage());
            return "clients/formulaire";
        } catch (RegleMetierException e) {
            if (e.getChamp() != null) {
                erreurs.rejectValue(e.getChamp(), "regle", e.getMessage());
            } else {
                erreurs.reject("regle", e.getMessage());
            }
            return "clients/formulaire";
        }
    }

    @PostMapping("/{id}/statut")
    public String basculerStatut(@PathVariable Long id, RedirectAttributes redirect) {
        try {
            Client client = clientService.basculerActif(id);
            redirect.addFlashAttribute("succes", "Client « " + client.getNom() + " »"
                    + (client.isActif() ? " réactivé." : " désactivé."));
        } catch (RegleMetierException e) {
            redirect.addFlashAttribute("erreur", e.getMessage());
        }
        return "redirect:/clients?statut=tous";
    }
}

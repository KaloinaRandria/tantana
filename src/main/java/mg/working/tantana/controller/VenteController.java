package mg.working.tantana.controller;

import lombok.RequiredArgsConstructor;
import mg.working.tantana.dto.AvoirForm;
import mg.working.tantana.dto.LigneVenteForm;
import mg.working.tantana.entity.Avoir;
import mg.working.tantana.entity.ModePaiement;
import mg.working.tantana.entity.Paiement;
import mg.working.tantana.service.AvoirService;
import mg.working.tantana.service.Montants;
import mg.working.tantana.service.PaiementService;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDate;
import mg.working.tantana.dto.VenteForm;
import mg.working.tantana.entity.Article;
import mg.working.tantana.entity.Client;
import mg.working.tantana.entity.StatutVente;
import mg.working.tantana.entity.Vente;
import mg.working.tantana.exception.RegleMetierException;
import mg.working.tantana.service.ClientService;
import mg.working.tantana.service.FactureService;
import mg.working.tantana.service.ParametreService;
import mg.working.tantana.service.StockService;
import mg.working.tantana.service.VenteService;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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

import java.math.BigDecimal;
import java.security.Principal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Ventes (acces ADMIN / VENDEUR via SecurityConfig). Le controle des ventes hors regles
 * est reserve aux CHECKER ; la regle « pas sa propre vente » est verifiee dans le service.
 */
@Controller
@RequestMapping("/ventes")
@RequiredArgsConstructor
public class VenteController {

    private static final int TAILLE_PAGE = 20;
    private static final String CONTROLE = "hasAnyRole('ADMIN','CHECKER')";

    private final VenteService venteService;
    private final ClientService clientService;
    private final StockService stockService;
    private final ParametreService parametreService;
    private final FactureService factureService;
    private final PaiementService paiementService;
    private final AvoirService avoirService;
    private final SmartValidator validator;

    // ===================== Liste et detail =====================

    @GetMapping
    public String liste(@RequestParam(defaultValue = "") String q,
                        @RequestParam(required = false) StatutVente statut,
                        @RequestParam(defaultValue = "false") boolean impayees,
                        @RequestParam(defaultValue = "0") int page,
                        Model model) {
        var pageable = PageRequest.of(Math.max(page, 0), TAILLE_PAGE, Sort.by(Sort.Direction.DESC, "id"));
        model.addAttribute("page", impayees ? venteService.rechercherImpayees(q, pageable)
                : venteService.rechercher(q, statut, pageable));
        model.addAttribute("q", q);
        model.addAttribute("statut", statut);
        model.addAttribute("impayees", impayees);
        model.addAttribute("statuts", StatutVente.values());
        return "ventes/liste";
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, Model model) {
        Vente vente = venteService.trouverAvecDetails(id);
        model.addAttribute("vente", vente);
        // brouillon : prevenir le vendeur si la validation demandera un controle
        model.addAttribute("motifsControle",
                vente.getStatut() == StatutVente.BROUILLON ? venteService.motifsDeControle(vente) : List.of());
        if (vente.getStatut() == StatutVente.VALIDEE) {
            model.addAttribute("paiements", paiementService.lister(id));
            model.addAttribute("avoirs", avoirService.lister(id));
            model.addAttribute("modes", ModePaiement.values());
            model.addAttribute("aujourdhui", LocalDate.now());
            model.addAttribute("creditable", vente.getLignes().stream().anyMatch(l -> l.getQuantiteCreditable().signum() > 0));
        }
        return "ventes/detail";
    }

    // ===================== Paiements =====================

    @PostMapping("/{id}/paiements")
    public String encaisser(@PathVariable Long id,
                            @RequestParam(required = false) BigDecimal montant,
                            @RequestParam(required = false) ModePaiement mode,
                            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                            @RequestParam(defaultValue = "") String reference,
                            Principal principal, RedirectAttributes redirect) {
        try {
            Paiement p = paiementService.enregistrer(id, date, montant, mode, reference, principal.getName());
            redirect.addFlashAttribute("succes", "Paiement de " + Montants.ariary(p.getMontant()) + " ("
                    + p.getMode().getLibelle() + ") enregistré.");
        } catch (RegleMetierException e) {
            redirect.addFlashAttribute("erreur", e.getMessage());
        } catch (ObjectOptimisticLockingFailureException e) {
            redirect.addFlashAttribute("erreur", "La facture a été modifiée entre-temps. Réessayez.");
        }
        return "redirect:/ventes/" + id;
    }

    @PostMapping("/paiements/{paiementId}/annuler")
    @PreAuthorize("hasRole('ADMIN')")
    public String annulerPaiement(@PathVariable Long paiementId, @RequestParam(defaultValue = "") String motif,
                                  Principal principal, RedirectAttributes redirect) {
        Long venteId = paiementService.venteDe(paiementId);
        try {
            paiementService.annuler(paiementId, motif, principal.getName());
            redirect.addFlashAttribute("succes", "Paiement annulé : le reste à payer a été recalculé.");
        } catch (RegleMetierException e) {
            redirect.addFlashAttribute("erreur", e.getMessage());
        }
        return "redirect:/ventes/" + venteId;
    }

    // ===================== Avoirs =====================

    @GetMapping("/{id}/avoirs/nouveau")
    @PreAuthorize(CONTROLE)
    public String nouvelAvoir(@PathVariable Long id, Model model, RedirectAttributes redirect) {
        Vente vente = venteService.trouverAvecDetails(id);
        if (vente.getStatut() != StatutVente.VALIDEE) {
            redirect.addFlashAttribute("erreur", "Un avoir ne peut porter que sur une facture validée.");
            return "redirect:/ventes/" + id;
        }
        return afficherAvoir(vente, avoirService.preparer(vente), null, model);
    }

    @PostMapping("/{id}/avoirs")
    @PreAuthorize(CONTROLE)
    public String creerAvoir(@PathVariable Long id, @ModelAttribute("form") AvoirForm form,
                             Principal principal, Model model, RedirectAttributes redirect) {
        try {
            Avoir avoir = avoirService.creer(id, form, principal.getName());
            redirect.addFlashAttribute("succes", "Avoir " + avoir.getNumero() + " émis pour "
                    + Montants.ariary(avoir.getTotalTtc()) + " TTC.");
            return "redirect:/ventes/" + id;
        } catch (RegleMetierException e) {
            return afficherAvoir(venteService.trouverAvecDetails(id), form, e.getMessage(), model);
        }
    }

    private String afficherAvoir(Vente vente, AvoirForm form, String erreur, Model model) {
        model.addAttribute("vente", vente);
        model.addAttribute("form", form);
        model.addAttribute("lignesVente", vente.getLignes().stream()
                .collect(java.util.stream.Collectors.toMap(l -> l.getId(), l -> l)));
        model.addAttribute("erreur", erreur);
        return "ventes/avoir";
    }

    @GetMapping("/{id}/facture.pdf")
    public ResponseEntity<byte[]> facture(@PathVariable Long id) {
        Vente vente = venteService.trouverAvecDetails(id);
        byte[] pdf = factureService.genererPdf(id);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.inline().filename("Facture-" + vente.getNumero() + ".pdf").build().toString())
                .body(pdf);
    }

    // ===================== Saisie =====================

    @GetMapping("/nouvelle")
    public String nouvelle(Model model) {
        VenteForm form = new VenteForm();
        form.setClientId(clientService.comptoir().getId());
        form.getLignes().add(new LigneVenteForm());
        return afficherFormulaire(form, null, model);
    }

    @GetMapping("/{id}/modifier")
    public String modifier(@PathVariable Long id, Model model, RedirectAttributes redirect) {
        Vente vente = venteService.trouverAvecDetails(id);
        if (!vente.isModifiable()) {
            redirect.addFlashAttribute("erreur", "Seul un brouillon peut être modifié.");
            return "redirect:/ventes/" + id;
        }
        return afficherFormulaire(venteService.versFormulaire(vente), vente, model);
    }

    @PostMapping("/enregistrer")
    public String enregistrer(@ModelAttribute("form") VenteForm form,
                              BindingResult erreurs,
                              Principal principal,
                              Model model,
                              RedirectAttributes redirect) {
        form.retirerLignesVides();
        validator.validate(form, erreurs);
        Vente existante = form.getId() == null ? null : venteService.trouverAvecDetails(form.getId());
        if (erreurs.hasErrors()) {
            return afficherFormulaire(form, existante, model);
        }
        try {
            Vente vente = venteService.enregistrer(form, principal.getName());
            redirect.addFlashAttribute("succes", vente.getLibelle()
                    + " enregistré. Vérifiez puis cliquez sur « Valider la vente » pour émettre la facture.");
            return "redirect:/ventes/" + vente.getId();
        } catch (RegleMetierException e) {
            if (e.getChamp() != null) {
                erreurs.rejectValue(e.getChamp(), "regle", e.getMessage());
            } else {
                erreurs.reject("regle", e.getMessage());
            }
            return afficherFormulaire(form, existante, model);
        } catch (ObjectOptimisticLockingFailureException e) {
            erreurs.reject("concurrence", "La vente a été modifiée entre-temps. Rechargez la page.");
            return afficherFormulaire(form, existante, model);
        }
    }

    @PostMapping("/{id}/supprimer")
    public String supprimer(@PathVariable Long id, Principal principal, RedirectAttributes redirect) {
        try {
            venteService.supprimer(id, principal.getName());
            redirect.addFlashAttribute("succes", "Brouillon supprimé.");
            return "redirect:/ventes";
        } catch (RegleMetierException e) {
            redirect.addFlashAttribute("erreur", e.getMessage());
            return "redirect:/ventes/" + id;
        }
    }

    // ===================== Validation et controle =====================

    @PostMapping("/{id}/valider")
    public String valider(@PathVariable Long id, Principal principal, RedirectAttributes redirect) {
        return agir(id, redirect, () -> venteService.soumettre(id, principal.getName()));
    }

    @PostMapping("/{id}/approuver")
    @PreAuthorize(CONTROLE)
    public String approuver(@PathVariable Long id, @RequestParam(defaultValue = "") String commentaire,
                            Principal principal, RedirectAttributes redirect) {
        return agir(id, redirect, () -> venteService.approuver(id, commentaire, principal.getName()));
    }

    @PostMapping("/{id}/refuser")
    @PreAuthorize(CONTROLE)
    public String refuser(@PathVariable Long id, @RequestParam(defaultValue = "") String commentaire,
                          Principal principal, RedirectAttributes redirect) {
        return agir(id, redirect, () -> venteService.refuser(id, commentaire, principal.getName()));
    }

    private String agir(Long id, RedirectAttributes redirect, Supplier<Vente> action) {
        try {
            Vente vente = action.get();
            redirect.addFlashAttribute("succes", switch (vente.getStatut()) {
                case VALIDEE -> "Facture " + vente.getNumero() + " émise : le stock a été mis à jour.";
                case A_VALIDER -> "La vente dépasse les règles (remise ou prix) : elle est envoyée en contrôle à un CHECKER.";
                case BROUILLON -> "Vente refusée et renvoyée au vendeur pour correction.";
            });
        } catch (RegleMetierException e) {
            redirect.addFlashAttribute("erreur", e.getMessage());
        } catch (ObjectOptimisticLockingFailureException e) {
            redirect.addFlashAttribute("erreur", "La vente ou le stock a été modifié entre-temps. Réessayez.");
        }
        return "redirect:/ventes/" + id;
    }

    // ===================== Donnees du formulaire =====================

    private String afficherFormulaire(VenteForm form, Vente existante, Model model) {
        List<Client> clients = new ArrayList<>(clientService.listerActifs());
        if (existante != null && !existante.getClient().isActif()) {
            clients.add(existante.getClient());
        }
        List<Article> articles = venteService.articlesVendables();
        Map<Long, BigDecimal> stocks = stockService.disponibles(articles.stream().map(Article::getId).toList());

        model.addAttribute("form", form);
        model.addAttribute("vente", existante);
        model.addAttribute("clients", clients);
        model.addAttribute("articles", articles);
        model.addAttribute("stocks", stocks);
        model.addAttribute("parametre", parametreService.obtenir());
        return "ventes/formulaire";
    }
}

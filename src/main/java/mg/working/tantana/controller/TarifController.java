package mg.working.tantana.controller;

import lombok.RequiredArgsConstructor;
import mg.working.tantana.entity.Article;
import mg.working.tantana.exception.RegleMetierException;
import mg.working.tantana.service.ArticleService;
import mg.working.tantana.service.ParametreService;
import mg.working.tantana.service.TarifService;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.security.Principal;

/** Prix de vente catalogue (acces ADMIN, voir SecurityConfig). */
@Controller
@RequestMapping("/tarifs")
@RequiredArgsConstructor
public class TarifController {

    private final TarifService tarifService;
    private final ArticleService articleService;
    private final ParametreService parametreService;

    @GetMapping
    public String liste(@RequestParam(defaultValue = "") String q,
                        @RequestParam(defaultValue = "0") int page,
                        Model model) {
        var pageable = PageRequest.of(Math.max(page, 0), 30, Sort.by("reference"));
        model.addAttribute("page", tarifService.lister(q, pageable));
        model.addAttribute("q", q);
        model.addAttribute("parametre", parametreService.obtenir());
        return "tarifs/tarif-liste";
    }

    @PostMapping("/{articleId}")
    public String modifierPrix(@PathVariable Long articleId,
                               @RequestParam(required = false) BigDecimal prix,
                               @RequestParam(defaultValue = "") String motif,
                               @RequestParam(defaultValue = "") String q,
                               Principal principal,
                               RedirectAttributes redirect) {
        try {
            Article article = tarifService.modifierPrix(articleId, prix, motif, principal.getName());
            redirect.addFlashAttribute("succes", "Prix de " + article.getReference() + " mis à jour.");
        } catch (RegleMetierException e) {
            redirect.addFlashAttribute("erreur", e.getMessage());
        }
        redirect.addAttribute("q", q);
        return "redirect:/tarifs";
    }

    @GetMapping("/{articleId}/historique")
    public String historique(@PathVariable Long articleId, Model model) {
        model.addAttribute("article", articleService.trouver(articleId));
        model.addAttribute("historique", tarifService.historique(articleId));
        return "tarifs/tarif-historique";
    }
}

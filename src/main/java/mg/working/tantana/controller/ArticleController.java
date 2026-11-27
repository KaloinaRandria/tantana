package mg.working.tantana.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import mg.working.tantana.entity.Article;
import mg.working.tantana.entity.Unite;
import mg.working.tantana.service.ArticleService;
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
@RequestMapping("/articles")
@RequiredArgsConstructor
public class ArticleController {

    private static final int TAILLE_PAGE = 20;

    private final ArticleService articleService;

    @ModelAttribute("unites")
    public Unite[] unites() {
        return Unite.values();
    }

    @GetMapping
    public String liste(@RequestParam(defaultValue = "") String q,
                        @RequestParam(defaultValue = FiltreStatut.ACTIFS) String statut,
                        @RequestParam(defaultValue = "0") int page,
                        Model model) {
        var pageable = PageRequest.of(Math.max(page, 0), TAILLE_PAGE, Sort.by("reference"));
        model.addAttribute("page", articleService.rechercher(q, FiltreStatut.versBooleen(statut), pageable));
        model.addAttribute("q", q);
        model.addAttribute("statut", statut);
        return "articles/article-liste";
    }

    @GetMapping("/nouveau")
    public String nouveau(Model model) {
        model.addAttribute("article", new Article());
        return "articles/article-saisie";
    }

    @GetMapping("/{id}/modifier")
    public String modifier(@PathVariable Long id, Model model) {
        model.addAttribute("article", articleService.trouver(id));
        return "articles/article-saisie";
    }

    @PostMapping("/enregistrer")
    public String enregistrer(@Valid @ModelAttribute("article") Article article,
                              BindingResult erreurs,
                              RedirectAttributes redirect) {
        if (erreurs.hasErrors()) {
            return "articles/article-saisie";
        }
        Article enregistre = articleService.enregistrer(article);
        redirect.addFlashAttribute("succes", "Article " + enregistre.getReference() + " enregistré.");
        return "redirect:/articles";
    }

    @PostMapping("/{id}/statut")
    public String basculerStatut(@PathVariable Long id, RedirectAttributes redirect) {
        Article article = articleService.basculerActif(id);
        redirect.addFlashAttribute("succes", "Article " + article.getReference()
                + (article.isActif() ? " réactivé." : " désactivé."));
        return "redirect:/articles?statut=tous";
    }
}

package mg.working.tantana.controller;

import lombok.RequiredArgsConstructor;
import mg.working.tantana.service.ArticleService;
import mg.working.tantana.service.StockService;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

/** Consultation du stock (lecture seule) : etat par article et historique des mouvements. */
@Controller
@RequestMapping("/stock")
@RequiredArgsConstructor
public class StockController {

    private final StockService stockService;
    private final ArticleService articleService;

    @GetMapping
    public String etat(@RequestParam(defaultValue = "") String q,
                       @RequestParam(defaultValue = "0") int page,
                       Model model) {
        var pageable = PageRequest.of(Math.max(page, 0), 30, Sort.by("article.reference"));
        model.addAttribute("page", stockService.rechercher(q, pageable));
        model.addAttribute("valeurTotale", stockService.valeurTotale());
        model.addAttribute("q", q);
        return "stock/liste";
    }

    @GetMapping("/articles/{articleId}")
    public String mouvements(@PathVariable Long articleId,
                             @RequestParam(defaultValue = "0") int page,
                             Model model) {
        model.addAttribute("article", articleService.trouver(articleId));
        model.addAttribute("stock", stockService.trouverParArticle(articleId).orElse(null));
        model.addAttribute("page", stockService.mouvements(articleId, page));
        return "stock/mouvements";
    }
}

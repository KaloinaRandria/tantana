package mg.working.tantana.service;

import lombok.RequiredArgsConstructor;
import mg.working.tantana.entity.Article;
import mg.working.tantana.exception.ElementIntrouvableException;
import mg.working.tantana.repository.ArticleRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ArticleService {

    private final ArticleRepository articleRepository;
    private final NumerotationService numerotationService;
    private final ReferentielService referentielService;

    /** @param actif null = tous, true = actifs, false = inactifs */
    public Page<Article> rechercher(String texte, Boolean actif, Pageable pageable) {
        String t = texte == null ? "" : texte.trim();
        return actif == null
                ? articleRepository.rechercher(t, pageable)
                : articleRepository.rechercher(t, actif, pageable);
    }

    public Article trouver(Long id) {
        return articleRepository.findById(id)
                .orElseThrow(() -> new ElementIntrouvableException("Article", id));
    }

    public List<Article> listerActifs() {
        return articleRepository.findByActifTrueOrderByDesignationAsc();
    }

    /**
     * Cree l'article (id null) ou met a jour ses champs modifiables.
     * La reference est generee a la creation et ne change plus ensuite :
     * la valeur eventuellement envoyee par le formulaire est ignoree.
     */
    @Transactional
    public Article enregistrer(Article saisie) {
        Article article;
        if (saisie.getId() == null) {
            article = new Article();
            article.setReference(nouvelleReference());
        } else {
            article = trouver(saisie.getId());
        }
        article.setDesignation(saisie.getDesignation().trim());
        Short uniteId = saisie.getUnite() == null ? null : saisie.getUnite().getId();
        article.setUnite(referentielService.choisirUnite(uniteId, article.getUnite()));
        article.setDescription(vide(saisie.getDescription()));
        return articleRepository.save(article);
    }

    /** Active / desactive : un article desactive n'est plus propose dans les nouvelles commandes. */
    @Transactional
    public Article basculerActif(Long id) {
        Article article = trouver(id);
        article.setActif(!article.isActif());
        return article;
    }

    /** Saute les numeros deja pris (reference saisie a la main avant la numerotation automatique). */
    private String nouvelleReference() {
        String reference;
        do {
            reference = numerotationService.prochaineReferenceArticle();
        } while (articleRepository.findByReferenceIgnoreCase(reference).isPresent());
        return reference;
    }

    private static String vide(String valeur) {
        return valeur == null || valeur.isBlank() ? null : valeur.trim();
    }
}

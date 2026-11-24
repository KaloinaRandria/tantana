package mg.working.tantana.service;

import lombok.RequiredArgsConstructor;
import mg.working.tantana.entity.Article;
import mg.working.tantana.exception.DoublonException;
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

    /** Cree l'article (id null) ou met a jour ses champs modifiables. */
    @Transactional
    public Article enregistrer(Article saisie) {
        String reference = saisie.getReference().trim().toUpperCase();

        articleRepository.findByReferenceIgnoreCase(reference)
                .filter(existant -> !existant.getId().equals(saisie.getId()))
                .ifPresent(existant -> {
                    throw new DoublonException("reference", "La référence " + reference + " est déjà utilisée");
                });

        Article article = saisie.getId() == null ? new Article() : trouver(saisie.getId());
        article.setReference(reference);
        article.setDesignation(saisie.getDesignation().trim());
        article.setUnite(saisie.getUnite());
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

    private static String vide(String valeur) {
        return valeur == null || valeur.isBlank() ? null : valeur.trim();
    }
}

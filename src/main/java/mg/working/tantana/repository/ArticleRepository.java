package mg.working.tantana.repository;

import mg.working.tantana.entity.Article;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ArticleRepository extends JpaRepository<Article, Long> {

    Optional<Article> findByReferenceIgnoreCase(String reference);

    /** Recherche sur la reference ou la designation ; texte vide = tout. */
    @Query("""
            select a from Article a
            where lower(a.reference) like lower(concat('%', :texte, '%'))
               or lower(a.designation) like lower(concat('%', :texte, '%'))
            """)
    Page<Article> rechercher(@Param("texte") String texte, Pageable pageable);

    @Query("""
            select a from Article a
            where a.actif = :actif
              and (lower(a.reference) like lower(concat('%', :texte, '%'))
                   or lower(a.designation) like lower(concat('%', :texte, '%')))
            """)
    Page<Article> rechercher(@Param("texte") String texte, @Param("actif") boolean actif, Pageable pageable);

    /** Pour les listes deroulantes (commandes) : uniquement les articles actifs. */
    List<Article> findByActifTrueOrderByDesignationAsc();
}

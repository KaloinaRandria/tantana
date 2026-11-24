package mg.working.tantana.repository;

import jakarta.persistence.LockModeType;
import mg.working.tantana.entity.Stock;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface StockRepository extends JpaRepository<Stock, Long> {

    /**
     * Verrou en ecriture (SELECT ... FOR UPDATE) : deux validations simultanees
     * sur le meme article s'executent l'une apres l'autre, le CMUP reste juste.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from Stock s where s.article.id = :articleId")
    Optional<Stock> findPourMiseAJour(@Param("articleId") Long articleId);

    @EntityGraph(attributePaths = "article")
    @Query("""
            select s from Stock s
            where lower(s.article.reference) like lower(concat('%', :texte, '%'))
               or lower(s.article.designation) like lower(concat('%', :texte, '%'))
            """)
    Page<Stock> rechercher(@Param("texte") String texte, Pageable pageable);

    @EntityGraph(attributePaths = "article")
    Optional<Stock> findByArticleId(Long articleId);

    List<Stock> findByArticleIdIn(Collection<Long> articleIds);

    @Query("select coalesce(sum(s.quantite * s.cmup), 0) from Stock s")
    BigDecimal valeurTotale();

    @Query("select coalesce(sum(s.quantite * s.cmupRevient), 0) from Stock s")
    BigDecimal valeurTotaleRevient();
}

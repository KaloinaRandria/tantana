package mg.working.tantana.repository;

import mg.working.tantana.entity.Inventaire;
import mg.working.tantana.entity.StatutInventaire;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface InventaireRepository extends JpaRepository<Inventaire, Long> {

    /** Detail complet : utilisateurs, lignes et articles. */
    @Query("""
            select distinct i from Inventaire i
            join fetch i.creePar
            left join fetch i.comptePar
            left join fetch i.rejetePar
            left join fetch i.validePar
            left join fetch i.annulePar
            left join fetch i.lignes l
            left join fetch l.article
            where i.id = :id
            """)
    Optional<Inventaire> findAvecDetails(@Param("id") Long id);

    @EntityGraph(type = EntityGraph.EntityGraphType.LOAD, attributePaths = {"creePar", "validePar"})
    @Query("select i from Inventaire i where lower(i.numero) like lower(concat('%', :texte, '%'))")
    Page<Inventaire> rechercher(@Param("texte") String texte, Pageable pageable);

    @EntityGraph(type = EntityGraph.EntityGraphType.LOAD, attributePaths = {"creePar", "validePar"})
    @Query("""
            select i from Inventaire i
            where i.statut = :statut and lower(i.numero) like lower(concat('%', :texte, '%'))
            """)
    Page<Inventaire> rechercher(@Param("texte") String texte, @Param("statut") StatutInventaire statut, Pageable pageable);

    long countByStatut(StatutInventaire statut);

    /** Inventaires ouverts contenant l'article : tant qu'il y en a un, l'article est gele. */
    @Query("""
            select distinct i from LigneInventaire l join l.inventaire i
            where l.article.id = :articleId and i.statut in :statuts
            """)
    List<Inventaire> findContenantArticle(@Param("articleId") Long articleId,
                                          @Param("statuts") Collection<StatutInventaire> statuts);

    /** Articles geles (presents dans un inventaire ouvert), avec le numero de l'inventaire. */
    @Query("""
            select l.article.id, i.numero from LigneInventaire l join l.inventaire i
            where i.statut in :statuts
            """)
    List<Object[]> articlesGeles(@Param("statuts") Collection<StatutInventaire> statuts);
}

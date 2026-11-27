package mg.working.tantana.repository;

import mg.working.tantana.entity.Reception;
import mg.working.tantana.entity.StatutReception;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ReceptionRepository extends JpaRepository<Reception, Long> {

    /** Detail complet : commande, fournisseur, transitaire, utilisateurs, lignes, articles et frais. */
    @Query("""
            select distinct r from Reception r
            join fetch r.commande c
            join fetch c.fournisseur
            join fetch r.transitaire
            join fetch r.saisiPar
            left join fetch r.validePar
            left join fetch r.lignes l
            left join fetch l.ligneCommande lc
            left join fetch lc.article
            left join fetch r.frais
            where r.id = :id
            """)
    Optional<Reception> findAvecDetails(@Param("id") Long id);

    @EntityGraph(type = EntityGraph.EntityGraphType.LOAD, attributePaths = {"commande", "commande.fournisseur", "transitaire", "saisiPar"})
    @Query("""
            select r from Reception r
            where lower(r.numero) like lower(concat('%', :texte, '%'))
               or lower(r.commande.numero) like lower(concat('%', :texte, '%'))
            """)
    Page<Reception> rechercher(@Param("texte") String texte, Pageable pageable);

    @EntityGraph(type = EntityGraph.EntityGraphType.LOAD, attributePaths = {"commande", "commande.fournisseur", "transitaire", "saisiPar"})
    @Query("""
            select r from Reception r
            where r.statut = :statut
              and (lower(r.numero) like lower(concat('%', :texte, '%'))
                   or lower(r.commande.numero) like lower(concat('%', :texte, '%')))
            """)
    Page<Reception> rechercher(@Param("texte") String texte, @Param("statut") StatutReception statut, Pageable pageable);

    @EntityGraph(type = EntityGraph.EntityGraphType.LOAD, attributePaths = {"transitaire", "saisiPar", "validePar"})
    List<Reception> findByCommandeIdOrderByIdDesc(Long commandeId);

    boolean existsByCommandeId(Long commandeId);

    /** Une reception en attente (saisie ou rejetee) bloque une nouvelle saisie sur la meme commande. */
    boolean existsByCommandeIdAndStatutIn(Long commandeId, List<StatutReception> statuts);

    long countByStatut(StatutReception statut);
}

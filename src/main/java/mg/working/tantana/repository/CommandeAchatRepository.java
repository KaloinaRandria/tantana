package mg.working.tantana.repository;

import jakarta.persistence.LockModeType;
import mg.working.tantana.entity.CommandeAchat;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface CommandeAchatRepository extends JpaRepository<CommandeAchat, Long>,
        JpaSpecificationExecutor<CommandeAchat> {

    /** Liste paginee : le fournisseur est charge dans la meme requete. */
    @Override
    @EntityGraph(attributePaths = "fournisseur")
    Page<CommandeAchat> findAll(Specification<CommandeAchat> spec, Pageable pageable);

    /** Detail complet : fournisseur, createur, lignes et articles. */
    @Query("""
            select distinct c from CommandeAchat c
            join fetch c.fournisseur
            join fetch c.creePar
            left join fetch c.lignes l
            left join fetch l.article
            where c.id = :id
            """)
    Optional<CommandeAchat> findAvecDetails(@Param("id") Long id);

    /** Verrouille la commande pendant la validation d'une reception (quantites recues, statut). */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from CommandeAchat c where c.id = :id")
    Optional<CommandeAchat> findPourMiseAJour(@Param("id") Long id);
}

package mg.working.tantana.repository;

import mg.working.tantana.entity.MouvementStock;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MouvementStockRepository extends JpaRepository<MouvementStock, Long> {

    @EntityGraph(attributePaths = {"utilisateur", "reception", "vente", "avoir"})
    Page<MouvementStock> findByArticleId(Long articleId, Pageable pageable);
}

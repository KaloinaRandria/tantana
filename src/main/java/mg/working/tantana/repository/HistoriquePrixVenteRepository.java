package mg.working.tantana.repository;

import mg.working.tantana.entity.HistoriquePrixVente;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface HistoriquePrixVenteRepository extends JpaRepository<HistoriquePrixVente, Long> {

    @EntityGraph(type = EntityGraph.EntityGraphType.LOAD, attributePaths = "modifiePar")
    List<HistoriquePrixVente> findByArticleIdOrderByDateModificationDescIdDesc(Long articleId);
}

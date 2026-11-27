package mg.working.tantana.repository;

import mg.working.tantana.entity.Paiement;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PaiementRepository extends JpaRepository<Paiement, Long> {

    @EntityGraph(type = EntityGraph.EntityGraphType.LOAD, attributePaths = {"saisiPar", "annulePar"})
    List<Paiement> findByVenteIdOrderByIdAsc(Long venteId);
}

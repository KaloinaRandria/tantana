package mg.working.tantana.repository;

import mg.working.tantana.entity.Avoir;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AvoirRepository extends JpaRepository<Avoir, Long> {

    @EntityGraph(attributePaths = {"saisiPar", "lignes", "lignes.ligneVente", "lignes.ligneVente.article"})
    List<Avoir> findByVenteIdOrderByIdAsc(Long venteId);
}

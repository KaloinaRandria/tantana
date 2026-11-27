package mg.working.tantana.repository;

import mg.working.tantana.entity.ReferentielModifiable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.NoRepositoryBean;

import java.util.List;
import java.util.Optional;

/** Requetes communes aux listes metier gerees par l'ADMIN. */
@NoRepositoryBean
public interface ReferentielRepository<T extends ReferentielModifiable> extends JpaRepository<T, Short> {

    Optional<T> findByCodeIgnoreCase(String code);

    Optional<T> findByLibelleIgnoreCase(String libelle);

    List<T> findAllByOrderByLibelleAsc();

    List<T> findByActifTrueOrderByLibelleAsc();

    long countByActifTrue();
}

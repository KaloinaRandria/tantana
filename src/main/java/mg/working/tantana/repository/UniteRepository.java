package mg.working.tantana.repository;

import mg.working.tantana.entity.Unite;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface UniteRepository extends ReferentielRepository<Unite> {

    /** Nombre d'utilisations par valeur : [id, nombre]. */
    @Query("select a.unite.id, count(a) from Article a group by a.unite.id")
    List<Object[]> compterUtilisations();
}

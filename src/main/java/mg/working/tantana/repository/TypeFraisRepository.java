package mg.working.tantana.repository;

import mg.working.tantana.entity.TypeFrais;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface TypeFraisRepository extends ReferentielRepository<TypeFrais> {

    /** Nombre d'utilisations par valeur : [id, nombre]. */
    @Query("select f.type.id, count(f) from FraisReception f group by f.type.id")
    List<Object[]> compterUtilisations();
}

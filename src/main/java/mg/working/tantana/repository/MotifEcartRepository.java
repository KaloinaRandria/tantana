package mg.working.tantana.repository;

import mg.working.tantana.entity.MotifEcart;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface MotifEcartRepository extends ReferentielRepository<MotifEcart> {

    /** Nombre d'utilisations par valeur : [id, nombre]. */
    @Query("select l.motifEcart.id, count(l) from LigneInventaire l where l.motifEcart is not null group by l.motifEcart.id")
    List<Object[]> compterUtilisations();
}

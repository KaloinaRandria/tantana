package mg.working.tantana.repository;

import mg.working.tantana.entity.ModePaiement;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ModePaiementRepository extends ReferentielRepository<ModePaiement> {

    /** Nombre d'utilisations par valeur : [id, nombre]. */
    @Query("select p.mode.id, count(p) from Paiement p group by p.mode.id")
    List<Object[]> compterUtilisations();
}

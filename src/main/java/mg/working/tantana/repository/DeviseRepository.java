package mg.working.tantana.repository;

import mg.working.tantana.entity.Devise;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface DeviseRepository extends ReferentielRepository<Devise> {

    Optional<Devise> findFirstByMonnaieLocaleTrue();

    /** Nombre d'utilisations par devise (fournisseurs + commandes) : [id, nombre]. */
    @Query("""
            select d.id, (select count(f) from Fournisseur f where f.devise = d)
                       + (select count(c) from CommandeAchat c where c.devise = d)
            from Devise d""")
    List<Object[]> compterUtilisations();
}

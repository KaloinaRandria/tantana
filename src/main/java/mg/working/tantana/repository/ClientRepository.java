package mg.working.tantana.repository;

import mg.working.tantana.entity.Client;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ClientRepository extends JpaRepository<Client, Long> {

    Optional<Client> findByNifIgnoreCase(String nif);

    Optional<Client> findByComptoirTrue();

    /** Recherche sur le nom, le telephone ou le NIF ; texte vide = tout. Le client comptoir en premier. */
    @Query("""
            select c from Client c
            where lower(c.nom) like lower(concat('%', :texte, '%'))
               or lower(coalesce(c.telephone, '')) like lower(concat('%', :texte, '%'))
               or lower(coalesce(c.nif, '')) like lower(concat('%', :texte, '%'))
            """)
    Page<Client> rechercher(@Param("texte") String texte, Pageable pageable);

    @Query("""
            select c from Client c
            where c.actif = :actif
              and (lower(c.nom) like lower(concat('%', :texte, '%'))
                   or lower(coalesce(c.telephone, '')) like lower(concat('%', :texte, '%'))
                   or lower(coalesce(c.nif, '')) like lower(concat('%', :texte, '%')))
            """)
    Page<Client> rechercher(@Param("texte") String texte, @Param("actif") boolean actif, Pageable pageable);

    /** Pour la saisie des ventes : clients actifs, le comptoir en premier. */
    List<Client> findByActifTrueOrderByComptoirDescNomAsc();
}

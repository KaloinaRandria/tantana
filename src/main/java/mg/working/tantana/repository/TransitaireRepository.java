package mg.working.tantana.repository;

import mg.working.tantana.entity.Transitaire;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface TransitaireRepository extends JpaRepository<Transitaire, Long> {

    Optional<Transitaire> findByNomIgnoreCase(String nom);

    /** Recherche sur le nom ou le contact ; texte vide = tout. */
    @Query("""
            select t from Transitaire t
            where lower(t.nom) like lower(concat('%', :texte, '%'))
               or lower(coalesce(t.contact, '')) like lower(concat('%', :texte, '%'))
            """)
    Page<Transitaire> rechercher(@Param("texte") String texte, Pageable pageable);

    @Query("""
            select t from Transitaire t
            where t.actif = :actif
              and (lower(t.nom) like lower(concat('%', :texte, '%'))
                   or lower(coalesce(t.contact, '')) like lower(concat('%', :texte, '%')))
            """)
    Page<Transitaire> rechercher(@Param("texte") String texte, @Param("actif") boolean actif, Pageable pageable);

    List<Transitaire> findByActifTrueOrderByNomAsc();
}

package mg.working.tantana.repository;

import mg.working.tantana.entity.Fournisseur;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface FournisseurRepository extends JpaRepository<Fournisseur, Long> {

    Optional<Fournisseur> findByNomIgnoreCase(String nom);

    /** Recherche sur le nom ou le pays ; texte vide = tout. */
    @Query("""
            select f from Fournisseur f
            where lower(f.nom) like lower(concat('%', :texte, '%'))
               or lower(f.pays) like lower(concat('%', :texte, '%'))
            """)
    Page<Fournisseur> rechercher(@Param("texte") String texte, Pageable pageable);

    @Query("""
            select f from Fournisseur f
            where f.actif = :actif
              and (lower(f.nom) like lower(concat('%', :texte, '%'))
                   or lower(f.pays) like lower(concat('%', :texte, '%')))
            """)
    Page<Fournisseur> rechercher(@Param("texte") String texte, @Param("actif") boolean actif, Pageable pageable);

    List<Fournisseur> findByActifTrueOrderByNomAsc();
}

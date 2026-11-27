package mg.working.tantana.repository;

import jakarta.persistence.LockModeType;
import mg.working.tantana.entity.StatutVente;
import org.springframework.data.jpa.repository.Lock;
import mg.working.tantana.entity.Vente;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

public interface VenteRepository extends JpaRepository<Vente, Long> {

    @Query("""
            select distinct v from Vente v
            join fetch v.client
            join fetch v.vendeur
            left join fetch v.controleur
            left join fetch v.lignes l
            left join fetch l.article
            where v.id = :id
            """)
    Optional<Vente> findAvecDetails(@Param("id") Long id);

    /** Recherche sur le numero de facture ou le nom du client (texte vide = tout), sur une periode de vente. */
    @EntityGraph(type = EntityGraph.EntityGraphType.LOAD, attributePaths = {"client", "vendeur"})
    @Query("""
            select v from Vente v
            where v.dateVente between :du and :au
              and (lower(coalesce(v.numero, '')) like lower(concat('%', :texte, '%'))
                   or lower(v.client.nom) like lower(concat('%', :texte, '%')))
            """)
    Page<Vente> rechercher(@Param("texte") String texte, @Param("du") LocalDate du, @Param("au") LocalDate au,
                           Pageable pageable);

    @EntityGraph(type = EntityGraph.EntityGraphType.LOAD, attributePaths = {"client", "vendeur"})
    @Query("""
            select v from Vente v
            where v.statut = :statut
              and v.dateVente between :du and :au
              and (lower(coalesce(v.numero, '')) like lower(concat('%', :texte, '%'))
                   or lower(v.client.nom) like lower(concat('%', :texte, '%')))
            """)
    Page<Vente> rechercher(@Param("texte") String texte, @Param("statut") StatutVente statut,
                           @Param("du") LocalDate du, @Param("au") LocalDate au, Pageable pageable);

    long countByStatut(StatutVente statut);

    /** Verrouille la facture pendant un paiement ou un avoir (cumuls coherents). */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select v from Vente v where v.id = :id")
    Optional<Vente> findPourMiseAJour(@Param("id") Long id);

    /** Encours du client : total restant du sur ses factures validees (avoirs et paiements deduits). */
    @Query("""
            select coalesce(sum(v.totalTtc - v.montantAvoirs - v.montantPaye), 0) from Vente v
            where v.client.id = :clientId and v.statut = :validee
              and v.totalTtc - v.montantAvoirs - v.montantPaye > 0
            """)
    BigDecimal encoursClient(@Param("clientId") Long clientId, @Param("validee") StatutVente validee);

    default BigDecimal encoursClient(Long clientId) {
        return encoursClient(clientId, StatutVente.VALIDEE);
    }

    /** Factures validees avec un reste a payer (filtre « impayees »). */
    @EntityGraph(type = EntityGraph.EntityGraphType.LOAD, attributePaths = {"client", "vendeur"})
    @Query("""
            select v from Vente v
            where v.statut = :validee
              and v.totalTtc - v.montantAvoirs - v.montantPaye > 0
              and v.dateVente between :du and :au
              and (lower(coalesce(v.numero, '')) like lower(concat('%', :texte, '%'))
                   or lower(v.client.nom) like lower(concat('%', :texte, '%')))
            """)
    Page<Vente> rechercherImpayees(@Param("texte") String texte, @Param("du") LocalDate du, @Param("au") LocalDate au,
                                   @Param("validee") StatutVente validee, Pageable pageable);

    default Page<Vente> rechercherImpayees(String texte, LocalDate du, LocalDate au, Pageable pageable) {
        return rechercherImpayees(texte, du, au, StatutVente.VALIDEE, pageable);
    }
}

package mg.working.tantana.repository;

import mg.working.tantana.entity.Utilisateur;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface UtilisateurRepository extends JpaRepository<Utilisateur, Long> {

    /** Charge l'utilisateur avec ses roles en une seule requete (open-in-view est desactive). */
    @Query("select u from Utilisateur u left join fetch u.roles where lower(u.username) = lower(:username)")
    Optional<Utilisateur> findByUsernameAvecRoles(@Param("username") String username);

    @Query("select u from Utilisateur u left join fetch u.roles where u.id = :id")
    Optional<Utilisateur> findAvecRoles(@Param("id") Long id);

    Optional<Utilisateur> findByUsernameIgnoreCase(String username);

    /** Recherche sur le login, le nom ou l'e-mail ; actif null = tous. Les roles sont charges par lot (@BatchSize). */
    @Query("""
            select u from Utilisateur u
            where (:actif is null or u.actif = :actif)
              and (lower(u.username) like lower(concat('%', :texte, '%'))
                   or lower(u.nomComplet) like lower(concat('%', :texte, '%'))
                   or lower(coalesce(u.email, '')) like lower(concat('%', :texte, '%')))
            """)
    Page<Utilisateur> rechercher(@Param("texte") String texte, @Param("actif") Boolean actif, Pageable pageable);

    boolean existsByUsernameIgnoreCase(String username);

    @Query("select count(u) > 0 from Utilisateur u where lower(u.email) = lower(:email) and (:id is null or u.id <> :id)")
    boolean emailUtilise(@Param("email") String email, @Param("id") Long id);

    /** Administrateurs actifs (on ne peut pas descendre a zero). */
    @Query("select count(u) from Utilisateur u join u.roles r where u.actif = true and r.code = 'ADMIN'")
    long nombreAdminsActifs();

    /** Le mot de passe doit-il etre change avant d'utiliser l'application ? */
    @Query("select u.doitChangerMotDePasse from Utilisateur u where lower(u.username) = lower(:username)")
    Optional<Boolean> doitChangerMotDePasse(@Param("username") String username);

    @Modifying
    @Query("update Utilisateur u set u.derniereConnexion = :date where lower(u.username) = lower(:username)")
    void enregistrerConnexion(@Param("username") String username, @Param("date") LocalDateTime date);
}

package mg.working.tantana.repository;

import mg.working.tantana.entity.Utilisateur;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UtilisateurRepository extends JpaRepository<Utilisateur, Long> {

    /** Charge l'utilisateur avec ses roles en une seule requete (open-in-view est desactive). */
    @Query("select u from Utilisateur u left join fetch u.roles where lower(u.username) = lower(:username)")
    Optional<Utilisateur> findByUsernameAvecRoles(@Param("username") String username);

    Optional<Utilisateur> findByUsernameIgnoreCase(String username);
}

package mg.working.tantana.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.BatchSize;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "utilisateur")
@Getter
@Setter
@NoArgsConstructor
public class Utilisateur {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String username;

    /** Hash du mot de passe, au format {bcrypt}$2a$... */
    @Column(nullable = false)
    private String password;

    @Column(name = "nom_complet", nullable = false, length = 150)
    private String nomComplet;

    @Column(unique = true, length = 150)
    private String email;

    @Column(nullable = false)
    private boolean actif = true;

    /** Mot de passe temporaire (cree ou reinitialise par l'ADMIN) : a changer a la prochaine connexion. */
    @Column(name = "doit_changer_mot_de_passe", nullable = false)
    private boolean doitChangerMotDePasse;

    @Column(name = "date_mot_de_passe")
    private LocalDateTime dateMotDePasse;

    @Column(name = "derniere_connexion")
    private LocalDateTime derniereConnexion;

    @Column(name = "date_modification")
    private LocalDateTime dateModification;

    @Column(name = "date_creation", nullable = false, insertable = false, updatable = false)
    private LocalDateTime dateCreation;

    /** Un utilisateur peut avoir plusieurs roles, un role peut etre donne a plusieurs utilisateurs. */
    @BatchSize(size = 50)
    @ManyToMany
    @JoinTable(
            name = "utilisateur_role",
            joinColumns = @JoinColumn(name = "utilisateur_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id"))
    private Set<Role> roles = new HashSet<>();

    public boolean aLeRole(String code) {
        return roles.stream().anyMatch(r -> r.getCode().equals(code));
    }
}

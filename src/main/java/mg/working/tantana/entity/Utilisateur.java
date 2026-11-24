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

    @Column(name = "date_creation", nullable = false, insertable = false, updatable = false)
    private LocalDateTime dateCreation;

    /** Un utilisateur peut avoir plusieurs roles, un role peut etre donne a plusieurs utilisateurs. */
    @ManyToMany
    @JoinTable(
            name = "utilisateur_role",
            joinColumns = @JoinColumn(name = "utilisateur_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id"))
    private Set<Role> roles = new HashSet<>();
}

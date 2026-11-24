package mg.working.tantana.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/** Fournisseur a l'etranger. Sa devise est proposee par defaut sur ses commandes. */
@Entity
@Table(name = "fournisseur")
@Getter
@Setter
@NoArgsConstructor
public class Fournisseur {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Le nom est obligatoire")
    @Size(max = 150, message = "150 caractères maximum")
    @Column(nullable = false, unique = true, length = 150)
    private String nom;

    @NotBlank(message = "Le pays est obligatoire")
    @Size(max = 100, message = "100 caractères maximum")
    @Column(nullable = false, length = 100)
    private String pays;

    @NotNull(message = "La devise est obligatoire")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 3)
    private Devise devise;

    @Size(max = 150, message = "150 caractères maximum")
    @Column(length = 150)
    private String contact;

    @Size(max = 50, message = "50 caractères maximum")
    @Column(length = 50)
    private String telephone;

    @Email(message = "Adresse e-mail invalide")
    @Size(max = 150, message = "150 caractères maximum")
    @Column(length = 150)
    private String email;

    @Size(max = 255, message = "255 caractères maximum")
    @Column(length = 255)
    private String adresse;

    @Column(nullable = false)
    private boolean actif = true;

    @Column(name = "date_creation", nullable = false, insertable = false, updatable = false)
    private LocalDateTime dateCreation;
}

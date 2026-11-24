package mg.working.tantana.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "client")
@Getter
@Setter
@NoArgsConstructor
public class Client {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Le nom est obligatoire")
    @Size(max = 150, message = "150 caractères maximum")
    @Column(nullable = false, length = 150)
    private String nom;

    @NotNull(message = "Choisissez le type")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TypeClient type = TypeClient.PARTICULIER;

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

    @Size(max = 50, message = "50 caractères maximum")
    @Column(length = 50)
    private String nif;

    @Size(max = 50, message = "50 caractères maximum")
    @Column(length = 50)
    private String stat;

    /** Montant TTC maximal que le client peut devoir (0 = comptant uniquement). */
    @NotNull(message = "Indiquez 0 si le client paie comptant")
    @DecimalMin(value = "0", message = "Ne peut pas être négatif")
    @Digits(integer = 16, fraction = 2, message = "2 décimales maximum")
    @Column(name = "plafond_credit", nullable = false, precision = 18, scale = 2)
    private BigDecimal plafondCredit = BigDecimal.ZERO;

    @Size(max = 2000, message = "2000 caractères maximum")
    @Column(columnDefinition = "TEXT")
    private String observations;

    /** Client generique des ventes anonymes : ni desactivable, ni modifiable dans son type. */
    @Column(nullable = false, updatable = false)
    private boolean comptoir;

    @Column(nullable = false)
    private boolean actif = true;

    @Column(name = "date_creation", nullable = false, insertable = false, updatable = false)
    private LocalDateTime dateCreation;

    public boolean isCreditAutorise() {
        return plafondCredit.signum() > 0;
    }
}

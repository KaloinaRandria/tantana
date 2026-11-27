package mg.working.tantana.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Parametres generaux (une seule ligne, id = 1) : TVA, remise maximale, identite de la societe. */
@Entity
@Table(name = "parametre")
@Getter
@Setter
@NoArgsConstructor
public class Parametre {

    public static final int ID_UNIQUE = 1;

    @Id
    private Integer id = ID_UNIQUE;

    @NotNull(message = "Taux obligatoire")
    @DecimalMin(value = "0", message = "Entre 0 et 100")
    @DecimalMax(value = "100", message = "Entre 0 et 100")
    @Column(name = "taux_tva", nullable = false, precision = 5, scale = 2)
    private BigDecimal tauxTva;

    /** Remise que le vendeur peut accorder seul ; au-dela, validation par un CHECKER. */
    @NotNull(message = "Remise obligatoire")
    @DecimalMin(value = "0", message = "Entre 0 et 100")
    @DecimalMax(value = "100", message = "Entre 0 et 100")
    @Column(name = "remise_max_vendeur", nullable = false, precision = 5, scale = 2)
    private BigDecimal remiseMaxVendeur;

    @NotBlank(message = "La raison sociale est obligatoire")
    @Size(max = 150, message = "150 caractères maximum")
    @Column(name = "raison_sociale", nullable = false, length = 150)
    private String raisonSociale;

    @Size(max = 255, message = "255 caractères maximum")
    private String adresse;

    @Size(max = 50, message = "50 caractères maximum")
    private String telephone;

    @Email(message = "Adresse e-mail invalide")
    @Size(max = 150, message = "150 caractères maximum")
    private String email;

    @Size(max = 50, message = "50 caractères maximum")
    private String nif;

    @Size(max = 50, message = "50 caractères maximum")
    private String stat;

    @Size(max = 50, message = "50 caractères maximum")
    private String rcs;

    @Size(max = 1000, message = "1000 caractères maximum")
    @Column(name = "mentions_facture", columnDefinition = "TEXT")
    private String mentionsFacture;

    @Column(name = "date_modification", nullable = false)
    private LocalDateTime dateModification = LocalDateTime.now();

    @Version
    private Long version;

    /** TVA 0 % : entreprise non assujettie (ex. PME a l'impot synthetique) -> pas de TVA ni de HT/TTC affiches. */
    public boolean isTvaApplicable() {
        return tauxTva != null && tauxTva.signum() > 0;
    }

    @PreUpdate
    void avantMiseAJour() {
        dateModification = LocalDateTime.now();
    }
}

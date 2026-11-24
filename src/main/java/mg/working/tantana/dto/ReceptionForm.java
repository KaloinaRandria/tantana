package mg.working.tantana.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import mg.working.tantana.entity.Devise;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class ReceptionForm {

    /** null = nouvelle reception. */
    private Long id;

    private Long version;

    @NotNull
    private Long commandeId;

    @NotNull(message = "Choisissez le transitaire")
    private Long transitaireId;

    @NotNull(message = "Date obligatoire")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate dateReception = LocalDate.now();

    @NotNull(message = "Taux obligatoire")
    @DecimalMin(value = "0", inclusive = false, message = "Doit être supérieur à 0")
    private BigDecimal tauxChange;

    @Size(max = 2000, message = "2000 caractères maximum")
    private String observations;

    @Valid
    private List<LigneReceptionForm> lignes = new ArrayList<>();

    @Valid
    private List<FraisReceptionForm> frais = new ArrayList<>();

    // ----- affichage -----
    private String numeroCommande;
    private String fournisseur;
    private Devise devise;

    public void retirerFraisVides() {
        frais.removeIf(f -> f == null || f.estVide());
    }
}

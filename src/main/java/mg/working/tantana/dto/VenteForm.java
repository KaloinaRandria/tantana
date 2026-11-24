package mg.working.tantana.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class VenteForm {

    /** null = nouvelle vente. */
    private Long id;

    private Long version;

    @NotNull(message = "Choisissez un client")
    private Long clientId;

    @Size(max = 2000, message = "2000 caractères maximum")
    private String observations;

    @Valid
    @NotEmpty(message = "Ajoutez au moins un article")
    private List<LigneVenteForm> lignes = new ArrayList<>();

    public void retirerLignesVides() {
        lignes.removeIf(l -> l == null || l.estVide());
    }
}

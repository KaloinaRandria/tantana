package mg.working.tantana.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import mg.working.tantana.entity.TypeInventaire;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** Ouverture d'un inventaire : type, date et (inventaire tournant) articles a compter. */
@Getter
@Setter
public class InventaireForm {

    @NotNull(message = "Choisissez le type d'inventaire")
    private TypeInventaire type = TypeInventaire.TOURNANT;

    @NotNull(message = "Obligatoire")
    @PastOrPresent(message = "Un inventaire ne peut pas être daté dans le futur")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate dateInventaire = LocalDate.now();

    @Size(max = 2000, message = "2000 caractères maximum")
    private String observations;

    /** Inventaire tournant uniquement ; ignore pour un inventaire complet. */
    private List<Long> articleIds = new ArrayList<>();
}

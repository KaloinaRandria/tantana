package mg.working.tantana.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Devise d'achat (code ISO 4217). Le stock est toujours valorise dans la monnaie locale. */
@Entity
@Table(name = "devise")
@Getter
@Setter
@NoArgsConstructor
public class Devise extends ReferentielModifiable {

    /** Monnaie de valorisation du stock (Ariary) : taux de change force a 1. Une seule. */
    @Column(name = "monnaie_locale", nullable = false)
    private boolean monnaieLocale;
}

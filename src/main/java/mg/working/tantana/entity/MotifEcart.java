package mg.working.tantana.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Cause d'un ecart d'inventaire : obligatoire pour toute ligne dont le compte differe du theorique. */
@Entity
@Table(name = "motif_ecart")
@Getter
@Setter
@NoArgsConstructor
public class MotifEcart extends ReferentielModifiable {

    /** Le compteur doit preciser l'ecart en commentaire (ex. « Autre »). */
    @Column(name = "commentaire_obligatoire", nullable = false)
    private boolean commentaireObligatoire;
}

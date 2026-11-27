package mg.working.tantana.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Nature des frais factures par le transitaire (fret, douane...). */
@Entity
@Table(name = "type_frais")
@Getter
@Setter
@NoArgsConstructor
public class TypeFrais extends ReferentielModifiable {
}

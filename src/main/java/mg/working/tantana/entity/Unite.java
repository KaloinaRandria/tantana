package mg.working.tantana.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Unite dans laquelle un article est compte en stock (Piece, Carton, Kilogramme...). */
@Entity
@Table(name = "unite")
@Getter
@Setter
@NoArgsConstructor
public class Unite extends ReferentielModifiable {
}

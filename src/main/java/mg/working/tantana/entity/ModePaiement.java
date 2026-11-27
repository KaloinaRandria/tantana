package mg.working.tantana.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Moyen d'encaissement d'une facture (especes, MVola, virement...). */
@Entity
@Table(name = "mode_paiement")
@Getter
@Setter
@NoArgsConstructor
public class ModePaiement extends ReferentielModifiable {

    /** Icone Bootstrap Icons affichee devant le libelle (ex. bi-cash). */
    @Column(nullable = false, length = 40)
    private String icone = "bi-wallet2";

    /** Numero de transaction / cheque / virement exige a l'encaissement (rapprochement). */
    @Column(name = "reference_obligatoire", nullable = false)
    private boolean referenceObligatoire = true;
}

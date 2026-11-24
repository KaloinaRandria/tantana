package mg.working.tantana.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum StatutVente {
    BROUILLON("Brouillon", "secondary"),
    /** Remise au-dela du plafond ou prix sous le revient : un CHECKER doit approuver. */
    A_VALIDER("À valider", "warning"),
    /** Facture emise : stock sorti, numero attribue, plus modifiable. */
    VALIDEE("Facturée", "success");

    private final String libelle;
    private final String couleur;
}

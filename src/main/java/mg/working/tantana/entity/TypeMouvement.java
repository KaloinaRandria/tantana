package mg.working.tantana.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** Nature d'un mouvement de stock ; le signe indique le sens (+1 entree, -1 sortie). */
@Getter
@RequiredArgsConstructor
public enum TypeMouvement {
    ENTREE_ACHAT("Entrée (réception achat)", 1),
    SORTIE_VENTE("Sortie (vente)", -1),
    AJUSTEMENT_POSITIF("Ajustement +", 1),
    AJUSTEMENT_NEGATIF("Ajustement −", -1);

    private final String libelle;
    private final int sens;
}

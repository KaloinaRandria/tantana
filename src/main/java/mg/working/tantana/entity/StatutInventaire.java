package mg.working.tantana.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * EN_COMPTAGE (comptage a l'aveugle, articles geles)
 *   -> A_VALIDER (ecarts visibles, a justifier)
 *        -> VALIDE (CHECKER : ajustements de stock passes)
 *        -> EN_COMPTAGE (CHECKER rejette : recomptage)
 * EN_COMPTAGE / A_VALIDER -> ANNULE (aucun effet sur le stock)
 */
@Getter
@RequiredArgsConstructor
public enum StatutInventaire {
    EN_COMPTAGE("En comptage", "info"),
    A_VALIDER("À valider", "warning"),
    VALIDE("Validé", "success"),
    ANNULE("Annulé", "secondary");

    private final String libelle;
    private final String couleur;

    /** Inventaire ouvert : ses articles sont geles (aucun mouvement de stock possible). */
    public boolean estOuvert() {
        return this == EN_COMPTAGE || this == A_VALIDER;
    }
}

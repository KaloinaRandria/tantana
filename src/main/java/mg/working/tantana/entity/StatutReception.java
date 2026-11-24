package mg.working.tantana.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * SAISIE (par le MAKER) -> VALIDEE (par le CHECKER : le stock bouge)
 *                       -> REJETEE (par le CHECKER, avec motif) -> corrigee -> SAISIE
 */
@Getter
@RequiredArgsConstructor
public enum StatutReception {
    SAISIE("À valider", "warning"),
    VALIDEE("Validée", "success"),
    REJETEE("Rejetée", "danger");

    private final String libelle;
    private final String couleur;
}

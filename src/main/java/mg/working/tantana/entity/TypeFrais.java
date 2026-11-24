package mg.working.tantana.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** Nature des frais factures par le transitaire. */
@Getter
@RequiredArgsConstructor
public enum TypeFrais {
    FRET("Fret / transport"),
    DOUANE("Droits et taxes de douane"),
    TRANSIT("Honoraires de transit"),
    MANUTENTION("Manutention"),
    STOCKAGE("Magasinage / stockage"),
    AUTRE("Autre");

    private final String libelle;
}

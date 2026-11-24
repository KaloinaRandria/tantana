package mg.working.tantana.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** Unite dans laquelle un article est compte en stock. */
@Getter
@RequiredArgsConstructor
public enum Unite {
    PIECE("Pièce"),
    CARTON("Carton"),
    SAC("Sac"),
    LOT("Lot"),
    KG("Kilogramme"),
    LITRE("Litre"),
    METRE("Mètre");

    private final String libelle;
}

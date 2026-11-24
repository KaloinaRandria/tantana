package mg.working.tantana.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** Devises d'achat (code ISO 4217). Le stock est toujours valorise en MGA. */
@Getter
@RequiredArgsConstructor
public enum Devise {
    USD("Dollar américain"),
    EUR("Euro"),
    CNY("Yuan chinois"),
    AED("Dirham des Émirats"),
    INR("Roupie indienne"),
    JPY("Yen japonais"),
    ZAR("Rand sud-africain"),
    MGA("Ariary malgache");

    private final String libelle;
}

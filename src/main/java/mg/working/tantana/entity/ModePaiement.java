package mg.working.tantana.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ModePaiement {
    ESPECES("Espèces", "bi-cash"),
    MVOLA("MVola", "bi-phone"),
    ORANGE_MONEY("Orange Money", "bi-phone"),
    AIRTEL_MONEY("Airtel Money", "bi-phone"),
    VIREMENT("Virement bancaire", "bi-bank"),
    CHEQUE("Chèque", "bi-journal-check");

    private final String libelle;
    private final String icone;
}

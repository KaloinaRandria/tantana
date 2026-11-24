package mg.working.tantana.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** Etat d'encaissement d'une facture (calcule, non stocke). */
@Getter
@RequiredArgsConstructor
public enum StatutPaiement {
    NON_PAYEE("Non payée", "danger"),
    PARTIELLE("Partiellement payée", "warning"),
    PAYEE("Payée", "success"),
    /** Facture entierement annulee par avoir, rien a encaisser. */
    SOLDEE_PAR_AVOIR("Soldée par avoir", "secondary");

    private final String libelle;
    private final String couleur;
}

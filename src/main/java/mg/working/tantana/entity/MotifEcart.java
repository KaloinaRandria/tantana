package mg.working.tantana.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** Cause d'un ecart d'inventaire : obligatoire pour toute ligne dont le compte differe du theorique. */
@Getter
@RequiredArgsConstructor
public enum MotifEcart {
    CASSE("Casse / détérioration"),
    PEREMPTION("Produit périmé"),
    VOL_PERTE("Vol ou perte inexpliquée"),
    ERREUR_RECEPTION("Erreur de réception (quantité saisie)"),
    ERREUR_VENTE("Erreur de vente / livraison"),
    ECHANTILLON("Échantillon / usage interne"),
    AUTRE("Autre (préciser en commentaire)");

    private final String libelle;
}

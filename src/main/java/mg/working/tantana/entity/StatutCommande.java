package mg.working.tantana.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Cycle de vie d'une commande d'achat :
 * BROUILLON -> COMMANDEE -> PARTIELLEMENT_RECUE -> RECUE
 *                        \-> CLOTUREE (reste abandonne)
 * BROUILLON / COMMANDEE (rien recu) -> ANNULEE
 */
@Getter
@RequiredArgsConstructor
public enum StatutCommande {
    BROUILLON("Brouillon", "secondary"),
    COMMANDEE("Commandée", "primary"),
    PARTIELLEMENT_RECUE("Partiellement reçue", "warning"),
    RECUE("Reçue", "success"),
    CLOTUREE("Clôturée", "dark"),
    ANNULEE("Annulée", "danger");

    private final String libelle;
    /** Couleur Bootstrap du badge (text-bg-...). */
    private final String couleur;

    /** En attente d'arrivage : peut etre en retard et recevoir des receptions. */
    public boolean estEnAttente() {
        return this == COMMANDEE || this == PARTIELLEMENT_RECUE;
    }
}

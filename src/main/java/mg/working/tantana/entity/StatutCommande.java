package mg.working.tantana.entity;

/**
 * Cycle de vie d'une commande d'achat :
 * BROUILLON -> COMMANDEE -> PARTIELLEMENT_RECUE -> RECUE
 *                        \-> CLOTUREE (reste abandonne)
 * BROUILLON / COMMANDEE (rien recu) -> ANNULEE
 * Stocke en base par son id (commande_achat.statut_id).
 */
public enum StatutCommande implements ValeurSysteme {
    BROUILLON(1),
    COMMANDEE(2),
    PARTIELLEMENT_RECUE(3),
    RECUE(4),
    CLOTUREE(5),
    ANNULEE(6);

    private final short id;

    StatutCommande(int id) {
        this.id = (short) id;
    }

    @Override
    public short getId() {
        return id;
    }

    @Override
    public String getTable() {
        return "statut_commande";
    }

    /** Libelle affiche (table statut_commande). */
    public String getLibelle() {
        return LibellesSysteme.libelle(this);
    }

    /** Couleur Bootstrap du badge (text-bg-...). */
    public String getCouleur() {
        return LibellesSysteme.couleur(this);
    }

    /** En attente d'arrivage : peut etre en retard et recevoir des receptions. */
    public boolean estEnAttente() {
        return this == COMMANDEE || this == PARTIELLEMENT_RECUE;
    }
}

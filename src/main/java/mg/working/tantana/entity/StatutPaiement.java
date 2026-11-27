package mg.working.tantana.entity;

/** Etat d'encaissement d'une facture (calcule, non stocke ; libelles en base). */
public enum StatutPaiement implements ValeurSysteme {
    NON_PAYEE(1),
    PARTIELLE(2),
    PAYEE(3),
    /** Facture entierement annulee par avoir, rien a encaisser. */
    SOLDEE_PAR_AVOIR(4);

    private final short id;

    StatutPaiement(int id) {
        this.id = (short) id;
    }

    @Override
    public short getId() {
        return id;
    }

    @Override
    public String getTable() {
        return "statut_paiement";
    }

    /** Libelle affiche (table statut_paiement). */
    public String getLibelle() {
        return LibellesSysteme.libelle(this);
    }

    /** Couleur Bootstrap du badge (text-bg-...). */
    public String getCouleur() {
        return LibellesSysteme.couleur(this);
    }
}

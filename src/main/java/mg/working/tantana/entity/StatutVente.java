package mg.working.tantana.entity;

public enum StatutVente implements ValeurSysteme {
    BROUILLON(1),
    /** Remise au-dela du plafond ou prix sous le revient : un CHECKER doit approuver. */
    A_VALIDER(2),
    /** Facture emise : stock sorti, numero attribue, plus modifiable. */
    VALIDEE(3);

    private final short id;

    StatutVente(int id) {
        this.id = (short) id;
    }

    @Override
    public short getId() {
        return id;
    }

    @Override
    public String getTable() {
        return "statut_vente";
    }

    /** Libelle affiche (table statut_vente). */
    public String getLibelle() {
        return LibellesSysteme.libelle(this);
    }

    /** Couleur Bootstrap du badge (text-bg-...). */
    public String getCouleur() {
        return LibellesSysteme.couleur(this);
    }
}

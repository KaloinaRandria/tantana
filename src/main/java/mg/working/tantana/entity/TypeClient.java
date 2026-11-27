package mg.working.tantana.entity;

public enum TypeClient implements ValeurSysteme {
    PARTICULIER(1),
    /** Entreprise : NIF et STAT obligatoires (mentions des factures). */
    PROFESSIONNEL(2);

    private final short id;

    TypeClient(int id) {
        this.id = (short) id;
    }

    @Override
    public short getId() {
        return id;
    }

    @Override
    public String getTable() {
        return "type_client";
    }

    /** Libelle affiche (table type_client). */
    public String getLibelle() {
        return LibellesSysteme.libelle(this);
    }

    /** Couleur Bootstrap du badge (text-bg-...). */
    public String getCouleur() {
        return LibellesSysteme.couleur(this);
    }
}

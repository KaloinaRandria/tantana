package mg.working.tantana.entity;

/** Nature d'un mouvement de stock ; le signe indique le sens (+1 entree, -1 sortie). */
public enum TypeMouvement implements ValeurSysteme {
    ENTREE_ACHAT(1, 1),
    SORTIE_VENTE(2, -1),
    RETOUR_VENTE(3, 1),
    AJUSTEMENT_POSITIF(4, 1),
    AJUSTEMENT_NEGATIF(5, -1);

    private final short id;
    /** +1 entree, -1 sortie : fige, utilise par les calculs. */
    private final int sens;

    TypeMouvement(int id, int sens) {
        this.id = (short) id;
        this.sens = sens;
    }

    @Override
    public short getId() {
        return id;
    }

    @Override
    public String getTable() {
        return "type_mouvement";
    }

    /** Libelle affiche (table type_mouvement). */
    public String getLibelle() {
        return LibellesSysteme.libelle(this);
    }

    /** Couleur Bootstrap du badge (text-bg-...). */
    public String getCouleur() {
        return LibellesSysteme.couleur(this);
    }

    public int getSens() {
        return sens;
    }
}

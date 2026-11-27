package mg.working.tantana.entity;

/**
 * EN_COMPTAGE (comptage a l'aveugle, articles geles)
 *   -> A_VALIDER (ecarts visibles, a justifier)
 *        -> VALIDE (CHECKER : ajustements de stock passes)
 *        -> EN_COMPTAGE (CHECKER rejette : recomptage)
 * EN_COMPTAGE / A_VALIDER -> ANNULE (aucun effet sur le stock)
 */
public enum StatutInventaire implements ValeurSysteme {
    EN_COMPTAGE(1),
    A_VALIDER(2),
    VALIDE(3),
    ANNULE(4);

    private final short id;

    StatutInventaire(int id) {
        this.id = (short) id;
    }

    @Override
    public short getId() {
        return id;
    }

    @Override
    public String getTable() {
        return "statut_inventaire";
    }

    /** Libelle affiche (table statut_inventaire). */
    public String getLibelle() {
        return LibellesSysteme.libelle(this);
    }

    /** Couleur Bootstrap du badge (text-bg-...). */
    public String getCouleur() {
        return LibellesSysteme.couleur(this);
    }

    /** Inventaire ouvert : ses articles sont geles (aucun mouvement de stock possible). */
    public boolean estOuvert() {
        return this == EN_COMPTAGE || this == A_VALIDER;
    }
}

package mg.working.tantana.entity;

/**
 * SAISIE (par le MAKER) -> VALIDEE (par le CHECKER : le stock bouge)
 *                       -> REJETEE (par le CHECKER, avec motif) -> corrigee -> SAISIE
 */
public enum StatutReception implements ValeurSysteme {
    SAISIE(1),
    VALIDEE(2),
    REJETEE(3);

    private final short id;

    StatutReception(int id) {
        this.id = (short) id;
    }

    @Override
    public short getId() {
        return id;
    }

    @Override
    public String getTable() {
        return "statut_reception";
    }

    /** Libelle affiche (table statut_reception). */
    public String getLibelle() {
        return LibellesSysteme.libelle(this);
    }

    /** Couleur Bootstrap du badge (text-bg-...). */
    public String getCouleur() {
        return LibellesSysteme.couleur(this);
    }
}

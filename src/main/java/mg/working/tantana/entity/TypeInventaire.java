package mg.working.tantana.entity;

public enum TypeInventaire implements ValeurSysteme {
    /** Tous les articles actifs (et les inactifs qui ont encore du stock) : inventaire annuel. */
    COMPLET(1),
    /** Quelques articles a la fois, tout au long de l'annee (classe A chaque mois, etc.). */
    TOURNANT(2);

    private final short id;

    TypeInventaire(int id) {
        this.id = (short) id;
    }

    @Override
    public short getId() {
        return id;
    }

    @Override
    public String getTable() {
        return "type_inventaire";
    }

    /** Libelle affiche (table type_inventaire). */
    public String getLibelle() {
        return LibellesSysteme.libelle(this);
    }

    /** Couleur Bootstrap du badge (text-bg-...). */
    public String getCouleur() {
        return LibellesSysteme.couleur(this);
    }

    public String getDescription() {
        return LibellesSysteme.description(this);
    }
}

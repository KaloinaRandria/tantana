package mg.working.tantana.entity;

/**
 * Valeur systeme (statut ou type qui pilote une regle du code).
 * L'identifiant numerique est stocke en base (colonne SMALLINT) ; le libelle,
 * la couleur et la description sont lus dans la table de reference (modifiables par l'ADMIN)
 * et mis en cache dans {@link LibellesSysteme}.
 */
public interface ValeurSysteme {

    /** Identifiant stocke en base (fige : le code s'y refere). */
    short getId();

    /** Table de reference qui porte le libelle et la couleur. */
    String getTable();

    /** Nom technique (BROUILLON, VALIDEE...) : identique a la colonne code de la table. */
    String name();
}

package mg.working.tantana.entity;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Cache des libelles, couleurs et descriptions des valeurs systeme, lus en base
 * (tables statut_* et type_*). Rempli au demarrage puis apres chaque modification
 * par ReferentielSystemeService. Tant qu'il n'est pas charge, le nom technique est affiche.
 */
public final class LibellesSysteme {

    /** Contenu d'une ligne de table de reference. */
    public record Entree(String libelle, String couleur, String description) {
    }

    private static final Map<String, Entree> CACHE = new ConcurrentHashMap<>();

    private LibellesSysteme() {
    }

    private static String cle(String table, short id) {
        return table + ":" + id;
    }

    public static void enregistrer(String table, short id, Entree entree) {
        CACHE.put(cle(table, id), entree);
    }

    private static Entree entree(ValeurSysteme v) {
        return CACHE.get(cle(v.getTable(), v.getId()));
    }

    public static String libelle(ValeurSysteme v) {
        Entree e = entree(v);
        return e != null ? e.libelle() : v.name();
    }

    public static String couleur(ValeurSysteme v) {
        Entree e = entree(v);
        return e != null ? e.couleur() : "secondary";
    }

    public static String description(ValeurSysteme v) {
        Entree e = entree(v);
        return e != null ? e.description() : null;
    }
}

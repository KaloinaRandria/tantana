package mg.working.tantana.controller;

/** Filtre « Actifs / Inactifs / Tous » des listes de referentiels. */
final class FiltreStatut {

    static final String ACTIFS = "actifs";

    private FiltreStatut() {
    }

    /** @return true, false, ou null pour « tous » */
    static Boolean versBooleen(String statut) {
        return switch (statut == null ? ACTIFS : statut) {
            case "inactifs" -> false;
            case "tous" -> null;
            default -> true;
        };
    }
}

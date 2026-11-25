package mg.working.tantana.dto;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.IsoFields;
import java.util.function.Function;

/**
 * Periodes predefinies des filtres de dates. Chaque periode calcule ses bornes (incluses)
 * a partir de la date du jour ; les semaines commencent le lundi, les trimestres sont civils.
 */
@Getter
@RequiredArgsConstructor
public enum Periode {
    AUJOURDHUI("Aujourd'hui", j -> new LocalDate[]{j, j}),
    HIER("Hier", j -> new LocalDate[]{j.minusDays(1), j.minusDays(1)}),
    SEMAINE("Cette semaine", j -> new LocalDate[]{j.with(DayOfWeek.MONDAY), j}),
    SEMAINE_PRECEDENTE("Semaine précédente",
            j -> new LocalDate[]{j.with(DayOfWeek.MONDAY).minusWeeks(1), j.with(DayOfWeek.MONDAY).minusDays(1)}),
    SEPT_JOURS("7 derniers jours", j -> new LocalDate[]{j.minusDays(6), j}),
    TRENTE_JOURS("30 derniers jours", j -> new LocalDate[]{j.minusDays(29), j}),
    MOIS("Ce mois", j -> new LocalDate[]{j.withDayOfMonth(1), j}),
    MOIS_PRECEDENT("Mois précédent",
            j -> new LocalDate[]{j.withDayOfMonth(1).minusMonths(1), j.withDayOfMonth(1).minusDays(1)}),
    TRIMESTRE("Ce trimestre", j -> new LocalDate[]{debutTrimestre(j), j}),
    TRIMESTRE_PRECEDENT("Dernier trimestre",
            j -> new LocalDate[]{debutTrimestre(j).minusMonths(3), debutTrimestre(j).minusDays(1)}),
    DOUZE_MOIS("12 derniers mois", j -> new LocalDate[]{j.minusMonths(12).plusDays(1), j}),
    ANNEE("Cette année", j -> new LocalDate[]{j.withDayOfYear(1), j}),
    ANNEE_PRECEDENTE("Année précédente",
            j -> new LocalDate[]{j.withDayOfYear(1).minusYears(1), j.withDayOfYear(1).minusDays(1)}),
    /** Pas de filtre de date (listes uniquement). */
    TOUT("Toutes les dates", j -> new LocalDate[]{null, null}),
    /** Dates saisies a la main (Du / Au). */
    PERSONNALISEE("Personnalisée", j -> null);

    private final String libelle;
    private final Function<LocalDate, LocalDate[]> calcul;

    /** Bornes [du, au] au jour donne ; null pour PERSONNALISEE. */
    public LocalDate[] bornes(LocalDate aujourdhui) {
        return calcul.apply(aujourdhui);
    }

    private static LocalDate debutTrimestre(LocalDate j) {
        return j.with(IsoFields.DAY_OF_QUARTER, 1);
    }
}

package mg.working.tantana.dto;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

/**
 * Filtre de dates resolu a partir des parametres de la requete (periode, du, au).
 *
 * <ul>
 *   <li>periode predefinie : ses bornes, les dates saisies sont ignorees ;</li>
 *   <li>dates saisies sans periode : plage personnalisee ; si elle correspond a une periode
 *       predefinie (ex. du 1er au jour = « Ce mois »), ce bouton apparait actif ;</li>
 *   <li>rien : la periode par defaut de l'ecran.</li>
 * </ul>
 *
 * @param du null = sans borne de debut (periode TOUT)
 * @param au null = sans borne de fin
 */
public record FiltrePeriode(Periode periode, LocalDate du, LocalDate au, List<Periode> proposees) {

    private static final DateTimeFormatter FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    /** Bornes techniques quand la periode n'en a pas (requetes plus simples qu'avec des NULL). */
    private static final LocalDate MIN = LocalDate.of(1900, 1, 1);
    private static final LocalDate MAX = LocalDate.of(9999, 12, 31);

    /** Periodes des ecrans d'analyse (pas de « Toutes les dates »). */
    public static final List<Periode> ANALYSE = Arrays.stream(Periode.values())
            .filter(p -> p != Periode.TOUT && p != Periode.PERSONNALISEE).toList();
    /** Periodes des listes : « Toutes les dates » en premier. */
    public static final List<Periode> LISTE = Stream.concat(
            Stream.of(Periode.TOUT), ANALYSE.stream()).toList();

    public static FiltrePeriode resoudre(Periode periode, LocalDate du, LocalDate au,
                                         Periode parDefaut, List<Periode> proposees) {
        LocalDate aujourdhui = LocalDate.now();
        if (periode != null && periode != Periode.PERSONNALISEE) {
            LocalDate[] b = periode.bornes(aujourdhui);
            return new FiltrePeriode(periode, b[0], b[1], proposees);
        }
        if (du != null || au != null) {
            Periode reconnue = proposees.stream()
                    .filter(p -> p != Periode.TOUT)
                    .filter(p -> {
                        LocalDate[] b = p.bornes(aujourdhui);
                        return b[0].equals(du) && b[1].equals(au);
                    })
                    .findFirst().orElse(Periode.PERSONNALISEE);
            return new FiltrePeriode(reconnue, du, au, proposees);
        }
        LocalDate[] b = parDefaut.bornes(aujourdhui);
        return new FiltrePeriode(parDefaut, b[0], b[1], proposees);
    }

    /** Borne de debut pour une requete (jamais null). */
    public LocalDate debutRequete() {
        return du == null ? MIN : du;
    }

    /** Borne de fin pour une requete (jamais null). */
    public LocalDate finRequete() {
        return au == null ? MAX : au;
    }

    public boolean isFiltre() {
        return du != null || au != null;
    }

    /** « du 01/11/2026 au 25/11/2026 », « depuis le ... », « jusqu'au ... » ou « toutes les dates ». */
    public String getDescription() {
        if (du != null && au != null) {
            return du.equals(au) ? "le " + du.format(FORMAT) : "du " + du.format(FORMAT) + " au " + au.format(FORMAT);
        }
        if (du != null) {
            return "depuis le " + du.format(FORMAT);
        }
        if (au != null) {
            return "jusqu'au " + au.format(FORMAT);
        }
        return "toutes les dates";
    }

    /** Boutons visibles directement : les periodes les plus utilisees. */
    private static final List<Periode> RAPIDES = List.of(Periode.TOUT, Periode.AUJOURDHUI, Periode.SEMAINE,
            Periode.MOIS, Periode.MOIS_PRECEDENT, Periode.TRIMESTRE_PRECEDENT, Periode.ANNEE);

    public List<Periode> getRapides() {
        return RAPIDES.stream().filter(proposees::contains).toList();
    }

    /** Les autres periodes, dans le menu « Plus ». */
    public List<Periode> getAutres() {
        return proposees.stream().filter(p -> !RAPIDES.contains(p)).toList();
    }

    /** La periode active est-elle dans le menu « Plus » (pour afficher son nom sur le bouton) ? */
    public boolean isActiveDansAutres() {
        return getAutres().contains(periode);
    }

    public Periode getPeriode() { return periode; }
    public LocalDate getDu() { return du; }
    public LocalDate getAu() { return au; }
}

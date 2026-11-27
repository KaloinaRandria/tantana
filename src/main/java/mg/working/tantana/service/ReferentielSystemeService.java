package mg.working.tantana.service;

import lombok.RequiredArgsConstructor;
import mg.working.tantana.entity.LibellesSysteme;
import mg.working.tantana.exception.RegleMetierException;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Statuts et types systeme (tables statut_* et type_*) : valeurs fixes dont le code depend,
 * libelles, couleurs et descriptions modifiables par l'ADMIN. Les libelles sont charges
 * dans {@link LibellesSysteme} au demarrage et apres chaque modification.
 */
@Service
@RequiredArgsConstructor
public class ReferentielSystemeService {

    /** Une table de reference systeme, dans l'ordre d'affichage. */
    public record Table(String nom, String titre, boolean avecDescription) {
    }

    /** Une valeur systeme telle qu'affichee dans l'ecran. */
    public record Valeur(String table, short id, String code, String libelle, String couleur, String description) {
    }

    public record Groupe(Table table, List<Valeur> valeurs) {
    }

    public static final List<Table> TABLES = List.of(
            new Table("statut_commande", "Statuts des commandes d'achat", false),
            new Table("statut_reception", "Statuts des réceptions", false),
            new Table("statut_vente", "Statuts des ventes", false),
            new Table("statut_paiement", "Statuts de paiement des factures", false),
            new Table("statut_inventaire", "Statuts des inventaires", false),
            new Table("type_mouvement", "Types de mouvement de stock", false),
            new Table("type_inventaire", "Types d'inventaire", true),
            new Table("type_client", "Types de client", false));

    /** Couleurs Bootstrap proposees pour les badges. */
    public static final List<String> COULEURS = List.of(
            "primary", "secondary", "success", "danger", "warning", "info", "dark", "light");

    private static final Set<String> NOMS = Set.copyOf(TABLES.stream().map(Table::nom).toList());

    private final JdbcTemplate jdbc;

    @EventListener(ApplicationReadyEvent.class)
    public void charger() {
        for (Groupe g : lister()) {
            for (Valeur v : g.valeurs()) {
                LibellesSysteme.enregistrer(v.table(), v.id(),
                        new LibellesSysteme.Entree(v.libelle(), v.couleur(), v.description()));
            }
        }
    }

    public List<Groupe> lister() {
        List<Groupe> groupes = new ArrayList<>();
        for (Table t : TABLES) {
            String colonneDescription = t.avecDescription() ? "description" : "NULL AS description";
            List<Valeur> valeurs = jdbc.query(
                    "SELECT id, code, libelle, couleur, " + colonneDescription + " FROM " + t.nom() + " ORDER BY id",
                    (rs, i) -> new Valeur(t.nom(), rs.getShort("id"), rs.getString("code"), rs.getString("libelle"),
                            rs.getString("couleur"), rs.getString("description")));
            groupes.add(new Groupe(t, valeurs));
        }
        return groupes;
    }

    public Table table(String nom) {
        return TABLES.stream().filter(t -> t.nom().equals(nom)).findFirst()
                .orElseThrow(() -> new RegleMetierException("Table de référence inconnue : " + nom));
    }

    public Valeur trouver(String table, short id) {
        verifierTable(table);
        return lister().stream().filter(g -> g.table().nom().equals(table))
                .flatMap(g -> g.valeurs().stream()).filter(v -> v.id() == id).findFirst()
                .orElseThrow(() -> new RegleMetierException("Valeur introuvable"));
    }

    /** Modifie le libelle, la couleur (et la description) : l'identifiant et le code restent figes. */
    @Transactional
    public void modifier(String table, short id, String libelle, String couleur, String description) {
        Table t = table(table);
        if (libelle == null || libelle.isBlank()) {
            throw new RegleMetierException("libelle", "Le libellé est obligatoire");
        }
        String l = libelle.trim();
        if (l.length() > 60) {
            throw new RegleMetierException("libelle", "60 caractères maximum");
        }
        if (!COULEURS.contains(couleur)) {
            throw new RegleMetierException("couleur", "Couleur inconnue");
        }
        Integer doublons = jdbc.queryForObject(
                "SELECT count(*) FROM " + t.nom() + " WHERE lower(libelle) = lower(?) AND id <> ?", Integer.class, l, id);
        if (doublons != null && doublons > 0) {
            throw new RegleMetierException("libelle", "Ce libellé est déjà utilisé dans la même liste");
        }
        if (t.avecDescription()) {
            String d = description == null || description.isBlank() ? null : description.trim();
            if (d != null && d.length() > 200) {
                throw new RegleMetierException("description", "200 caractères maximum");
            }
            jdbc.update("UPDATE " + t.nom() + " SET libelle = ?, couleur = ?, description = ? WHERE id = ?", l, couleur, d, id);
        } else {
            jdbc.update("UPDATE " + t.nom() + " SET libelle = ?, couleur = ? WHERE id = ?", l, couleur, id);
        }
        charger();
    }

    private void verifierTable(String table) {
        if (!NOMS.contains(table)) {
            throw new RegleMetierException("Table de référence inconnue : " + table);
        }
    }
}

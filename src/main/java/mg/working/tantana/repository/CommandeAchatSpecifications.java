package mg.working.tantana.repository;

import jakarta.persistence.criteria.Predicate;
import mg.working.tantana.entity.CommandeAchat;
import mg.working.tantana.entity.StatutCommande;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/** Filtres combinables de la liste des commandes (chaque filtre absent est ignore). */
public final class CommandeAchatSpecifications {

    private CommandeAchatSpecifications() {
    }

    public static Specification<CommandeAchat> numeroContient(String texte) {
        if (texte == null || texte.isBlank()) {
            return null;
        }
        String motif = "%" + texte.trim().toLowerCase() + "%";
        return (root, query, cb) -> cb.like(cb.lower(root.get("numero")), motif);
    }

    public static Specification<CommandeAchat> statut(StatutCommande statut) {
        return statut == null ? null : (root, query, cb) -> cb.equal(root.get("statut"), statut);
    }

    public static Specification<CommandeAchat> fournisseur(Long fournisseurId) {
        return fournisseurId == null ? null
                : (root, query, cb) -> cb.equal(root.get("fournisseur").get("id"), fournisseurId);
    }

    /** Date de commande comprise dans la periode (bornes incluses ; une borne null est ignoree). */
    public static Specification<CommandeAchat> commandeeEntre(LocalDate du, LocalDate au) {
        if (du == null && au == null) {
            return null;
        }
        return (root, query, cb) -> {
            if (du == null) {
                return cb.lessThanOrEqualTo(root.get("dateCommande"), au);
            }
            return au == null ? cb.greaterThanOrEqualTo(root.get("dateCommande"), du)
                    : cb.between(root.get("dateCommande"), du, au);
        };
    }

    /** Combine les filtres presents par un ET (les filtres null sont ignores). */
    @SafeVarargs
    public static Specification<CommandeAchat> tous(Specification<CommandeAchat>... filtres) {
        List<Specification<CommandeAchat>> presents = Arrays.stream(filtres).filter(Objects::nonNull).toList();
        return (root, query, cb) -> cb.and(presents.stream()
                .map(f -> f.toPredicate(root, query, cb))
                .filter(Objects::nonNull)
                .toArray(Predicate[]::new));
    }

    /** Commandes attendues dont la date d'arrivee prevue est depassee. */
    public static Specification<CommandeAchat> enRetard(boolean actif) {
        if (!actif) {
            return null;
        }
        return (root, query, cb) -> cb.and(
                root.get("statut").in(List.of(StatutCommande.COMMANDEE, StatutCommande.PARTIELLEMENT_RECUE)),
                cb.lessThan(root.get("dateArriveePrevue"), LocalDate.now()));
    }
}

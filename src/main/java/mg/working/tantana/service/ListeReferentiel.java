package mg.working.tantana.service;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** Listes metier gerees dans l'ecran Administration > Referentiels (un onglet chacune). */
@Getter
@RequiredArgsConstructor
public enum ListeReferentiel {
    UNITES("unites", "Unités de mesure", "Unité", "bi-rulers", "Articles"),
    MODES_PAIEMENT("modes-paiement", "Modes de paiement", "Mode de paiement", "bi-wallet2", "Paiements"),
    MOTIFS_ECART("motifs-ecart", "Motifs d'écart", "Motif d'écart", "bi-clipboard-x", "Lignes d'inventaire"),
    TYPES_FRAIS("types-frais", "Types de frais", "Type de frais", "bi-receipt", "Frais de réception"),
    DEVISES("devises", "Devises", "Devise", "bi-currency-exchange", "Fournisseurs + commandes");

    /** Valeur du parametre ?onglet= */
    private final String onglet;
    private final String titre;
    /** Nom au singulier (messages, titre du formulaire). */
    private final String element;
    private final String icone;
    /** En-tete de la colonne « utilisations ». */
    private final String utilisations;

    public static ListeReferentiel depuisOnglet(String onglet) {
        for (ListeReferentiel l : values()) {
            if (l.onglet.equals(onglet)) {
                return l;
            }
        }
        return null;
    }
}

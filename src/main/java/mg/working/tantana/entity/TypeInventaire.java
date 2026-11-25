package mg.working.tantana.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum TypeInventaire {
    /** Tous les articles actifs (et les inactifs qui ont encore du stock) : inventaire annuel. */
    COMPLET("Complet", "Tous les articles — inventaire annuel de clôture"),
    /** Quelques articles a la fois, tout au long de l'annee (classe A chaque mois, etc.). */
    TOURNANT("Tournant", "Une sélection d'articles — comptage régulier sans fermer le magasin");

    private final String libelle;
    private final String description;
}

package mg.working.tantana.service;

import mg.working.tantana.entity.LigneReception;
import mg.working.tantana.entity.Reception;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Repartition des frais du transitaire sur les articles recus, au prorata de leur valeur d'achat.
 *
 * Exemple : 10 000 000 MGA de marchandises recues, 1 000 000 MGA de frais.
 * Une ligne valant 3 000 000 MGA (30 %) porte 300 000 MGA de frais ;
 * si elle compte 60 unites, chaque unite porte 5 000 MGA de frais.
 *
 * Seules les quantites recues en bon etat portent les frais (ce sont elles qui entrent en stock).
 * Si toutes les lignes ont une valeur nulle (articles gratuits), la repartition se fait a la quantite.
 */
public final class RepartitionFrais {

    private RepartitionFrais() {
    }

    /** @return frais par unite pour chaque ligne ayant une quantite recue (en Ariary, 4 decimales) */
    public static Map<LigneReception, BigDecimal> fraisUnitaires(Reception reception) {
        Map<LigneReception, BigDecimal> resultat = new LinkedHashMap<>();
        List<LigneReception> lignes = reception.getLignes().stream()
                .filter(l -> l.getQuantiteRecue().signum() > 0)
                .toList();
        BigDecimal totalFrais = reception.getTotalFrais();
        if (lignes.isEmpty()) {
            return resultat;
        }
        if (totalFrais.signum() == 0) {
            lignes.forEach(l -> resultat.put(l, BigDecimal.ZERO));
            return resultat;
        }

        BigDecimal totalValeur = lignes.stream().map(LigneReception::getValeurAchatMga)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        boolean parQuantite = totalValeur.signum() == 0;
        BigDecimal base = parQuantite
                ? lignes.stream().map(LigneReception::getQuantiteRecue).reduce(BigDecimal.ZERO, BigDecimal::add)
                : totalValeur;

        for (LigneReception ligne : lignes) {
            BigDecimal poids = parQuantite ? ligne.getQuantiteRecue() : ligne.getValeurAchatMga();
            // frais de la ligne = total frais x poids / base, puis ramene a l'unite
            BigDecimal fraisLigne = totalFrais.multiply(poids);
            BigDecimal parUnite = fraisLigne.divide(base.multiply(ligne.getQuantiteRecue()), 4, RoundingMode.HALF_UP);
            resultat.put(ligne, parUnite);
        }
        return resultat;
    }
}

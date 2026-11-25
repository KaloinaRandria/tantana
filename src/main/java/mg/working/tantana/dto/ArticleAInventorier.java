package mg.working.tantana.dto;

import java.math.BigDecimal;

/**
 * Article propose a l'ouverture d'un inventaire tournant.
 *
 * @param classe        classe ABC sur la valeur du stock au revient (A = 80 % de la valeur)
 * @param inventaireGel numero de l'inventaire ouvert qui gele deja l'article (null sinon)
 */
public record ArticleAInventorier(Long id, String reference, String designation, String unite,
                                  BigDecimal quantite, BigDecimal valeur, String classe,
                                  boolean actif, String inventaireGel) {

    public boolean isGele() {
        return inventaireGel != null;
    }
}

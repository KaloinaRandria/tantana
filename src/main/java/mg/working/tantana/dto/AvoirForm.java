package mg.working.tantana.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/** Saisie d'un avoir : quantites a crediter par ligne de facture, et le motif. */
@Getter
@Setter
public class AvoirForm {

    private String motif;

    private List<Ligne> lignes = new ArrayList<>();

    @Getter
    @Setter
    @NoArgsConstructor
    public static class Ligne {
        private Long ligneVenteId;
        private BigDecimal quantite = BigDecimal.ZERO;
        /**
         * Faux si non transmis (par securite : rien ne rentre en stock sans case cochee).
         * Le formulaire, lui, propose la case cochee par defaut (voir le constructeur).
         */
        private boolean retourStock;

        public Ligne(Long ligneVenteId) {
            this.ligneVenteId = ligneVenteId;
            this.retourStock = true;
        }
    }
}

package mg.working.tantana.repository;

import lombok.RequiredArgsConstructor;
import mg.working.tantana.dto.RapportVentes.Creance;
import mg.working.tantana.dto.RapportVentes.Encaissement;
import mg.working.tantana.dto.RapportVentes.LigneMarge;
import mg.working.tantana.dto.RapportVentes.ModeAffiche;
import mg.working.tantana.dto.RapportVentes.Synthese;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.Date;
import java.time.LocalDate;
import java.util.List;

/**
 * Requetes d'agregation du tableau de bord (SQL direct : plus simple et plus rapide pour des sommes).
 * Les marges utilisent les couts FIGES sur les lignes de vente ; les avoirs sont deduits.
 */
@Repository
@RequiredArgsConstructor
public class RapportVentesRepository {

    private final JdbcTemplate jdbc;

    public Synthese synthese(LocalDate du, LocalDate au) {
        Object[] ventes = jdbc.queryForObject("""
                SELECT count(*), coalesce(sum(total_ht), 0), coalesce(sum(cout_revient_total), 0), coalesce(sum(total_ttc), 0)
                FROM vente WHERE statut_id = 3 AND date_vente BETWEEN ? AND ?
                """, (rs, i) -> new Object[]{rs.getLong(1), rs.getBigDecimal(2), rs.getBigDecimal(3), rs.getBigDecimal(4)},
                Date.valueOf(du), Date.valueOf(au));
        Object[] avoirs = jdbc.queryForObject("""
                SELECT count(*), coalesce(sum(total_ht), 0), coalesce(sum(cout_revient_total), 0), coalesce(sum(total_ttc), 0)
                FROM avoir WHERE date_avoir BETWEEN ? AND ?
                """, (rs, i) -> new Object[]{rs.getLong(1), rs.getBigDecimal(2), rs.getBigDecimal(3), rs.getBigDecimal(4)},
                Date.valueOf(du), Date.valueOf(au));
        return new Synthese((Long) ventes[0], (BigDecimal) ventes[1], (BigDecimal) ventes[2], (BigDecimal) ventes[3],
                (Long) avoirs[0], (BigDecimal) avoirs[1], (BigDecimal) avoirs[2], (BigDecimal) avoirs[3]);
    }

    public List<LigneMarge> parArticle(LocalDate du, LocalDate au) {
        return jdbc.query("""
                SELECT a.id, a.reference, a.designation, sum(x.q) AS qte, sum(x.ca) AS ca, sum(x.cout) AS cout
                FROM (
                    SELECT lv.article_id, lv.quantite AS q, lv.montant_ht AS ca, lv.cout_revient_unitaire * lv.quantite AS cout
                    FROM ligne_vente lv JOIN vente v ON v.id = lv.vente_id
                    WHERE v.statut_id = 3 AND v.date_vente BETWEEN ? AND ?
                    UNION ALL
                    SELECT lv.article_id, -la.quantite, -la.montant_ht, -lv.cout_revient_unitaire * la.quantite
                    FROM ligne_avoir la JOIN ligne_vente lv ON lv.id = la.ligne_vente_id JOIN avoir av ON av.id = la.avoir_id
                    WHERE av.date_avoir BETWEEN ? AND ?
                ) x JOIN article a ON a.id = x.article_id
                GROUP BY a.id, a.reference, a.designation
                ORDER BY sum(x.ca) - sum(x.cout) DESC
                """, (rs, i) -> new LigneMarge(rs.getLong("id"), rs.getString("reference"), rs.getString("designation"),
                        rs.getBigDecimal("qte"), arrondi(rs.getBigDecimal("ca")), arrondi(rs.getBigDecimal("cout"))),
                Date.valueOf(du), Date.valueOf(au), Date.valueOf(du), Date.valueOf(au));
    }

    public List<LigneMarge> parClient(LocalDate du, LocalDate au) {
        return jdbc.query("""
                SELECT c.id, c.nom, count(DISTINCT x.vente_id) AS nb, sum(x.ca) AS ca, sum(x.cout) AS cout
                FROM (
                    SELECT v.client_id, v.id AS vente_id, v.total_ht AS ca, v.cout_revient_total AS cout
                    FROM vente v WHERE v.statut_id = 3 AND v.date_vente BETWEEN ? AND ?
                    UNION ALL
                    SELECT v.client_id, NULL, -av.total_ht, -av.cout_revient_total
                    FROM avoir av JOIN vente v ON v.id = av.vente_id
                    WHERE av.date_avoir BETWEEN ? AND ?
                ) x JOIN client c ON c.id = x.client_id
                GROUP BY c.id, c.nom
                ORDER BY sum(x.ca) DESC
                """, (rs, i) -> new LigneMarge(rs.getLong("id"), rs.getString("nom"), rs.getLong("nb") + " facture(s)",
                        BigDecimal.valueOf(rs.getLong("nb")), arrondi(rs.getBigDecimal("ca")), arrondi(rs.getBigDecimal("cout"))),
                Date.valueOf(du), Date.valueOf(au), Date.valueOf(du), Date.valueOf(au));
    }

    public List<Encaissement> encaissements(LocalDate du, LocalDate au) {
        return jdbc.query("""
                SELECT m.libelle, m.icone, sum(p.montant) AS montant, count(*) AS nb
                FROM paiement p JOIN mode_paiement m ON m.id = p.mode_paiement_id
                WHERE NOT p.annule AND p.date_paiement BETWEEN ? AND ?
                GROUP BY m.id, m.libelle, m.icone ORDER BY sum(p.montant) DESC
                """, (rs, i) -> new Encaissement(new ModeAffiche(rs.getString("libelle"), rs.getString("icone")),
                        rs.getBigDecimal("montant"), rs.getLong("nb")),
                Date.valueOf(du), Date.valueOf(au));
    }

    /** CA HT net et cout de revient net par mois (avoirs deduits), depuis le 1er jour du mois donne. */
    public java.util.Map<java.time.YearMonth, BigDecimal[]> parMois(LocalDate depuis) {
        java.util.Map<java.time.YearMonth, BigDecimal[]> resultat = new java.util.TreeMap<>();
        jdbc.query("""
                SELECT to_char(x.d, 'YYYY-MM') AS mois, sum(x.ca) AS ca, sum(x.cout) AS cout
                FROM (
                    SELECT date_vente AS d, total_ht AS ca, cout_revient_total AS cout FROM vente
                    WHERE statut_id = 3 AND date_vente >= ?
                    UNION ALL
                    SELECT date_avoir, -total_ht, -cout_revient_total FROM avoir WHERE date_avoir >= ?
                ) x
                GROUP BY to_char(x.d, 'YYYY-MM')
                """, rs -> {
            resultat.put(java.time.YearMonth.parse(rs.getString("mois")),
                    new BigDecimal[]{arrondi(rs.getBigDecimal("ca")), arrondi(rs.getBigDecimal("cout"))});
        }, Date.valueOf(depuis), Date.valueOf(depuis));
        return resultat;
    }

    /** Creances a ce jour (independant de la periode). */
    public List<Creance> creances() {
        return jdbc.query("""
                SELECT c.id, c.nom, c.plafond_credit,
                       sum(v.total_ttc - v.montant_avoirs - v.montant_paye) AS reste,
                       count(*) AS nb, min(v.date_vente) AS plus_ancienne
                FROM vente v JOIN client c ON c.id = v.client_id
                WHERE v.statut_id = 3 AND v.total_ttc - v.montant_avoirs - v.montant_paye > 0
                GROUP BY c.id, c.nom, c.plafond_credit
                ORDER BY reste DESC
                """, (rs, i) -> new Creance(rs.getLong("id"), rs.getString("nom"), rs.getBigDecimal("reste"),
                rs.getLong("nb"), rs.getDate("plus_ancienne").toLocalDate(), rs.getBigDecimal("plafond_credit")));
    }

    private static BigDecimal arrondi(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v.setScale(0, java.math.RoundingMode.HALF_UP);
    }
}

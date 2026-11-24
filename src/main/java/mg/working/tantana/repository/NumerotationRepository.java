package mg.working.tantana.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * Compteurs de numeros de documents par prefixe et par annee.
 * L'UPSERT PostgreSQL est atomique : deux saisies simultanees ne peuvent
 * jamais obtenir le meme numero.
 */
@Repository
@RequiredArgsConstructor
public class NumerotationRepository {

    private final JdbcTemplate jdbcTemplate;

    /** Incremente et retourne le compteur (1 pour le premier document de l'annee). */
    public int incrementer(String prefixe, int annee) {
        Integer valeur = jdbcTemplate.queryForObject("""
                INSERT INTO numerotation (prefixe, annee, dernier) VALUES (?, ?, 1)
                ON CONFLICT (prefixe, annee) DO UPDATE SET dernier = numerotation.dernier + 1
                RETURNING dernier
                """, Integer.class, prefixe, annee);
        return valeur == null ? 1 : valeur;
    }
}

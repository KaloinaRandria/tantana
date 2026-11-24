package mg.working.tantana.service;

import lombok.RequiredArgsConstructor;
import mg.working.tantana.entity.Parametre;
import mg.working.tantana.exception.RegleMetierException;
import mg.working.tantana.repository.ParametreRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Parametres generaux (TVA, remise maximale, identite de la societe sur les factures). */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ParametreService {

    private final ParametreRepository parametreRepository;

    public Parametre obtenir() {
        return parametreRepository.findById(Parametre.ID_UNIQUE)
                .orElseThrow(() -> new IllegalStateException("Ligne de paramètres absente (migration V6)"));
    }

    @Transactional
    public Parametre enregistrer(Parametre saisie) {
        Parametre parametre = obtenir();
        if (saisie.getVersion() != null && !saisie.getVersion().equals(parametre.getVersion())) {
            throw new RegleMetierException("Les paramètres ont été modifiés entre-temps. Rechargez la page.");
        }
        parametre.setTauxTva(saisie.getTauxTva());
        parametre.setRemiseMaxVendeur(saisie.getRemiseMaxVendeur());
        parametre.setRaisonSociale(saisie.getRaisonSociale().trim());
        parametre.setAdresse(texte(saisie.getAdresse()));
        parametre.setTelephone(texte(saisie.getTelephone()));
        parametre.setEmail(texte(saisie.getEmail()));
        parametre.setNif(texte(saisie.getNif()));
        parametre.setStat(texte(saisie.getStat()));
        parametre.setRcs(texte(saisie.getRcs()));
        parametre.setMentionsFacture(texte(saisie.getMentionsFacture()));
        return parametre;
    }

    private static String texte(String valeur) {
        return valeur == null || valeur.isBlank() ? null : valeur.trim();
    }
}

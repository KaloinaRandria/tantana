package mg.working.tantana.service;

import lombok.RequiredArgsConstructor;
import mg.working.tantana.entity.Transitaire;
import mg.working.tantana.exception.DoublonException;
import mg.working.tantana.exception.ElementIntrouvableException;
import mg.working.tantana.repository.TransitaireRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TransitaireService {

    private final TransitaireRepository transitaireRepository;

    /** @param actif null = tous, true = actifs, false = inactifs */
    public Page<Transitaire> rechercher(String texte, Boolean actif, Pageable pageable) {
        String t = texte == null ? "" : texte.trim();
        return actif == null
                ? transitaireRepository.rechercher(t, pageable)
                : transitaireRepository.rechercher(t, actif, pageable);
    }

    public Transitaire trouver(Long id) {
        return transitaireRepository.findById(id)
                .orElseThrow(() -> new ElementIntrouvableException("Transitaire", id));
    }

    public List<Transitaire> listerActifs() {
        return transitaireRepository.findByActifTrueOrderByNomAsc();
    }

    @Transactional
    public Transitaire enregistrer(Transitaire saisie) {
        String nom = saisie.getNom().trim();

        transitaireRepository.findByNomIgnoreCase(nom)
                .filter(existant -> !existant.getId().equals(saisie.getId()))
                .ifPresent(existant -> {
                    throw new DoublonException("nom", "Un transitaire nommé « " + nom + " » existe déjà");
                });

        Transitaire transitaire = saisie.getId() == null ? new Transitaire() : trouver(saisie.getId());
        transitaire.setNom(nom);
        transitaire.setContact(vide(saisie.getContact()));
        transitaire.setTelephone(vide(saisie.getTelephone()));
        transitaire.setEmail(vide(saisie.getEmail()));
        transitaire.setAdresse(vide(saisie.getAdresse()));
        return transitaireRepository.save(transitaire);
    }

    @Transactional
    public Transitaire basculerActif(Long id) {
        Transitaire transitaire = trouver(id);
        transitaire.setActif(!transitaire.isActif());
        return transitaire;
    }

    private static String vide(String valeur) {
        return valeur == null || valeur.isBlank() ? null : valeur.trim();
    }
}

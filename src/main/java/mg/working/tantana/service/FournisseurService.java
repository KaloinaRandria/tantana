package mg.working.tantana.service;

import lombok.RequiredArgsConstructor;
import mg.working.tantana.entity.Fournisseur;
import mg.working.tantana.exception.DoublonException;
import mg.working.tantana.exception.ElementIntrouvableException;
import mg.working.tantana.repository.FournisseurRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FournisseurService {

    private final FournisseurRepository fournisseurRepository;

    /** @param actif null = tous, true = actifs, false = inactifs */
    public Page<Fournisseur> rechercher(String texte, Boolean actif, Pageable pageable) {
        String t = texte == null ? "" : texte.trim();
        return actif == null
                ? fournisseurRepository.rechercher(t, pageable)
                : fournisseurRepository.rechercher(t, actif, pageable);
    }

    public Fournisseur trouver(Long id) {
        return fournisseurRepository.findById(id)
                .orElseThrow(() -> new ElementIntrouvableException("Fournisseur", id));
    }

    public List<Fournisseur> listerActifs() {
        return fournisseurRepository.findByActifTrueOrderByNomAsc();
    }

    @Transactional
    public Fournisseur enregistrer(Fournisseur saisie) {
        String nom = saisie.getNom().trim();

        fournisseurRepository.findByNomIgnoreCase(nom)
                .filter(existant -> !existant.getId().equals(saisie.getId()))
                .ifPresent(existant -> {
                    throw new DoublonException("nom", "Un fournisseur nommé « " + nom + " » existe déjà");
                });

        Fournisseur fournisseur = saisie.getId() == null ? new Fournisseur() : trouver(saisie.getId());
        fournisseur.setNom(nom);
        fournisseur.setPays(saisie.getPays().trim());
        fournisseur.setDevise(saisie.getDevise());
        fournisseur.setContact(vide(saisie.getContact()));
        fournisseur.setTelephone(vide(saisie.getTelephone()));
        fournisseur.setEmail(vide(saisie.getEmail()));
        fournisseur.setAdresse(vide(saisie.getAdresse()));
        return fournisseurRepository.save(fournisseur);
    }

    @Transactional
    public Fournisseur basculerActif(Long id) {
        Fournisseur fournisseur = trouver(id);
        fournisseur.setActif(!fournisseur.isActif());
        return fournisseur;
    }

    private static String vide(String valeur) {
        return valeur == null || valeur.isBlank() ? null : valeur.trim();
    }
}

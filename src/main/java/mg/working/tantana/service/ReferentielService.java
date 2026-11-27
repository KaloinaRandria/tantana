package mg.working.tantana.service;

import lombok.RequiredArgsConstructor;
import mg.working.tantana.dto.ReferentielForm;
import mg.working.tantana.entity.Devise;
import mg.working.tantana.entity.ModePaiement;
import mg.working.tantana.entity.MotifEcart;
import mg.working.tantana.entity.ReferentielModifiable;
import mg.working.tantana.entity.TypeFrais;
import mg.working.tantana.entity.Unite;
import mg.working.tantana.exception.DoublonException;
import mg.working.tantana.exception.ElementIntrouvableException;
import mg.working.tantana.exception.RegleMetierException;
import mg.working.tantana.repository.DeviseRepository;
import mg.working.tantana.repository.ModePaiementRepository;
import mg.working.tantana.repository.MotifEcartRepository;
import mg.working.tantana.repository.ReferentielRepository;
import mg.working.tantana.repository.TypeFraisRepository;
import mg.working.tantana.repository.UniteRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

/**
 * Listes metier gerees par l'ADMIN : unites, modes de paiement, motifs d'ecart, types de frais, devises.
 * Regles communes : code unique en majuscules, fige apres creation ; libelle unique (casse ignoree) ;
 * desactivation au lieu de suppression ; il reste toujours au moins une valeur active.
 * Les autres services passent par les methodes « choisir » pour valider la valeur saisie.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReferentielService {

    private final UniteRepository uniteRepository;
    private final ModePaiementRepository modePaiementRepository;
    private final MotifEcartRepository motifEcartRepository;
    private final TypeFraisRepository typeFraisRepository;
    private final DeviseRepository deviseRepository;

    // ===================== Ecran d'administration =====================

    public List<? extends ReferentielModifiable> lister(ListeReferentiel liste) {
        return repository(liste).findAllByOrderByLibelleAsc();
    }

    /** Nombre de documents qui utilisent chaque valeur (id -> nombre). */
    public Map<Short, Long> utilisations(ListeReferentiel liste) {
        List<Object[]> lignes = switch (liste) {
            case UNITES -> uniteRepository.compterUtilisations();
            case MODES_PAIEMENT -> modePaiementRepository.compterUtilisations();
            case MOTIFS_ECART -> motifEcartRepository.compterUtilisations();
            case TYPES_FRAIS -> typeFraisRepository.compterUtilisations();
            case DEVISES -> deviseRepository.compterUtilisations();
        };
        Map<Short, Long> resultat = new HashMap<>();
        for (Object[] l : lignes) {
            resultat.put((Short) l[0], ((Number) l[1]).longValue());
        }
        return resultat;
    }

    public ReferentielModifiable trouver(ListeReferentiel liste, Short id) {
        return repository(liste).findById(id)
                .orElseThrow(() -> new ElementIntrouvableException(liste.getElement(), id.longValue()));
    }

    public ReferentielForm versFormulaire(ListeReferentiel liste, Short id) {
        ReferentielForm form = new ReferentielForm();
        if (id == null) {
            if (liste == ListeReferentiel.MODES_PAIEMENT) {
                form.setIcone("bi-wallet2");
            }
            return form;
        }
        ReferentielModifiable r = trouver(liste, id);
        form.setId(r.getId());
        form.setCode(r.getCode());
        form.setLibelle(r.getLibelle());
        if (r instanceof ModePaiement m) {
            form.setIcone(m.getIcone());
            form.setReferenceObligatoire(m.isReferenceObligatoire());
        } else if (r instanceof MotifEcart m) {
            form.setCommentaireObligatoire(m.isCommentaireObligatoire());
        } else if (r instanceof Devise d) {
            form.setMonnaieLocale(d.isMonnaieLocale());
        }
        return form;
    }

    /** Cree (id null) ou modifie. Le code n'est jamais modifie apres creation. */
    @Transactional
    public ReferentielModifiable enregistrer(ListeReferentiel liste, ReferentielForm saisie) {
        ReferentielRepository<ReferentielModifiable> repo = repository(liste);
        String libelle = saisie.getLibelle().trim();
        repo.findByLibelleIgnoreCase(libelle)
                .filter(existant -> !existant.getId().equals(saisie.getId()))
                .ifPresent(existant -> {
                    throw new DoublonException("libelle", "Le libellé « " + libelle + " » est déjà utilisé (code "
                            + existant.getCode() + ")");
                });

        ReferentielModifiable r;
        if (saisie.getId() == null) {
            String code = saisie.getCode().trim().toUpperCase(Locale.ROOT);
            if (liste == ListeReferentiel.DEVISES && !code.matches("^[A-Z]{3}$")) {
                throw new DoublonException("code", "Code ISO à 3 lettres attendu (ex. GBP)");
            }
            repo.findByCodeIgnoreCase(code).ifPresent(existant -> {
                throw new DoublonException("code", "Le code " + code + " existe déjà (" + existant.getLibelle() + ")");
            });
            r = nouveau(liste);
            r.setCode(code);
        } else {
            r = trouver(liste, saisie.getId());
            r.setDateModification(LocalDateTime.now());
        }
        r.setLibelle(libelle);
        if (r instanceof ModePaiement m) {
            m.setIcone(saisie.getIcone() == null || saisie.getIcone().isBlank() ? "bi-wallet2" : saisie.getIcone().trim());
            m.setReferenceObligatoire(saisie.isReferenceObligatoire());
        } else if (r instanceof MotifEcart m) {
            m.setCommentaireObligatoire(saisie.isCommentaireObligatoire());
        }
        // Devise : la monnaie locale (valorisation du stock) n'est pas modifiable depuis l'ecran
        return repo.save(r);
    }

    @Transactional
    public ReferentielModifiable basculerActif(ListeReferentiel liste, Short id) {
        ReferentielRepository<ReferentielModifiable> repo = repository(liste);
        ReferentielModifiable r = trouver(liste, id);
        if (r.isActif()) {
            if (r instanceof Devise d && d.isMonnaieLocale()) {
                throw new RegleMetierException("La monnaie locale (" + d.getCode() + ") sert à valoriser le stock : "
                        + "elle ne peut pas être désactivée.");
            }
            if (repo.countByActifTrue() <= 1) {
                throw new RegleMetierException("Impossible de désactiver la dernière valeur active de la liste « "
                        + liste.getTitre() + " ».");
            }
        }
        r.setActif(!r.isActif());
        r.setDateModification(LocalDateTime.now());
        return r;
    }

    // ===================== Listes pour les formulaires =====================

    /** Unites actives + l'unite actuelle de l'article si elle a ete desactivee. */
    public List<Unite> unites(Unite actuelle) {
        return pourSaisie(uniteRepository.findByActifTrueOrderByLibelleAsc(), actuelle);
    }

    public List<ModePaiement> modesPaiement() {
        return modePaiementRepository.findByActifTrueOrderByLibelleAsc();
    }

    public List<MotifEcart> motifsEcart() {
        return motifEcartRepository.findByActifTrueOrderByLibelleAsc();
    }

    /** Types de frais actifs + ceux deja utilises par le document (meme desactives). */
    public List<TypeFrais> typesFrais(List<TypeFrais> dejaUtilises) {
        List<TypeFrais> liste = new ArrayList<>(typeFraisRepository.findByActifTrueOrderByLibelleAsc());
        dejaUtilises.stream().filter(t -> t != null && !t.isActif() && !liste.contains(t)).forEach(liste::add);
        return liste;
    }

    /** Devises actives + la devise actuelle du document si elle a ete desactivee. */
    public List<Devise> devises(Devise actuelle) {
        return pourSaisie(deviseRepository.findByActifTrueOrderByLibelleAsc(), actuelle);
    }

    public Devise monnaieLocale() {
        return deviseRepository.findFirstByMonnaieLocaleTrue()
                .orElseThrow(() -> new IllegalStateException("Aucune monnaie locale définie (table devise)"));
    }

    // ===================== Validation d'une valeur saisie =====================

    public Unite choisirUnite(Short id, Unite actuelle) {
        return choisir(uniteRepository::findById, id, actuelle, "unite.id", "L'unité");
    }

    public ModePaiement choisirModePaiement(Short id) {
        return choisir(modePaiementRepository::findById, id, null, "mode", "Le mode de paiement");
    }

    public MotifEcart choisirMotifEcart(Short id, MotifEcart actuel, String champ) {
        return choisir(motifEcartRepository::findById, id, actuel, champ, "Le motif");
    }

    public TypeFrais choisirTypeFrais(Short id, String champ) {
        return choisir(typeFraisRepository::findById, id, null, champ, "Le type de frais");
    }

    public Devise choisirDevise(Short id, Devise actuelle, String champ) {
        return choisir(deviseRepository::findById, id, actuelle, champ, "La devise");
    }

    /**
     * Valeur choisie dans un formulaire : obligatoire, existante et active,
     * sauf si le document l'avait deja (on ne force pas a changer une valeur desactivee entre-temps).
     */
    private <T extends ReferentielModifiable> T choisir(Function<Short, Optional<T>> recherche, Short id,
                                                        T actuelle, String champ, String nom) {
        if (id == null) {
            throw new RegleMetierException(champ, nom + " est obligatoire");
        }
        T valeur = recherche.apply(id).orElseThrow(() -> new RegleMetierException(champ, nom + " est introuvable"));
        boolean inchangee = actuelle != null && valeur.getId().equals(actuelle.getId());
        if (!valeur.isActif() && !inchangee) {
            throw new RegleMetierException(champ, "« " + valeur.getLibelle() + " » est désactivé(e)");
        }
        return valeur;
    }

    private static <T extends ReferentielModifiable> List<T> pourSaisie(List<T> actives, T actuelle) {
        List<T> liste = new ArrayList<>(actives);
        if (actuelle != null && actuelle.getId() != null && !actuelle.isActif()) {
            liste.add(actuelle);
        }
        return liste;
    }

    private ReferentielModifiable nouveau(ListeReferentiel liste) {
        return switch (liste) {
            case UNITES -> new Unite();
            case MODES_PAIEMENT -> new ModePaiement();
            case MOTIFS_ECART -> new MotifEcart();
            case TYPES_FRAIS -> new TypeFrais();
            case DEVISES -> new Devise();
        };
    }

    @SuppressWarnings("unchecked")
    private ReferentielRepository<ReferentielModifiable> repository(ListeReferentiel liste) {
        ReferentielRepository<?> repo = switch (liste) {
            case UNITES -> uniteRepository;
            case MODES_PAIEMENT -> modePaiementRepository;
            case MOTIFS_ECART -> motifEcartRepository;
            case TYPES_FRAIS -> typeFraisRepository;
            case DEVISES -> deviseRepository;
        };
        return (ReferentielRepository<ReferentielModifiable>) repo;
    }
}

package mg.working.tantana.service;

import lombok.RequiredArgsConstructor;
import mg.working.tantana.dto.FraisReceptionForm;
import mg.working.tantana.dto.LigneReceptionForm;
import mg.working.tantana.dto.ReceptionForm;
import mg.working.tantana.entity.CommandeAchat;
import mg.working.tantana.entity.FraisReception;
import mg.working.tantana.entity.LigneCommandeAchat;
import mg.working.tantana.entity.LigneReception;
import mg.working.tantana.entity.Reception;
import mg.working.tantana.entity.StatutCommande;
import mg.working.tantana.entity.StatutReception;
import mg.working.tantana.entity.Transitaire;
import mg.working.tantana.entity.Utilisateur;
import mg.working.tantana.exception.ElementIntrouvableException;
import mg.working.tantana.exception.RegleMetierException;
import mg.working.tantana.repository.CommandeAchatRepository;
import mg.working.tantana.repository.ReceptionRepository;
import mg.working.tantana.repository.TransitaireRepository;
import mg.working.tantana.repository.UtilisateurRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Receptions des commandes d'achat, avec double controle :
 * le MAKER saisit, un CHECKER (autre personne) valide ou rejette.
 * Le stock n'est mis a jour qu'a la validation.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReceptionService {

    private static final List<StatutReception> EN_ATTENTE = List.of(StatutReception.SAISIE, StatutReception.REJETEE);

    private final ReceptionRepository receptionRepository;
    private final CommandeAchatRepository commandeRepository;
    private final TransitaireRepository transitaireRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final NumerotationService numerotationService;
    private final StockService stockService;

    // ===================== Lecture =====================

    public Page<Reception> rechercher(String texte, StatutReception statut, Pageable pageable) {
        String t = texte == null ? "" : texte.trim();
        return statut == null ? receptionRepository.rechercher(t, pageable)
                : receptionRepository.rechercher(t, statut, pageable);
    }

    public Reception trouverAvecDetails(Long id) {
        return receptionRepository.findAvecDetails(id)
                .orElseThrow(() -> new ElementIntrouvableException("Réception", id));
    }

    public List<Reception> listerPourCommande(Long commandeId) {
        return receptionRepository.findByCommandeIdOrderByIdDesc(commandeId);
    }

    public long nombreAValider() {
        return receptionRepository.countByStatut(StatutReception.SAISIE);
    }

    public boolean aUneReceptionEnAttente(Long commandeId) {
        return receptionRepository.existsByCommandeIdAndStatutIn(commandeId, EN_ATTENTE);
    }

    // ===================== Formulaires =====================

    /** Nouvelle reception : chaque ligne restante est pre-remplie avec la quantite attendue. */
    public ReceptionForm preparerNouvelle(Long commandeId) {
        CommandeAchat commande = commandeAvecDetails(commandeId);
        verifierCommandeReceptionnable(commande);

        ReceptionForm form = new ReceptionForm();
        form.setCommandeId(commande.getId());
        form.setTauxChange(commande.getTauxChange());
        for (LigneCommandeAchat lc : commande.getLignes()) {
            if (lc.getQuantiteRestante().signum() > 0) {
                LigneReceptionForm l = new LigneReceptionForm();
                l.setLigneCommandeId(lc.getId());
                l.setQuantiteRecue(lc.getQuantiteRestante());
                form.getLignes().add(l);
            }
        }
        completerAffichage(form, commande);
        return form;
    }

    /** Charge la reception et sa commande dans la meme transaction (lignes de commande comprises). */
    public ReceptionForm versFormulaire(Long receptionId) {
        Reception reception = trouverAvecDetails(receptionId);
        ReceptionForm form = new ReceptionForm();
        form.setId(reception.getId());
        form.setVersion(reception.getVersion());
        form.setCommandeId(reception.getCommande().getId());
        form.setTransitaireId(reception.getTransitaire().getId());
        form.setDateReception(reception.getDateReception());
        form.setTauxChange(reception.getTauxChange());
        form.setObservations(reception.getObservations());

        Map<Long, LigneReception> saisies = reception.getLignes().stream()
                .collect(Collectors.toMap(l -> l.getLigneCommande().getId(), Function.identity()));
        for (LigneCommandeAchat lc : reception.getCommande().getLignes()) {
            LigneReception existante = saisies.get(lc.getId());
            if (existante == null && lc.getQuantiteRestante().signum() == 0) {
                continue;
            }
            LigneReceptionForm l = new LigneReceptionForm();
            l.setLigneCommandeId(lc.getId());
            if (existante != null) {
                l.setQuantiteRecue(existante.getQuantiteRecue());
                l.setQuantiteRefusee(existante.getQuantiteRefusee());
                l.setCommentaire(existante.getCommentaire());
            }
            form.getLignes().add(l);
        }
        reception.getFrais().forEach(f -> form.getFrais().add(new FraisReceptionForm(
                f.getType(), f.getMontant(), f.getReferenceFacture(), f.getCommentaire())));
        completerAffichage(form, reception.getCommande());
        return form;
    }

    /** Remplit les colonnes d'affichage (article, commande, reste) a partir de la base. */
    public void completerAffichage(ReceptionForm form, CommandeAchat commande) {
        form.setNumeroCommande(commande.getNumero());
        form.setFournisseur(commande.getFournisseur().getNom());
        form.setDevise(commande.getDevise());
        Map<Long, LigneCommandeAchat> lignes = commande.getLignes().stream()
                .collect(Collectors.toMap(LigneCommandeAchat::getId, Function.identity()));
        for (LigneReceptionForm l : form.getLignes()) {
            LigneCommandeAchat lc = lignes.get(l.getLigneCommandeId());
            if (lc != null) {
                l.setReference(lc.getArticle().getReference());
                l.setDesignation(lc.getArticle().getDesignation());
                l.setUnite(lc.getArticle().getUnite().getLibelle());
                l.setQuantiteCommandee(lc.getQuantiteCommandee());
                l.setQuantiteRestante(lc.getQuantiteRestante());
                l.setPrixUnitaire(lc.getPrixUnitaire());
            }
        }
    }

    public CommandeAchat commandeAvecDetails(Long commandeId) {
        return commandeRepository.findAvecDetails(commandeId)
                .orElseThrow(() -> new ElementIntrouvableException("Commande", commandeId));
    }

    // ===================== Saisie (MAKER) =====================

    @Transactional
    public Reception enregistrer(ReceptionForm form, String username) {
        Utilisateur utilisateur = utilisateur(username);
        Reception reception;
        CommandeAchat commande;

        if (form.getId() == null) {
            commande = commandeAvecDetails(form.getCommandeId());
            verifierCommandeReceptionnable(commande);
            reception = new Reception();
            reception.setCommande(commande);
            reception.setSaisiPar(utilisateur);
            reception.setNumero(numerotationService.prochainNumero(NumerotationService.RECEPTION, form.getDateReception()));
        } else {
            reception = trouverAvecDetails(form.getId());
            commande = reception.getCommande();
            if (!reception.isModifiable()) {
                throw new RegleMetierException("Une réception validée ne peut plus être modifiée.");
            }
            if (!reception.getSaisiPar().getId().equals(utilisateur.getId())) {
                throw new RegleMetierException("Seul l'auteur de la saisie ("
                        + reception.getSaisiPar().getNomComplet() + ") peut la modifier.");
            }
            if (form.getVersion() != null && !form.getVersion().equals(reception.getVersion())) {
                throw new RegleMetierException("La réception a été modifiée entre-temps. Rechargez la page.");
            }
            // Une reception rejetee puis corrigee repart en validation
            reception.setStatut(StatutReception.SAISIE);
            reception.setMotifRejet(null);
            reception.setDateSaisie(LocalDateTime.now());
        }

        if (form.getDateReception().isAfter(LocalDate.now())) {
            throw new RegleMetierException("dateReception", "La date de réception ne peut pas être dans le futur");
        }
        if (form.getDateReception().isBefore(commande.getDateCommande())) {
            throw new RegleMetierException("dateReception", "La réception ne peut pas précéder la commande");
        }
        Transitaire transitaire = transitaireRepository.findById(form.getTransitaireId())
                .orElseThrow(() -> new RegleMetierException("transitaireId", "Transitaire introuvable"));
        boolean memeTransitaire = reception.getTransitaire() != null
                && reception.getTransitaire().getId().equals(transitaire.getId());
        if (!transitaire.isActif() && !memeTransitaire) {
            throw new RegleMetierException("transitaireId", "Ce transitaire est désactivé");
        }

        reception.setTransitaire(transitaire);
        reception.setDateReception(form.getDateReception());
        reception.setTauxChange(form.getTauxChange());
        reception.setObservations(texte(form.getObservations()));

        appliquerLignes(form, commande, reception);
        appliquerFrais(form, reception);
        return receptionRepository.save(reception);
    }

    @Transactional
    public void supprimer(Long id, String username) {
        Reception reception = trouverAvecDetails(id);
        if (!reception.isModifiable()) {
            throw new RegleMetierException("Une réception validée ne peut pas être supprimée : le stock a déjà été mis à jour.");
        }
        if (!reception.getSaisiPar().getUsername().equalsIgnoreCase(username)) {
            throw new RegleMetierException("Seul l'auteur de la saisie peut la supprimer.");
        }
        receptionRepository.delete(reception);
    }

    // ===================== Controle (CHECKER) =====================

    /**
     * Valide la reception : met a jour les quantites recues de la commande, son statut,
     * et fait entrer les marchandises en stock au cout (prix fournisseur x taux du jour).
     * Tout se fait dans une seule transaction.
     */
    @Transactional
    public Reception valider(Long id, String username) {
        // 1. Verrou sur la commande AVANT de lire les quantites : deux validations simultanees
        //    s'executent l'une apres l'autre et ne peuvent pas depasser la quantite commandee.
        Long commandeId = receptionRepository.findById(id)
                .orElseThrow(() -> new ElementIntrouvableException("Réception", id))
                .getCommande().getId();
        commandeRepository.findPourMiseAJour(commandeId);

        // 2. Lecture complete, controle des 4 yeux
        Reception reception = trouverAvecDetails(id);
        Utilisateur validateur = verifierControle(reception, username);
        CommandeAchat commande = reception.getCommande();
        if (!commande.getStatut().estEnAttente()) {
            throw new RegleMetierException("La commande " + commande.getNumero() + " est "
                    + commande.getStatut().getLibelle().toLowerCase() + " : la réception ne peut plus être validée.");
        }

        // Frais du transitaire repartis au prorata de la valeur recue, figes sur chaque ligne
        Map<LigneReception, BigDecimal> frais = RepartitionFrais.fraisUnitaires(reception);

        for (LigneReception ligne : reception.getLignes()) {
            BigDecimal quantite = ligne.getQuantiteRecue();
            if (quantite.signum() == 0) {
                continue;
            }
            ligne.setFraisUnitaire(frais.getOrDefault(ligne, BigDecimal.ZERO));
            LigneCommandeAchat lc = ligne.getLigneCommande();
            if (quantite.compareTo(lc.getQuantiteRestante()) > 0) {
                throw new RegleMetierException("Article " + lc.getArticle().getReference() + " : " + quantite.stripTrailingZeros().toPlainString()
                        + " reçu(s) mais il n'en reste que " + lc.getQuantiteRestante().stripTrailingZeros().toPlainString()
                        + " à recevoir. Rejetez la réception pour correction.");
            }
            lc.setQuantiteRecue(lc.getQuantiteRecue().add(quantite));
            stockService.entrerAchat(lc.getArticle(), quantite, ligne.getCoutUnitaireMga(),
                    ligne.getCoutRevientUnitaireMga(), reception, validateur);
        }

        boolean toutRecu = commande.getLignes().stream().allMatch(l -> l.getQuantiteRestante().signum() == 0);
        commande.setStatut(toutRecu ? StatutCommande.RECUE : StatutCommande.PARTIELLEMENT_RECUE);

        reception.setStatut(StatutReception.VALIDEE);
        reception.setValidePar(validateur);
        reception.setDateValidation(LocalDateTime.now());
        return reception;
    }

    @Transactional
    public Reception rejeter(Long id, String motif, String username) {
        Reception reception = trouverAvecDetails(id);
        Utilisateur validateur = verifierControle(reception, username);
        if (motif == null || motif.isBlank()) {
            throw new RegleMetierException("Indiquez le motif du rejet pour que l'auteur puisse corriger.");
        }
        reception.setStatut(StatutReception.REJETEE);
        reception.setMotifRejet(motif.trim());
        reception.setValidePar(validateur);
        reception.setDateValidation(LocalDateTime.now());
        return reception;
    }

    // ===================== Regles =====================

    private void verifierCommandeReceptionnable(CommandeAchat commande) {
        if (!commande.getStatut().estEnAttente()) {
            throw new RegleMetierException("La commande " + commande.getNumero() + " est "
                    + commande.getStatut().getLibelle().toLowerCase() + " : aucune réception possible.");
        }
        if (aUneReceptionEnAttente(commande.getId())) {
            throw new RegleMetierException("Une réception de cette commande attend déjà une validation ou une correction.");
        }
    }

    /** Principe des 4 yeux : le controleur doit etre different de l'auteur de la saisie. */
    private Utilisateur verifierControle(Reception reception, String username) {
        if (reception.getStatut() != StatutReception.SAISIE) {
            throw new RegleMetierException("Cette réception est " + reception.getStatut().getLibelle().toLowerCase()
                    + " : elle ne peut pas être contrôlée.");
        }
        Utilisateur validateur = utilisateur(username);
        if (reception.getSaisiPar().getId().equals(validateur.getId())) {
            throw new RegleMetierException("Vous ne pouvez pas valider ou rejeter votre propre saisie : "
                    + "elle doit être contrôlée par une autre personne.");
        }
        return validateur;
    }

    private void appliquerLignes(ReceptionForm form, CommandeAchat commande, Reception reception) {
        Map<Long, LigneCommandeAchat> lignesCommande = commande.getLignes().stream()
                .collect(Collectors.toMap(LigneCommandeAchat::getId, Function.identity()));
        Map<Long, LigneReception> existantes = reception.getLignes().stream()
                .collect(Collectors.toMap(l -> l.getLigneCommande().getId(), Function.identity()));

        BigDecimal total = BigDecimal.ZERO;
        for (int i = 0; i < form.getLignes().size(); i++) {
            LigneReceptionForm saisie = form.getLignes().get(i);
            LigneCommandeAchat lc = lignesCommande.get(saisie.getLigneCommandeId());
            if (lc == null) {
                throw new RegleMetierException("Ligne de commande inconnue");
            }
            if (saisie.getQuantiteRecue().compareTo(lc.getQuantiteRestante()) > 0) {
                throw new RegleMetierException("lignes[" + i + "].quantiteRecue",
                        "Maximum " + lc.getQuantiteRestante().stripTrailingZeros().toPlainString() + " (reste à recevoir)");
            }
            total = total.add(saisie.getQuantiteRecue()).add(saisie.getQuantiteRefusee());

            LigneReception ligne = existantes.remove(lc.getId());
            boolean vide = saisie.getQuantiteRecue().signum() == 0 && saisie.getQuantiteRefusee().signum() == 0;
            if (vide) {
                if (ligne != null) {
                    reception.getLignes().remove(ligne);
                }
                continue;
            }
            if (ligne == null) {
                ligne = new LigneReception();
                ligne.setLigneCommande(lc);
                reception.ajouterLigne(ligne);
            }
            ligne.setQuantiteRecue(saisie.getQuantiteRecue());
            ligne.setQuantiteRefusee(saisie.getQuantiteRefusee());
            ligne.setCommentaire(texte(saisie.getCommentaire()));
        }
        // lignes plus presentes dans le formulaire
        reception.getLignes().removeAll(existantes.values());

        if (total.signum() == 0) {
            throw new RegleMetierException("Saisissez au moins une quantité reçue ou refusée.");
        }
    }

    private void appliquerFrais(ReceptionForm form, Reception reception) {
        reception.getFrais().clear();
        for (FraisReceptionForm f : form.getFrais()) {
            FraisReception frais = new FraisReception();
            frais.setType(f.getType());
            frais.setMontant(f.getMontant());
            frais.setReferenceFacture(texte(f.getReferenceFacture()));
            frais.setCommentaire(texte(f.getCommentaire()));
            reception.ajouterFrais(frais);
        }
    }

    private Utilisateur utilisateur(String username) {
        return utilisateurRepository.findByUsernameIgnoreCase(username)
                .orElseThrow(() -> new RegleMetierException("Utilisateur connecté introuvable"));
    }

    private static String texte(String valeur) {
        return valeur == null || valeur.isBlank() ? null : valeur.trim();
    }
}

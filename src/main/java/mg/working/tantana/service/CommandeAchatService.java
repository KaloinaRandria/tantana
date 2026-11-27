package mg.working.tantana.service;

import lombok.RequiredArgsConstructor;
import mg.working.tantana.dto.CommandeAchatForm;
import mg.working.tantana.dto.FiltrePeriode;
import mg.working.tantana.dto.LigneCommandeForm;
import mg.working.tantana.entity.Article;
import mg.working.tantana.entity.CommandeAchat;
import mg.working.tantana.entity.Devise;
import mg.working.tantana.entity.Fournisseur;
import mg.working.tantana.entity.LigneCommandeAchat;
import mg.working.tantana.entity.StatutCommande;
import mg.working.tantana.entity.StatutReception;
import mg.working.tantana.repository.ReceptionRepository;
import mg.working.tantana.entity.Utilisateur;
import mg.working.tantana.exception.ElementIntrouvableException;
import mg.working.tantana.exception.RegleMetierException;
import mg.working.tantana.repository.ArticleRepository;
import mg.working.tantana.repository.CommandeAchatRepository;
import mg.working.tantana.repository.CommandeAchatSpecifications;
import mg.working.tantana.repository.FournisseurRepository;
import mg.working.tantana.repository.UtilisateurRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import static mg.working.tantana.repository.CommandeAchatSpecifications.commandeeEntre;
import static mg.working.tantana.repository.CommandeAchatSpecifications.enRetard;
import static mg.working.tantana.repository.CommandeAchatSpecifications.fournisseur;
import static mg.working.tantana.repository.CommandeAchatSpecifications.numeroContient;
import static mg.working.tantana.repository.CommandeAchatSpecifications.statut;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CommandeAchatService {

    private final CommandeAchatRepository commandeRepository;
    private final FournisseurRepository fournisseurRepository;
    private final ArticleRepository articleRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final NumerotationService numerotationService;
    private final ReceptionRepository receptionRepository;
    private final ReferentielService referentielService;

    // ===================== Lecture =====================

    public Page<CommandeAchat> rechercher(String numero, StatutCommande statutFiltre, Long fournisseurId,
                                          boolean seulementEnRetard, FiltrePeriode periode, Pageable pageable) {
        return commandeRepository.findAll(CommandeAchatSpecifications.tous(
                numeroContient(numero), statut(statutFiltre), fournisseur(fournisseurId),
                enRetard(seulementEnRetard), commandeeEntre(periode.du(), periode.au())), pageable);
    }

    public CommandeAchat trouverAvecDetails(Long id) {
        return commandeRepository.findAvecDetails(id)
                .orElseThrow(() -> new ElementIntrouvableException("Commande", id));
    }

    /** Pre-remplit le formulaire de modification a partir de la commande. */
    public CommandeAchatForm versFormulaire(CommandeAchat commande) {
        CommandeAchatForm form = new CommandeAchatForm();
        form.setId(commande.getId());
        form.setVersion(commande.getVersion());
        form.setFournisseurId(commande.getFournisseur().getId());
        form.setDateCommande(commande.getDateCommande());
        form.setDateArriveePrevue(commande.getDateArriveePrevue());
        form.setDeviseId(commande.getDevise().getId());
        form.setTauxChange(Nombres.sansZeros(commande.getTauxChange()));
        form.setObservations(commande.getObservations());
        form.setLignes(commande.getLignes().stream()
                .map(l -> new LigneCommandeForm(l.getArticle().getId(),
                        Nombres.sansZeros(l.getQuantiteCommandee()), Nombres.sansZeros(l.getPrixUnitaire())))
                .collect(Collectors.toList()));
        return form;
    }

    // ===================== Ecriture =====================

    /** Cree une commande en BROUILLON avec un nouveau numero. */
    @Transactional
    public CommandeAchat creer(CommandeAchatForm form, String username) {
        Utilisateur createur = utilisateurRepository.findByUsernameIgnoreCase(username)
                .orElseThrow(() -> new RegleMetierException("Utilisateur connecté introuvable"));

        CommandeAchat commande = new CommandeAchat();
        commande.setCreePar(createur);
        appliquer(form, commande);
        commande.setNumero(numerotationService.prochainNumero(NumerotationService.COMMANDE_ACHAT, form.getDateCommande()));
        return commandeRepository.save(commande);
    }

    @Transactional
    public CommandeAchat modifier(Long id, CommandeAchatForm form) {
        CommandeAchat commande = trouverAvecDetails(id);
        if (!estModifiable(commande)) {
            throw new RegleMetierException("La commande " + commande.getNumero() + " ("
                    + commande.getStatut().getLibelle() + ") n'est plus modifiable"
                    + (receptionRepository.existsByCommandeId(id) ? " : une réception a déjà été saisie." : "."));
        }
        if (form.getVersion() != null && !form.getVersion().equals(commande.getVersion())) {
            throw new RegleMetierException("La commande a été modifiée par un autre utilisateur pendant votre saisie. "
                    + "Rechargez la page pour voir la dernière version.");
        }
        appliquer(form, commande);
        return commande;
    }

    /** BROUILLON -> COMMANDEE : la commande est envoyee au fournisseur. */
    @Transactional
    public CommandeAchat passer(Long id) {
        CommandeAchat commande = trouverAvecDetails(id);
        if (commande.getStatut() != StatutCommande.BROUILLON) {
            throw new RegleMetierException("Seule une commande en brouillon peut être passée.");
        }
        if (commande.getLignes().isEmpty()) {
            throw new RegleMetierException("La commande ne contient aucun article.");
        }
        commande.setStatut(StatutCommande.COMMANDEE);
        return commande;
    }

    @Transactional
    public CommandeAchat annuler(Long id) {
        CommandeAchat commande = trouverAvecDetails(id);
        if (!estModifiable(commande)) {
            throw new RegleMetierException("La commande " + commande.getNumero() + " ne peut plus être annulée.");
        }
        commande.setStatut(StatutCommande.ANNULEE);
        return commande;
    }

    /**
     * Partiellement recue -> CLOTUREE : le reste ne sera jamais livre.
     * Les quantites recues et le stock ne changent pas.
     */
    @Transactional
    public CommandeAchat cloturer(Long id) {
        CommandeAchat commande = trouverAvecDetails(id);
        if (commande.getStatut() != StatutCommande.PARTIELLEMENT_RECUE) {
            throw new RegleMetierException("Seule une commande partiellement reçue peut être clôturée.");
        }
        if (receptionRepository.existsByCommandeIdAndStatutIn(id, List.of(StatutReception.SAISIE, StatutReception.REJETEE))) {
            throw new RegleMetierException("Une réception est encore en attente : validez-la ou supprimez-la avant de clôturer.");
        }
        commande.setStatut(StatutCommande.CLOTUREE);
        return commande;
    }

    /** Modifiable / annulable : brouillon ou commandee, et aucune reception saisie (meme non validee). */
    public boolean estModifiable(CommandeAchat commande) {
        return commande.isModifiable() && !receptionRepository.existsByCommandeId(commande.getId());
    }

    // ===================== Regles de saisie =====================

    private void appliquer(CommandeAchatForm form, CommandeAchat commande) {
        if (form.getDateArriveePrevue().isBefore(form.getDateCommande())) {
            throw new RegleMetierException("dateArriveePrevue", "L'arrivée prévue ne peut pas précéder la date de commande");
        }

        Fournisseur fournisseur = fournisseurRepository.findById(form.getFournisseurId())
                .orElseThrow(() -> new RegleMetierException("fournisseurId", "Fournisseur introuvable"));
        boolean memeFournisseur = commande.getFournisseur() != null
                && commande.getFournisseur().getId().equals(fournisseur.getId());
        if (!fournisseur.isActif() && !memeFournisseur) {
            throw new RegleMetierException("fournisseurId", "Ce fournisseur est désactivé");
        }

        commande.setFournisseur(fournisseur);
        commande.setDateCommande(form.getDateCommande());
        commande.setDateArriveePrevue(form.getDateArriveePrevue());
        Devise devise = referentielService.choisirDevise(form.getDeviseId(), commande.getDevise(), "deviseId");
        commande.setDevise(devise);
        // Dans la monnaie locale (Ariary), le taux est forcement 1
        commande.setTauxChange(devise.isMonnaieLocale() ? BigDecimal.ONE : form.getTauxChange());
        commande.setObservations(form.getObservations() == null || form.getObservations().isBlank()
                ? null : form.getObservations().trim());

        synchroniserLignes(form.getLignes(), commande);
        commande.recalculerMontantTotal();
    }

    /**
     * Met a jour les lignes existantes (meme article), ajoute les nouvelles et supprime les absentes.
     * On ne vide pas la liste : la contrainte unique (commande, article) serait violee
     * car Hibernate insere avant de supprimer.
     */
    private void synchroniserLignes(List<LigneCommandeForm> saisies, CommandeAchat commande) {
        Set<Long> dejaVus = new HashSet<>();
        for (int i = 0; i < saisies.size(); i++) {
            if (!dejaVus.add(saisies.get(i).getArticleId())) {
                throw new RegleMetierException("lignes[" + i + "].articleId",
                        "Article déjà présent sur une autre ligne : modifiez plutôt sa quantité");
            }
        }

        Map<Long, LigneCommandeAchat> existantes = commande.getLignes().stream()
                .collect(Collectors.toMap(l -> l.getArticle().getId(), Function.identity()));
        Map<Long, Article> articles = articleRepository.findAllById(dejaVus).stream()
                .collect(Collectors.toMap(Article::getId, Function.identity()));

        for (int i = 0; i < saisies.size(); i++) {
            LigneCommandeForm saisie = saisies.get(i);
            Article article = articles.get(saisie.getArticleId());
            if (article == null) {
                throw new RegleMetierException("lignes[" + i + "].articleId", "Article introuvable");
            }
            LigneCommandeAchat ligne = existantes.get(article.getId());
            if (ligne == null) {
                if (!article.isActif()) {
                    throw new RegleMetierException("lignes[" + i + "].articleId",
                            "L'article " + article.getReference() + " est désactivé");
                }
                ligne = new LigneCommandeAchat();
                ligne.setArticle(article);
                commande.ajouterLigne(ligne);
            }
            ligne.setQuantiteCommandee(saisie.getQuantite());
            ligne.setPrixUnitaire(saisie.getPrixUnitaire());
        }

        commande.getLignes().removeIf(l -> !dejaVus.contains(l.getArticle().getId()));
    }
}

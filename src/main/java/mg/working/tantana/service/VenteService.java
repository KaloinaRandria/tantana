package mg.working.tantana.service;

import lombok.RequiredArgsConstructor;
import mg.working.tantana.dto.FiltrePeriode;
import mg.working.tantana.dto.LigneVenteForm;
import mg.working.tantana.dto.VenteForm;
import mg.working.tantana.entity.Article;
import mg.working.tantana.entity.Client;
import mg.working.tantana.entity.LigneVente;
import mg.working.tantana.entity.Parametre;
import mg.working.tantana.entity.StatutVente;
import mg.working.tantana.entity.Stock;
import mg.working.tantana.entity.Utilisateur;
import mg.working.tantana.entity.Vente;
import mg.working.tantana.exception.ElementIntrouvableException;
import mg.working.tantana.exception.RegleMetierException;
import mg.working.tantana.repository.ArticleRepository;
import mg.working.tantana.repository.ClientRepository;
import mg.working.tantana.repository.UtilisateurRepository;
import mg.working.tantana.repository.VenteRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Ventes : saisie par le vendeur, controle par un CHECKER si la remise depasse le plafond
 * ou si le prix est sous le revient, puis validation = facture numerotee + sortie de stock.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class VenteService {

    private final VenteRepository venteRepository;
    private final ClientRepository clientRepository;
    private final ArticleRepository articleRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final ParametreService parametreService;
    private final StockService stockService;
    private final NumerotationService numerotationService;

    // ===================== Lecture =====================

    public Page<Vente> rechercher(String texte, StatutVente statut, FiltrePeriode periode, Pageable pageable) {
        String t = texte == null ? "" : texte.trim();
        LocalDate du = periode.debutRequete();
        LocalDate au = periode.finRequete();
        return statut == null ? venteRepository.rechercher(t, du, au, pageable)
                : venteRepository.rechercher(t, statut, du, au, pageable);
    }

    /** Factures validees avec un reste a payer. */
    public Page<Vente> rechercherImpayees(String texte, FiltrePeriode periode, Pageable pageable) {
        return venteRepository.rechercherImpayees(texte == null ? "" : texte.trim(),
                periode.debutRequete(), periode.finRequete(), pageable);
    }

    public BigDecimal encoursClient(Long clientId) {
        return venteRepository.encoursClient(clientId);
    }

    public Vente trouverAvecDetails(Long id) {
        return venteRepository.findAvecDetails(id).orElseThrow(() -> new ElementIntrouvableException("Vente", id));
    }

    public long nombreAValider() {
        return venteRepository.countByStatut(StatutVente.A_VALIDER);
    }

    public VenteForm versFormulaire(Vente vente) {
        VenteForm form = new VenteForm();
        form.setId(vente.getId());
        form.setVersion(vente.getVersion());
        form.setClientId(vente.getClient().getId());
        form.setObservations(vente.getObservations());
        vente.getLignes().forEach(l -> form.getLignes().add(
                new LigneVenteForm(l.getArticle().getId(),
                        Nombres.sansZeros(l.getQuantite()), Nombres.sansZeros(l.getRemisePct()))));
        return form;
    }

    /** Articles vendables : actifs et avec un prix de vente fixe. */
    public List<Article> articlesVendables() {
        return articleRepository.findByActifTrueOrderByDesignationAsc().stream()
                .filter(a -> a.getPrixVenteHt() != null)
                .toList();
    }

    // ===================== Saisie (vendeur) =====================

    /** Cree ou met a jour un brouillon. Les prix sont relus dans le catalogue. */
    @Transactional
    public Vente enregistrer(VenteForm form, String username) {
        Utilisateur utilisateur = utilisateur(username);
        Vente vente;
        if (form.getId() == null) {
            vente = new Vente();
            vente.setVendeur(utilisateur);
        } else {
            vente = trouverAvecDetails(form.getId());
            verifierAuteur(vente, utilisateur, "modifier");
            if (!vente.isModifiable()) {
                throw new RegleMetierException("Seul un brouillon peut être modifié.");
            }
            if (form.getVersion() != null && !form.getVersion().equals(vente.getVersion())) {
                throw new RegleMetierException("La vente a été modifiée entre-temps. Rechargez la page.");
            }
        }

        Client client = clientRepository.findById(form.getClientId())
                .orElseThrow(() -> new RegleMetierException("clientId", "Client introuvable"));
        boolean memeClient = vente.getClient() != null && vente.getClient().getId().equals(client.getId());
        if (!client.isActif() && !memeClient) {
            throw new RegleMetierException("clientId", "Ce client est désactivé");
        }

        vente.setClient(client);
        vente.setObservations(texte(form.getObservations()));
        vente.setTauxTva(parametreService.obtenir().getTauxTva());
        vente.setDateVente(LocalDate.now());
        synchroniserLignes(form.getLignes(), vente);
        vente.recalculerTotaux();
        return venteRepository.save(vente);
    }

    @Transactional
    public void supprimer(Long id, String username) {
        Vente vente = trouverAvecDetails(id);
        verifierAuteur(vente, utilisateur(username), "supprimer");
        if (!vente.isModifiable()) {
            throw new RegleMetierException("Seul un brouillon peut être supprimé. Une facture se corrige par un avoir.");
        }
        venteRepository.delete(vente);
    }

    /**
     * Le vendeur valide sa vente : facturee immediatement si tout est dans les regles,
     * sinon envoyee en controle (A_VALIDER) avec le motif.
     */
    @Transactional
    public Vente soumettre(Long id, String username) {
        Vente vente = trouverAvecDetails(id);
        Utilisateur vendeur = utilisateur(username);
        verifierAuteur(vente, vendeur, "valider");
        if (vente.getStatut() != StatutVente.BROUILLON) {
            throw new RegleMetierException("Cette vente n'est plus un brouillon.");
        }
        if (vente.getLignes().isEmpty()) {
            throw new RegleMetierException("La vente ne contient aucun article.");
        }
        verifierDisponibilite(vente);

        List<String> motifs = motifsDeControle(vente);
        if (!motifs.isEmpty()) {
            vente.setStatut(StatutVente.A_VALIDER);
            vente.setMotifControle(String.join("\n", motifs));
            vente.setCommentaireControle(null);
            return vente;
        }
        return finaliser(vente, vendeur);
    }

    // ===================== Controle (CHECKER) =====================

    @Transactional
    public Vente approuver(Long id, String commentaire, String username) {
        Vente vente = trouverAvecDetails(id);
        Utilisateur controleur = verifierControle(vente, username);
        vente.setControleur(controleur);
        vente.setDateControle(LocalDateTime.now());
        vente.setCommentaireControle(texte(commentaire));
        return finaliser(vente, controleur);
    }

    @Transactional
    public Vente refuser(Long id, String commentaire, String username) {
        Vente vente = trouverAvecDetails(id);
        Utilisateur controleur = verifierControle(vente, username);
        if (texte(commentaire) == null) {
            throw new RegleMetierException("Indiquez pourquoi la vente est refusée, pour que le vendeur puisse la corriger.");
        }
        vente.setStatut(StatutVente.BROUILLON);
        vente.setControleur(controleur);
        vente.setDateControle(LocalDateTime.now());
        vente.setCommentaireControle(commentaire.trim());
        return vente;
    }

    // ===================== Facturation =====================

    /**
     * Validation definitive : sortie de stock (verrouillee, jamais negative), couts figes,
     * marge calculee, numero de facture attribue. Tout ou rien (une seule transaction).
     */
    private Vente finaliser(Vente vente, Utilisateur utilisateur) {
        // Numero et statut poses ensemble AVANT les requetes de stock : Hibernate peut enregistrer
        // la vente a tout moment, et la base exige « VALIDEE <=> numero renseigne ».
        // En cas d'echec plus loin (stock insuffisant), toute la transaction est annulee.
        vente.setDateVente(LocalDate.now());
        vente.setNumero(numerotationService.prochainNumero(NumerotationService.FACTURE, vente.getDateVente()));
        vente.setStatut(StatutVente.VALIDEE);
        vente.setDateValidation(LocalDateTime.now());

        // Verrous pris toujours dans le meme ordre (id d'article) : pas d'interblocage entre deux ventes
        List<LigneVente> lignes = new ArrayList<>(vente.getLignes());
        lignes.sort(Comparator.comparing(l -> l.getArticle().getId()));
        for (LigneVente ligne : lignes) {
            Stock stock = stockService.sortirVente(ligne.getArticle(), ligne.getQuantite(), vente, utilisateur);
            ligne.setCoutAchatUnitaire(stock.getCmup());
            ligne.setCoutRevientUnitaire(stock.getCmupRevient());
        }
        vente.recalculerTotaux();
        vente.figerMarge();
        return vente;
    }

    // ===================== Regles =====================

    /** Raisons qui imposent un controle : remise au-dela du plafond, prix net sous le revient. */
    public List<String> motifsDeControle(Vente vente) {
        Parametre parametre = parametreService.obtenir();
        Map<Long, BigDecimal> revients = stockService.coutsRevient(
                vente.getLignes().stream().map(l -> l.getArticle().getId()).toList());
        List<String> motifs = new ArrayList<>();
        for (LigneVente l : vente.getLignes()) {
            String ref = l.getArticle().getReference();
            if (l.getRemisePct().compareTo(parametre.getRemiseMaxVendeur()) > 0) {
                motifs.add(ref + " : remise de " + l.getRemisePct().stripTrailingZeros().toPlainString()
                        + " % (plafond vendeur " + parametre.getRemiseMaxVendeur().stripTrailingZeros().toPlainString() + " %)");
            }
            BigDecimal revient = revients.get(l.getArticle().getId());
            if (revient != null && l.getPrixUnitaireHt().compareTo(revient) < 0) {
                motifs.add(ref + " : prix net inférieur au prix de revient (vente à perte)");
            }
        }
        // Plafond de credit : seulement pour les clients autorises a payer a credit (plafond > 0).
        // Plafond 0 = paiement comptant, encaisse au moment de la vente.
        Client client = vente.getClient();
        if (!client.isComptoir() && client.isCreditAutorise()) {
            BigDecimal encours = venteRepository.encoursClient(client.getId());
            BigDecimal apres = encours.add(vente.getTotalTtc());
            if (apres.compareTo(client.getPlafondCredit()) > 0) {
                motifs.add("Plafond de crédit dépassé : encours " + Montants.ariary(encours) + " + cette vente "
                        + Montants.ariary(vente.getTotalTtc()) + " > plafond " + Montants.ariary(client.getPlafondCredit()));
            }
        }
        return motifs;
    }

    private void verifierDisponibilite(Vente vente) {
        Map<Long, BigDecimal> dispo = stockService.disponibles(
                vente.getLignes().stream().map(l -> l.getArticle().getId()).toList());
        for (LigneVente l : vente.getLignes()) {
            BigDecimal disponible = dispo.getOrDefault(l.getArticle().getId(), BigDecimal.ZERO);
            if (disponible.compareTo(l.getQuantite()) < 0) {
                throw new RegleMetierException("Stock insuffisant pour " + l.getArticle().getReference() + " : "
                        + disponible.stripTrailingZeros().toPlainString() + " disponible(s), "
                        + l.getQuantite().stripTrailingZeros().toPlainString() + " demandé(s).");
            }
        }
    }

    private void synchroniserLignes(List<LigneVenteForm> saisies, Vente vente) {
        Set<Long> vus = new HashSet<>();
        for (int i = 0; i < saisies.size(); i++) {
            if (!vus.add(saisies.get(i).getArticleId())) {
                throw new RegleMetierException("lignes[" + i + "].articleId",
                        "Article déjà présent sur une autre ligne : modifiez plutôt sa quantité");
            }
        }
        Map<Long, Article> articles = articleRepository.findAllById(vus).stream()
                .collect(Collectors.toMap(Article::getId, Function.identity()));
        Map<Long, LigneVente> existantes = vente.getLignes().stream()
                .collect(Collectors.toMap(l -> l.getArticle().getId(), Function.identity()));

        for (int i = 0; i < saisies.size(); i++) {
            LigneVenteForm saisie = saisies.get(i);
            Article article = articles.get(saisie.getArticleId());
            if (article == null) {
                throw new RegleMetierException("lignes[" + i + "].articleId", "Article introuvable");
            }
            if (!article.isActif()) {
                throw new RegleMetierException("lignes[" + i + "].articleId", "L'article " + article.getReference() + " est désactivé");
            }
            if (article.getPrixVenteHt() == null) {
                throw new RegleMetierException("lignes[" + i + "].articleId",
                        "L'article " + article.getReference() + " n'a pas encore de prix de vente");
            }
            LigneVente ligne = existantes.get(article.getId());
            if (ligne == null) {
                ligne = new LigneVente();
                ligne.setArticle(article);
                vente.ajouterLigne(ligne);
            }
            // prix toujours relu dans le catalogue, jamais pris du navigateur
            ligne.appliquer(article.getPrixVenteHt(), saisie.getRemisePct(), saisie.getQuantite());
        }
        vente.getLignes().removeIf(l -> !vus.contains(l.getArticle().getId()));
    }

    /** Seul le vendeur qui a cree la vente peut la modifier, supprimer ou valider. */
    private void verifierAuteur(Vente vente, Utilisateur utilisateur, String action) {
        if (!vente.getVendeur().getId().equals(utilisateur.getId())) {
            throw new RegleMetierException("Seul le vendeur qui a saisi la vente ("
                    + vente.getVendeur().getNomComplet() + ") peut la " + action + ".");
        }
    }

    /** Principe des 4 yeux : le controleur doit etre different du vendeur. */
    private Utilisateur verifierControle(Vente vente, String username) {
        if (vente.getStatut() != StatutVente.A_VALIDER) {
            throw new RegleMetierException("Cette vente n'attend pas de contrôle.");
        }
        Utilisateur controleur = utilisateur(username);
        if (vente.getVendeur().getId().equals(controleur.getId())) {
            throw new RegleMetierException("Vous ne pouvez pas contrôler votre propre vente : "
                    + "elle doit être approuvée par une autre personne.");
        }
        return controleur;
    }

    private Utilisateur utilisateur(String username) {
        return utilisateurRepository.findByUsernameIgnoreCase(username)
                .orElseThrow(() -> new RegleMetierException("Utilisateur connecté introuvable"));
    }

    private static String texte(String valeur) {
        return valeur == null || valeur.isBlank() ? null : valeur.trim();
    }
}

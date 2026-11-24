package mg.working.tantana.service;

import lombok.RequiredArgsConstructor;
import mg.working.tantana.dto.AvoirForm;
import mg.working.tantana.entity.Avoir;
import mg.working.tantana.entity.LigneAvoir;
import mg.working.tantana.entity.LigneVente;
import mg.working.tantana.entity.StatutVente;
import mg.working.tantana.entity.Utilisateur;
import mg.working.tantana.entity.Vente;
import mg.working.tantana.exception.ElementIntrouvableException;
import mg.working.tantana.exception.RegleMetierException;
import mg.working.tantana.repository.AvoirRepository;
import mg.working.tantana.repository.UtilisateurRepository;
import mg.working.tantana.repository.VenteRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Avoirs : seule facon de corriger une facture validee (retour client, erreur, geste commercial).
 * Reserve aux roles ADMIN / CHECKER (voir le controleur).
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AvoirService {

    private final AvoirRepository avoirRepository;
    private final VenteRepository venteRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final VenteService venteService;
    private final StockService stockService;
    private final NumerotationService numerotationService;

    public List<Avoir> lister(Long venteId) {
        return avoirRepository.findByVenteIdOrderByIdAsc(venteId);
    }

    /** Formulaire pre-rempli : une ligne par ligne de facture encore creditable, quantite 0. */
    public AvoirForm preparer(Vente vente) {
        AvoirForm form = new AvoirForm();
        vente.getLignes().stream()
                .filter(l -> l.getQuantiteCreditable().signum() > 0)
                .forEach(l -> form.getLignes().add(new AvoirForm.Ligne(l.getId())));
        return form;
    }

    @Transactional
    public Avoir creer(Long venteId, AvoirForm form, String username) {
        venteRepository.findPourMiseAJour(venteId)
                .orElseThrow(() -> new ElementIntrouvableException("Vente", venteId));
        Vente vente = venteService.trouverAvecDetails(venteId);
        if (vente.getStatut() != StatutVente.VALIDEE) {
            throw new RegleMetierException("Un avoir ne peut porter que sur une facture validée.");
        }
        String motif = form.getMotif() == null ? null : form.getMotif().trim();
        if (motif == null || motif.isEmpty()) {
            throw new RegleMetierException("Le motif de l'avoir est obligatoire (il figure sur le document).");
        }
        Utilisateur utilisateur = utilisateurRepository.findByUsernameIgnoreCase(username)
                .orElseThrow(() -> new RegleMetierException("Utilisateur connecté introuvable"));

        Map<Long, LigneVente> lignesVente = vente.getLignes().stream()
                .collect(Collectors.toMap(LigneVente::getId, Function.identity()));

        Avoir avoir = new Avoir();
        avoir.setVente(vente);
        avoir.setMotif(motif.length() > 255 ? motif.substring(0, 255) : motif);
        avoir.setTauxTva(vente.getTauxTva());
        avoir.setSaisiPar(utilisateur);

        for (AvoirForm.Ligne saisie : form.getLignes()) {
            BigDecimal quantite = saisie.getQuantite() == null ? BigDecimal.ZERO : saisie.getQuantite();
            if (quantite.signum() == 0) {
                continue;
            }
            LigneVente lv = lignesVente.get(saisie.getLigneVenteId());
            if (lv == null) {
                throw new RegleMetierException("Ligne de facture inconnue.");
            }
            if (quantite.signum() < 0 || quantite.compareTo(lv.getQuantiteCreditable()) > 0) {
                throw new RegleMetierException(lv.getArticle().getReference() + " : au maximum "
                        + lv.getQuantiteCreditable().stripTrailingZeros().toPlainString() + " peut encore être crédité.");
            }
            LigneAvoir la = new LigneAvoir();
            la.setLigneVente(lv);
            la.setQuantite(quantite);
            la.setMontantHt(lv.getPrixUnitaireHt().multiply(quantite).setScale(2, RoundingMode.HALF_UP));
            la.setRetourStock(saisie.isRetourStock());
            avoir.ajouterLigne(la);
            lv.setQuantiteAvoir(lv.getQuantiteAvoir().add(quantite));
        }
        if (avoir.getLignes().isEmpty()) {
            throw new RegleMetierException("Indiquez au moins une quantité à créditer.");
        }

        avoir.calculerTotaux();
        // Dernier avoir qui solde toute la facture : on ajuste a l'Ariary pres pour que
        // le total des avoirs egale exactement la facture (pas d'ecart d'arrondi residuel).
        boolean toutCredite = vente.getLignes().stream().allMatch(l -> l.getQuantiteCreditable().signum() == 0);
        if (toutCredite) {
            BigDecimal htDejaCredite = avoirRepository.findByVenteIdOrderByIdAsc(venteId).stream()
                    .map(Avoir::getTotalHt).reduce(BigDecimal.ZERO, BigDecimal::add);
            avoir.setTotalHt(vente.getTotalHt().subtract(htDejaCredite));
            avoir.setTotalTtc(vente.getTotalTtc().subtract(vente.getMontantAvoirs()));
            avoir.setTotalTva(avoir.getTotalTtc().subtract(avoir.getTotalHt()));
        }

        avoir.setNumero(numerotationService.prochainNumero(NumerotationService.AVOIR, avoir.getDateAvoir()));
        avoirRepository.save(avoir);

        for (LigneAvoir la : avoir.getLignes()) {
            if (la.isRetourStock()) {
                LigneVente lv = la.getLigneVente();
                stockService.retourVente(lv.getArticle(), la.getQuantite(),
                        lv.getCoutAchatUnitaire(), lv.getCoutRevientUnitaire(), avoir, utilisateur);
            }
        }
        vente.setMontantAvoirs(vente.getMontantAvoirs().add(avoir.getTotalTtc()));
        return avoir;
    }
}

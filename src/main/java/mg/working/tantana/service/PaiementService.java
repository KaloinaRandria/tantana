package mg.working.tantana.service;

import lombok.RequiredArgsConstructor;
import mg.working.tantana.entity.ModePaiement;
import mg.working.tantana.entity.Paiement;
import mg.working.tantana.entity.StatutVente;
import mg.working.tantana.entity.Utilisateur;
import mg.working.tantana.entity.Vente;
import mg.working.tantana.exception.ElementIntrouvableException;
import mg.working.tantana.exception.RegleMetierException;
import mg.working.tantana.repository.PaiementRepository;
import mg.working.tantana.repository.UtilisateurRepository;
import mg.working.tantana.repository.VenteRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Encaissements des factures. Le cumul « montant paye » de la facture est mis a jour
 * dans la meme transaction, facture verrouillee (deux encaissements simultanes ne
 * peuvent pas depasser le reste a payer).
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PaiementService {

    private final PaiementRepository paiementRepository;
    private final VenteRepository venteRepository;
    private final UtilisateurRepository utilisateurRepository;

    public List<Paiement> lister(Long venteId) {
        return paiementRepository.findByVenteIdOrderByIdAsc(venteId);
    }

    /** Facture d'un paiement (pour revenir a la bonne page, meme en cas d'erreur). */
    public Long venteDe(Long paiementId) {
        return paiementRepository.findById(paiementId)
                .orElseThrow(() -> new ElementIntrouvableException("Paiement", paiementId))
                .getVente().getId();
    }

    @Transactional
    public Paiement enregistrer(Long venteId, LocalDate date, BigDecimal montant, ModePaiement mode,
                                String reference, String username) {
        Vente vente = venteRepository.findPourMiseAJour(venteId)
                .orElseThrow(() -> new ElementIntrouvableException("Vente", venteId));
        if (vente.getStatut() != StatutVente.VALIDEE) {
            throw new RegleMetierException("Seule une facture validée peut être encaissée.");
        }
        if (mode == null) {
            throw new RegleMetierException("Choisissez le mode de paiement.");
        }
        if (montant == null || montant.signum() <= 0) {
            throw new RegleMetierException("Le montant doit être supérieur à 0.");
        }
        BigDecimal m = montant.setScale(0, RoundingMode.HALF_UP);
        if (m.compareTo(vente.getResteAPayer()) > 0) {
            throw new RegleMetierException("Le montant dépasse le reste à payer ("
                    + Montants.ariary(vente.getResteAPayer()) + ").");
        }
        LocalDate jour = date == null ? LocalDate.now() : date;
        if (jour.isAfter(LocalDate.now())) {
            throw new RegleMetierException("La date de paiement ne peut pas être dans le futur.");
        }
        if (jour.isBefore(vente.getDateVente())) {
            throw new RegleMetierException("La date de paiement ne peut pas précéder la facture.");
        }
        String ref = reference == null || reference.isBlank() ? null : reference.trim();
        if (mode != ModePaiement.ESPECES && ref == null) {
            throw new RegleMetierException("Indiquez la référence (n° de transaction, de chèque ou de virement) : "
                    + "elle permet de rapprocher le paiement.");
        }

        Paiement paiement = new Paiement();
        paiement.setVente(vente);
        paiement.setDatePaiement(jour);
        paiement.setMontant(m);
        paiement.setMode(mode);
        paiement.setReference(ref);
        paiement.setSaisiPar(utilisateur(username));
        vente.setMontantPaye(vente.getMontantPaye().add(m));
        return paiementRepository.save(paiement);
    }

    /** Annulation tracee (erreur de saisie, cheque rejete...). Le paiement reste visible, barre. */
    @Transactional
    public Paiement annuler(Long paiementId, String motif, String username) {
        Paiement paiement = paiementRepository.findById(paiementId)
                .orElseThrow(() -> new ElementIntrouvableException("Paiement", paiementId));
        Vente vente = venteRepository.findPourMiseAJour(paiement.getVente().getId()).orElseThrow();
        if (paiement.isAnnule()) {
            throw new RegleMetierException("Ce paiement est déjà annulé.");
        }
        if (motif == null || motif.isBlank()) {
            throw new RegleMetierException("Indiquez le motif de l'annulation.");
        }
        paiement.setAnnule(true);
        paiement.setMotifAnnulation(motif.trim());
        paiement.setAnnulePar(utilisateur(username));
        paiement.setDateAnnulation(LocalDateTime.now());
        vente.setMontantPaye(vente.getMontantPaye().subtract(paiement.getMontant()));
        return paiement;
    }

    private Utilisateur utilisateur(String username) {
        return utilisateurRepository.findByUsernameIgnoreCase(username)
                .orElseThrow(() -> new RegleMetierException("Utilisateur connecté introuvable"));
    }
}

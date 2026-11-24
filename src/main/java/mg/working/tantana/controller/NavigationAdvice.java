package mg.working.tantana.controller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import mg.working.tantana.service.ReceptionService;
import mg.working.tantana.service.UtilisateurService;
import mg.working.tantana.service.VenteService;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.security.Principal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/** Donnees communes a toutes les pages (sidebar). */
@ControllerAdvice
@RequiredArgsConstructor
public class NavigationAdvice {

    private final ReceptionService receptionService;
    private final VenteService venteService;
    private final UtilisateurService utilisateurService;

    /** URL courante, pour surligner le lien actif du sidebar. */
    @ModelAttribute("urlCourante")
    public String urlCourante(HttpServletRequest request) {
        return request.getRequestURI().substring(request.getContextPath().length());
    }

    /** Nombre de receptions en attente de validation (pastille du sidebar). */
    @ModelAttribute("receptionsAValider")
    public long receptionsAValider(Principal principal) {
        return principal == null ? 0 : receptionService.nombreAValider();
    }

    /** Nom complet de l'utilisateur connecte (sidebar et menu du compte). */
    @ModelAttribute("nomUtilisateur")
    public String nomUtilisateur(Principal principal) {
        if (principal == null) {
            return "";
        }
        return utilisateurService.trouverParUsername(principal.getName())
                .map(u -> u.getNomComplet())
                .orElse(principal.getName());
    }

    /** Initiale de l'avatar (jamais vide, meme sans utilisateur). */
    @ModelAttribute("initialeUtilisateur")
    public String initialeUtilisateur(Principal principal) {
        String nom = nomUtilisateur(principal);
        return nom.isBlank() ? "?" : nom.substring(0, 1).toUpperCase();
    }

    /** « Mardi 25 novembre 2026 » pour la barre du haut. */
    @ModelAttribute("dateDuJour")
    public String dateDuJour() {
        String date = LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE d MMMM yyyy", Locale.FRANCE));
        return Character.toUpperCase(date.charAt(0)) + date.substring(1);
    }

    /** Nombre de ventes en attente de controle (pastille du sidebar). */
    @ModelAttribute("ventesAValider")
    public long ventesAValider(Principal principal) {
        return principal == null ? 0 : venteService.nombreAValider();
    }
}

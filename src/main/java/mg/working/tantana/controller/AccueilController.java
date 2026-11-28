package mg.working.tantana.controller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import mg.working.tantana.dto.FiltrePeriode;
import mg.working.tantana.dto.Periode;
import mg.working.tantana.exception.RegleMetierException;
import mg.working.tantana.service.AccueilService;
import mg.working.tantana.service.UtilisateurService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;

@Controller
@RequiredArgsConstructor
public class AccueilController {

    private final UtilisateurService utilisateurService;
    private final AccueilService accueilService;

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    /**
     * Accueil : indicateurs « a traiter » pour tous ; pour l'ADMIN et le CHECKER, en plus,
     * le tableau de bord des ventes et des marges sur une periode (par defaut le mois en cours).
     */
    @GetMapping("/")
    public String accueil(@RequestParam(required = false) Periode periode,
                          @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate du,
                          @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate au,
                          Authentication authentication, Model model) {
        utilisateurService.trouverParUsername(authentication.getName())
                .ifPresent(u -> model.addAttribute("utilisateur", u));
        // chiffres financiers (CA, marges) reserves a l'ADMIN et au CHECKER
        boolean financier = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN") || a.getAuthority().equals("ROLE_CHECKER"));

        // par defaut : le mois en cours ; une date manquante est completee par la periode du mois
        FiltrePeriode filtre = FiltrePeriode.resoudre(periode, du, au, Periode.MOIS, FiltrePeriode.ANALYSE);
        if (filtre.du() == null || filtre.au() == null) {
            LocalDate[] mois = Periode.MOIS.bornes(LocalDate.now());
            filtre = FiltrePeriode.resoudre(Periode.PERSONNALISEE, filtre.du() == null ? mois[0] : filtre.du(),
                    filtre.au() == null ? mois[1] : filtre.au(), Periode.MOIS, FiltrePeriode.ANALYSE);
        }
        try {
            model.addAttribute("tableau", accueilService.tableau(financier, filtre.du(), filtre.au()));
        } catch (RegleMetierException e) {
            model.addAttribute("erreur", e.getMessage());
            filtre = FiltrePeriode.resoudre(Periode.MOIS, null, null, Periode.MOIS, FiltrePeriode.ANALYSE);
            model.addAttribute("tableau", accueilService.tableau(financier, filtre.du(), filtre.au()));
        }
        model.addAttribute("filtre", filtre);
        return "index";
    }

    /** Ancienne adresse du tableau de bord (favoris) : desormais fusionne dans l'accueil. */
    @GetMapping("/ventes/tableau-de-bord")
    public String ancienTableauDeBord(HttpServletRequest request) {
        String requete = request.getQueryString();
        return "redirect:/" + (requete == null ? "" : "?" + requete);
    }

    /**
     * Toutes methodes : Spring Security fait un « forward » vers cette page en conservant
     * la methode de la requete refusee (un POST refuse arrive donc ici en POST).
     */
    @RequestMapping("/acces-refuse")
    public String accesRefuse() {
        return "acces-refuse";
    }
}

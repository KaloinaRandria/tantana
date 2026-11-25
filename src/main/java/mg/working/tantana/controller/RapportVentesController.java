package mg.working.tantana.controller;

import lombok.RequiredArgsConstructor;
import mg.working.tantana.dto.FiltrePeriode;
import mg.working.tantana.dto.Periode;
import mg.working.tantana.exception.RegleMetierException;
import mg.working.tantana.service.RapportVentesService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;

/** Tableau de bord des ventes et des marges (ADMIN / CHECKER : les couts y sont visibles). */
@Controller
@RequiredArgsConstructor
public class RapportVentesController {

    private final RapportVentesService rapportService;

    @GetMapping("/ventes/tableau-de-bord")
    @PreAuthorize("hasAnyRole('ADMIN','CHECKER')")
    public String tableau(@RequestParam(required = false) Periode periode,
                          @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate du,
                          @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate au,
                          Model model) {
        // par defaut : le mois en cours ; une date manquante est completee par la periode du mois
        FiltrePeriode filtre = FiltrePeriode.resoudre(periode, du, au, Periode.MOIS, FiltrePeriode.ANALYSE);
        if (filtre.du() == null || filtre.au() == null) {
            LocalDate[] mois = Periode.MOIS.bornes(LocalDate.now());
            filtre = FiltrePeriode.resoudre(Periode.PERSONNALISEE, filtre.du() == null ? mois[0] : filtre.du(),
                    filtre.au() == null ? mois[1] : filtre.au(), Periode.MOIS, FiltrePeriode.ANALYSE);
        }
        try {
            model.addAttribute("tableau", rapportService.tableau(filtre.du(), filtre.au()));
        } catch (RegleMetierException e) {
            model.addAttribute("erreur", e.getMessage());
            filtre = FiltrePeriode.resoudre(Periode.MOIS, null, null, Periode.MOIS, FiltrePeriode.ANALYSE);
            model.addAttribute("tableau", rapportService.tableau(filtre.du(), filtre.au()));
        }
        model.addAttribute("filtre", filtre);
        return "ventes/vente-tableau-de-bord";
    }
}

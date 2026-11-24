package mg.working.tantana.controller;

import lombok.RequiredArgsConstructor;
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
    public String tableau(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate du,
                          @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate au,
                          Model model) {
        // par defaut : le mois en cours
        LocalDate fin = au == null ? LocalDate.now() : au;
        LocalDate debut = du == null ? fin.withDayOfMonth(1) : du;
        try {
            model.addAttribute("tableau", rapportService.tableau(debut, fin));
        } catch (RegleMetierException e) {
            model.addAttribute("erreur", e.getMessage());
            model.addAttribute("tableau", rapportService.tableau(fin.withDayOfMonth(1), fin));
        }
        return "ventes/tableau-de-bord";
    }
}

package mg.working.tantana.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** Encaissement d'une facture. Jamais supprime : une erreur se corrige par annulation tracee. */
@Entity
@Table(name = "paiement")
@Getter
@Setter
@NoArgsConstructor
public class Paiement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "vente_id", updatable = false)
    private Vente vente;

    @Column(name = "date_paiement", nullable = false)
    private LocalDate datePaiement;

    @Column(nullable = false, precision = 18, scale = 2, updatable = false)
    private BigDecimal montant;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "mode_paiement_id", nullable = false)
    private ModePaiement mode;

    /** N° de transaction mobile money, de cheque ou de virement. */
    @Column(length = 100)
    private String reference;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "saisi_par_id", updatable = false)
    private Utilisateur saisiPar;

    @Column(name = "date_saisie", nullable = false, updatable = false)
    private LocalDateTime dateSaisie = LocalDateTime.now();

    @Column(nullable = false)
    private boolean annule;

    @Column(name = "motif_annulation", length = 255)
    private String motifAnnulation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "annule_par_id")
    private Utilisateur annulePar;

    @Column(name = "date_annulation")
    private LocalDateTime dateAnnulation;
}

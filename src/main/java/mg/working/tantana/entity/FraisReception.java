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

/** Frais du transitaire en Ariary, enregistres a part (non repartis sur les articles). */
@Entity
@Table(name = "frais_reception")
@Getter
@Setter
@NoArgsConstructor
public class FraisReception {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "reception_id")
    private Reception reception;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "type_frais_id", nullable = false)
    private TypeFrais type;

    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal montant;

    @Column(name = "reference_facture", length = 100)
    private String referenceFacture;

    @Column(length = 255)
    private String commentaire;
}

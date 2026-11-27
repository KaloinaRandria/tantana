package mg.working.tantana.entity.conversion;

import jakarta.persistence.Converter;
import mg.working.tantana.entity.StatutCommande;

@Converter(autoApply = true)
public class ConvertisseurStatutCommande extends ConvertisseurValeurSysteme<StatutCommande> {

    public ConvertisseurStatutCommande() {
        super(StatutCommande.class);
    }
}

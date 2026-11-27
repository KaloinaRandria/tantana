package mg.working.tantana.entity.conversion;

import jakarta.persistence.Converter;
import mg.working.tantana.entity.StatutInventaire;

@Converter(autoApply = true)
public class ConvertisseurStatutInventaire extends ConvertisseurValeurSysteme<StatutInventaire> {

    public ConvertisseurStatutInventaire() {
        super(StatutInventaire.class);
    }
}

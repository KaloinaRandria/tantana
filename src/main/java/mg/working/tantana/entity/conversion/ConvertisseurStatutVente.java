package mg.working.tantana.entity.conversion;

import jakarta.persistence.Converter;
import mg.working.tantana.entity.StatutVente;

@Converter(autoApply = true)
public class ConvertisseurStatutVente extends ConvertisseurValeurSysteme<StatutVente> {

    public ConvertisseurStatutVente() {
        super(StatutVente.class);
    }
}

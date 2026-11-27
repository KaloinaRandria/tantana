package mg.working.tantana.entity.conversion;

import jakarta.persistence.Converter;
import mg.working.tantana.entity.StatutReception;

@Converter(autoApply = true)
public class ConvertisseurStatutReception extends ConvertisseurValeurSysteme<StatutReception> {

    public ConvertisseurStatutReception() {
        super(StatutReception.class);
    }
}

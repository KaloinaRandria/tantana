package mg.working.tantana.entity.conversion;

import jakarta.persistence.Converter;
import mg.working.tantana.entity.TypeInventaire;

@Converter(autoApply = true)
public class ConvertisseurTypeInventaire extends ConvertisseurValeurSysteme<TypeInventaire> {

    public ConvertisseurTypeInventaire() {
        super(TypeInventaire.class);
    }
}

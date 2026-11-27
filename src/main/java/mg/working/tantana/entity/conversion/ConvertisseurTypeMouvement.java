package mg.working.tantana.entity.conversion;

import jakarta.persistence.Converter;
import mg.working.tantana.entity.TypeMouvement;

@Converter(autoApply = true)
public class ConvertisseurTypeMouvement extends ConvertisseurValeurSysteme<TypeMouvement> {

    public ConvertisseurTypeMouvement() {
        super(TypeMouvement.class);
    }
}

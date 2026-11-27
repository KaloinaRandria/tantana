package mg.working.tantana.entity.conversion;

import jakarta.persistence.Converter;
import mg.working.tantana.entity.TypeClient;

@Converter(autoApply = true)
public class ConvertisseurTypeClient extends ConvertisseurValeurSysteme<TypeClient> {

    public ConvertisseurTypeClient() {
        super(TypeClient.class);
    }
}

package mg.working.tantana.entity.conversion;

import jakarta.persistence.AttributeConverter;
import mg.working.tantana.entity.ValeurSysteme;

/**
 * Stocke une valeur systeme par son identifiant numerique (SMALLINT) au lieu de son nom.
 * Une sous-classe par type, declaree autoApply : aucune annotation n'est necessaire sur les champs.
 */
public abstract class ConvertisseurValeurSysteme<E extends Enum<E> & ValeurSysteme> implements AttributeConverter<E, Short> {

    private final Class<E> type;

    protected ConvertisseurValeurSysteme(Class<E> type) {
        this.type = type;
    }

    @Override
    public Short convertToDatabaseColumn(E valeur) {
        return valeur == null ? null : valeur.getId();
    }

    @Override
    public E convertToEntityAttribute(Short id) {
        if (id == null) {
            return null;
        }
        for (E valeur : type.getEnumConstants()) {
            if (valeur.getId() == id) {
                return valeur;
            }
        }
        throw new IllegalStateException("Valeur inconnue pour " + type.getSimpleName() + " : " + id);
    }
}

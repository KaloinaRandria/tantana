package mg.working.tantana.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum TypeClient {
    PARTICULIER("Particulier"),
    /** Entreprise : NIF et STAT obligatoires (mentions des factures). */
    PROFESSIONNEL("Professionnel");

    private final String libelle;
}

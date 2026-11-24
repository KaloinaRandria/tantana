package mg.working.tantana.service;

import lombok.RequiredArgsConstructor;
import mg.working.tantana.entity.Client;
import mg.working.tantana.entity.TypeClient;
import mg.working.tantana.exception.DoublonException;
import mg.working.tantana.exception.ElementIntrouvableException;
import mg.working.tantana.exception.RegleMetierException;
import mg.working.tantana.repository.ClientRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ClientService {

    private final ClientRepository clientRepository;

    /** @param actif null = tous, true = actifs, false = inactifs */
    public Page<Client> rechercher(String texte, Boolean actif, Pageable pageable) {
        String t = texte == null ? "" : texte.trim();
        return actif == null ? clientRepository.rechercher(t, pageable) : clientRepository.rechercher(t, actif, pageable);
    }

    public Client trouver(Long id) {
        return clientRepository.findById(id).orElseThrow(() -> new ElementIntrouvableException("Client", id));
    }

    public List<Client> listerActifs() {
        return clientRepository.findByActifTrueOrderByComptoirDescNomAsc();
    }

    public Client comptoir() {
        return clientRepository.findByComptoirTrue()
                .orElseThrow(() -> new IllegalStateException("Client comptoir absent (migration V7)"));
    }

    @Transactional
    public Client enregistrer(Client saisie) {
        Client client = saisie.getId() == null ? new Client() : trouver(saisie.getId());

        String nif = texte(saisie.getNif());
        String stat = texte(saisie.getStat());
        TypeClient type = saisie.getType();

        if (client.isComptoir() && type != TypeClient.PARTICULIER) {
            throw new RegleMetierException("type", "Le client comptoir reste un particulier.");
        }
        if (type == TypeClient.PROFESSIONNEL) {
            if (nif == null) {
                throw new RegleMetierException("nif", "Le NIF est obligatoire pour un professionnel (mention de facture)");
            }
            if (stat == null) {
                throw new RegleMetierException("stat", "Le STAT est obligatoire pour un professionnel (mention de facture)");
            }
        }
        if (nif != null) {
            clientRepository.findByNifIgnoreCase(nif)
                    .filter(existant -> !existant.getId().equals(saisie.getId()))
                    .ifPresent(existant -> {
                        throw new DoublonException("nif", "Ce NIF est déjà celui du client « " + existant.getNom() + " »");
                    });
        }

        client.setNom(saisie.getNom().trim());
        client.setType(type);
        client.setTelephone(texte(saisie.getTelephone()));
        client.setEmail(texte(saisie.getEmail()));
        client.setAdresse(texte(saisie.getAdresse()));
        client.setNif(nif);
        client.setStat(stat);
        client.setPlafondCredit(saisie.getPlafondCredit());
        client.setObservations(texte(saisie.getObservations()));
        return clientRepository.save(client);
    }

    @Transactional
    public Client basculerActif(Long id) {
        Client client = trouver(id);
        if (client.isComptoir()) {
            throw new RegleMetierException("Le client comptoir ne peut pas être désactivé : il sert aux ventes anonymes.");
        }
        client.setActif(!client.isActif());
        return client;
    }

    private static String texte(String valeur) {
        return valeur == null || valeur.isBlank() ? null : valeur.trim();
    }
}

package mg.working.tantana.service;

import lombok.RequiredArgsConstructor;
import mg.working.tantana.entity.Utilisateur;
import mg.working.tantana.repository.UtilisateurRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UtilisateurService {

    private final UtilisateurRepository utilisateurRepository;

    @Transactional(readOnly = true)
    public Optional<Utilisateur> trouverParUsername(String username) {
        return utilisateurRepository.findByUsernameAvecRoles(username);
    }
}

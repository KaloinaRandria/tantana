package mg.working.tantana.service;

import lombok.RequiredArgsConstructor;
import mg.working.tantana.repository.UtilisateurRepository;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Indique a Spring Security comment charger un utilisateur depuis PostgreSQL. */
@Service
@RequiredArgsConstructor
public class UtilisateurDetailsService implements UserDetailsService {

    private final UtilisateurRepository utilisateurRepository;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) {
        var utilisateur = utilisateurRepository.findByUsernameAvecRoles(username)
                .orElseThrow(() -> new UsernameNotFoundException("Utilisateur introuvable : " + username));

        var autorites = utilisateur.getRoles().stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role.getCode()))
                .toList();

        return User.withUsername(utilisateur.getUsername())
                .password(utilisateur.getPassword())
                .disabled(!utilisateur.isActif())
                .authorities(autorites)
                .build();
    }
}

package mg.working.tantana.service;

import lombok.RequiredArgsConstructor;
import mg.working.tantana.dto.MotDePasseForm;
import mg.working.tantana.dto.UtilisateurForm;
import mg.working.tantana.entity.Role;
import mg.working.tantana.entity.Utilisateur;
import mg.working.tantana.exception.DoublonException;
import mg.working.tantana.exception.ElementIntrouvableException;
import mg.working.tantana.exception.RegleMetierException;
import mg.working.tantana.repository.RoleRepository;
import mg.working.tantana.repository.UtilisateurRepository;
import org.hibernate.Hibernate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Comptes utilisateurs : consultation, et gestion par l'ADMIN.
 *
 * Bonnes pratiques appliquees :
 *  - l'ADMIN ne choisit jamais le mot de passe d'un autre : le systeme genere un mot de passe
 *    temporaire, affiche une seule fois, que l'utilisateur doit changer a sa connexion ;
 *  - pas de suppression (tracabilite des operations) : on desactive ;
 *  - garde-fous : un ADMIN ne peut ni se desactiver ni se retirer le role ADMIN,
 *    et il reste toujours au moins un ADMIN actif.
 */
@Service
@RequiredArgsConstructor
public class UtilisateurService {

    public static final String ROLE_ADMIN = "ADMIN";

    private final UtilisateurRepository utilisateurRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    /** Utilisateur cree ou reinitialise, avec son mot de passe temporaire en clair (a afficher une fois). */
    public record AvecMotDePasse(Utilisateur utilisateur, String motDePasseTemporaire) {
    }

    // ===================== Lecture =====================

    @Transactional(readOnly = true)
    public Optional<Utilisateur> trouverParUsername(String username) {
        return utilisateurRepository.findByUsernameAvecRoles(username);
    }

    @Transactional(readOnly = true)
    public Page<Utilisateur> rechercher(String texte, Boolean actif, Pageable pageable) {
        Page<Utilisateur> page = utilisateurRepository.rechercher(texte == null ? "" : texte.trim(), actif, pageable);
        page.forEach(u -> Hibernate.initialize(u.getRoles())); // une requete par lot (@BatchSize)
        return page;
    }

    @Transactional(readOnly = true)
    public Utilisateur trouver(Long id) {
        return utilisateurRepository.findAvecRoles(id)
                .orElseThrow(() -> new ElementIntrouvableException("Utilisateur", id));
    }

    @Transactional(readOnly = true)
    public List<Role> roles() {
        return roleRepository.findAllByOrderByIdAsc();
    }

    @Transactional(readOnly = true)
    public UtilisateurForm versFormulaire(Long id) {
        Utilisateur u = trouver(id);
        UtilisateurForm form = new UtilisateurForm();
        form.setId(u.getId());
        form.setUsername(u.getUsername());
        form.setNomComplet(u.getNomComplet());
        form.setEmail(u.getEmail());
        form.setActif(u.isActif());
        u.getRoles().forEach(r -> form.getRoleIds().add(r.getId()));
        return form;
    }

    /** Le mot de passe de cet utilisateur doit-il etre change avant toute autre action ? */
    @Transactional(readOnly = true)
    public boolean doitChangerMotDePasse(String username) {
        return utilisateurRepository.doitChangerMotDePasse(username).orElse(false);
    }

    @Transactional
    public void enregistrerConnexion(String username) {
        utilisateurRepository.enregistrerConnexion(username, LocalDateTime.now());
    }

    // ===================== Gestion par l'ADMIN =====================

    @Transactional
    public AvecMotDePasse creer(UtilisateurForm form) {
        String username = form.getUsername().trim();
        if (utilisateurRepository.existsByUsernameIgnoreCase(username)) {
            throw new DoublonException("username", "Cet identifiant est déjà utilisé.");
        }
        verifierEmail(form.getEmail(), null);

        Utilisateur u = new Utilisateur();
        u.setUsername(username);
        appliquer(form, u);
        u.setActif(true); // un compte se cree actif
        String temporaire = MotsDePasse.genererTemporaire();
        definirMotDePasseTemporaire(u, temporaire);
        return new AvecMotDePasse(utilisateurRepository.save(u), temporaire);
    }

    @Transactional
    public Utilisateur modifier(UtilisateurForm form, String adminUsername) {
        Utilisateur u = trouver(form.getId());
        verifierEmail(form.getEmail(), u.getId());
        // compte AVANT modification : la requete pourrait sinon voir les changements deja flushes
        long adminsAvant = utilisateurRepository.nombreAdminsActifs();
        boolean etaitAdminActif = u.isActif() && u.aLeRole(ROLE_ADMIN);
        boolean soiMeme = u.getUsername().equalsIgnoreCase(adminUsername);

        appliquer(form, u);
        u.setActif(form.isActif());

        if (soiMeme && !u.isActif()) {
            throw new RegleMetierException("Vous ne pouvez pas désactiver votre propre compte.");
        }
        if (soiMeme && !u.aLeRole(ROLE_ADMIN)) {
            throw new RegleMetierException("roleIds", "Vous ne pouvez pas retirer votre propre rôle Administrateur.");
        }
        verifierDernierAdmin(etaitAdminActif, u, adminsAvant);
        u.setDateModification(LocalDateTime.now());
        return u;
    }

    @Transactional
    public Utilisateur basculerActif(Long id, String adminUsername) {
        Utilisateur u = trouver(id);
        if (u.getUsername().equalsIgnoreCase(adminUsername)) {
            throw new RegleMetierException("Vous ne pouvez pas désactiver votre propre compte.");
        }
        long adminsAvant = utilisateurRepository.nombreAdminsActifs();
        boolean etaitAdminActif = u.isActif() && u.aLeRole(ROLE_ADMIN);
        u.setActif(!u.isActif());
        verifierDernierAdmin(etaitAdminActif, u, adminsAvant);
        u.setDateModification(LocalDateTime.now());
        return u;
    }

    /** Nouveau mot de passe temporaire ; l'ancien ne fonctionne plus. */
    @Transactional
    public AvecMotDePasse reinitialiserMotDePasse(Long id, String adminUsername) {
        Utilisateur u = trouver(id);
        if (u.getUsername().equalsIgnoreCase(adminUsername)) {
            throw new RegleMetierException("Pour votre propre compte, utilisez « Changer mon mot de passe ».");
        }
        String temporaire = MotsDePasse.genererTemporaire();
        definirMotDePasseTemporaire(u, temporaire);
        u.setDateModification(LocalDateTime.now());
        return new AvecMotDePasse(u, temporaire);
    }

    // ===================== Mon compte =====================

    @Transactional
    public void changerMotDePasse(String username, MotDePasseForm form) {
        Utilisateur u = utilisateurRepository.findByUsernameIgnoreCase(username)
                .orElseThrow(() -> new RegleMetierException("Utilisateur connecté introuvable"));
        if (!passwordEncoder.matches(form.getActuel(), u.getPassword())) {
            throw new RegleMetierException("actuel", "Mot de passe actuel incorrect.");
        }
        if (!form.getNouveau().equals(form.getConfirmation())) {
            throw new RegleMetierException("confirmation", "La confirmation ne correspond pas au nouveau mot de passe.");
        }
        String erreur = MotsDePasse.verifier(form.getNouveau(), u.getUsername());
        if (erreur != null) {
            throw new RegleMetierException("nouveau", erreur);
        }
        if (passwordEncoder.matches(form.getNouveau(), u.getPassword())) {
            throw new RegleMetierException("nouveau", "Choisissez un mot de passe différent de l'actuel.");
        }
        u.setPassword(passwordEncoder.encode(form.getNouveau()));
        u.setDoitChangerMotDePasse(false);
        u.setDateMotDePasse(LocalDateTime.now());
    }

    // ===================== Regles =====================

    private void appliquer(UtilisateurForm form, Utilisateur u) {
        u.setNomComplet(form.getNomComplet().trim());
        u.setEmail(form.getEmail() == null || form.getEmail().isBlank() ? null : form.getEmail().trim().toLowerCase());
        Set<Role> roles = new HashSet<>(roleRepository.findAllById(form.getRoleIds()));
        if (roles.isEmpty()) {
            throw new RegleMetierException("roleIds", "Choisissez au moins un rôle.");
        }
        u.setRoles(roles);
    }

    private void definirMotDePasseTemporaire(Utilisateur u, String temporaire) {
        u.setPassword(passwordEncoder.encode(temporaire));
        u.setDoitChangerMotDePasse(true);
        u.setDateMotDePasse(LocalDateTime.now());
    }

    private void verifierEmail(String email, Long id) {
        if (email != null && !email.isBlank() && utilisateurRepository.emailUtilise(email.trim(), id)) {
            throw new DoublonException("email", "Cette adresse e-mail est déjà utilisée par un autre compte.");
        }
    }

    /** Un ADMIN actif qui perd son statut (desactive ou role retire) ne doit pas etre le dernier. */
    private void verifierDernierAdmin(boolean etaitAdminActif, Utilisateur u, long adminsAvant) {
        boolean resteAdminActif = u.isActif() && u.aLeRole(ROLE_ADMIN);
        if (etaitAdminActif && !resteAdminActif && adminsAvant <= 1) {
            throw new RegleMetierException("Il doit rester au moins un administrateur actif.");
        }
    }
}

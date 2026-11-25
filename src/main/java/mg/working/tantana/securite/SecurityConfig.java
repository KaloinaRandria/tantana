package mg.working.tantana.securite;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableMethodSecurity // permet @PreAuthorize("hasRole('ADMIN')") sur les services
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                // Ressources publiques
                .requestMatchers("/login", "/webjars/**", "/css/**", "/js/**", "/images/**", "/favicon.ico", "/error").permitAll()
                // Acces par module
                .requestMatchers("/admin/**").hasRole("ADMIN")
                // Prix de vente et marges : reserves a l'ADMIN
                .requestMatchers("/tarifs/**").hasRole("ADMIN")
                // Inventaires : comptes par le stock, controles par un CHECKER (les actions sont filtrees par @PreAuthorize)
                .requestMatchers("/stock/inventaires/**").hasAnyRole("ADMIN", "GESTIONNAIRE_STOCK", "CHECKER", "ACHETEUR")
                // Consultation du stock : l'acheteur voit l'effet de ses receptions
                .requestMatchers("/stock/**").hasAnyRole("ADMIN", "GESTIONNAIRE_STOCK", "ACHETEUR")
                .requestMatchers("/ventes/**").hasAnyRole("ADMIN", "VENDEUR")
                .requestMatchers("/clients/**").hasAnyRole("ADMIN", "VENDEUR")
                .requestMatchers("/achats/**").hasAnyRole("ADMIN", "ACHETEUR")
                // Catalogue partage : consulte par les achats et le stock
                .requestMatchers("/articles/**").hasAnyRole("ADMIN", "ACHETEUR", "GESTIONNAIRE_STOCK")
                // Tout le reste : il suffit d'etre connecte
                .anyRequest().authenticated())
            .formLogin(form -> form
                .loginPage("/login")
                // true : toujours aller a l'accueil apres connexion (evite d'atterrir sur une
                // requete annexe du navigateur, ex. /.well-known/... ou /favicon.ico)
                .defaultSuccessUrl("/", true)
                .failureUrl("/login?erreur")
                .permitAll())
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/login?deconnecte")
                .deleteCookies("JSESSIONID")
                .permitAll())
            .exceptionHandling(ex -> ex.accessDeniedPage("/acces-refuse"));

        return http.build();
    }

    /** Encodeur "{bcrypt}..." : permet de changer d'algorithme plus tard sans casser les anciens mots de passe. */
    @Bean
    PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }
}

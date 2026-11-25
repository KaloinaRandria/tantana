package mg.working.tantana.securite;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

    private final ChangementMotDePasseInterceptor changementMotDePasse;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(changementMotDePasse)
                .addPathPatterns("/**")
                .excludePathPatterns(ChangementMotDePasseInterceptor.URL_CHANGEMENT, "/login", "/logout", "/error",
                        "/acces-refuse", "/css/**", "/js/**", "/webjars/**", "/images/**", "/favicon.ico");
    }
}

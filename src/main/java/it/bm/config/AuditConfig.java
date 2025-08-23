package it.bm.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import javax.naming.AuthenticationException;
import java.util.Optional;

@Configuration
@EnableJpaAuditing(auditorAwareRef = "auditorProvider")
public class AuditConfig {
    @Bean
    public AuditorAware<String> auditorProvider() {
        return new AuditorAwareImpl();
    }

    public static class AuditorAwareImpl implements AuditorAware<String> {

        @Override
        public Optional<String> getCurrentAuditor() {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

            if (authentication != null && authentication.isAuthenticated()) {
                if (authentication instanceof JwtAuthenticationToken jwtAuth) {

                    String preferredUsername = jwtAuth.getToken().getClaimAsString("preferred_username");
                    if (preferredUsername != null && !preferredUsername.trim().isEmpty()) {
                        return Optional.of(preferredUsername);
                    }

                    String email = jwtAuth.getToken().getClaimAsString("email");
                    if (email != null && !email.trim().isEmpty()) {
                        return Optional.of(email);
                    }

                    String name = jwtAuth.getToken().getClaimAsString("name");
                    if (name != null && !name.trim().isEmpty()) {
                        return Optional.of(name);
                    }

                    return Optional.of(authentication.getName());
                }

                return Optional.of(authentication.getName());
            }

            return Optional.of("system");
        }
    }

}

package it.bm.config;



import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.*;

import java.util.List;

@Configuration
public class JwtConfig {

    @Value("${keycloak.resourceserver.external.jwt.issuer-uri}")
    private String externalIssuer;
    @Value("${keycloak.resourceserver.internal.jwt.issuer-uri}")
    private String internalIssuer;
    @Value("${keycloak.resourceserver.jwt.jwk-set-uri}")
    private String jwkSetUri;

    @Bean("customJwtDecoder")
    public JwtDecoder jwtDecoder() {
        NimbusJwtDecoder jwtDecoder = NimbusJwtDecoder.withJwkSetUri(jwkSetUri).build();
        // Custom validator to accept tokens from both internal (within docker network) and external issuers
        OAuth2TokenValidator<Jwt> customIssuerValidator = new CustomIssuerValidator(List.of(internalIssuer, externalIssuer));
        OAuth2TokenValidator<Jwt> withIssuer = new DelegatingOAuth2TokenValidator<>(customIssuerValidator);
        jwtDecoder.setJwtValidator(withIssuer);
        return jwtDecoder;
    }


    private record CustomIssuerValidator(List<String> validIssuers) implements OAuth2TokenValidator<Jwt> {

        @Override
            public OAuth2TokenValidatorResult validate(Jwt jwt) {
                String issuer = jwt.getIssuer().toString();
                if (validIssuers.contains(issuer)) {
                    return OAuth2TokenValidatorResult.success();
                }
                return OAuth2TokenValidatorResult.failure(new OAuth2Error("The iss claim is not valid. Expected one of: " + validIssuers + " but was: " + issuer));
            }
        }
}

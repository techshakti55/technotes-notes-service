package com.technotes.notes.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri}")
    private String issuerUri;

    @Value("${spring.security.oauth2.resourceserver.jwt.jwk-set-uri}")
    private String jwkSetUri;

    /*
     * First-Live API audience frozen by the shared
     * SECURITY-INTEGRATION-CONTRACT.
     */
    private static final String REQUIRED_AUDIENCE = "technotes-api";

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http)
            throws Exception {

        http
                /*
                 * Notes Service is a stateless REST Resource Server.
                 * Authentication is carried by Bearer access tokens.
                 */
                .csrf(csrf -> csrf.disable())

                .sessionManagement(session ->
                        session.sessionCreationPolicy(
                                SessionCreationPolicy.STATELESS
                        )
                )

                .authorizeHttpRequests(auth -> auth

                        /*
                         * Public First-Live Notes APIs.
                         */
                        .requestMatchers(
                                "/api/v1/public/categories",
                                "/api/v1/public/notes",
                                "/api/v1/public/notes/**"
                        ).permitAll()

                        /*
                         * Spring Boot health endpoint.
                         * Useful for local/service health checks.
                         */
                        .requestMatchers("/actuator/health")
                        .permitAll()

                        /*
                         * All remaining Notes APIs require
                         * a successfully validated access token.
                         *
                         * Fine-grained role/scope/business authorization
                         * is enforced at the service/method boundary.
                         */
                        .anyRequest()
                        .authenticated()
                )

                .oauth2ResourceServer(oauth2 ->
                        oauth2.jwt(jwt ->
                                jwt.jwtAuthenticationConverter(
                                        jwtAuthenticationConverter()
                                )
                        )
                );

        return http.build();
    }

    /*
     * Builds the JWT decoder used by Notes Service.
     *
     * Validation:
     * 1. Signature -> OAuth JWKS
     * 2. Standard timestamps
     * 3. Issuer
     * 4. Audience -> technotes-api
     */
    @Bean
    public JwtDecoder jwtDecoder() {

        NimbusJwtDecoder jwtDecoder =
                NimbusJwtDecoder.withJwkSetUri(jwkSetUri).build();

        OAuth2TokenValidator<Jwt> issuerValidator =
                JwtValidators.createDefaultWithIssuer(issuerUri);

        OAuth2TokenValidator<Jwt> audienceValidator =
                new AudienceValidator(REQUIRED_AUDIENCE);

        OAuth2TokenValidator<Jwt> validator =
                new DelegatingOAuth2TokenValidator<>(
                        issuerValidator,
                        audienceValidator
                );

        jwtDecoder.setJwtValidator(validator);

        return jwtDecoder;
    }

    /*
     * Converts JWT claims into Spring Security authorities.
     *
     * scope:
     *
     * "notes.read notes.write taxonomy.write"
     *
     * becomes:
     *
     * SCOPE_notes.read
     * SCOPE_notes.write
     * SCOPE_taxonomy.write
     *
     * roles:
     *
     * ["ADMIN"]
     *
     * becomes:
     *
     * ROLE_ADMIN
     */
    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {

        JwtGrantedAuthoritiesConverter scopeConverter =
                new JwtGrantedAuthoritiesConverter();

        scopeConverter.setAuthoritiesClaimName("scope");
        scopeConverter.setAuthorityPrefix("SCOPE_");

        Converter<Jwt, Collection<GrantedAuthority>> authoritiesConverter =
                jwt -> {

                    Collection<GrantedAuthority> authorities =
                            new ArrayList<>();

                    Collection<GrantedAuthority> scopeAuthorities =
                            scopeConverter.convert(jwt);

                    if (scopeAuthorities != null) {
                        authorities.addAll(scopeAuthorities);
                    }

                    List<String> roles =
                            jwt.getClaimAsStringList("roles");

                    if (roles != null) {
                        roles.stream()
                                .filter(role -> role != null
                                        && !role.isBlank())
                                .map(role ->
                                        new SimpleGrantedAuthority(
                                                "ROLE_" + role
                                        )
                                )
                                .forEach(authorities::add);
                    }

                    return authorities;
                };

        JwtAuthenticationConverter converter =
                new JwtAuthenticationConverter();

        converter.setJwtGrantedAuthoritiesConverter(
                authoritiesConverter
        );

        return converter;
    }
}
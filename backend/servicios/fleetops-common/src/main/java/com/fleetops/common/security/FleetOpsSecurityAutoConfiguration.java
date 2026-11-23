package com.fleetops.common.security;

import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * Cadena de filtros de seguridad común: cada petición pasa por BearerTokenAuthenticationFilter,
 * que valida firma (JWKS de Entra ID), emisor (tenant), expiración y audiencia del JWT.
 * Se registra automáticamente en todo microservicio que dependa de fleetops-common.
 */
@AutoConfiguration
@EnableWebSecurity
@EnableMethodSecurity
@EnableConfigurationProperties(FleetOpsSecurityProperties.class)
public class FleetOpsSecurityAutoConfiguration {

    private static final Logger log = LoggerFactory.getLogger(FleetOpsSecurityAutoConfiguration.class);

    @Bean
    @ConditionalOnMissingBean
    public JwtDecoder jwtDecoder(FleetOpsSecurityProperties props) {
        // Las llaves se descargan de forma perezosa en la primera petición (no bloquea el arranque).
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withJwkSetUri(props.jwks()).build();
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
                JwtValidators.createDefaultWithIssuer(props.issuer()),
                new AudienceValidator(props.audiences())));
        return decoder;
    }

    @Bean
    @ConditionalOnMissingBean
    public SecurityFilterChain fleetOpsFilterChain(HttpSecurity http, FleetOpsSecurityProperties props) throws Exception {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(new EntraIdAuthoritiesConverter());
        converter.setPrincipalClaimName("preferred_username");

        http.csrf(csrf -> csrf.disable())
            .cors(cors -> cors.configurationSource(corsSource(props)))
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                .requestMatchers(props.publicPaths().toArray(String[]::new)).permitAll()
                .requestMatchers("/api/v1/admin/**").hasRole(props.adminRole())
                .requestMatchers("/api/**").hasAnyAuthority("SCOPE_" + props.requiredScope(), "ROLE_" + props.adminRole())
                .anyRequest().authenticated())
            .oauth2ResourceServer(o -> o
                .jwt(jwt -> jwt.jwtAuthenticationConverter(converter))
                .authenticationEntryPoint(unauthorized())
                .accessDeniedHandler(forbidden()));
        return http.build();
    }

    private AuthenticationEntryPoint unauthorized() {
        return (request, response, ex) -> {
            log.warn("JWT rechazado en {} {}: {}", request.getMethod(), request.getRequestURI(), ex.getMessage());
            writeJson(response, HttpServletResponse.SC_UNAUTHORIZED, "Token inválido o expirado");
        };
    }

    private AccessDeniedHandler forbidden() {
        return (request, response, ex) ->
                writeJson(response, HttpServletResponse.SC_FORBIDDEN, "El token no tiene el permiso requerido");
    }

    private static void writeJson(HttpServletResponse response, int status, String message) throws java.io.IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write("{\"status\":" + status + ",\"error\":\"" + message + "\"}");
    }

    private CorsConfigurationSource corsSource(FleetOpsSecurityProperties props) {
        CorsConfiguration cfg = new CorsConfiguration();
        cfg.setAllowedOrigins(props.corsAllowedOrigins());
        cfg.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        cfg.setAllowedHeaders(List.of("Authorization", "Content-Type", "Ocp-Apim-Subscription-Key"));
        cfg.setMaxAge(3600L);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", cfg);
        return source;
    }
}

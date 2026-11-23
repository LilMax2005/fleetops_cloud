package com.fleetops.common.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * Propiedades de validación del JWT emitido por Microsoft Entra ID (IDaaS).
 *
 * @param tenantId          tenant de Entra ID
 * @param audiences         audiencias aceptadas (client id de fleetops-api y/o api://...)
 * @param requiredScope     scope delegado que debe traer el token (claim scp)
 * @param adminRole         app role requerido para /api/v1/admin/**
 * @param publicPaths       rutas sin autenticación (health checks de Container Apps)
 * @param corsAllowedOrigins orígenes permitidos (Angular local y Azure Storage)
 * @param issuerUri         opcional: sobrescribe el emisor (por defecto el de Entra ID v2.0 del tenant)
 * @param jwkSetUri         opcional: sobrescribe el endpoint de llaves públicas (JWKS)
 */
@ConfigurationProperties(prefix = "fleetops.security")
public record FleetOpsSecurityProperties(
        String tenantId,
        List<String> audiences,
        String requiredScope,
        String adminRole,
        List<String> publicPaths,
        List<String> corsAllowedOrigins,
        String issuerUri,
        String jwkSetUri
) {
    public FleetOpsSecurityProperties {
        audiences = audiences == null ? List.of() : audiences;
        requiredScope = requiredScope == null ? "api.access" : requiredScope;
        adminRole = adminRole == null ? "FleetOps.Admin" : adminRole;
        publicPaths = publicPaths == null ? List.of("/actuator/health/**", "/actuator/info") : publicPaths;
        corsAllowedOrigins = corsAllowedOrigins == null ? List.of("http://localhost:4200") : corsAllowedOrigins;
    }

    public String issuer() {
        return hasText(issuerUri) ? issuerUri : "https://login.microsoftonline.com/" + tenantId + "/v2.0";
    }

    public String jwks() {
        return hasText(jwkSetUri) ? jwkSetUri : "https://login.microsoftonline.com/" + tenantId + "/discovery/v2.0/keys";
    }

    private static boolean hasText(String s) {
        return s != null && !s.isBlank();
    }
}

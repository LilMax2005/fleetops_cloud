package com.fleetops.common.security;

import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.List;

/** Rechaza tokens emitidos para otra API (claim aud distinto a fleetops-api). */
public class AudienceValidator implements OAuth2TokenValidator<Jwt> {

    private final List<String> allowed;

    public AudienceValidator(List<String> allowed) {
        this.allowed = allowed;
    }

    @Override
    public OAuth2TokenValidatorResult validate(Jwt jwt) {
        if (allowed.isEmpty() || jwt.getAudience().stream().anyMatch(allowed::contains)) {
            return OAuth2TokenValidatorResult.success();
        }
        return OAuth2TokenValidatorResult.failure(
                new OAuth2Error("invalid_token", "Audiencia no permitida: " + jwt.getAudience(), null));
    }
}

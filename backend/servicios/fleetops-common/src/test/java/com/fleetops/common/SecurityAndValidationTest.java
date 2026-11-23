package com.fleetops.common;

import com.fleetops.common.events.TelemetryEvent;
import com.fleetops.common.kafka.EventValidator;
import com.fleetops.common.kafka.InvalidEventException;
import com.fleetops.common.security.AudienceValidator;
import com.fleetops.common.security.EntraIdAuthoritiesConverter;
import jakarta.validation.Validation;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SecurityAndValidationTest {

    private static Jwt jwt(String aud, String scp, List<String> roles) {
        Jwt.Builder b = Jwt.withTokenValue("t").header("alg", "RS256").audience(List.of(aud))
                .issuedAt(Instant.now()).expiresAt(Instant.now().plusSeconds(300));
        if (scp != null) b.claim("scp", scp);
        if (roles != null) b.claim("roles", roles);
        return b.build();
    }

    @Test
    void convierteScopesYRolesDeEntraId() {
        var authorities = new EntraIdAuthoritiesConverter()
                .convert(jwt("api", "api.access User.Read", List.of("FleetOps.Admin")))
                .stream().map(GrantedAuthority::getAuthority).toList();
        assertThat(authorities).containsExactlyInAnyOrder("SCOPE_api.access", "SCOPE_User.Read", "ROLE_FleetOps.Admin");
    }

    @Test
    void rechazaAudienciaAjena() {
        var validator = new AudienceValidator(List.of("api://fleetops-api"));
        assertThat(validator.validate(jwt("api://fleetops-api", "api.access", null)).hasErrors()).isFalse();
        assertThat(validator.validate(jwt("https://graph.microsoft.com", "api.access", null)).hasErrors()).isTrue();
    }

    @Test
    void validaPayloadDeTelemetria() {
        var validator = new EventValidator(Validation.buildDefaultValidatorFactory().getValidator());
        var ok = new TelemetryEvent("evt-1", "veh-1", "trip-1", -33.4, -70.6, 80.0, 50.0, 1200.0, Instant.now(), "test");
        assertThat(validator.validate(ok)).isSameAs(ok);

        var bad = new TelemetryEvent("evt-2", "", "trip-1", -133.4, -70.6, -5.0, 50.0, 1200.0, Instant.now(), "test");
        assertThatThrownBy(() -> validator.validate(bad))
                .isInstanceOf(InvalidEventException.class)
                .hasMessageContaining("latitude").hasMessageContaining("speedKmh").hasMessageContaining("vehicleId");
        assertThatThrownBy(() -> validator.validate(null)).isInstanceOf(InvalidEventException.class);
    }
}

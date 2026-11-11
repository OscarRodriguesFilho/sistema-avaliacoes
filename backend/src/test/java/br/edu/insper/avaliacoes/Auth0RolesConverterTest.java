package br.edu.insper.avaliacoes;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

class Auth0RolesConverterTest {
    private final Auth0RolesConverter converter = new Auth0RolesConverter("https://avaliacoes-api/roles");

    private static Jwt.Builder jwtBase() {
        return Jwt.withTokenValue("token").header("alg", "RS256").subject("auth0|123")
                .issuedAt(Instant.now()).expiresAt(Instant.now().plusSeconds(60));
    }

    @Test
    void converteRolesDoAuth0EmAuthorities() {
        Jwt jwt = jwtBase().claim("https://avaliacoes-api/roles", List.of("ADMIN", "user")).build();

        assertThat(converter.convert(jwt)).extracting(GrantedAuthority::getAuthority)
                .containsExactly("ROLE_ADMIN", "ROLE_USER");
    }

    @Test
    void semClaimDeRolesNaoTemAuthorities() {
        Jwt jwt = jwtBase().claim("scope", "openid").build();

        assertThat(converter.convert(jwt)).isEmpty();
    }
}

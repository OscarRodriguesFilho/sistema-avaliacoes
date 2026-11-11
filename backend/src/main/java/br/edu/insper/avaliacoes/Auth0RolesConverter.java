package br.edu.insper.avaliacoes;

import java.util.Collection;
import java.util.List;
import java.util.Locale;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

/**
 * Lê a claim de roles que a Action do Auth0 coloca no access token
 * (ex.: "https://avaliacoes-api/roles": ["ADMIN"]) e transforma em ROLE_ADMIN / ROLE_USER.
 */
public class Auth0RolesConverter implements Converter<Jwt, Collection<GrantedAuthority>> {
    private final String rolesClaim;

    public Auth0RolesConverter(String rolesClaim) {
        this.rolesClaim = rolesClaim;
    }

    @Override
    public Collection<GrantedAuthority> convert(Jwt jwt) {
        List<String> roles = jwt.getClaimAsStringList(rolesClaim);
        if (roles == null) {
            return List.of();
        }
        return roles.stream()
                .map(role -> (GrantedAuthority) new SimpleGrantedAuthority("ROLE_" + role.trim().toUpperCase(Locale.ROOT)))
                .toList();
    }
}

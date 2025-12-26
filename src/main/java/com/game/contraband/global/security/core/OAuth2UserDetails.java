package com.game.contraband.global.security.core;

import com.game.contraband.global.security.core.exception.UnsupportedSecurityOperationException;
import java.util.Collection;
import java.util.Set;
import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

@RequiredArgsConstructor
@EqualsAndHashCode(of = "id")
public class OAuth2UserDetails implements UserDetails {

    private final Long id;
    private final Set<GrantedAuthority> authorities;

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        throw new UnsupportedSecurityOperationException();
    }

    @Override
    public String getUsername() {
        return String.valueOf(id);
    }

    @Override
    public boolean isAccountNonExpired() {
        throw new UnsupportedSecurityOperationException();
    }

    @Override
    public boolean isAccountNonLocked() {
        throw new UnsupportedSecurityOperationException();
    }

    @Override
    public boolean isCredentialsNonExpired() {
        throw new UnsupportedSecurityOperationException();
    }

    @Override
    public boolean isEnabled() {
        throw new UnsupportedSecurityOperationException();
    }
}

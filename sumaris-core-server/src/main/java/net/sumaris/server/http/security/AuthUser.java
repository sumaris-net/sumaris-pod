package net.sumaris.server.http.security;

import net.sumaris.server.vo.security.AuthDataVO;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

/**
 * Authenticated user class implementing {@link UserDetails} for Spring security context
 *
 * @author peck7 on 03/12/2018.
 */
public class AuthUser implements UserDetails {

    private final AuthDataVO authData;
    private final List<? extends GrantedAuthority> authorities;

    AuthUser(AuthDataVO authData, List<? extends GrantedAuthority> authorities) {
        this.authData = authData;
        this.authorities = authorities;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return authData.asToken();
    }

    @Override
    public String getUsername() {
        return authData.getPubkey();
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}

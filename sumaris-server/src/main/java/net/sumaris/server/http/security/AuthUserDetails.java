/*
 * #%L
 * SUMARiS
 * %%
 * Copyright (C) 2019 SUMARiS Consortium
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 * #L%
 */

package net.sumaris.server.http.security;

import net.sumaris.server.util.security.AuthTokenVO;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;

/**
 * Authenticated user class implementing {@link UserDetails} for Spring security context
 *
 * @author peck7 on 03/12/2018.
 */
public class AuthUserDetails implements UserDetails {

    private final AuthTokenVO authData;
    private final Collection<? extends GrantedAuthority> authorities;

    public AuthUserDetails(AuthTokenVO authData, Collection<? extends GrantedAuthority> authorities) {
        this.authData = authData;
        this.authorities = authorities;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return authData != null ? authData.asToken() : null;
    }

    @Override
    public String getUsername() {
        return authData != null
            ? (authData.getUsername() != null ? authData.getUsername() : authData.getPubkey())
            : null;
    }

    public String getPubkey() {
        return authData != null ? authData.getPubkey() : null;
    }

    public String getChallenge() {
        return authData != null ? authData.getChallenge() : null;
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

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

import com.google.common.collect.ImmutableList;
import net.sumaris.core.model.referential.UserProfileEnum;
import net.sumaris.core.vo.administration.user.PersonVO;
import net.sumaris.server.security.IAuthService;
import net.sumaris.server.util.security.AuthTokenVO;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

public interface AuthService extends IAuthService<PersonVO> {

    String AUTHORITY_PREFIX = "ROLE_";

    List<String> PRIORITIZED_AUTHORITIES = ImmutableList.of("ROLE_ADMIN", "ROLE_SUPERVISOR", "ROLE_USER", "ROLE_GUEST");

    /**
     * Check in the security context, that user has the expected authority
     * @param role
     * @return
     */
    boolean hasAuthority(String authority);

    default boolean isLogin() {
        return getAuthenticatedUser().isPresent();
    }

    default boolean isGuest() {
        return hasAuthority(AUTHORITY_PREFIX + UserProfileEnum.GUEST.name());
    }

    default boolean isUser() {
        return hasAuthority(AUTHORITY_PREFIX + UserProfileEnum.USER.name());
    }

    default boolean isSupervisor() {
        return hasAuthority(AUTHORITY_PREFIX + UserProfileEnum.SUPERVISOR.name());
    }

    default boolean isAdmin() {
        return hasAuthority(AUTHORITY_PREFIX + UserProfileEnum.ADMIN.name());
    }

    AuthTokenVO createNewChallenge();

    @Transactional(readOnly = true)
    Optional<PersonVO> getAuthenticatedUser();

    @Transactional
    UserDetails authenticateByToken(String token) throws AuthenticationException;

    @Transactional
    UserDetails authenticateByUsername(String username, UsernamePasswordAuthenticationToken authentication) throws AuthenticationException;

}

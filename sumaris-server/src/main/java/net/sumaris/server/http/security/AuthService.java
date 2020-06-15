package net.sumaris.server.http.security;

/*-
 * #%L
 * SUMARiS:: Server
 * %%
 * Copyright (C) 2018 SUMARiS Consortium
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

import com.google.common.collect.ImmutableList;
import net.sumaris.core.vo.administration.user.PersonVO;
import net.sumaris.server.vo.security.AuthDataVO;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Transactional
public interface AuthService {

    List<String> PRIORITIZED_AUTHORITIES = ImmutableList.of("ROLE_ADMIN", "ROLE_SUPERVISOR", "ROLE_USER", "ROLE_GUEST");

    default boolean isSupervisor() {
        return hasAuthority("ROLE_SUPERVISOR");
    }

    default boolean isAdmin() {
        return hasAuthority("ROLE_ADMIN");
    }

    default boolean isUser() {
        return hasAuthority("ROLE_USER");
    }

    Optional<AuthUser> authenticate(String token);

    @Transactional(readOnly = true)
    AuthDataVO createNewChallenge();

    /**
     * Check in the security context, that user has the expected authority
     * @param role
     * @return
     */
    boolean hasAuthority(String authority);

    Optional<PersonVO> getAuthenticatedUser();

}

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

package net.sumaris.server.http.graphql.security;

import io.leangen.graphql.annotations.GraphQLArgument;
import io.leangen.graphql.annotations.GraphQLQuery;
import lombok.extern.slf4j.Slf4j;
import net.sumaris.core.model.administration.user.UserToken;
import net.sumaris.core.util.StringUtils;
import net.sumaris.server.http.graphql.GraphQLApi;
import net.sumaris.server.http.security.AuthService;
import net.sumaris.server.util.security.AuthTokenVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.ParseException;

@Service
@GraphQLApi
@Transactional
@Slf4j
public class AuthGraphQLService {

    @Autowired
    private AuthService authService;

    /* -- Authentication -- */

    @GraphQLQuery(name = "authenticate", description = "Authenticate using a token")
    public boolean authenticate(@GraphQLArgument(name = "token") String token) {
        try {
            authService.authenticateByToken(token);
            return true;
        }
        catch (AuthenticationException e) {
            try {
                String pubkey = AuthTokenVO.parse(token).getPubkey();
                if (StringUtils.isNotBlank(pubkey)) {
                    log.warn("Cannot authenticate user with pubkey {{}}: {}", pubkey, e.getMessage());
                }
                return false;
            } catch(ParseException pe) {
                // continue
            }
            log.warn(e.getMessage());
            return false;
        }

    }

    @GraphQLQuery(name = "authChallenge", description = "Ask for a new auth challenge")
    public AuthTokenVO newAuthChallenge() {
        return authService.createNewChallenge();
    }

}

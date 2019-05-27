package net.sumaris.server.http.graphql.security;

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

import io.leangen.graphql.annotations.*;
import net.sumaris.core.exception.SumarisTechnicalException;
import net.sumaris.server.exception.ErrorCodes;
import net.sumaris.server.http.security.AuthService;
import net.sumaris.server.vo.security.AuthDataVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class AuthGraphQLService {

    private static final Logger log = LoggerFactory.getLogger(AuthGraphQLService.class);

    @Autowired
    private AuthService authService;

    /* -- Authentication -- */

    @GraphQLQuery(name = "authenticate", description = "Authenticate using a token")
    public boolean authenticate( @GraphQLArgument(name = "token") String token) {
        if (!authService.authenticate(token).isPresent()) {
            log.warn("Invalid authentication token: " + token);
            //throw new SumarisTechnicalException(ErrorCodes.UNAUTHORIZED, "Invalid authentication token");
            return false;
        }
        return true;
    }

    @GraphQLQuery(name = "authChallenge", description = "Ask for a new auth challenge")
    @Transactional(readOnly = true)
    public AuthDataVO newAuthChallenge() {
        return authService.createNewChallenge();
    }

}

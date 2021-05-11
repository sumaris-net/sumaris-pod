package net.sumaris.server.http.graphql.security;

/*-
 * #%L
 * Sumaris3 Core :: Server
 * %%
 * Copyright (C) 2017 - 2020 Ifremer
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

import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import net.sumaris.core.exception.SumarisTechnicalException;
import net.sumaris.core.util.StringUtils;
import net.sumaris.server.http.graphql.AbstractGraphQLServiceTest;
import net.sumaris.server.util.security.AuthTokenVO;
import org.junit.Test;
import static org.junit.Assert.*;
@Slf4j
public class AuthGraphQLServiceTest extends AbstractGraphQLServiceTest {

    /**
     * Get authentication challenge from server.
     * The public key is composed with KEYPAIR_SALT and KEYPAIR_PASSWORD from SumarisServerConfigurationOption
     */
    @Test
    public void authChallenge() {
        AuthTokenVO auth = getResponse("authChallenge", AuthTokenVO.class);
        assertNotNull(auth);
        assertEquals("G2CBgZBPLe6FSFUgpx2Jf1Aqsgta6iib3vmDRA1yLiqU", auth.getPubkey());
        assertTrue(StringUtils.isNotEmpty(auth.getChallenge()));
        assertTrue(StringUtils.isNotEmpty(auth.getSignature()));
    }

    @Test
    public void authenticate() {

        // Authenticate with good credential
        assertTrue(doAuthenticate("admin@sumaris.net", "admin"));
        assertTrue(doAuthenticate("demo@sumaris.net", "demo"));
//        assertTrue(doAuthenticate("a1ed59", "q22006")); // = admq2 but extranet login

        // Authenticate with empty or bad credential
        assertThrows(SumarisTechnicalException.class, () -> doAuthenticate("admin@sumaris.net", "bad"));
        assertThrows(SumarisTechnicalException.class, () -> doAuthenticate("demo@sumaris.net", "null"));

        // Authenticate with unknown user
        assertThrows(SumarisTechnicalException.class, () -> doAuthenticate("unknown", "unknown"));

        // Authenticate with disabled user
        assertThrows(SumarisTechnicalException.class, () -> doAuthenticate("disable@sumaris.net", "demo"));

    }

    protected boolean doAuthenticate(String login, String password) throws SumarisTechnicalException {

        // Ask for challenge from server
        AuthTokenVO serverAuthData = getResponse("authChallenge", AuthTokenVO.class);
        assertNotNull(serverAuthData);
        assertNotNull(serverAuthData.getChallenge());
        assertNotNull(serverAuthData.getSignature());
        assertNotNull(serverAuthData.getPubkey());

        // Build user AuthData
        String token = createToken(serverAuthData.getChallenge(), login, password);

        ObjectNode variables = objectMapper.createObjectNode();
        variables.put("token", token);

        return getResponse("authenticate", Boolean.class, variables);
    }
}

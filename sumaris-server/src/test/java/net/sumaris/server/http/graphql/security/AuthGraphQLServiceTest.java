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
import net.sumaris.server.DatabaseResource;
import net.sumaris.server.http.graphql.AbstractGraphQLServiceTest;
import net.sumaris.server.util.security.AuthTokenVO;
import org.junit.ClassRule;
import org.junit.Test;
import org.springframework.http.HttpHeaders;

import static org.junit.Assert.*;
@Slf4j
public class AuthGraphQLServiceTest extends AbstractGraphQLServiceTest {

    @ClassRule
    public static final DatabaseResource dbResource = DatabaseResource.writeDb();

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
    public void authenticateValidUser() {
        // Authenticate with good credential
        assertTrue(authenticate("admin@sumaris.net", "admin"));
        assertTrue(authenticate("demo@sumaris.net", "demo"));
    }

    @Test
    public void authenticateBadCredentials() {
        // Authenticate with empty or bad credential
        assertFalse(authenticate("admin@sumaris.net", "bad"));
        assertFalse(authenticate("demo@sumaris.net", "null"));

    }

    @Test
    public void authenticateUnknownUser() {
        // Authenticate with unknown user
        assertFalse(authenticate("unknown", "unknown"));
    }

    @Test
    public void authenticateDisabledUser() {
        // Authenticate with disabled user
        assertFalse(authenticate("disable@sumaris.net", "demo"));
    }

}

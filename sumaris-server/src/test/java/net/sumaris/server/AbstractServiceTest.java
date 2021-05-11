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

package net.sumaris.server;

import net.sumaris.core.util.crypto.CryptoUtils;
import net.sumaris.core.util.crypto.KeyPair;
import net.sumaris.server.config.SumarisServerConfiguration;
import net.sumaris.server.service.crypto.ServerCryptoService;
import net.sumaris.server.util.security.AuthTokenVO;
import org.junit.Ignore;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.concurrent.Callable;

import static org.junit.Assert.fail;

@ActiveProfiles("test")
@RunWith(SpringRunner.class)
@SpringBootTest(classes = {ServiceTestConfiguration.class}, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(locations="classpath:sumaris-server-test.properties")
@Ignore
public abstract class AbstractServiceTest {

    @Autowired
    protected SumarisServerConfiguration configuration;

    @Autowired
    private ServerCryptoService cryptoService;

    /* -- Internal method -- */

    protected SumarisServerConfiguration getConfiguration() {
        return configuration;
    }

    protected String createToken(String challenge, String login, String password) {

        KeyPair userKeyPair = cryptoService.getKeyPair(login, password);
        String userPubkey = CryptoUtils.encodeBase58(userKeyPair.getPubKey());

        AuthTokenVO userAuthData = new AuthTokenVO();
        userAuthData.setPubkey(userPubkey);
        userAuthData.setChallenge(challenge);
        userAuthData.setSignature(cryptoService.sign(challenge, userKeyPair.getSecKey()));

        return userAuthData.asToken();
    }

    protected <T> T assertDoesNotThrow(Callable<T> caller) {
        try {
            return caller.call();
        }
        catch (Exception e) {
            e.printStackTrace();
            fail("Must not send exception, but get: " + e.getMessage());
        }
        return null;
    }
}

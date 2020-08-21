package net.sumaris.core.dao.administration;

/*-
 * #%L
 * SUMARiS:: Core
 * %%
 * Copyright (C) 2018 - 2019 SUMARiS Consortium
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

import net.sumaris.core.dao.AbstractDaoTest;
import net.sumaris.core.dao.DatabaseResource;
import net.sumaris.core.dao.administration.user.UserTokenRepository;
import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author peck7 on 15/10/2019.
 */
public class UserTokenRepositoryWriteTest extends AbstractDaoTest {

    @ClassRule
    public static final DatabaseResource dbResource = DatabaseResource.writeDb();

    private static final String OBSERVER_PUBKEY = "5rojwz7mTRFE9LCJXSGB2w48kcZtg7vM4SDQkN2s9GFe";
    private static final String FAKE_TOKEN_1 = "fake_token_1";
    private static final String FAKE_TOKEN_2 = "fake_token_2";

    @Override
    public void setUp() throws Exception {
        super.setUp();
        setCommitOnTearDown(false);
    }

    @Autowired
    private UserTokenRepository repository;


    @Test
    public void test() {

        Assert.assertFalse(repository.existsByTokenAndPubkey(FAKE_TOKEN_1, OBSERVER_PUBKEY));
        Assert.assertFalse(repository.existsByTokenAndPubkey(FAKE_TOKEN_2, OBSERVER_PUBKEY));

        List<String> tokens = repository.findTokenByPubkey(OBSERVER_PUBKEY) // demo observer
            .stream().map(UserTokenRepository.TokenOnly::getToken).collect(Collectors.toList());

        Assert.assertNotNull(tokens);
        Assert.assertTrue(tokens.isEmpty());

        // add 1 token
        repository.add(FAKE_TOKEN_1, OBSERVER_PUBKEY);

        Assert.assertTrue(repository.existsByTokenAndPubkey(FAKE_TOKEN_1, OBSERVER_PUBKEY));
        Assert.assertFalse(repository.existsByTokenAndPubkey(FAKE_TOKEN_2, OBSERVER_PUBKEY));

        tokens = repository.findTokenByPubkey(OBSERVER_PUBKEY) // demo observer
            .stream().map(UserTokenRepository.TokenOnly::getToken).collect(Collectors.toList());

        Assert.assertNotNull(tokens);
        Assert.assertEquals(1, tokens.size());
        Assert.assertEquals(FAKE_TOKEN_1, tokens.get(0));

        // add 2nd token
        repository.add(FAKE_TOKEN_2, OBSERVER_PUBKEY);

        Assert.assertTrue(repository.existsByTokenAndPubkey(FAKE_TOKEN_1, OBSERVER_PUBKEY));
        Assert.assertTrue(repository.existsByTokenAndPubkey(FAKE_TOKEN_2, OBSERVER_PUBKEY));

        tokens = repository.findTokenByPubkey(OBSERVER_PUBKEY) // demo observer
            .stream().map(UserTokenRepository.TokenOnly::getToken).collect(Collectors.toList());

        Assert.assertNotNull(tokens);
        Assert.assertEquals(2, tokens.size());
        Assert.assertTrue(tokens.containsAll(Arrays.asList(FAKE_TOKEN_1, FAKE_TOKEN_2)));

    }

}

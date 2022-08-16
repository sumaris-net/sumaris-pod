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
import net.sumaris.core.dao.administration.user.UserSettingsRepository;
import net.sumaris.core.vo.administration.user.UserSettingsVO;
import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Date;

/**
 * @author peck7 on 15/10/2019.
 */
public class UserSettingsRepositoryWriteTest extends AbstractDaoTest {

    @ClassRule
    public static final DatabaseResource dbResource = DatabaseResource.writeDb();

    private static final String OBSERVER_PUBKEY = "5rojwz7mTRFE9LCJXSGB2w48kcZtg7vM4SDQkN2s9GFe";
    private static final String FAKE_PUBKEY = "xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx";

    @Autowired
    private UserSettingsRepository repository;

    @Test
    public void getByIssuer() {
        UserSettingsVO settings = repository.findByIssuer(OBSERVER_PUBKEY).orElse(null);
        Assert.assertNotNull(settings);
        Assert.assertEquals("en", settings.getLocale());
        Assert.assertEquals("DDMM", settings.getLatLongFormat());
    }

    @Test
    public void createAndUpdate() {
        UserSettingsVO settings = new UserSettingsVO();
        settings.setIssuer(FAKE_PUBKEY);
        settings.setLocale("fr");
        settings.setLatLongFormat("DDMM");
        settings = repository.save(settings);
        Assert.assertNotNull(settings);
        Assert.assertNotNull(settings.getId());
        int settingId = settings.getId();
        Assert.assertNotNull(settings.getUpdateDate());
        Date settingUd = settings.getUpdateDate();
        Assert.assertEquals("fr", settings.getLocale());
        Assert.assertEquals("DDMM", settings.getLatLongFormat());
        Assert.assertNull(settings.getContent());

        commit();

        // reload
        settings = repository.findByIssuer(FAKE_PUBKEY).orElse(null);
        Assert.assertNotNull(settings);
        Assert.assertEquals(settingId, settings.getId().intValue());
        Assert.assertEquals(settingUd, settings.getUpdateDate());
        Assert.assertEquals("fr", settings.getLocale());
        Assert.assertEquals("DDMM", settings.getLatLongFormat());
        Assert.assertNull(settings.getContent());

        // modify
        settings.setLocale("en");
        settings.setContent("_");
        settings = repository.save(settings);
        Assert.assertNotNull(settings);
        Assert.assertEquals(settingId, settings.getId().intValue());
        Assert.assertNotEquals(settingUd, settings.getUpdateDate());
        Assert.assertEquals("en", settings.getLocale());
        Assert.assertEquals("DDMM", settings.getLatLongFormat());
        Assert.assertNotNull(settings.getContent());

    }

}

package net.sumaris.core.service.technical;

/*-
 * #%L
 * SUMARiS:: Core
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

import lombok.extern.slf4j.Slf4j;
import net.sumaris.core.dao.DatabaseResource;
import net.sumaris.core.model.referential.QualityFlagEnum;
import net.sumaris.core.model.referential.UserProfileEnum;
import net.sumaris.core.service.AbstractServiceTest;
import net.sumaris.core.service.administration.PersonService;
import net.sumaris.core.vo.administration.user.PersonVO;
import org.junit.*;
import org.junit.runners.MethodSorters;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

@Ignore("Use only on SFA Oracle database")
@ActiveProfiles("oracle")
@TestPropertySource(locations = "classpath:application-oracle.properties")
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@Slf4j
public class ConfigurationServiceOracleTest extends AbstractServiceTest {

    @ClassRule
    public static final DatabaseResource dbResource = DatabaseResource.writeDb("oracle");

    @Autowired
    private PersonService service;

    @Autowired
    private ConfigurationService configurationService;

    @Before
    public void setup() {
        // force apply software configuration
        configurationService.applySoftwareProperties();
    }

    @Test
    public void checkUserProfileEnumOverride() {
        // ADMIN label must be changed
        Assert.assertEquals("ALLEGRO_ADMINISTRATEUR", UserProfileEnum.ADMIN.getLabel());

        PersonVO person = service.getById(33); // jlucas
        Assert.assertNotNull(person);
        Assert.assertNotNull(person.getProfiles());
        Assert.assertTrue(person.getProfiles().size() > 1);
        // The ADMIN profile must be found
        Assert.assertTrue(person.getProfiles().stream().anyMatch(profile -> UserProfileEnum.ADMIN.toString().equals(profile)));

    }

    @Test
    public void checkQualityFlagEnumOverride() {

        Assert.assertEquals((Integer) 0, QualityFlagEnum.NOT_QUALIFIED.getId());
        Assert.assertEquals((Integer) 1, QualityFlagEnum.GOOD.getId());
        Assert.assertEquals((Integer) 2, QualityFlagEnum.OUT_STATS.getId());
        Assert.assertEquals((Integer) 3, QualityFlagEnum.DOUBTFUL.getId());
        Assert.assertEquals((Integer) 4, QualityFlagEnum.BAD.getId());
        Assert.assertEquals((Integer) 5, QualityFlagEnum.FIXED.getId());
        Assert.assertEquals((Integer) 6, QualityFlagEnum.NOT_COMPLETED.getId());
        Assert.assertEquals((Integer) 7, QualityFlagEnum.MISSING.getId());

    }
}

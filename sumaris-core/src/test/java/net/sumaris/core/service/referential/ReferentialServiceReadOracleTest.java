package net.sumaris.core.service.referential;

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

import net.sumaris.core.dao.DatabaseResource;
import net.sumaris.core.model.referential.StatusEnum;
import net.sumaris.core.model.referential.gradient.DistanceToCoastGradient;
import net.sumaris.core.model.referential.location.Location;
import net.sumaris.core.model.referential.location.LocationLevelEnum;
import net.sumaris.core.model.referential.metier.Metier;
import net.sumaris.core.service.AbstractServiceTest;
import net.sumaris.core.service.technical.ConfigurationService;
import net.sumaris.core.vo.filter.ReferentialFilterVO;
import net.sumaris.core.vo.referential.ReferentialVO;
import org.apache.commons.collections4.CollectionUtils;
import org.junit.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.util.List;

@ActiveProfiles("oracle")
@TestPropertySource(locations = "classpath:application-test-oracle.properties")
//@Ignore("Use only on Ifremer Oracle database")
public class ReferentialServiceReadOracleTest extends AbstractServiceTest{

    @ClassRule
    public static final DatabaseResource dbResource = DatabaseResource.readDb("oracle");

    @Autowired
    private ReferentialService service;

    @Autowired
    private ConfigurationService configurationService;

    @Before
    public void setUp() throws Exception {
        configurationService.applySoftwareProperties();
    }

    @Test
    public void findAllByLocationIds() {
        List<ReferentialVO> rectangles = service.findByFilter(Location.class.getSimpleName(),
            ReferentialFilterVO.builder()
            .label("24E5")
            .levelId(LocationLevelEnum.RECTANGLE_ICES.getId())
            .statusIds(new Integer[]{StatusEnum.ENABLE.getId()})
            .build(), 0, 1 );
        Assume.assumeFalse(rectangles.isEmpty());
        Integer icesRectangleId = rectangles.get(0).getId();


        // Metier
        {
            String entityName = Metier.ENTITY_NAME;
            Long countAll = service.count(entityName);

            List<ReferentialVO> items = service.findByFilter(entityName, ReferentialFilterVO.builder()
                .locationIds(new Integer[]{icesRectangleId})
                .build(), 0, 100);
            Assert.assertNotNull(items);
            Assert.assertTrue(CollectionUtils.isNotEmpty(items));
            Assert.assertTrue(CollectionUtils.size(items) < countAll);
        }

        // Distance to coast gradient
        {
            String entityName = DistanceToCoastGradient.ENTITY_NAME;
            Long countAll = service.count(entityName);

            List<ReferentialVO> items = service.findByFilter(entityName, ReferentialFilterVO.builder()
                .locationIds(new Integer[]{icesRectangleId})
                .build(), 0, 100);
            Assert.assertNotNull(items);
            Assert.assertTrue(CollectionUtils.isNotEmpty(items));
            Assert.assertTrue(CollectionUtils.size(items) < countAll);
        }
    }
}

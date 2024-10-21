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

import lombok.NonNull;
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
        // Metier
        {
            String entityName = Metier.ENTITY_NAME;
            Long countAll = service.count(entityName);

            // Get FAO Zone "27" (Atlantic North East)
            int atlanticNorthEastId = getLocationIdByLabel(LocationLevelEnum.AREA_FAO, "27", false);
            List<ReferentialVO> atlanticNorthEastMetiers = service.findByFilter(entityName, ReferentialFilterVO.builder()
                .locationIds(new Integer[]{atlanticNorthEastId})
                .build(), 0, 1000);
            Assert.assertNotNull(atlanticNorthEastMetiers);
            Assert.assertTrue(CollectionUtils.isNotEmpty(atlanticNorthEastMetiers));
            Assert.assertTrue(CollectionUtils.size(atlanticNorthEastMetiers) < countAll);

            // Get FAO Zone "37" (Mediterranean sea)
            int mediterraneanId = getLocationIdByLabel(LocationLevelEnum.AREA_FAO, "37", false);
            List<ReferentialVO> mediterraneanMetiers = service.findByFilter(entityName, ReferentialFilterVO.builder()
                .locationIds(new Integer[]{mediterraneanId})
                .build(), 0, 1000);
            Assert.assertNotNull(mediterraneanMetiers);
            Assert.assertTrue(CollectionUtils.isNotEmpty(mediterraneanMetiers));
            Assert.assertTrue(CollectionUtils.size(mediterraneanMetiers) < countAll);
            Assert.assertTrue(CollectionUtils.size(mediterraneanMetiers) < CollectionUtils.size(atlanticNorthEastMetiers));
        }

        // Distance to coast gradient
        {
            int icesRectangleId = getLocationIdByLabel(LocationLevelEnum.RECTANGLE_ICES, "24E5", true);

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

    private int getLocationIdByLabel(@NonNull LocationLevelEnum locationLevel, @NonNull String label, boolean onlyEnableStatus) {
        Assume.assumeTrue(String.format("LocationLevelEnum.%s not resolved. Please set enumeration option in config", locationLevel.name()), locationLevel.getId() != -1);
        List<ReferentialVO> rectangles = service.findByFilter(Location.class.getSimpleName(),
            ReferentialFilterVO.builder()
                .label(label)
                .levelIds(new Integer[]{locationLevel.getId()})
                .statusIds(onlyEnableStatus ? new Integer[]{StatusEnum.ENABLE.getId()} : null)
                .build(), 0, 1 );
        Assume.assumeFalse(String.format("Cannot found the %slocation with label '%s'",
            onlyEnableStatus ? "enabled ": "",
            label), rectangles.isEmpty());
        return rectangles.get(0).getId();
    }
}

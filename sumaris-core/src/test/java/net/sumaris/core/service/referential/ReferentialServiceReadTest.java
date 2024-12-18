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
import net.sumaris.core.dao.referential.ReferentialEntities;
import net.sumaris.core.model.referential.SaleType;
import net.sumaris.core.model.referential.Status;
import net.sumaris.core.model.referential.StatusEnum;
import net.sumaris.core.model.referential.gear.Gear;
import net.sumaris.core.model.referential.gradient.DepthGradient;
import net.sumaris.core.model.referential.gradient.DistanceToCoastGradient;
import net.sumaris.core.model.referential.gradient.NearbySpecificArea;
import net.sumaris.core.model.referential.location.Location;
import net.sumaris.core.model.referential.location.LocationLevelEnum;
import net.sumaris.core.model.referential.metier.Metier;
import net.sumaris.core.service.AbstractServiceTest;
import net.sumaris.core.vo.filter.ReferentialFilterVO;
import net.sumaris.core.vo.referential.ReferentialTypeVO;
import net.sumaris.core.vo.referential.ReferentialVO;
import org.apache.commons.collections4.CollectionUtils;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.ClassRule;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;

import java.util.Date;
import java.util.List;


@TestPropertySource(locations = "classpath:application-test-hsqldb.properties")
public class ReferentialServiceReadTest extends AbstractServiceTest{

    @ClassRule
    public static final DatabaseResource dbResource = DatabaseResource.readDb();

    @Autowired
    private ReferentialService service;

    @Test
    public void getLastUpdateDate() {
        Date date = service.getLastUpdateDate();
        Assert.assertNotNull(date);
    }

    @Test
    public void getAllTypes() {
        List<ReferentialTypeVO> types = service.getAllTypes();
        Assert.assertNotNull(types);
        Assert.assertEquals(ReferentialEntities.CLASSES.size(), types.size());
    }

    @Test
    public void get() {
        // find by entity class
        {
            ReferentialVO ref = service.get(Metier.class, 1);
            Assert.assertNotNull(ref);
            Assert.assertEquals("FAG_CAT", ref.getLabel());
            Assert.assertNotNull(ref.getLevelId());
            Assert.assertEquals(89, ref.getLevelId().intValue()); // gear_fk(aka level) = 89
        }
        // find by entity class name
        {
            ReferentialVO ref = service.get(Metier.class.getSimpleName(), 2);
            Assert.assertNotNull(ref);
            Assert.assertEquals("GNS_CAT", ref.getLabel());
            Assert.assertNotNull(ref.getLevelId());
            Assert.assertEquals(11, ref.getLevelId().intValue()); // gear_fk(aka level) = 89
        }
        // find invalid ref
        try {
            service.get(Metier.class.getSimpleName(), 999);
            Assert.fail("should throw exception");
        } catch (Exception e) {
            Assert.assertNotNull(e);
        }
    }

    @Test
    public void getAllLevels() {
        List<ReferentialVO> levels = service.getAllLevels(Location.class.getSimpleName());
        Assert.assertNotNull(levels);
        Assert.assertEquals(14, levels.size());
    }

    @Test
    public void getLevelById() {
        ReferentialVO level = service.getLevelById(Location.class.getSimpleName(), 1);
        Assert.assertNotNull(level);
        Assert.assertEquals("Country", level.getLabel());
    }

    @Test
    public void findByFilter() {
        ReferentialFilterVO filter = ReferentialFilterVO
                .builder()
                .searchText("FRA")
                .statusIds(new Integer[]{StatusEnum.ENABLE.getId()})
                .build();

        List<ReferentialVO> results = service.findByFilter(Location.class.getSimpleName(), filter, 0, 100);
        Assert.assertNotNull(results);
        Assert.assertTrue(CollectionUtils.isNotEmpty(results));

        filter.setLevelIds(new Integer[]{-999});
        results = service.findByFilter(Location.class.getSimpleName(), filter, 0, 100);
        Assert.assertTrue(CollectionUtils.isEmpty(results));
    }

    @Test
    public void findStatusByFilter() {
        // Status has no status ans no level but this filter should return all status
        ReferentialFilterVO filter = ReferentialFilterVO
                .builder()
                .statusIds(new Integer[]{StatusEnum.ENABLE.getId()})
                .build();

        List<ReferentialVO> results = service.findByFilter(Status.class.getSimpleName(), filter, 0, 100);
        Assert.assertNotNull(results);
        Assert.assertEquals(3, results.size());

        filter.setLevelIds(new Integer[]{-999});
        results = service.findByFilter(Status.class.getSimpleName(), filter, 0, 100);
        Assert.assertNotNull(results);
        Assert.assertEquals(3, results.size());

        // find label
        filter.setLabel("Actif");
        results = service.findByFilter(Status.class.getSimpleName(), filter, 0, 100);
        Assert.assertNotNull(results);
        Assert.assertEquals(1, results.size());
    }

    @Test
    public void findByFilterWithStatus() {
        ReferentialFilterVO filter = new ReferentialFilterVO();

        filter.setSearchText("FRA");
        filter.setStatusIds(new Integer[]{StatusEnum.DISABLE.getId()});

        List<ReferentialVO> results = service.findByFilter(Location.class.getSimpleName(), filter, 0, 100);
        Assert.assertTrue(CollectionUtils.isEmpty(results));
    }

    @Test
    public void count() {
        // count all
        Long count = service.count(Gear.class.getSimpleName());
        Assert.assertNotNull(count);
        Assert.assertEquals(95, count.longValue());

        // count by level
        count = service.countByLevelId(Gear.class.getSimpleName(), 1);
        Assert.assertNotNull(count);
        Assert.assertEquals(94, count.longValue());
        count = service.countByLevelId(Gear.class.getSimpleName(), 0);
        Assert.assertNotNull(count);
        Assert.assertEquals(0, count.longValue());
        count = service.countByLevelId(Location.class.getSimpleName(), 1);
        Assert.assertNotNull(count);
        Assert.assertEquals(4, count.longValue());
        count = service.countByLevelId(Location.class.getSimpleName(), 1, 2);
        Assert.assertNotNull(count);
        Assert.assertEquals(20, count.longValue());
        count = service.countByLevelId(Location.class.getSimpleName(), 1, 2, 3);
        Assert.assertNotNull(count);
        Assert.assertEquals(23, count.longValue());

        // count by filter
        count = service.countByFilter(Location.class.getSimpleName(), ReferentialFilterVO.builder().levelId(1).build());
        Assert.assertNotNull(count);
        Assert.assertEquals(60, count.longValue());
        count = service.countByFilter(Location.class.getSimpleName(), ReferentialFilterVO.builder().levelIds(new Integer[]{1,2}).build());
        Assert.assertNotNull(count);
        Assert.assertEquals(20, count.longValue());
        count = service.countByFilter(Location.class.getSimpleName(), ReferentialFilterVO.builder().label("FR").build());
        Assert.assertNotNull(count);
        Assert.assertEquals(0, count.longValue());
        count = service.countByFilter(Location.class.getSimpleName(), ReferentialFilterVO.builder().searchText("FR").build());
        Assert.assertNotNull(count);
        Assert.assertEquals(14, count.longValue());
    }

    @Test
    public void findByUniqueLabel() {
        ReferentialVO item = service.findByUniqueLabel(SaleType.class.getSimpleName(), "Fish auction");
        Assert.assertNotNull(item);
        Assert.assertNotNull(item.getId());
        Assert.assertEquals(1, item.getId().intValue());

        try {
            service.findByUniqueLabel(SaleType.class.getSimpleName(), "ZZZ");
            Assert.fail("should throw exception");
        } catch (Exception e) {
            Assert.assertNotNull(e);
        }
    }

    @Test
    public void findMetiersByLocationIds() {
        String entityName = Metier.ENTITY_NAME;
        Long countAll = service.count(entityName);

        // Get FAO Zone "27" (Atlantic North East)
        int atlanticNorthEastId = getLocationIdByLabel(LocationLevelEnum.AREA_FAO, "27", false);
        List<ReferentialVO> atlanticNorthEastItems = service.findByFilter(entityName, ReferentialFilterVO.builder()
            .locationIds(new Integer[]{atlanticNorthEastId})
            .build(), 0, 1000);
        Assert.assertNotNull(atlanticNorthEastItems);
        Assert.assertTrue(CollectionUtils.isNotEmpty(atlanticNorthEastItems));
        Assert.assertTrue(CollectionUtils.size(atlanticNorthEastItems) < countAll);

        // Get FAO Zone "57" (Indian Ocean East)
        int indianOceanEastId = getLocationIdByLabel(LocationLevelEnum.AREA_FAO, "57", false);
        List<ReferentialVO> indianOceanEastItems = service.findByFilter(entityName, ReferentialFilterVO.builder()
            .locationIds(new Integer[]{indianOceanEastId})
            .build(), 0, 1000);
        Assert.assertNotNull(indianOceanEastItems);
        Assert.assertTrue(CollectionUtils.isNotEmpty(indianOceanEastItems));
        Assert.assertTrue(CollectionUtils.size(indianOceanEastItems) < countAll);
        Assert.assertTrue(CollectionUtils.size(indianOceanEastItems) < CollectionUtils.size(atlanticNorthEastItems));
    }

    @Test
    public void findGradientByLocationIds() {

        // Distance To Coast Gradient
        {
            int icesRectangleId = getLocationIdByLabel(LocationLevelEnum.RECTANGLE_ICES, "24E4", true);
            String entityName = DistanceToCoastGradient.ENTITY_NAME;
            Long countAll = service.count(entityName);

            List<ReferentialVO> items = service.findByFilter(entityName, ReferentialFilterVO.builder()
                .locationIds(new Integer[]{icesRectangleId})
                .build(), 0, 100);
            Assert.assertNotNull(items);
            Assert.assertTrue(CollectionUtils.isNotEmpty(items));
            Assert.assertTrue("Should have less distance to coast gradient when using localization",CollectionUtils.size(items) < countAll);
        }

        // Depth Gradient
        {
            int icesRectangleId = getLocationIdByLabel(LocationLevelEnum.RECTANGLE_ICES, "24E4", true);
            String entityName = DepthGradient.ENTITY_NAME;
            Long countAll = service.count(entityName);

            List<ReferentialVO> items = service.findByFilter(entityName, ReferentialFilterVO.builder()
                .locationIds(new Integer[]{icesRectangleId})
                .build(), 0, 100);
            Assert.assertNotNull(items);
            // FIXME
            //Assert.assertTrue(CollectionUtils.isNotEmpty(items));
            //Assert.assertTrue("Should have less depth gradient when using localization", CollectionUtils.size(items) < countAll);
        }

        // Nearby Specific Area / La Réunion
        {
            int reunionSectorId = getLocationIdByLabel(151 /*Sector La Réunion*/, "RUNE3", false);
            String entityName = NearbySpecificArea.ENTITY_NAME;
            Long countAll = service.count(entityName);

            List<ReferentialVO> items = service.findByFilter(entityName, ReferentialFilterVO.builder()
                .locationIds(new Integer[]{reunionSectorId})
                .build(), 0, 100);
            Assert.assertNotNull(items);
            Assert.assertTrue(CollectionUtils.isNotEmpty(items));
            Assert.assertTrue("Should have less nearby specific area when using localization", CollectionUtils.size(items) < countAll);
        }
    }

    /* -- private functions -- */

    private int getLocationIdByLabel(@NonNull LocationLevelEnum locationLevel, @NonNull String label, boolean onlyEnableStatus) {
        Assume.assumeTrue(String.format("LocationLevelEnum.%s not resolved. Please set enumeration option in config", locationLevel.name()), locationLevel.getId() != -1);
        return getLocationIdByLabel(locationLevel.getId(), label, onlyEnableStatus);
    }


    private int getLocationIdByLabel(int locationLevelId, @NonNull String label, boolean onlyEnableStatus) {
        List<ReferentialVO> rectangles = service.findByFilter(Location.class.getSimpleName(),
            ReferentialFilterVO.builder()
                .label(label)
                .levelIds(new Integer[]{locationLevelId})
                .statusIds(onlyEnableStatus ? new Integer[]{StatusEnum.ENABLE.getId()} : null)
                .build(), 0, 1 );
        Assume.assumeFalse(String.format("Cannot found the %slocation with label '%s'",
            onlyEnableStatus ? "enabled ": "",
            label), rectangles.isEmpty());
        return rectangles.get(0).getId();
    }
}

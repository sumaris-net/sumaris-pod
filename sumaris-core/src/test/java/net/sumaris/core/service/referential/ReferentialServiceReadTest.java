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
import net.sumaris.core.dao.referential.ReferentialEntities;
import net.sumaris.core.model.referential.DistanceToCoastGradient;
import net.sumaris.core.model.referential.SaleType;
import net.sumaris.core.model.referential.Status;
import net.sumaris.core.model.referential.StatusEnum;
import net.sumaris.core.model.referential.gear.Gear;
import net.sumaris.core.model.referential.location.Location;
import net.sumaris.core.model.referential.metier.Metier;
import net.sumaris.core.service.AbstractServiceTest;
import net.sumaris.core.vo.filter.ReferentialFilterVO;
import net.sumaris.core.vo.referential.ReferentialTypeVO;
import net.sumaris.core.vo.referential.ReferentialVO;
import org.apache.commons.collections4.CollectionUtils;
import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Date;
import java.util.List;

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
    public void findByLocationIds() {
        String entityName = DistanceToCoastGradient.class.getSimpleName();
        Long countAll = service.countByFilter(entityName, null);

        int rectangle = fixtures.getRectangleId(0); // 65F1
        List<ReferentialVO> items = service.findByFilter(entityName, ReferentialFilterVO.builder()
                .locationIds(new Integer[]{rectangle})
                .build(), 0, 100);
        Assert.assertNotNull(items);
        Assert.assertTrue(CollectionUtils.isNotEmpty(items));
        Assert.assertTrue(CollectionUtils.size(items) < countAll);

    }

}

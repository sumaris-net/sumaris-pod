package net.sumaris.core.dao.referential;

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
import net.sumaris.core.dao.referential.location.LocationLevelRepository;
import net.sumaris.core.dao.referential.location.LocationRepository;
import net.sumaris.core.model.referential.location.Location;
import net.sumaris.core.model.referential.location.LocationLevel;
import net.sumaris.core.vo.filter.ReferentialFilterVO;
import net.sumaris.core.vo.referential.LocationVO;
import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

/**
 * @author peck7 on 15/10/2019.
 */
public class LocationRepositoryReadTest extends AbstractDaoTest {

    @ClassRule
    public static final DatabaseResource dbResource = DatabaseResource.readDb();

    @Autowired
    private LocationRepository locationRepository;

    @Autowired
    private LocationLevelRepository locationLevelRepository;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        setCommitOnTearDown(false);
    }

    @Test
    public void getLocationByLabel() {
        LocationVO location = locationRepository.findByLabel("FRA");
        Assert.assertNotNull(location);
        Assert.assertEquals("FRA", location.getLabel());
        location = locationRepository.findByLabel("BEL");
        Assert.assertNotNull(location);
        Assert.assertEquals("BEL", location.getLabel());
        location = locationRepository.findByLabel("ZZZZ");
        Assert.assertNull(location);

        location = locationRepository.getByLabel("FRA");
        Assert.assertNotNull(location);

        try {
            locationRepository.getByLabel("ZZZZ");
            Assert.fail("should throw exception");
        } catch (Exception e) {
            Assert.assertNotNull(e);
        }
    }

    @Test
    public void searchLocation() {
        // France
        assertFilterResult(ReferentialFilterVO.builder().label("FRA").build(), 1);
        // France
        assertFilterResult(ReferentialFilterVO.builder().searchAttribute(Location.Fields.LABEL).searchText("fra").build(), 1);

        // Port-en-Bessin, Saint-Quay-Portrieux and Port du Bloscon - Roscoff
        assertFilterResult(ReferentialFilterVO.builder().searchText("port").build(), 3);
        // Port-en-Bessin and Port du Bloscon - Roscoff (Saint-Quay-Portrieux not beginning by 'port', with search attribute the like parameter is more restrictive)
        assertFilterResult(ReferentialFilterVO.builder().searchAttribute(Location.Fields.NAME).searchText("port").build(), 2);

        // All countries by search
        assertFilterResult(ReferentialFilterVO.builder().searchJoin(Location.Fields.LOCATION_LEVEL).searchAttribute(LocationLevel.Fields.NAME).searchText("Country").build(), 4);
        // All countries by level
        assertFilterResult(ReferentialFilterVO.builder().levelId(1).build(), 4);
        // All countries and port by level
        assertFilterResult(ReferentialFilterVO.builder().levelIds(new Integer[]{1,2}).build(), 19);
    }

    private void assertFilterResult(ReferentialFilterVO filter, int expectedSize) {
        List<LocationVO> locations = locationRepository.findAll(filter);
        Assert.assertNotNull(locations);
        Assert.assertEquals(expectedSize, locations.size());
    }

    @Test
    public void findLocationLevel() {
        // Unknown level
        LocationLevel locationLevel = locationLevelRepository.findByLabel("AAA");
        Assert.assertNull(locationLevel);
        // Existing level
        locationLevel = locationLevelRepository.findByLabel("Country");
        Assert.assertNotNull(locationLevel);
        Assert.assertNotNull(locationLevel.getId());
        Assert.assertEquals(1, locationLevel.getId().intValue());
    }

}

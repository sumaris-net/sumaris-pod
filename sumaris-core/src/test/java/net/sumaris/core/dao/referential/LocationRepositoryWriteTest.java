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
import net.sumaris.core.dao.referential.location.LocationAreaRepository;
import net.sumaris.core.dao.referential.location.LocationLevelRepository;
import net.sumaris.core.model.referential.location.LocationArea;
import net.sumaris.core.model.referential.location.LocationLevel;
import net.sumaris.core.util.Geometries;
import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Date;

/**
 * @author peck7 on 15/10/2019.
 */
public class LocationRepositoryWriteTest extends AbstractDaoTest {

    @ClassRule
    public static final DatabaseResource dbResource = DatabaseResource.writeDb();

    @Override
    public void setUp() throws Exception {
        super.setUp();
        setCommitOnTearDown(false);
    }

    @Autowired
    private StatusRepository statusRepository;

    @Autowired
    private LocationAreaRepository locationAreaRepository;

    @Autowired
    private LocationLevelRepository locationLevelRepository;

    @Test
    @Ignore
    public void testGeometry() {

        LocationArea area = new LocationArea();
        area.setId(1); // France
        area.setPosition(Geometries.createPoint(-55,20));
        locationAreaRepository.save(area);
    }

    @Test
    public void saveLocationLevel() {
        LocationLevel locationLevel = new LocationLevel();
        locationLevel.setLabel("TEST LABEL");
        locationLevel.setName("TEST NAME");
        locationLevel.setCreationDate(new Date());
        locationLevel.setStatus(statusRepository.getEnableStatus());
        LocationLevel savedEntity = locationLevelRepository.save(locationLevel);
        Assert.assertNotNull(savedEntity);
    }
}

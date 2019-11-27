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
import net.sumaris.core.dao.referential.location.LocationAreaDao;
import net.sumaris.core.dao.referential.location.LocationDao;
import net.sumaris.core.model.referential.location.LocationArea;
import net.sumaris.core.util.Geometries;
import org.junit.ClassRule;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author peck7 on 15/10/2019.
 */
public class LocationWriteTest extends AbstractDaoTest {

    @ClassRule
    public static final DatabaseResource dbResource = DatabaseResource.writeDb();
//    public static final DatabaseResource dbResource = DatabaseResource.writeDb("oracle");

    @Autowired
    private LocationDao locationDao;

    @Autowired
    private LocationAreaDao locationAreaDao;

    @Test
    public void testGeometry() {

        LocationArea area = new LocationArea();
        area.setId(1);
        area.setLocation(locationDao.get(1)); // France
        area.setPosition(Geometries.createPoint(-55,20));
        locationAreaDao.saveAndFlush(area);
    }
}

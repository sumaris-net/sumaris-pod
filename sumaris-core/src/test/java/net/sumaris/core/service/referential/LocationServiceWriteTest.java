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
import net.sumaris.core.service.AbstractServiceTest;
import org.junit.ClassRule;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class LocationServiceWriteTest extends AbstractServiceTest{

    @Autowired
    private LocationService service;

    @ClassRule
    public static final DatabaseResource dbResource = DatabaseResource.writeDb();

    @Test
    public void a_insertOrUpdateRectangleLocations() {
        // Create rectangles
        service.insertOrUpdateRectangleLocations();

        // Insert geometry
        service.insertOrUpdateRectangleAndSquareAreas();

        //service.updateLocationHierarchy();
    }

    @Test
    public void updateLocationHierarchy() throws FileNotFoundException {
        service.updateLocationHierarchy();

        PrintStream ps = new PrintStream(new File("target/location_ports_tree-"+System.nanoTime()+".txt"));
        service.printLocationPorts(ps, " - ");
        ps.close();
    }

    /**
     * must be run in LocationServiceWriteTest because insertOrUpdateRectangleLocations must be run first
     */
    @Test
    public void getLocationIdByLatLong() {
        // Check label with a position inside the Atlantic sea
        Integer locationId = service.getLocationIdByLatLong(47.6f, -5.05f);
        assertNotNull("Location Id could not found in Allegro DB, in the Atlantic Sea. Bad enumeration value for RECTANGLE_STATISTIQUE ?", locationId);
        assertEquals(new Integer(937), locationId); // =id of location '24E4'

        // Check label with a position inside the Mediterranean sea
        locationId = service.getLocationIdByLatLong(42.27f, 5.4f);
        assertNotNull("Location Id could not found in Allegro DB, in the Mediterranean Sea. Bad enumeration value for RECTANGLE_STATISTIQUE_MED ?", locationId);
        assertEquals(new Integer(7650), locationId); // =id of location 'M24C2'
    }

}

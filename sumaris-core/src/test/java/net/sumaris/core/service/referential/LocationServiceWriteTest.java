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

    }

    @Test
    public void b_updateLocationHierarchy() throws FileNotFoundException {
        service.updateLocationHierarchy();

        PrintStream ps = new PrintStream("target/location_ports_tree-"+System.nanoTime()+".txt");
        printLocationPorts(ps, " - ");
        ps.close();
    }

    /**
     * /!\ Must be inside LocationServiceWriteTest because insertOrUpdateRectangleLocations must be run first
     */
    @Test
    public void getLocationIdByLatLong() {
        // Check label with a position inside the Atlantic sea
        Integer locationId = service.getLocationIdByLatLong(47.6f, -5.05f);
        assertNotNull("Location Id could not found in DB, in the Atlantic Sea. Bad enumeration value for LocationLevelEnum.RECTANGLE_ICES ?", locationId);
        assertEquals(new Integer(115), locationId); // =id of location '24E4'

        // Check label with a position inside the Mediterranean sea
        locationId = service.getLocationIdByLatLong(42.27f, 5.4f);
        assertNotNull("Location Id could not found in DB, in the Mediterranean Sea. Bad enumeration value for LocationLevelEnum.RECTANGLE_GFCM ?", locationId);
        assertEquals(new Integer(140), locationId); // =id of location 'M24C2'
    }

    protected void printLocationPorts(PrintStream out, String indentation) {
        // FIXME porter le code DAO en Repository
        /*Preconditions.checkArgument(StringUtils.isNotBlank(indentation));
        Preconditions.checkNotNull(out);

        Map<String, LocationLevel> locationLevels = createAndGetLocationLevels(ImmutableMap.<String, String>builder()
                .put(LocationLevelLabels.HARBOUR, "Port")
                .put(LocationLevelLabels.COUNTRY, "Country")
                .build());

        LocationLevel countryLocationLevel = locationLevels.find(LocationLevelLabels.COUNTRY);
        LocationLevel portLocationLevel = locationLevels.find(LocationLevelLabels.HARBOUR);

        List<Integer> processedPorts = Lists.newArrayList();
        List<Location> countries = locationDao.getLocationByLocationLevel(countryLocationLevel.getId());
        for (Location country: countries) {
            if ("1".equals(country.getValidityStatus().getCode())) {
                out.println(String.format("%s - %s (%s)", country.getLabel(), country.getName(), country.getId()));

                List<Location> nuts3list = locationDao.getLocationByLevelAndParent(
                        nut3LocationLevel.getId(),
                        country.getId());
                for (Location nuts3: nuts3list) {
                    if ("1".equals(nuts3.getValidityStatus().getCode())) {
                        out.println(String.format("%s%s - %s (%s)", indentation, nuts3.getLabel(), nuts3.getName(), nuts3.getId()));

                        List<Location> ports = locationDao.getLocationByLevelAndParent(
                                portLocationLevel.getId(),
                                nuts3.getId());
                        for (Location port: ports) {
                            if ("1".equals(port.getValidityStatus().getCode())) {
                                out.println(String.format("%s%s%s - %s (%s)", indentation, indentation, port.getLabel(), port.getName(), port.getId()));
                                processedPorts.add(port.getId());
                            }
                        }
                    }
                }

                // Port sans NUTS 3 :
                List<Location> ports = locationDao.getLocationByLevelAndParent(
                        portLocationLevel.getId(),
                        country.getId());
                boolean firstActivePort = true;
                for (Location port: ports) {

                    if ("1".equals(port.getValidityStatus().getCode())
                            && !processedPorts.contains(port.getId())) {
                        if (firstActivePort) {
                            out.println(String.format("%sSANS NUTS3 (ou sans nuts3 valide):", indentation));
                            firstActivePort = false;
                        }
                        out.println(String.format("%s%s%s - %s (%s)", indentation, indentation, port.getLabel(), port.getName(), port.getId()));
                        processedPorts.add(port.getId());
                    }
                }

            }
        }

        // Port sans pays :
        List<Location> ports = locationDao.getLocationByLocationLevel(
                portLocationLevel.getId());
        boolean firstActivePort = true;
        for (Location port: ports) {

            if ("1".equals(port.getValidityStatus().getCode())
                    && !processedPorts.contains(port.getId())) {
                if (firstActivePort) {
                    out.println(String.format("SANS PAYS :", indentation));
                    out.println(String.format("%sSANS NUTS3 :", indentation));
                    firstActivePort = false;
                }
                out.println(String.format("%s%s%s - %s (%s)", indentation, indentation, port.getLabel(), port.getName(), port.getId()));
                processedPorts.add(port.getId());
            }
        }*/
    }
}

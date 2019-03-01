package net.sumaris.core.service.referential;

/*
 * #%L
 * SIH-Adagio :: Core
 * $Id:$
 * $HeadURL:$
 * %%
 * Copyright (C) 2012 - 2014 Ifremer
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * #L%
 */

import net.sumaris.core.dao.DatabaseResource;
import net.sumaris.core.service.AbstractServiceTest;
import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class LocationServiceReadTest extends AbstractServiceTest {

	@Autowired
	private LocationService service;

	@ClassRule
	public static final DatabaseResource dbResource = DatabaseResource.readDb();

	@Test
	public void getLocationLabelByLatLong() {
		// Check label with a position inside the Atlantic sea
		String label = service.getLocationLabelByLatLong(47.6f, -5.05f);
		assertEquals("24E4", label);

		// Check label with a position inside the Mediterranean sea
		label = service.getLocationLabelByLatLong(42.27f, 5.4f);
		assertEquals("M24C2", label);
	}

	@Test
	@Ignore
	public void getLocationIdByLatLong() {
		// Check label with a position inside the Atlantic sea
		Integer locationId = service.getLocationIdByLatLong(47.6f, -5.05f);
		assertNotNull("Location Id could not found in Allegro DB, in the Atlantic Sea. Bad enumeration value for RECTANGLE_STATISTIQUE ?", locationId);
		// FIXME: change the ID
		assertEquals(new Integer(6080), locationId); // =id of location '24E4'

		// Check label with a position inside the Mediterranean sea
		locationId = service.getLocationIdByLatLong(42.27f, 5.4f);
		assertNotNull("Location Id could not found in Allegro DB, in the Mediterranean Sea. Bad enumeration value for RECTANGLE_STATISTIQUE_MED ?",
				locationId);
		// FIXME: change the ID
		assertEquals(new Integer(18648), locationId); // =id of location 'M24C2'
	}


}

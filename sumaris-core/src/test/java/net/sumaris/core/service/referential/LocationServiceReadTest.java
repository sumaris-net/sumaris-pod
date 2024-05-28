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
import net.sumaris.core.model.referential.location.LocationLevelEnum;
import net.sumaris.core.service.AbstractServiceTest;
import net.sumaris.core.vo.filter.LocationFilterVO;
import net.sumaris.core.vo.referential.LocationVO;
import org.junit.Assume;
import org.junit.ClassRule;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Objects;

import static org.junit.Assert.*;

public class LocationServiceReadTest extends AbstractServiceTest {

	@ClassRule
	public static final DatabaseResource dbResource = DatabaseResource.readDb();

	@Autowired
	private LocationService service;

	@Test
	public void getLocationLabelByLatLong() {
		// Check label with a position inside the Atlantic sea
		String label = service.getStatisticalRectangleLabelByLatLong(47.6f, -5.05f).orElse(null);
		assertEquals("24E4", label);

		// Check label with a position inside the Mediterranean sea
		label = service.getStatisticalRectangleLabelByLatLong(42.27f, 5.4f).orElse(null);
		assertEquals("M24C2", label);
	}

	@Test
	public void findByFilterName() {
		LocationVO existingLocation = service.get(fixtures.getLocationPortId(0));
		Assume.assumeNotNull(existingLocation);
		Assume.assumeNotNull(existingLocation.getName());
		Assume.assumeTrue(Objects.equals(existingLocation.getStatusId(), StatusEnum.ENABLE.getId()));

		LocationFilterVO filter = LocationFilterVO.builder()
			.statusIds(new Integer[]{StatusEnum.ENABLE.getId(), StatusEnum.TEMPORARY.getId()})
			.levelIds(new Integer[]{LocationLevelEnum.HARBOUR.getId()})
			.name(existingLocation.getName())
			.build();

		List<LocationVO> matches = service.findByFilter(filter);
		assertNotNull(matches);
		assertEquals(1, matches.size());
		LocationVO match = matches.get(0);
		assertEquals(existingLocation.getId(), match.getId());
	}

	@Test
	public void findByFilterDescendants() {
		LocationVO portLocation = service.get(fixtures.getLocationPortId(0));
		Assume.assumeNotNull(portLocation);
		Assume.assumeNotNull(portLocation.getName());
		Assume.assumeTrue(Objects.equals(portLocation.getStatusId(), StatusEnum.ENABLE.getId()));

		LocationFilterVO filter = LocationFilterVO.builder()
			.statusIds(new Integer[]{StatusEnum.ENABLE.getId(), StatusEnum.TEMPORARY.getId()})
			.levelIds(new Integer[]{LocationLevelEnum.COUNTRY.getId()})
			.descendantIds(new Integer[]{portLocation.getId()})
			.build();

		List<LocationVO> matches = service.findByFilter(filter);
		assertNotNull(matches);
		assertEquals(1, matches.size());
		LocationVO match = matches.get(0);
		assertEquals("FRA", match.getLabel());
	}


	@Test
	public void findByFilterAncestors() {
		LocationVO countryLocation = service.get(fixtures.getLocationCountryId(0)); // FRA
		Assume.assumeNotNull(countryLocation);
		Assume.assumeNotNull(countryLocation.getName());
		Assume.assumeTrue(Objects.equals(countryLocation.getStatusId(), StatusEnum.ENABLE.getId()));

		LocationFilterVO filter = LocationFilterVO.builder()
			.statusIds(new Integer[]{StatusEnum.ENABLE.getId(), StatusEnum.TEMPORARY.getId()})
			.levelIds(new Integer[]{LocationLevelEnum.HARBOUR.getId()})
			.ancestorIds(new Integer[]{countryLocation.getId()})
			.build();

		List<LocationVO> matches = service.findByFilter(filter);
		assertNotNull(matches);
		assertTrue(matches.size() > 10);
		matches.forEach(portLocation -> {
			assertNotNull(portLocation);
			assertNotNull(portLocation.getLabel());
			assertTrue(portLocation.getLabel().startsWith("FR")); // FR port
		});
	}
}

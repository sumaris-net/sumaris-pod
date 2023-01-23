package net.sumaris.core.service.referential.conversion;

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
import net.sumaris.core.model.referential.location.LocationLevels;
import net.sumaris.core.service.AbstractServiceTest;
import net.sumaris.core.service.referential.LocationService;
import net.sumaris.core.vo.filter.LocationFilterVO;
import net.sumaris.core.vo.referential.conversion.WeightLengthConversionFetchOptions;
import net.sumaris.core.vo.referential.conversion.WeightLengthConversionFilterVO;
import net.sumaris.core.vo.referential.conversion.WeightLengthConversionVO;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.ClassRule;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

public class RoundWeightConversionServiceReadTest extends AbstractServiceTest {

	@ClassRule
	public static final DatabaseResource dbResource = DatabaseResource.readDb();

	@Autowired
	private WeightLengthConversionService service;

	@Autowired
	private LocationService locationService;

	@Test
	public void countByFilter() {

		// Count all
		long countAll = service.countByFilter(null);
		Assert.assertTrue(countAll > 0);
	}

	@Test
	public void findByFilter() {

		// Count all
		long countAll = service.countByFilter(null);
		Assume.assumeTrue(countAll > 0);

		WeightLengthConversionFetchOptions fetchOptions = WeightLengthConversionFetchOptions.builder()
			.withRectangleLabels(true)
			.withLocation(true)
			.build();

		// Filter on status
		{
			List<WeightLengthConversionVO> result = service.findByFilter(WeightLengthConversionFilterVO.builder()
				.statusIds(new Integer[]{StatusEnum.DISABLE.getId()})
				.build(), null, fetchOptions);
			Assert.assertNotNull(result);
			Assert.assertTrue(result.size() < countAll);

			assertAllValid(result, fetchOptions);
		}

		// Filter by reference taxon
		{
			List<WeightLengthConversionVO> result = service.findByFilter(WeightLengthConversionFilterVO.builder()
				.referenceTaxonIds(new Integer[]{fixtures.getReferenceTaxonIdCOD()})
				.build(), null, fetchOptions);
			Assert.assertNotNull(result);
			int count = result.size();
			Assert.assertTrue(count > 0);
			Assert.assertTrue(count < countAll);

			assertAllValid(result, fetchOptions);
		}
	}

	protected void assertAllValid(List<WeightLengthConversionVO> sources, WeightLengthConversionFetchOptions fetchOptions) {
		sources.forEach(s -> this.assertValid(s, fetchOptions));
	}

	protected void assertValid(WeightLengthConversionVO source, WeightLengthConversionFetchOptions fetchOptions) {
		Assert.assertNotNull(source);
		Assert.assertNotNull(source.getId());
		Assert.assertNotNull(source.getLocationId());
		Assert.assertNotNull(source.getStatusId());

		if (fetchOptions.isWithLocation()) {
			Assert.assertNotNull(source.getLocation());
		}

		if (fetchOptions.isWithRectangleLabels()) {
			Assert.assertNotNull(String.format("WeightLengthConversion (id=%s) has no corresponding statistical rectangle ", source.getId()),
				source.getRectangleLabels());

			// Make no sens to have no rectangle
			long rectangleCount = source.getRectangleLabels().length;
			Assert.assertTrue(rectangleCount > 0 || hasNoRectangle(source.getLocationId()));
		}
	}

	private boolean hasNoRectangle(int locationId) {
		return locationService.countByFilter(LocationFilterVO.builder()
				.ancestorIds(new Integer[]{locationId})
				.levelIds(LocationLevels.getStatisticalRectangleLevelIds())
			.build()) == 0L;
	}
}

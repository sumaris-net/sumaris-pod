package net.sumaris.core.service.data.denormalize;

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

import lombok.extern.slf4j.Slf4j;
import net.sumaris.core.dao.DatabaseResource;
import net.sumaris.core.service.AbstractServiceTest;
import net.sumaris.core.util.TimeUtils;
import net.sumaris.core.vo.filter.TripFilterVO;
import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

@Slf4j
public class DenormalizedTripServiceTest extends AbstractServiceTest{

    @ClassRule
    public static final DatabaseResource dbResource = DatabaseResource.writeDb();

    @Autowired
    private DenormalizedTripService service;

    @Test
    public void denormalizeById() {

        long startTime = System.currentTimeMillis();
        DenormalizedTripResultVO result = service.denormalizeById(fixtures.getTripIdWithBatches());

        // Observers
        Assert.assertNotNull(result);
        Assert.assertTrue(result.getOperationCount() > 0);

        log.info("Denormalized {} operations in {}", result.getOperationCount(), TimeUtils.printDurationFrom(startTime));
    }

    @Test
    public void denormalizeByFilter() {

        DenormalizedTripResultVO result = service.denormalizeByFilter(TripFilterVO.builder()
            .tripId(fixtures.getTripIdWithBatches())
            .build());

        // Observers
        Assert.assertNotNull(result);
        Assert.assertTrue(result.getOperationCount() > 0);

    }
}

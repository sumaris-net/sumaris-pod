package net.sumaris.core.service.data;

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
import net.sumaris.core.util.Beans;
import net.sumaris.core.util.Dates;
import net.sumaris.core.vo.data.DataFetchOptions;
import net.sumaris.core.vo.data.OperationVO;
import net.sumaris.core.vo.data.PhysicalGearVO;
import net.sumaris.core.vo.data.TripVO;
import net.sumaris.core.vo.data.batch.BatchVO;
import net.sumaris.core.vo.data.sample.SampleVO;
import net.sumaris.core.vo.filter.TripFilterVO;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.text.ParseException;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

public class TripServiceReadTest extends AbstractServiceTest{

    @ClassRule
    public static final DatabaseResource dbResource = DatabaseResource.readDb();

    @Autowired
    private TripService service;

    @Test
    public void findTrips() throws ParseException {

        Date tripDay = Dates.parseDate("2018-03-03", "yyyy-MM-dd");
        assertFindResultCount(TripFilterVO.builder()
            .startDate(Dates.resetTime(tripDay))
            .endDate(Dates.lastSecondOfTheDay(tripDay))
            .build(),
            1);

        assertFindResultCount(TripFilterVO.builder()
            .startDate(Dates.parseDate("2018-01-01", "yyyy-MM-dd"))
            .endDate(Dates.parseDate("2018-03-30", "yyyy-MM-dd"))
            .build(),
            1);

        assertFindResultCount(TripFilterVO.builder()
            .startDate(Dates.parseDate("2018-01-01", "yyyy-MM-dd"))
            .endDate(Dates.parseDate("2018-05-30", "yyyy-MM-dd"))
            .build(),
            2);

        assertFindResultCount(TripFilterVO.builder()
            .startDate(Dates.parseDate("2018-02-28", "yyyy-MM-dd"))
            .endDate(Dates.parseDate("2018-04-18", "yyyy-MM-dd"))
            .build(),
            2);

        assertFindResultCount(TripFilterVO.builder()
            .startDate(Dates.parseDate("2018-02-28", "yyyy-MM-dd"))
            .build(),
            3);

        assertFindResultCount(TripFilterVO.builder()
            .startDate(Dates.parseDate("2018-03-03", "yyyy-MM-dd"))
            .build(),
            3);

        assertFindResultCount(TripFilterVO.builder()
            .startDate(Dates.parseDate("2018-03-04", "yyyy-MM-dd"))
            .build(),
            2);

        assertFindResultCount(TripFilterVO.builder()
            .endDate(Dates.parseDate("2018-03-04", "yyyy-MM-dd"))
            .build(),
            1);

        assertFindResultCount(TripFilterVO.builder()
            .endDate(Dates.parseDate("2018-04-20", "yyyy-MM-dd"))
            .build(),
            2);

        assertFindResultCount(TripFilterVO.builder()
            .recorderDepartmentId(1)
            .build(),
            1);

        assertFindResultCount(TripFilterVO.builder()
            .recorderPersonId(2)
            .build(),
            2);
    }


    @Test
    public void getFullGraph() {

        Integer id = fixtures.getTripId(0);
        TripVO trip = service.get(id, DataFetchOptions.FULL_GRAPH);
        Assert.assertNotNull(trip);
        Assert.assertNotNull(trip.getVesselSnapshot());
        Assert.assertNotNull(trip.getVesselSnapshot().getExteriorMarking());

        // PhysicalGear
        {
            Assert.assertTrue(CollectionUtils.isNotEmpty(trip.getGears()));

            // Gear Measurement
            Assert.assertTrue(trip.getGears()
                    .stream()
                    .map(PhysicalGearVO::getMeasurementValues)
                    .anyMatch(MapUtils::isNotEmpty));

        }


        // Check operations
        {
            Assert.assertTrue(CollectionUtils.isNotEmpty(trip.getOperations()));

            // Check positions
            {
                Assert.assertTrue(trip.getOperations()
                        .stream().map(OperationVO::getPositions).anyMatch(CollectionUtils::isNotEmpty));
            }

            // Measurements
            {
                Assert.assertTrue(trip.getOperations()
                        .stream()
                        .flatMap(o -> Stream.concat(Beans.getStream(o.getMeasurements()), Beans.getStream(o.getGearMeasurements())))
                        .findAny()
                        .isPresent());
            }
            // Check samples
            {
                Assert.assertTrue(trip.getOperations()
                        .stream().map(OperationVO::getSamples).anyMatch(CollectionUtils::isNotEmpty));

                Assert.assertTrue(trip.getOperations()
                        .stream()
                        .map(OperationVO::getSamples)
                        .flatMap(Beans::getStream)
                        .map(SampleVO::getMeasurementValues)
                        .anyMatch(MapUtils::isNotEmpty));

                // Check batches parentId
                Assert.assertTrue(trip.getOperations()
                        .stream()
                        .map(OperationVO::getSamples)
                        .flatMap(Beans::getStream)
                        .map(SampleVO::getParentId)
                        .anyMatch(Objects::nonNull));
            }

            // Check batches
            {
                Assert.assertTrue(trip.getOperations()
                        .stream().map(OperationVO::getBatches)
                        .anyMatch(CollectionUtils::isNotEmpty));

                // Check batches measurements
                Assert.assertTrue(trip.getOperations()
                        .stream()
                        .map(OperationVO::getBatches)
                        .flatMap(Beans::getStream)
                        .map(BatchVO::getMeasurementValues)
                        .anyMatch(MapUtils::isNotEmpty));

                // Check batches parentId
                Assert.assertTrue(trip.getOperations()
                        .stream()
                        .map(OperationVO::getBatches)
                        .flatMap(Beans::getStream)
                        .map(BatchVO::getParentId)
                        .anyMatch(Objects::nonNull));
            }
        }
    }

    private void assertFindResultCount(TripFilterVO filter, int expectedSize) {
        List<TripVO> trips = service.findByFilter(filter, 0, 100);
        Assert.assertNotNull(trips);
        Assert.assertEquals(expectedSize, trips.size());
    }

}

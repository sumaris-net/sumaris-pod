/*
 * #%L
 * SUMARiS
 * %%
 * Copyright (C) 2019 SUMARiS Consortium
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

package net.sumaris.extraction.core.service.hsqldb;

import net.sumaris.core.service.data.TripService;
import net.sumaris.core.vo.data.TripVO;
import net.sumaris.extraction.core.DatabaseResource;
import net.sumaris.extraction.core.config.ExtractionConfiguration;
import net.sumaris.extraction.core.service.ExtractionServiceTest;
import net.sumaris.extraction.core.specification.data.trip.RdbSpecification;
import net.sumaris.extraction.core.type.LiveExtractionTypeEnum;
import net.sumaris.extraction.core.vo.trip.ExtractionTripFilterVO;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.ClassRule;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.File;
import java.io.IOException;

/**
 * @author Benoit LAVENIER <benoit.lavenier@e-is.pro>
 */
public class ExtractionServiceHsqlDbTest extends ExtractionServiceTest {

    @ClassRule
    public static final DatabaseResource dbResource = DatabaseResource.writeDb();

    @Autowired
    protected ExtractionConfiguration extractionConfiguration;

    @Autowired
    protected TripService tripService;

    @Test
    public void executeRdbWithDenormalisation() throws IOException {

        // Enable batch optimization, in extraction
        extractionConfiguration.setEnableBatchDenormalization(true);
        Assert.assertTrue(extractionConfiguration.enableBatchDenormalization());

        // Create filter for a trip (APASE)
        ExtractionTripFilterVO filter = createFilterForTrip(fixtures.getTripIdByProgramLabel("APASE"));

        // TODO remove this
        filter.setSheetName(RdbSpecification.SL_SHEET_NAME);
        filter.setPreview(true);

        // Test the RDB format
        File outputFile = service.executeAndDumpTrips(LiveExtractionTypeEnum.RDB, filter);
        Assert.assertTrue(outputFile.exists());
        File root = unpack(outputFile, LiveExtractionTypeEnum.RDB.getLabel());

        // TR.csv
        {
            File tripFile = new File(root, RdbSpecification.TR_SHEET_NAME + ".csv");
            Assert.assertTrue(countLineInCsvFile(tripFile) > 1);
        }

        // HH.csv
        {
            File stationFile = new File(root, RdbSpecification.HH_SHEET_NAME + ".csv");
            Assert.assertTrue(countLineInCsvFile(stationFile) > 1);

            // Make sure this column exists (column with a 'dbms' attribute)
            assertHasColumn(stationFile, RdbSpecification.COLUMN_FISHING_TIME);
        }

        // SL.csv
        {
            File speciesListFile = new File(root, RdbSpecification.SL_SHEET_NAME + ".csv");
            Assert.assertTrue(countLineInCsvFile(speciesListFile) > 1);

            // Make sure this column exists (column with a 'dbms' attribute)
            assertHasColumn(speciesListFile, RdbSpecification.COLUMN_WEIGHT);
        }
    }

    protected ExtractionTripFilterVO createFilterForTrip(int tripId) {
        TripVO trip = loadAndValidateTripById(70 /*APASE trip*/);

        // Create extraction filter
        ExtractionTripFilterVO filter = new ExtractionTripFilterVO();
        filter.setProgramLabel(trip.getProgram().getLabel());
        filter.setTripId(trip.getId());

        return filter;
    }

    protected TripVO loadAndValidateTripById(int tripId) {
        // Load
        TripVO trip = tripService.get(tripId);
        Assume.assumeNotNull(trip);

        // Control
        if (trip.getControlDate() == null) {
            trip = tripService.control(trip);
            Assume.assumeNotNull(trip);
            Assume.assumeNotNull(trip.getControlDate());
        }

        // Validate
        if (trip.getValidationDate() == null) {
            trip = tripService.validate(trip);
            Assume.assumeNotNull(trip);
            Assume.assumeNotNull(trip.getValidationDate());
        }

        return trip;
    }
}
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

package net.sumaris.extraction.core.service.oracle;

import lombok.extern.slf4j.Slf4j;
import net.sumaris.core.model.administration.programStrategy.ProgramEnum;
import net.sumaris.core.service.technical.ConfigurationService;
import net.sumaris.core.util.Files;
import net.sumaris.core.vo.filter.VesselFilterVO;
import net.sumaris.extraction.core.DatabaseResource;
import net.sumaris.extraction.core.service.AbstractServiceTest;
import net.sumaris.extraction.core.service.ExtractionService;
import net.sumaris.extraction.core.specification.data.trip.PmfmTripSpecification;
import net.sumaris.extraction.core.type.LiveExtractionTypeEnum;
import net.sumaris.extraction.core.vo.trip.ExtractionTripFilterVO;
import org.junit.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.io.File;
import java.io.IOException;

/**
 * @author peck7 on 17/12/2018.
 */
@Slf4j
@Ignore("Use only SFA Oracle database")
@ActiveProfiles("oracle")
@TestPropertySource(locations = "classpath:application-oracle-sfa.properties")
public class ExtractionServiceOracleSFATest extends AbstractServiceTest {
    @ClassRule
    public static final DatabaseResource dbResource = DatabaseResource.writeDb("oracle-sfa");

    @Autowired
    protected ExtractionService service;

    @Autowired
    protected ConfigurationService configurationService;

    @Before
    public void setup() {
        try {
            // force apply software configuration
            configurationService.applySoftwareProperties();
        }
        catch (Exception e) {
            // Continue
        }
    }

    @Test
    public void executeLogbookSeaCucumber() throws IOException {

        String programLabel = "LOGBOOK-SEA-CUCUMBER";

        ExtractionTripFilterVO filter = new ExtractionTripFilterVO();
        filter.setProgramLabel(programLabel);
        filter.setExcludeInvalidStation(false);

        // Test the RDB format
        File outputFile = service.executeAndDumpTrips(LiveExtractionTypeEnum.PMFM_TRIP, filter);
        Assert.assertTrue(outputFile.exists());
        File root = unpack(outputFile, LiveExtractionTypeEnum.PMFM_TRIP.getLabel() + "_SFA");

        // TR.csv
        {
            File tripFile = new File(root, PmfmTripSpecification.TR_SHEET_NAME + ".csv");
            Assert.assertTrue(countLineInCsvFile(tripFile) > 1);
        }

        // HH.csv
        {
            File stationFile = new File(root, PmfmTripSpecification.HH_SHEET_NAME + ".csv");
            Assert.assertTrue(countLineInCsvFile(stationFile) > 1);

            // Make sure this column exists (column with a 'dbms' attribute)
            assertHasColumn(stationFile, PmfmTripSpecification.COLUMN_FISHING_TIME);
        }

        // SL.csv
        {
            File speciesListFile = new File(root, PmfmTripSpecification.SL_SHEET_NAME + ".csv");
            Assert.assertTrue(countLineInCsvFile(speciesListFile) > 1);

            // Make sure this column exists (column with a 'dbms' attribute)
            assertHasColumn(speciesListFile, PmfmTripSpecification.COLUMN_WEIGHT);
        }

        // HL.csv => empty or not exists
        {
            File speciesLengthFile = new File(root, PmfmTripSpecification.HL_SHEET_NAME + ".csv");
            Assert.assertTrue(!Files.exists(speciesLengthFile.toPath()) || countLineInCsvFile(speciesLengthFile) == 0);
        }
    }

    @Test
    public void executeLogbookLobster() throws IOException {

        String programLabel = "LOGBOOK-LOBSTER";

        ExtractionTripFilterVO filter = new ExtractionTripFilterVO();
        filter.setProgramLabel(programLabel);
        filter.setExcludeInvalidStation(false);

        // Test the RDB format
        File outputFile = service.executeAndDumpTrips(LiveExtractionTypeEnum.PMFM_TRIP, filter);
        Assert.assertTrue(outputFile.exists());
        File root = unpack(outputFile, LiveExtractionTypeEnum.PMFM_TRIP.getLabel() + "_SFA");

        // TR.csv
        {
            File tripFile = new File(root, PmfmTripSpecification.TR_SHEET_NAME + ".csv");
            Assert.assertTrue(countLineInCsvFile(tripFile) > 1);
        }

        // HH.csv
        {
            File stationFile = new File(root, PmfmTripSpecification.HH_SHEET_NAME + ".csv");
            Assert.assertTrue(countLineInCsvFile(stationFile) > 1);

            // Make sure this column exists (column with a 'dbms' attribute)
            assertHasColumn(stationFile, PmfmTripSpecification.COLUMN_FISHING_TIME);
        }

        // SL.csv
        {
            File speciesListFile = new File(root, PmfmTripSpecification.SL_SHEET_NAME + ".csv");
            Assert.assertTrue(countLineInCsvFile(speciesListFile) > 1);

            // Make sure this column exists (column with a 'dbms' attribute)
            assertHasColumn(speciesListFile, PmfmTripSpecification.COLUMN_WEIGHT);
        }

        // HL.csv
        {
            File speciesLengthFile = new File(root, PmfmTripSpecification.HL_SHEET_NAME + ".csv");
            Assert.assertTrue(countLineInCsvFile(speciesLengthFile) > 1);

            assertHasColumn(speciesLengthFile, "sex");
        }
    }

    /* -- SPECIAL case -- */

    @Test
    public void executeLogbookSeaCucumber_issue456() throws IOException {

        String programLabel = "LOGBOOK-SEA-CUCUMBER";

        ExtractionTripFilterVO filter = new ExtractionTripFilterVO();
        filter.setProgramLabel(programLabel);
        filter.setExcludeInvalidStation(false);
        filter.setTripId(284875);
        filter.setOperationIds(new Integer[]{327730}); // An operation with some batches

        // Test the RDB format
        File outputFile = service.executeAndDumpTrips(LiveExtractionTypeEnum.PMFM_TRIP, filter);
        Assert.assertTrue(outputFile.exists());
        File root = unpack(outputFile, LiveExtractionTypeEnum.PMFM_TRIP.getLabel() + "_SFA");

        // TR.csv
        {
            File tripFile = new File(root, PmfmTripSpecification.TR_SHEET_NAME + ".csv");
            Assert.assertTrue(countLineInCsvFile(tripFile) > 1);
        }

        // HH.csv
        {
            File stationFile = new File(root, PmfmTripSpecification.HH_SHEET_NAME + ".csv");
            Assert.assertTrue(countLineInCsvFile(stationFile) > 1);
        }

        // SL.csv
        {
            File speciesListFile = new File(root, PmfmTripSpecification.SL_SHEET_NAME + ".csv");
            Assert.assertTrue(countLineInCsvFile(speciesListFile) > 1);
        }

        // HL.csv => empty or not exists
        {
            File speciesLengthFile = new File(root, PmfmTripSpecification.HL_SHEET_NAME + ".csv");
            Assert.assertTrue(!Files.exists(speciesLengthFile.toPath()) || countLineInCsvFile(speciesLengthFile) == 0);
        }
    }


    @Test
    public void executeVessel() throws IOException {

        String programLabel = getProgramLabelForVessel();

        VesselFilterVO filter = new VesselFilterVO();
        filter.setProgramLabel(programLabel);


        // Test the Vessel format
        File outputFile = service.executeAndDumpVessels(LiveExtractionTypeEnum.VESSEL, filter);
        Assert.assertTrue(outputFile.exists());
        File root = unpack(outputFile, LiveExtractionTypeEnum.VESSEL.getLabel() + "_SFA");

        // HL.csv => empty or not exists
        {
            File speciesLengthFile = new File(root, PmfmTripSpecification.HL_SHEET_NAME + ".csv");
            Assert.assertTrue(!Files.exists(speciesLengthFile.toPath()) || countLineInCsvFile(speciesLengthFile) == 0);
        }
    }

    /* -- protected methods -- */

    protected String getProgramLabelForVessel() {
        return ProgramEnum.SIH.getLabel();
    }

}

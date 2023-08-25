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

package net.sumaris.extraction.core.service.pgsql;

import net.sumaris.extraction.core.DatabaseResource;
import net.sumaris.extraction.core.service.ExtractionServiceTest;
import net.sumaris.extraction.core.specification.data.trip.PmfmTripSpecification;
import net.sumaris.extraction.core.specification.data.trip.RdbSpecification;
import net.sumaris.extraction.core.type.LiveExtractionTypeEnum;
import net.sumaris.extraction.core.vo.trip.ExtractionTripFilterVO;
import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.test.context.ActiveProfiles;

import java.io.File;
import java.io.IOException;

/**
 * @author peck7 on 17/12/2018.
 */
@ActiveProfiles("pgsql")
@Ignore("Use only a Pgsql database")
public class ExtractionServicePgsqlTest extends ExtractionServiceTest {

    @ClassRule
    public static final DatabaseResource dbResource = DatabaseResource.writeDb("pgsql");

    @Test
    public void executePmfmPIFIL() throws IOException {
        String programLabel = "PIFIL";
        ExtractionTripFilterVO filter = new ExtractionTripFilterVO();
        filter.setProgramLabel(programLabel);
        filter.setExcludeInvalidStation(false);

        // Test the RDB format
        File outputFile = service.executeAndDumpTrips(LiveExtractionTypeEnum.PMFM_TRIP, filter);
        Assert.assertTrue(outputFile.exists());
        File root = unpack(outputFile, LiveExtractionTypeEnum.PMFM_TRIP.getLabel());

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

        // ST.csv
        {

            File survivalTestFile = new File(root, PmfmTripSpecification.ST_SHEET_NAME + ".csv");
            Assert.assertTrue(countLineInCsvFile(survivalTestFile) > 1);
        }

        // RL.csv
        {
            File releaseFile = new File(root, PmfmTripSpecification.RL_SHEET_NAME + ".csv");
            Assert.assertTrue(countLineInCsvFile(releaseFile) > 1);
        }

    }

    @Test
    public void executeApase() throws IOException {
        String programLabel = "PIFIL";
        ExtractionTripFilterVO filter = new ExtractionTripFilterVO();
        filter.setProgramLabel(programLabel);

        // DEBUG
        //filter.setSheetName(RdbSpecification.SL_SHEET_NAME);
        //filter.setPreview(true);

        // Test the RDB format
        File outputFile = service.executeAndDumpTrips(LiveExtractionTypeEnum.APASE, filter);
        Assert.assertTrue(outputFile.exists());
        File root = unpack(outputFile, LiveExtractionTypeEnum.APASE.getLabel());

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

        // HL.csv
        {
            File speciesLengthFile = new File(root, RdbSpecification.HL_SHEET_NAME + ".csv");
            Assert.assertTrue(countLineInCsvFile(speciesLengthFile) > 1);

            assertHasColumn(speciesLengthFile, RdbSpecification.COLUMN_LENGTH_CLASS);
            assertHasColumn(speciesLengthFile, RdbSpecification.COLUMN_NUMBER_AT_LENGTH);
        }
    }

    /* -- protected methods -- */

    protected boolean canWriteData() {
        return false;
    }

    //@Override
    protected String getProgramLabelForVessel() {
        return "PIFIL"; // Open database
    }

    protected void assertHasColumn(File file, String columnName) throws IOException {
        //String headerName = StringUtils.underscoreToChangeCase(columnName);
        Assert.assertTrue(String.format("Missing header '%s' in file: %s", columnName, file.getPath()),
            hasHeaderInCsvFile(file, columnName));
    }
}

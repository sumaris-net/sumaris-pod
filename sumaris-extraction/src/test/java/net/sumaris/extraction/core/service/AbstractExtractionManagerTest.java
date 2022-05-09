package net.sumaris.extraction.core.service;

/*-
 * #%L
 * SUMARiS:: Core Extraction
 * %%
 * Copyright (C) 2018 - 2019 SUMARiS Consortium
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

import net.sumaris.core.model.technical.extraction.ExtractionCategoryEnum;
import net.sumaris.core.model.technical.extraction.IExtractionType;
import net.sumaris.core.util.StringUtils;
import net.sumaris.core.vo.technical.extraction.ExtractionProductVO;
import net.sumaris.extraction.core.DatabaseResource;
import net.sumaris.extraction.core.specification.administration.StratSpecification;
import net.sumaris.extraction.core.specification.data.trip.Free2Specification;
import net.sumaris.extraction.core.specification.data.trip.PmfmTripSpecification;
import net.sumaris.extraction.core.specification.data.trip.RdbSpecification;
import net.sumaris.extraction.core.specification.data.trip.SurvivalTestSpecification;
import net.sumaris.extraction.core.type.LiveExtractionTypeEnum;
import net.sumaris.extraction.core.vo.ExtractionTypeVO;
import net.sumaris.extraction.core.vo.trip.ExtractionTripFilterVO;
import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.File;
import java.io.IOException;

/**
 * @author Benoit LAVENIER <benoit.lavenier@e-is.pro>
 */
public abstract class AbstractExtractionManagerTest extends AbstractServiceTest {

    @Autowired
    private ExtractionManager service;

    @Test
    public void executeStrat() throws IOException {

        // Test the Strategy format
        File outputFile = service.executeAndDumpStrategies(LiveExtractionTypeEnum.STRAT, null);
        File root = unpack(outputFile, LiveExtractionTypeEnum.STRAT.getLabel());

        // ST.csv (strategy)
        {
            File strategyFile = new File(root, StratSpecification.ST_SHEET_NAME + ".csv");
            Assert.assertTrue(countLineInCsvFile(strategyFile) > 1);

        }
        // SM.csv (strategy monitoring)
        {
            File monitoringFile = new File(root, StratSpecification.SM_SHEET_NAME + ".csv");
            Assert.assertTrue(countLineInCsvFile(monitoringFile) > 1);

        }
    }

    @Test
    public void executeRdb() throws IOException {

        // Test the RDB format
        File outputFile = service.executeAndDumpTrips(LiveExtractionTypeEnum.RDB, null);
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

    @Test
    public void executeFree1() {

        // Test the FREE 1 format
        service.executeAndDumpTrips(LiveExtractionTypeEnum.FREE1, null);
    }

    @Test
    public void executeFree2() throws IOException {

        // Test the FREE v2 format
        File outputFile = service.executeAndDumpTrips(LiveExtractionTypeEnum.FREE2, null);

        File root = unpack(outputFile, LiveExtractionTypeEnum.FREE2);

        // MAREES.csv
        File tripFile = new File(root, Free2Specification.TRIP_SHEET_NAME + ".csv");
        Assert.assertTrue(countLineInCsvFile(tripFile) > 1);

        // OPERATION_PECHE.csv
        File stationFile = new File(root, Free2Specification.STATION_SHEET_NAME + ".csv");
        Assert.assertTrue(countLineInCsvFile(stationFile) > 1);

        // ENGINS.csv
        File gearFile = new File(root, Free2Specification.GEAR_SHEET_NAME + ".csv");
        Assert.assertTrue(countLineInCsvFile(gearFile) > 1);
    }

    @Test
    public void executeSurvivalTest() throws IOException  {

        // Test Survival test format
        File outputFile = service.executeAndDumpTrips(LiveExtractionTypeEnum.SURVIVAL_TEST, null);
        File root = unpack(outputFile, LiveExtractionTypeEnum.SURVIVAL_TEST);

        // RL (release)
        File releaseFile = new File(root, SurvivalTestSpecification.RL_SHEET_NAME+".csv");
        Assert.assertTrue(countLineInCsvFile(releaseFile) > 1);

        // ST (Survival test)
        File stFile = new File(root, SurvivalTestSpecification.ST_SHEET_NAME+".csv");
        Assert.assertTrue(countLineInCsvFile(stFile) > 1);
    }

    @Test
    public void executePmfm() throws IOException {

        ExtractionTripFilterVO filter = new ExtractionTripFilterVO();
        filter.setProgramLabel(fixtures.getProgramLabelForPmfmExtraction(0));

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

        // ST.csv
        {
            File survivalTestFile = new File(root, PmfmTripSpecification.ST_SHEET_NAME + ".csv");
            Assert.assertTrue(countLineInCsvFile(survivalTestFile) > 1);

            assertHasColumn(survivalTestFile, "picking_time");
            assertHasColumn(survivalTestFile, "injuries_body");
            assertHasColumn(survivalTestFile, "reflex_body_flex");
        }

        // RL.csv
        {
            File releaseFile = new File(root, PmfmTripSpecification.RL_SHEET_NAME + ".csv");
            Assert.assertTrue(countLineInCsvFile(releaseFile) > 1);

            assertHasColumn(releaseFile, "measure_time");
            assertHasColumn(releaseFile, "latitude");
            assertHasColumn(releaseFile, "longitude");
        }
    }



    /* -- protected methods -- */

    protected void assertHasColumn(File file, String columnName) throws IOException {
        String headerName = StringUtils.underscoreToChangeCase(columnName);
        Assert.assertTrue(String.format("Missing header '%s' in file: %s", headerName, file.getPath()),
            hasHeaderInCsvFile(file, headerName));
    }

}

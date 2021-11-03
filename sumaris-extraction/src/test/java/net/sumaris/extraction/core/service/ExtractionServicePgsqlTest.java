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

import net.sumaris.extraction.core.DatabaseResource;
import net.sumaris.extraction.core.format.LiveFormatEnum;
import net.sumaris.extraction.core.specification.data.trip.Free2Specification;
import net.sumaris.extraction.core.specification.data.trip.SurvivalTestSpecification;
import net.sumaris.extraction.core.vo.AggregationTypeVO;
import net.sumaris.extraction.core.vo.ExtractionTypeVO;
import net.sumaris.core.model.referential.StatusEnum;
import net.sumaris.core.model.technical.extraction.ExtractionCategoryEnum;
import net.sumaris.core.vo.administration.user.DepartmentVO;
import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;

import java.io.File;
import java.io.IOException;

/**
 * @author peck7 on 17/12/2018.
 */
@ActiveProfiles("pgsql")
public class ExtractionServicePgsqlTest extends AbstractServiceTest {

    @ClassRule
    public static final DatabaseResource dbResource = DatabaseResource.writeDb("pgsql");

    @Autowired
    private ExtractionService service;


    @Test
    public void exportStratFormat() {

        // Test the Strategy format
        File outputFile = service.executeAndDumpStrategies(LiveFormatEnum.STRAT, null);
        unpack(outputFile, LiveFormatEnum.STRAT.getLabel());
    }

    @Test
    public void exportRdbFormat() {

        // Test the RDB format
        service.executeAndDumpTrips(LiveFormatEnum.RDB, null);
    }

    @Test
    public void exportFree1Format() {

        // Test the FREE 1 format
        service.executeAndDumpTrips(LiveFormatEnum.FREE1, null);
    }

    @Test
    public void exportFree2Format() throws IOException {

        // Test the FREE v2 format
        File outputFile = service.executeAndDumpTrips(LiveFormatEnum.FREE2, null);

        File root = unpack(outputFile, LiveFormatEnum.FREE2);

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
    public void exportSurvivalTestFormat() throws IOException {

        // Test Survival test format
        File outputFile = service.executeAndDumpTrips(LiveFormatEnum.SURVIVAL_TEST, null);
        File root = unpack(outputFile, LiveFormatEnum.SURVIVAL_TEST);

        // RL (release)
        File releaseFile = new File(root, SurvivalTestSpecification.RL_SHEET_NAME + ".csv");
        Assert.assertTrue(countLineInCsvFile(releaseFile) > 1);

        // ST (Survival test)
        File stFile = new File(root, SurvivalTestSpecification.ST_SHEET_NAME + ".csv");
        Assert.assertTrue(countLineInCsvFile(stFile) > 1);
    }

    @Test
    public void save() {

        ExtractionTypeVO type = new ExtractionTypeVO();
        type.setCategory(ExtractionCategoryEnum.LIVE);
        type.setLabel(LiveFormatEnum.RDB.name() + "-ext");
        type.setName("Product - " + LiveFormatEnum.RDB.name());
        type.setStatusId(StatusEnum.TEMPORARY.getId());

        DepartmentVO recDep = new DepartmentVO();
        recDep.setId(fixtures.getDepartmentId(0));
        type.setRecorderDepartment(recDep);

        ExtractionTypeVO savedType = service.save(type, null);

        Assert.assertNotNull(savedType);
    }

    @Test
    public void getByFormat() {

        // Get valid live format
        {
            AggregationTypeVO format = new AggregationTypeVO();
            format.setLabel(LiveFormatEnum.RDB.getLabel());
            format.setCategory(LiveFormatEnum.RDB.getCategory());
            ExtractionTypeVO type = service.getByFormat(format);

            Assert.assertNotNull(type);
            Assert.assertEquals("type.label should be in lowerCase", format.getLabel().toLowerCase(), type.getLabel());
        }

        // Get invalid live format
        {
            AggregationTypeVO format = new AggregationTypeVO();
            format.setLabel("FAKE");
            format.setCategory(ExtractionCategoryEnum.LIVE);
            try {
                service.getByFormat(format);
                Assert.fail("Should failed on wrong format");
            } catch (Exception e) {
                // OK
            }
        }

        // Get a valid product
        {
            AggregationTypeVO format = new AggregationTypeVO();
            format.setLabel("rdb-01");
            format.setCategory(ExtractionCategoryEnum.PRODUCT);
            ExtractionTypeVO type = service.getByFormat(format);
            Assert.assertNotNull(type);
            Assert.assertEquals(format.getLabel(), type.getLabel());
        }


    }


    @Test
    public void saveLiveRdb() {
        ExtractionTypeVO type = new ExtractionTypeVO();
        type.setCategory(ExtractionCategoryEnum.LIVE);
        type.setLabel(LiveFormatEnum.RDB.name() + "-save");
        type.setName("RDB live extraction saved as product");
        type.setStatusId(StatusEnum.TEMPORARY.getId());

        DepartmentVO recDep = new DepartmentVO();
        recDep.setId(fixtures.getDepartmentId(0));
        type.setRecorderDepartment(recDep);

        ExtractionTypeVO savedType = service.save(type, null);
        Assert.assertNotNull(savedType);
        Assert.assertNotNull(savedType.getId());

    }

    /* -- protected methods -- */




}

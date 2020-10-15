package net.sumaris.core.extraction.service;

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

import liquibase.util.csv.opencsv.CSVReader;
import net.sumaris.core.extraction.dao.DatabaseResource;
import net.sumaris.core.extraction.specification.Free2Specification;
import net.sumaris.core.extraction.specification.SurvivalTestSpecification;
import net.sumaris.core.extraction.vo.ExtractionCategoryEnum;
import net.sumaris.core.extraction.utils.ExtractionRawFormatEnum;
import net.sumaris.core.extraction.vo.ExtractionTypeVO;
import net.sumaris.core.model.referential.StatusEnum;
import net.sumaris.core.util.Files;
import net.sumaris.core.util.ZipUtils;
import net.sumaris.core.vo.administration.user.DepartmentVO;
import org.apache.commons.io.FileUtils;
import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;

/**
 * @author peck7 on 17/12/2018.
 */
public class ExtractionServiceTest extends AbstractServiceTest {

    @ClassRule
    public static final DatabaseResource dbResource = DatabaseResource.writeDb();

    @Autowired
    private ExtractionService service;

    @Test
    public void exportRdbFormat() {

        // Test the RDB format
        service.executeAndDumpTrips(ExtractionRawFormatEnum.RDB, null);
    }

    @Test
    public void exportFree1Format() {

        // Test the FREE 1 format
        service.executeAndDumpTrips(ExtractionRawFormatEnum.FREE1, null);
    }

    @Test
    public void exportFree2Format() throws IOException {

        // Test the FREE v2 format
        File outputFile = service.executeAndDumpTrips(ExtractionRawFormatEnum.FREE2, null);

        File root = unpack(outputFile, ExtractionRawFormatEnum.FREE2);

        // MAREES.csv
        File tripFile = new File(root, Free2Specification.TRIP_SHEET_NAME + ".csv");
        Assert.assertTrue(countLine(tripFile) > 1);

        // OPERATION_PECHE.csv
        File stationFile = new File(root, Free2Specification.STATION_SHEET_NAME + ".csv");
        Assert.assertTrue(countLine(stationFile) > 1);

        // ENGINS.csv
        File gearFile = new File(root, Free2Specification.GEAR_SHEET_NAME+".csv");
        Assert.assertTrue(countLine(gearFile) > 1);
    }

    @Test
    public void exportSurvivalTestFormat() throws IOException  {

        // Test Survival test format
        File outputFile = service.executeAndDumpTrips(ExtractionRawFormatEnum.SURVIVAL_TEST, null);
        File root = unpack(outputFile, ExtractionRawFormatEnum.SURVIVAL_TEST);

        // RL (release)
        File releaseFile = new File(root, SurvivalTestSpecification.RL_SHEET_NAME+".csv");
        Assert.assertTrue(countLine(releaseFile) > 1);

        // ST (Survival test)
        File stFile = new File(root, SurvivalTestSpecification.ST_SHEET_NAME+".csv");
        Assert.assertTrue(countLine(stFile) > 1);
    }


    @Test
    public void save() {

        ExtractionTypeVO type = new ExtractionTypeVO();
        type.setCategory(ExtractionCategoryEnum.LIVE.name());
        type.setLabel(ExtractionRawFormatEnum.RDB.name() + "-ext");
        type.setStatusId(StatusEnum.TEMPORARY.getId());

        DepartmentVO recDep = new DepartmentVO();
        recDep.setId(dbResource.getFixtures().getDepartmentId(0));
        type.setRecorderDepartment(recDep);

        ExtractionTypeVO savedType = service.save(type, null);

        Assert.assertNotNull(savedType);
    }

    /* -- protected methods -- */

    protected int countLine(File file) throws IOException {
        Files.checkExists(file);

        FileReader fr = new FileReader(file);
        try {
            CSVReader read = new CSVReader(fr);
            List<String[]> lines = read.readAll();

            read.close();

            return lines.size();
        }
        finally {
            fr.close();
        }
    }

    protected File unpack(File sourceFile, ExtractionRawFormatEnum format) {

        File tempFile = new File("target/result.zip");
        File outputDirectory = new File("target/result/" + format.getLabel() + '_' + format.getVersion());
        try {
            Files.deleteQuietly(tempFile);
            Files.copyFile(sourceFile, tempFile);

            Files.deleteQuietly(outputDirectory);
            FileUtils.forceMkdir(outputDirectory);

            ZipUtils.uncompressFileToPath(tempFile, outputDirectory.getPath(), true);

        } catch (IOException e) {
            Assert.fail(e.getMessage());
        }
        return outputDirectory;
    }
}

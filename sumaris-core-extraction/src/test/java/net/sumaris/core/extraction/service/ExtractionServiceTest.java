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

import net.sumaris.core.extraction.dao.DatabaseResource;
import net.sumaris.core.extraction.vo.ExtractionCategoryEnum;
import net.sumaris.core.extraction.vo.ExtractionRawFormatEnum;
import net.sumaris.core.extraction.vo.ExtractionTypeVO;
import net.sumaris.core.extraction.vo.trip.free.ExtractionFreeTripVersion;
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
import java.io.IOException;

/**
 * @author peck7 on 17/12/2018.
 */
public class ExtractionServiceTest extends AbstractServiceTest {

    @ClassRule
    public static final DatabaseResource dbResource = DatabaseResource.writeDb();

    @Autowired
    private ExtractionService service;

    @Test
    public void extractLiveTripAsFile_RDB() {

        // Test the RDB format
        service.executeAndDumpTrips(ExtractionRawFormatEnum.RDB, null);
    }

    @Test
    public void extractLiveTripAsFile_FreeV1() {

        // Test the FREE 1 format
        service.executeAndDumpTrips(ExtractionRawFormatEnum.FREE1, null);
    }

    @Test
    public void extractLiveTripAsFile_FreeV2() {

        // Test the FREE v2 format
        service.executeAndDumpTrips(ExtractionRawFormatEnum.FREE2, null);
    }

    @Test
    public void extractLiveTripAsFile_SurvivalTest() {

        // Test Survival test format
        File outputFile = service.executeAndDumpTrips(ExtractionRawFormatEnum.SURVIVAL_TEST, null);

        File debugFile = new File("target/result.zip");
        File debugDirectory = new File("target/result");
        try {
            Files.deleteQuietly(debugFile);
            Files.copyFile(outputFile, debugFile);

            FileUtils.forceMkdir(debugDirectory);
            ZipUtils.uncompressFileToPath(debugFile, debugDirectory.getPath(), true);
        }
        catch(IOException e) {
            // Silent
        }
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
}

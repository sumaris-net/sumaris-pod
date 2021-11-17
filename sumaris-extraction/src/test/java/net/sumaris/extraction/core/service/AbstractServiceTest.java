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

import au.com.bytecode.opencsv.CSVReader;
import net.sumaris.core.config.SumarisConfiguration;
import net.sumaris.core.util.Files;
import net.sumaris.core.util.ZipUtils;
import net.sumaris.extraction.core.DatabaseFixtures;
import net.sumaris.extraction.core.format.LiveFormatEnum;
import net.sumaris.extraction.core.vo.ExtractionTypeVO;
import org.apache.commons.io.FileUtils;
import org.junit.Assert;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 * @author peck7 on 17/12/2018.
 *
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = {ServiceTestConfiguration.class})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public abstract class AbstractServiceTest {

    @Autowired
    protected SumarisConfiguration config;

    @Autowired
    protected DatabaseFixtures fixtures;

    /* -- Protected functions -- */

    protected File unpack(File zipFile, LiveFormatEnum format) {
        return unpack(zipFile, format.getLabel() + '_' + format.getVersion());
    }

    protected File unpack(File zipFile, ExtractionTypeVO type) {
        return unpack(zipFile,  type.getCategory().name() + '_' + type.getLabel());
    }

    protected File unpack(File zipFile, String dirName) {
        Assert.assertNotNull("No result file", zipFile);
        Assert.assertTrue("No result file", zipFile.exists());

        File outputDirectory = new File("target/result/" + dirName);
        try {
            Files.deleteQuietly(outputDirectory);
            FileUtils.forceMkdir(outputDirectory);

            ZipUtils.uncompressFileToPath(zipFile, outputDirectory.getPath(), false);

        } catch (IOException e) {
            Assert.fail(e.getMessage());
        }
        return outputDirectory;
    }

    protected int countLineInCsvFile(File file) throws IOException {
        List<String[]> lines = readCsvFile(file);
        return lines.size();
    }

    protected List<String[]> readCsvFile(File file) throws IOException {
        Files.checkExists(file);

        FileReader fr = new FileReader(file);
        try {
            CSVReader read = new CSVReader(fr, config.getCsvSeparator());
            List<String[]> lines = read.readAll();

            read.close();

            return lines;
        }
        finally {
            fr.close();
        }
    }

    protected String[] readHeaderInCsvFile(File file) throws IOException {
        List<String[]> lines = readCsvFile(file);
        Assert.assertTrue("No header row, in the CSV file",lines.size() > 0);
        return lines.get(0);
    }

    protected boolean hasHeaderInCsvFile(File file, String header) throws IOException {
        String[] headers = readHeaderInCsvFile(file);

        return Arrays.asList(headers).contains(header);
    }

}

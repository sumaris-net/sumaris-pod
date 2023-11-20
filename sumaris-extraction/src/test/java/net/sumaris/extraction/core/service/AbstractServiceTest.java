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
import net.sumaris.core.model.referential.StatusEnum;
import net.sumaris.core.model.technical.extraction.IExtractionType;
import net.sumaris.core.model.technical.history.ProcessingFrequencyEnum;
import net.sumaris.core.util.Files;
import net.sumaris.core.util.StringUtils;
import net.sumaris.core.util.ZipUtils;
import net.sumaris.core.vo.administration.user.DepartmentVO;
import net.sumaris.core.vo.administration.user.PersonVO;
import net.sumaris.core.vo.technical.extraction.ExtractionProductVO;
import net.sumaris.extraction.core.DatabaseFixtures;
import net.sumaris.extraction.core.type.AggExtractionTypeEnum;
import net.sumaris.extraction.core.type.LiveExtractionTypeEnum;
import net.sumaris.extraction.core.util.ExtractionProducts;
import net.sumaris.extraction.core.util.ExtractionTypes;
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
import java.util.*;

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

    protected File unpack(File zipFile, LiveExtractionTypeEnum type) {
        return unpack(zipFile, type.getFormat() + '_' + type.getVersion());
    }

    protected File unpack(File zipFile, IExtractionType type) {
        return unpack(zipFile,  type.getLabel());
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

    protected ExtractionProductVO createProduct(IExtractionType source) {

        ExtractionProductVO target = new ExtractionProductVO(source);
        target.setId(null);
        long time = System.currentTimeMillis();
        target.setUpdateDate(new Date(time));
        target.setLabel(ExtractionProducts.computeLabel(source.getFormat(), time));
        target.setName(ExtractionProducts.getProductDisplayName(source, time));
        target.setFormat(source.getFormat());
        target.setVersion(source.getVersion());
        target.setStatusId(StatusEnum.TEMPORARY.getId());
        target.setProcessingFrequencyId(ProcessingFrequencyEnum.NEVER.getId());

        target.setIsSpatial(ExtractionTypes.isAggregation(source));

        DepartmentVO recDep = new DepartmentVO();
        recDep.setId(fixtures.getDepartmentId(0));
        target.setRecorderDepartment(recDep);

        PersonVO recorder = new PersonVO();
        recorder.setId(fixtures.getPersonId(0));
        target.setRecorderPerson(recorder);

        return target;
    }

    protected ExtractionProductVO createAggProduct(AggExtractionTypeEnum source) {
        return createAggProduct(source, source.getParent());
    }

    protected ExtractionProductVO createAggProduct(AggExtractionTypeEnum source, IExtractionType parent) {

        ExtractionProductVO target = createProduct(source);

        target.setIsSpatial(true);
        target.setParent(parent);

        return target;
    }

    protected void assertHasColumn(File file, String columnName) throws IOException {
        String headerName = StringUtils.underscoreToChangeCase(columnName);
        Assert.assertTrue(String.format("Missing the column header '%s' in file: %s", headerName, file.getPath()),
            hasHeaderInCsvFile(file, headerName));
    }
    protected void assertHasNoColumn(File file, String columnName) throws IOException {
        String headerName = StringUtils.underscoreToChangeCase(columnName);
        Assert.assertFalse(String.format("Should not have the column header '%s' in file: %s", headerName, file.getPath()),
            hasHeaderInCsvFile(file, headerName));
    }

    protected List<Map<String, String>> readCsvFileToMaps(File file) throws IOException {
        List<String[]> lines = readCsvFile(file);
        String[] headers = lines.get(0);

        return lines.stream()
            .skip(1) // Skip headers
            .map(values -> {
            Map<String, String> result = new HashMap<>();
                for (int i = 0 ; i< headers.length; i++) {
                    String name = headers[i];
                    String value = StringUtils.defaultIfBlank(values[i], null);
                    result.put(name, value);
                }
                return result;
            }).toList();
    }
}

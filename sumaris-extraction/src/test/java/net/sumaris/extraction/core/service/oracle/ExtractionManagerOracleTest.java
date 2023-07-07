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

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.google.common.collect.ImmutableList;
import lombok.extern.slf4j.Slf4j;
import net.sumaris.core.dao.technical.Page;
import net.sumaris.core.dao.technical.SortDirection;
import net.sumaris.core.service.technical.ConfigurationService;
import net.sumaris.extraction.core.DatabaseResource;
import net.sumaris.extraction.core.service.ExtractionManagerTest;
import net.sumaris.extraction.core.specification.administration.StratSpecification;
import net.sumaris.extraction.core.type.LiveExtractionTypeEnum;
import net.sumaris.extraction.core.vo.ExtractionResultVO;
import net.sumaris.extraction.core.vo.administration.ExtractionStrategyFilterVO;
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
@Ignore("Use only Ifremer Oracle database")
@ActiveProfiles("oracle")
@TestPropertySource(locations = "classpath:application-oracle.properties")
public class ExtractionManagerOracleTest extends ExtractionManagerTest {

    @ClassRule
    public static final DatabaseResource dbResource = DatabaseResource.writeDb("oracle");

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
    public void executeStrat() throws IOException {

        ExtractionStrategyFilterVO filter = new ExtractionStrategyFilterVO();
        filter.setProgramLabel("SIH-OBSBIO");
        filter.setStrategyLabels(ImmutableList.of("22SOLESOL004"));

        File root = this.executeStrat(filter);

        // Check realized effort
        {
            File monitoringFile = new File(root, StratSpecification.SM_SHEET_NAME + ".csv");
            Assert.assertTrue(countLineInCsvFile(monitoringFile) > 3);
        }

    }

    @Test
    public void executeAndReadStratAsJson() throws IOException {

        ExtractionStrategyFilterVO filter = new ExtractionStrategyFilterVO();
        filter.setSheetName(StratSpecification.SM_SHEET_NAME);
        filter.setProgramLabel("SIH-OBSBIO");
        filter.setStrategyLabels(ImmutableList.of("22SOLESOL004"));

        ExtractionResultVO result = service.executeAndReadStrategies(LiveExtractionTypeEnum.STRAT, filter,
                Page.builder()
                        .size(1000)
                        .offset(0)
                        .sortBy(StratSpecification.COLUMN_END_DATE)
                        .sortDirection(SortDirection.ASC)
                .build());
        Assert.assertNotNull(result);

        ArrayNode array = service.toJsonArray(result);
        Assert.assertNotNull(array);
        String jsonStr = this.objectMapper.writeValueAsString(array);
        Assert.assertTrue(jsonStr.length() > 10);
    }

    /* -- protected methods -- */

    protected void assertHasColumn(File file, String columnName) throws IOException {
        //String headerName = StringUtils.underscoreToChangeCase(columnName);
        Assert.assertTrue(String.format("Missing header '%s' in file: %s", columnName, file.getPath()),
            hasHeaderInCsvFile(file, columnName));
    }
}

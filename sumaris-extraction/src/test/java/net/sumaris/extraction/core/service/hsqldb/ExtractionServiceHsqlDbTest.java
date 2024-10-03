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

import com.google.common.collect.Maps;
import lombok.extern.slf4j.Slf4j;
import net.sumaris.core.util.StringUtils;
import net.sumaris.extraction.core.DatabaseResource;
import net.sumaris.extraction.core.service.ExtractionServiceTest;
import net.sumaris.extraction.core.specification.administration.StratSpecification;
import net.sumaris.extraction.core.vo.administration.ExtractionStrategyFilterVO;
import org.junit.*;
import org.junit.runners.MethodSorters;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.util.List;
import java.util.Map;

/**
 * @author Benoit LAVENIER <benoit.lavenier@e-is.pro>
 */
@ActiveProfiles("hsqldb")
@TestPropertySource(locations = "classpath:application-hsqldb.properties")
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@Slf4j
public class ExtractionServiceHsqlDbTest extends ExtractionServiceTest {

    @ClassRule
    public static final DatabaseResource dbResource = DatabaseResource.writeDb();


    @Override @Test
    public void executeFree1() {
        super.executeFree1();
    }

    @Override @Test
    public void executeWithDenormalization() throws IOException {
        super.executeWithDenormalization();
    }

    @Override @Test
    public void executeRdb() throws IOException {
        super.executeRdb();
    }

    @Override @Test
    public void executeCost() throws IOException {
        super.executeCost();
    }

    @Override @Test
    public void executeFree2() throws IOException {
        super.executeFree2();
    }

    @Override @Test
    public void executeSurvivalTest() throws IOException {
        super.executeSurvivalTest();
    }

    @Override @Test
    public void executePmfmADAP() throws IOException {
        super.executePmfmADAP();
    }

    @Override @Test
    public void executeRjbADAP() throws IOException {
        super.executeRjbADAP();
    }

    @Override @Test
    public void executeApase() throws IOException {
        super.executeApase();
    }

    @Override @Test
    public void executeVessel() throws IOException {
        super.executeVessel();
    }

    @Override @Test
    public void aggregateRdb() throws IOException {
        super.aggregateRdb();
    }

    @Override @Test
    public void aggregateProductRdb() throws IOException {
        super.aggregateProductRdb();
    }

    @Override @Test
    public void aggregateSurvivalTest() throws IOException {
        super.aggregateSurvivalTest();
    }

    @Override @Test
    public void executeAggCost() throws IOException {
        super.executeAggCost();
    }

    @Override @Test
    public void executeAggFree1() throws IOException {
        super.executeAggFree1();
    }

    @Override @Test
    public void executeAggRjbTrip() throws IOException {
        super.executeAggRjbTrip();
    }

    @Test
    public void executeActivityMonitoringTest() throws IOException, ParseException {
        super.executeActivityMonitoringTest(null);
    }

    @Override @Test
    public void executeAndReadAggSurvivalTest() {
        super.executeAndReadAggSurvivalTest();
    }

    @Override @Test
    public void readAggSurvivalTest() {
        super.readAggSurvivalTest();
    }

    @Override @Test
    public void readAggTechSurvivalTest() {
        super.readAggTechSurvivalTest();
    }

    @Override @Test
    public void updatePmfmTrip() {
        super.updatePmfmTrip();
    }

    @Override @Test
    public void updateAggRdbProduct() {
        super.updateAggRdbProduct();
    }

    @Override
    @Test
    public void executeStrat() throws IOException {
        ExtractionStrategyFilterVO filter = new ExtractionStrategyFilterVO();
        filter.setProgramLabel("SIH-OBSBIO");

        File root = super.executeStrat(filter);

        // Check realized effort
        {
            File monitoringFile = new File(root, StratSpecification.SM_SHEET_NAME + ".csv");

            List<Map<String, String>> lines = readCsvFileToMaps(monitoringFile);

            Assert.assertEquals(8, lines.size());

            Map<String, Integer> realizedEffortByStrategy = Maps.newHashMap();
            lines.forEach(line -> {

                String strategyLabel = line.get(StratSpecification.COLUMN_STRATEGY);
                Assert.assertNotNull("Missing strategy", strategyLabel);

                String startDate = line.get(StringUtils.underscoreToChangeCase(StratSpecification.COLUMN_START_DATE));
                Assert.assertNotNull("Missing start_date", startDate);

                String expectedEffortEffortStr = line.get(StringUtils.underscoreToChangeCase(StratSpecification.COLUMN_EXPECTED_EFFORT));
                Assert.assertNotNull("Missing expected_effort", expectedEffortEffortStr);

                String realizedEffortStr = line.get(StringUtils.underscoreToChangeCase(StratSpecification.COLUMN_REALIZED_EFFORT));
                Assert.assertNotNull("Missing realized_effort", realizedEffortStr);

                try {
                    int realizedEffort = Integer.parseInt(realizedEffortStr);
                    if (realizedEffort > 0) {
                        log.info(String.format("%s - %s - %s/%s", strategyLabel, startDate, realizedEffortStr, expectedEffortEffortStr));
                    }

                    // SUM(realized_effort) group by strategy
                    Integer sum = realizedEffortByStrategy.get(strategyLabel);
                    sum = sum == null ? realizedEffort : sum + realizedEffort;
                    realizedEffortByStrategy.put(strategyLabel, sum);
                }
                catch (NumberFormatException e) {
                    log.error("Invalid realized_effort value. Should be a number: " + realizedEffortStr, e);
                    Assert.fail("Invalid realized_effort value. Should be a number: " + realizedEffortStr);
                }

            });

            // 20LEUCCIR001 - expected 5 realized effort
            {
                Integer realizedEffort = realizedEffortByStrategy.get("20LEUCCIR001").intValue();
                Assert.assertNotNull(realizedEffort);
                // FIXME
                // Change les données de tests afin qu'elle aliment TRIP/OPERATION (en plus de LANDING) pour que les SAMPLEs soient reliés à OPERATION
                //Assert.assertEquals("Expected 4 realized effort for 20LEUCCIR002", 4, realizedEffort.intValue());
            }

            // 20LEUCCIR002 - expected 1 realized effort (in scientific cruise)
            {
                Integer realizedEffort = realizedEffortByStrategy.get("20LEUCCIR002").intValue();
                Assert.assertNotNull(realizedEffort);
                // FIXME: waiting features/obsmer merge to have data
                //Assert.assertEquals("Expected 1 realized effort for 20LEUCCIR002", 1, realizedEffort.intValue());
            }
        }
    }

    @Override
    @Test
    @Ignore
    // FIXME
    public void z_dropTemporaryTables() {
        super.z_dropTemporaryTables();
    }
}
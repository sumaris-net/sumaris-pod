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

import net.sumaris.extraction.core.DatabaseResource;
import net.sumaris.extraction.core.service.ExtractionServiceTest;
import org.junit.ClassRule;
import org.junit.Test;

import java.io.IOException;

/**
 * @author Benoit LAVENIER <benoit.lavenier@e-is.pro>
 */
public class ExtractionServiceHsqlDbTest extends ExtractionServiceTest {

    @ClassRule
    public static final DatabaseResource dbResource = DatabaseResource.writeDb();


    @Override @Test
    public void executeFree1() {
        super.executeFree1();
    }

    @Override @Test
    public void executeStrat() throws IOException {
        super.executeStrat();
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

    @Override @Test
    public void z_dropTemporaryTables() {
        super.z_dropTemporaryTables();
    }
}
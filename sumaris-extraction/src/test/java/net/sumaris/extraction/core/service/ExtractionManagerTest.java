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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import net.sumaris.core.dao.technical.Page;
import net.sumaris.core.exception.DataNotFoundException;
import net.sumaris.core.model.technical.extraction.IExtractionType;
import net.sumaris.core.model.technical.extraction.rdb.ProductRdbStation;
import net.sumaris.core.util.StringUtils;
import net.sumaris.core.vo.technical.extraction.AggregationStrataVO;
import net.sumaris.core.vo.technical.extraction.ExtractionProductVO;
import net.sumaris.extraction.core.specification.administration.StratSpecification;
import net.sumaris.extraction.core.specification.data.trip.*;
import net.sumaris.extraction.core.type.AggExtractionTypeEnum;
import net.sumaris.extraction.core.type.LiveExtractionTypeEnum;
import net.sumaris.extraction.core.vo.*;
import net.sumaris.extraction.core.vo.trip.ExtractionTripFilterVO;
import org.junit.*;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.File;
import java.io.IOException;

/**
 * @author Benoit LAVENIER <benoit.lavenier@e-is.pro>
 */
public abstract class ExtractionManagerTest extends AbstractServiceTest {

    @Autowired
    private ExtractionManager service;
    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ExtractionProductService productService;


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

    @Test
    public void aggregateRdb() throws IOException {

        IExtractionType type = AggExtractionTypeEnum.AGG_RDB;

        AggregationStrataVO strata = new AggregationStrataVO();
        strata.setSpatialColumnName(ProductRdbStation.COLUMN_STATISTICAL_RECTANGLE);
        strata.setTimeColumnName(ProductRdbStation.COLUMN_YEAR);

        File outputFile = service.executeAndDump(type, null, strata);
        File root = unpack(outputFile, type);

        // HH.csv
        File stationFile = new File(root, AggRdbSpecification.HH_SHEET_NAME + ".csv");
        Assert.assertTrue(countLineInCsvFile(stationFile) > 1);

        // SL.csv
        File speciesListFile = new File(root, AggRdbSpecification.SL_SHEET_NAME + ".csv");
        Assert.assertTrue(countLineInCsvFile(speciesListFile) > 1);

        // HL.csv
        File speciesLengthFile = new File(root, AggRdbSpecification.HL_SHEET_NAME + ".csv");
        Assert.assertTrue(countLineInCsvFile(speciesLengthFile) > 1);
    }

    @Test
    public void aggregateProductRdb() throws IOException {

        IExtractionType parent = productService.getByLabel(fixtures.getRdbProductLabel(0), null);

        ExtractionProductVO product = createAggProduct(AggExtractionTypeEnum.AGG_RDB, parent);

        AggregationStrataVO strata = AggregationStrataVO.builder()
            .spatialColumnName(ProductRdbStation.COLUMN_STATISTICAL_RECTANGLE)
            .timeColumnName(ProductRdbStation.COLUMN_YEAR)
            .build();

        // Prepare a filter on year
        ExtractionFilterVO filter = new ExtractionFilterVO();
        {
            ExtractionFilterCriterionVO yearCriterion = ExtractionFilterCriterionVO.builder()
                .sheetName(RdbSpecification.TR_SHEET_NAME)
                .name(RdbSpecification.COLUMN_YEAR)
                .operator("=")
                .value(String.valueOf(fixtures.getYearRdbProduct()))
                .build();
            filter.setCriteria(ImmutableList.of(yearCriterion));
        }

        File outputFile = service.executeAndDump(product, filter, strata);
        File root = unpack(outputFile, product);

        // HH.csv
        File stationFile = new File(root, AggRdbSpecification.HH_SHEET_NAME + ".csv");
        Assert.assertTrue(countLineInCsvFile(stationFile) > 1);

        // SL.csv
        File speciesListFile = new File(root, AggRdbSpecification.SL_SHEET_NAME + ".csv");
        Assert.assertTrue(countLineInCsvFile(speciesListFile) > 1);

        // HL.csv
        // Fixme BLA: Cannot link HL rowsto SL rows, so the generated HL is empty
        //  - tests data mistake: P01_RDB_SPECIES_LENGTH should have same columns as SL rows, to be able to link to SL
        //File speciesLengthFile = new File(root, AggRdbSpecification.HL_SHEET_NAME + ".csv");
        //Assert.assertTrue(countLineInCsvFile(speciesLengthFile) > 1);
    }

    @Test
    public void aggregateSurvivalTest() throws IOException {

        IExtractionType type = AggExtractionTypeEnum.AGG_SURVIVAL_TEST;

        AggregationStrataVO strata = new AggregationStrataVO();
        strata.setSpatialColumnName(ProductRdbStation.COLUMN_STATISTICAL_RECTANGLE);
        strata.setTimeColumnName(ProductRdbStation.COLUMN_YEAR);

        File outputFile = service.executeAndDump(type, null, strata);
        Assert.assertTrue(outputFile.exists());
        File root = unpack(outputFile, type);

        // ST.csv
        File survivalTestFile = new File(root, AggSurvivalTestSpecification.ST_SHEET_NAME + ".csv");
        Assert.assertTrue(countLineInCsvFile(survivalTestFile) > 1);

        // RL.csv
        File releaseFile = new File(root, AggSurvivalTestSpecification.RL_SHEET_NAME + ".csv");
        Assert.assertTrue(releaseFile.exists()); // No release DATA in the test DB
    }

    @Test
    public void executeAggCost() throws IOException {
        IExtractionType source = LiveExtractionTypeEnum.COST;

        AggregationStrataVO strata = new AggregationStrataVO();
        strata.setSpatialColumnName(ProductRdbStation.COLUMN_STATISTICAL_RECTANGLE);
        strata.setTimeColumnName(AggRdbSpecification.COLUMN_QUARTER);

        ExtractionFilterVO filter = ExtractionFilterVO.builder()
            .sheetName(AggRjbTripSpecification.HH_SHEET_NAME)
            .criteria(ImmutableList.of(
                ExtractionFilterCriterionVO.builder()
                    .sheetName(AggRjbTripSpecification.HH_SHEET_NAME)
                    .name(ProductRdbStation.COLUMN_YEAR)
                    .operator(ExtractionFilterOperatorEnum.EQUALS.getSymbol())
                    .value(""+fixtures.getYearRawData())
                    .build()
            ))
            .build();

        File outputFile = service.executeAndDump(source, filter, strata);
        Assert.assertTrue(outputFile.exists());
        File root = unpack(outputFile, source);

        // HH.csv
        File stationFile = new File(root, AggRjbTripSpecification.HH_SHEET_NAME + ".csv");
        Assert.assertTrue(countLineInCsvFile(stationFile) > 1);

        // SL.csv
        File speciesListFile = new File(root, AggRjbTripSpecification.SL_SHEET_NAME + ".csv");
        Assert.assertTrue(countLineInCsvFile(speciesListFile) > 1);

        // HL.csv
        File speciesLengthFile = new File(root, AggRjbTripSpecification.HL_SHEET_NAME + ".csv");
        Assert.assertTrue(countLineInCsvFile(speciesLengthFile) > 1);
    }

    @Test
    public void executeAggFree1() throws IOException {

        IExtractionType source = LiveExtractionTypeEnum.FREE1;

        AggregationStrataVO strata = new AggregationStrataVO();
        strata.setSpatialColumnName(ProductRdbStation.COLUMN_STATISTICAL_RECTANGLE);
        strata.setTimeColumnName(AggRdbSpecification.COLUMN_QUARTER);

        ExtractionFilterVO filter = ExtractionFilterVO.builder()
            .sheetName(AggRjbTripSpecification.HH_SHEET_NAME)
            .criteria(ImmutableList.of(
                ExtractionFilterCriterionVO.builder()
                    .sheetName(AggRjbTripSpecification.HH_SHEET_NAME)
                    .name(ProductRdbStation.COLUMN_YEAR)
                    .operator(ExtractionFilterOperatorEnum.EQUALS.getSymbol())
                    .value(""+fixtures.getYearRawData())
                    .build()
            ))
            .build();

        File outputFile = service.executeAndDump(source, filter, strata);
        Assert.assertTrue(outputFile.exists());
        File root = unpack(outputFile, source);

        // HH.csv
        File stationFile = new File(root, AggRjbTripSpecification.HH_SHEET_NAME + ".csv");
        Assert.assertTrue(countLineInCsvFile(stationFile) > 1);

        // SL.csv
        File speciesListFile = new File(root, AggRjbTripSpecification.SL_SHEET_NAME + ".csv");
        Assert.assertTrue(countLineInCsvFile(speciesListFile) > 1);

        // HL.csv
//        File speciesLengthFile = new File(root, AggRjbTripSpecification.HL_SHEET_NAME + ".csv");
//        Assert.assertTrue(countLineInCsvFile(speciesLengthFile) > 1);
    }

    @Test
    @Ignore
    // FIXME: add BATCH on RJB species, with individual count only (no weights) in ADAP XML data
    public void executeAggRjbTrip() throws IOException {

        IExtractionType type = AggExtractionTypeEnum.AGG_RJB_TRIP;

        AggregationStrataVO strata = new AggregationStrataVO();
        strata.setSpatialColumnName(ProductRdbStation.COLUMN_STATISTICAL_RECTANGLE);
        strata.setTimeColumnName(ProductRdbStation.COLUMN_YEAR);

        ExtractionFilterVO filter = ExtractionFilterVO.builder()
            .sheetName(AggRjbTripSpecification.HH_SHEET_NAME)
            .criteria(ImmutableList.of(
                ExtractionFilterCriterionVO.builder()
                    .name(ProductRdbStation.COLUMN_TRIP_CODE)
                    .operator(ExtractionFilterOperatorEnum.EQUALS.getSymbol())
                    .value("1379") // a ADAP RJB trip
                    .build()
            ))
            .build();

        try {
            File outputFile = service.executeAndDump(type, filter, strata);
            Assert.assertTrue(outputFile.exists());
            File root = unpack(outputFile, type);

            // HH.csv
            File stationFile = new File(root, AggRjbTripSpecification.HH_SHEET_NAME + ".csv");
            Assert.assertTrue(countLineInCsvFile(stationFile) > 1);

            // SL.csv
            File speciesListFile = new File(root, AggRjbTripSpecification.SL_SHEET_NAME + ".csv");
            Assert.assertTrue(countLineInCsvFile(speciesListFile) > 1);

            // HL.csv
            File speciesLengthFile = new File(root, AggRjbTripSpecification.HL_SHEET_NAME + ".csv");
            Assert.assertTrue(countLineInCsvFile(speciesLengthFile) > 1);
        }
        catch (DataNotFoundException e) {
            Assume.assumeNoException("No RJB data found (Add RBJ into BATCH table - with individualCount and no weight)", e);
        }
    }

    @Test
    public void executeAndReadAggSurvivalTest() {

        IExtractionType type = AggExtractionTypeEnum.AGG_SURVIVAL_TEST;

        ExtractionFilterVO filter = ExtractionFilterVO.builder()
            .sheetName(AggRdbSpecification.SL_SHEET_NAME)
            .build();

        filter.setCriteria(ImmutableList.of(ExtractionFilterCriterionVO.builder()
                .sheetName(AggRdbSpecification.HH_SHEET_NAME)
                .name(AggRdbSpecification.COLUMN_YEAR)
                .operator("=")
                .value(""+fixtures.getYearRawData())
            .build()));

        AggregationStrataVO strata = AggregationStrataVO.builder()
            .sheetName(AggRdbSpecification.SL_SHEET_NAME)
            .spatialColumnName(AggRdbSpecification.COLUMN_AREA)
            .timeColumnName(AggRdbSpecification.COLUMN_MONTH)
            .build();

        ExtractionResultVO result = service.executeAndRead(
            type,
            filter, strata, Page.builder().size(100).build());

        Assert.assertNotNull(result);
        Assert.assertNotNull(result.getRows());
        Assert.assertTrue(result.getRows().size() > 0);

        Preconditions.checkArgument(result instanceof AggregationResultVO);
        AggregationResultVO aggResult = (AggregationResultVO)result;
        Assert.assertNotNull(aggResult.getSpaceStrata());
        Assert.assertTrue(aggResult.getSpaceStrata().size() > 0);
        Assert.assertTrue(aggResult.getSpaceStrata().contains(AggRdbSpecification.COLUMN_AREA));

        Assert.assertNotNull(aggResult.getTimeStrata());
        Assert.assertTrue(aggResult.getTimeStrata().size() > 0);
        Assert.assertTrue(aggResult.getTimeStrata().contains(AggRdbSpecification.COLUMN_MONTH));
        Assert.assertTrue(aggResult.getTimeStrata().contains(AggRdbSpecification.COLUMN_QUARTER));
        Assert.assertTrue(aggResult.getTimeStrata().contains(AggRdbSpecification.COLUMN_YEAR));
    }

    @Test
    public void readAggSurvivalTest() {
        ExtractionProductVO type = createAggProduct(AggExtractionTypeEnum.AGG_SURVIVAL_TEST);
        ExtractionProductVO savedProduct;
        // Save
        try {
            savedProduct = service.executeAndSave(type, null, null);
            Assume.assumeNotNull(savedProduct);
            Assume.assumeNotNull(savedProduct.getId());
        }
        catch (Exception e) {
            Assume.assumeNoException(String.format("Error during aggregating: %s", type), e);
            return;
        }

        ExtractionFilterVO filter = ExtractionFilterVO.builder()
            .sheetName(AggRdbSpecification.HH_SHEET_NAME)
            .criteria(ImmutableList.of(
                    ExtractionFilterCriterionVO.builder()
                        .sheetName(AggRdbSpecification.HH_SHEET_NAME)
                        .name(AggRdbSpecification.COLUMN_YEAR)
                        .operator("=")
                        .value(""+fixtures.getYearRawData())
                        .build()
                )
            ).build();

        AggregationStrataVO strata = AggregationStrataVO.builder()
            .spatialColumnName(AggRdbSpecification.COLUMN_AREA)
            .build();

        ExtractionResultVO result = service.read(savedProduct, filter, strata, Page.builder().size(1000).build());

        Assert.assertNotNull(result);
        Assert.assertNotNull(result.getRows());
        Assert.assertTrue(result.getRows().size() > 0);
    }

    @Test
    public void readAggTechSurvivalTest() {

        ExtractionProductVO type = createAggProduct(AggExtractionTypeEnum.AGG_SURVIVAL_TEST);
        ExtractionProductVO savedProduct;
        // Save
        try {
            savedProduct = service.executeAndSave(type, null, null);
            Assume.assumeNotNull(savedProduct);
            Assume.assumeNotNull(savedProduct.getId());
        }
        catch (Exception e) {
            Assume.assumeNoException(String.format("Error during aggregating: %s", type), e);
            return;
        }

        AggregationStrataVO strata = AggregationStrataVO.builder()
            .sheetName(AggRdbSpecification.HH_SHEET_NAME)
            .aggColumnName(AggRdbSpecification.COLUMN_FISHING_TIME)
            .spatialColumnName(AggRdbSpecification.COLUMN_SQUARE)
            .techColumnName(AggRdbSpecification.COLUMN_GEAR_TYPE)
            .timeColumnName(AggRdbSpecification.COLUMN_YEAR)
            .build();

        ExtractionFilterVO filter = ExtractionFilterVO.builder()
            .sheetName(RdbSpecification.HH_SHEET_NAME)
            .build();

        filter.setCriteria(ImmutableList.of(ExtractionFilterCriterionVO.builder()
                .sheetName(AggRdbSpecification.HH_SHEET_NAME)
                .name(AggRdbSpecification.COLUMN_YEAR)
                .operator("=")
                .value(""+fixtures.getYearRawData())
            .build()));

        // 2. Access to tech aggregation
        AggregationTechResultVO result = service.readByTech(savedProduct, filter, strata, null, null);
        Assert.assertNotNull(result);
        Assert.assertNotNull(result.getData());
        Assert.assertTrue(result.getData().size() > 0);
    }

    @Test
    public void updatePmfmTrip() {
        IExtractionType type = createProduct(LiveExtractionTypeEnum.PMFM_TRIP);
        ExtractionProductVO savedProduct;
        // Save product
        try {
            // Prepare a filter, on year + project
            ExtractionFilterVO filter = new ExtractionFilterVO();
            {
                ExtractionFilterCriterionVO projectCriterion = ExtractionFilterCriterionVO.builder()
                    .sheetName(PmfmTripSpecification.TR_SHEET_NAME)
                    .name(PmfmTripSpecification.COLUMN_PROJECT)
                    .operator("=")
                    .value(fixtures.getProgramLabelForPmfmExtraction(0))
                    .build();

                ExtractionFilterCriterionVO yearCriterion = ExtractionFilterCriterionVO.builder()
                    .sheetName(PmfmTripSpecification.TR_SHEET_NAME)
                    .name(PmfmTripSpecification.COLUMN_YEAR)
                    .operator("=")
                    .value("" + fixtures.getYearRawData())
                    .build();

                filter.setCriteria(ImmutableList.of(projectCriterion, yearCriterion));
            }

            // Execute and save the product
            savedProduct = service.executeAndSave(type, filter, null);
            Assume.assumeNotNull(savedProduct);
            Assume.assumeNotNull(savedProduct.getId());
            Assume.assumeNotNull(type.getFormat(), savedProduct.getFormat());
            Assume.assumeNotNull(type.getVersion(), savedProduct.getVersion());
        }
        catch (Exception e) {
            Assume.assumeNoException(e);
            return;
        }

        // Update product
        ExtractionProductVO updatedProduct = service.executeAndSave(savedProduct.getId());
        Assert.assertNotNull(updatedProduct);
        Assert.assertEquals(savedProduct.getId(), updatedProduct.getId()); // Same id
        Assert.assertEquals(savedProduct.getLabel(), updatedProduct.getLabel()); // Same label
    }

    @Test
    public void updateAggRdbProduct() {

        ExtractionProductVO savedProduct;
        try {
            IExtractionType parent = productService.getByLabel(fixtures.getRdbProductLabel(0), null);
            ExtractionProductVO product = createAggProduct(AggExtractionTypeEnum.AGG_RDB, parent);

            // Prepare a filter on year
            ExtractionFilterVO filter = ExtractionFilterVO.builder()
                .criteria(ImmutableList.of(ExtractionFilterCriterionVO.builder()
                    .sheetName(RdbSpecification.TR_SHEET_NAME)
                    .name(RdbSpecification.COLUMN_YEAR)
                    .operator("=")
                    .value("" + fixtures.getYearRdbProduct())
                    .build()))
                .build();
            product.setFilterContent(objectMapper.writeValueAsString(filter));

            // Prepare strata
            product.setStratum(ImmutableList.of(AggregationStrataVO.builder()
                .sheetName(AggRdbSpecification.HH_SHEET_NAME)
                .spatialColumnName(AggRjbTripSpecification.COLUMN_STATISTICAL_RECTANGLE)
                .timeColumnName(AggRjbTripSpecification.COLUMN_YEAR)
                .aggColumnName(AggRdbSpecification.COLUMN_FISHING_TIME)
                .techColumnName(AggRdbSpecification.COLUMN_GEAR_TYPE)
                .build()));

            // First execution
            savedProduct = productService.save(product);
            Assume.assumeNotNull(savedProduct);
            Assume.assumeNotNull(savedProduct.getId());
            Assume.assumeNotNull(savedProduct.getParentId());
        } catch (Exception e) {
            Assume.assumeNoException(e);
            return;
        }

        // Update product
        ExtractionProductVO updatedProduct = service.executeAndSave(savedProduct.getId());
        Assert.assertNotNull(updatedProduct);
        Assert.assertEquals(savedProduct.getId(), updatedProduct.getId()); // Same id
        Assert.assertEquals(savedProduct.getLabel(), updatedProduct.getLabel()); // Same label
    }


    /* -- protected methods -- */

    protected void assertHasColumn(File file, String columnName) throws IOException {
        String headerName = StringUtils.underscoreToChangeCase(columnName);
        Assert.assertTrue(String.format("Missing header '%s' in file: %s", headerName, file.getPath()),
            hasHeaderInCsvFile(file, headerName));
    }

}

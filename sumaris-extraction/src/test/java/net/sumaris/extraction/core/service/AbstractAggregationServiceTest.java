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
import net.sumaris.core.model.technical.extraction.ExtractionCategoryEnum;
import net.sumaris.core.model.technical.extraction.IExtractionType;
import net.sumaris.core.model.technical.extraction.rdb.ProductRdbStation;
import net.sumaris.core.vo.technical.extraction.AggregationStrataVO;
import net.sumaris.core.vo.technical.extraction.ExtractionProductVO;
import net.sumaris.extraction.core.specification.data.trip.*;
import net.sumaris.extraction.core.type.AggExtractionTypeEnum;
import net.sumaris.extraction.core.type.LiveExtractionTypeEnum;
import net.sumaris.extraction.core.vo.*;
import org.junit.*;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.File;
import java.io.IOException;

/**
 * @author peck7 on 17/12/2018.
 */
public abstract class AbstractAggregationServiceTest extends AbstractServiceTest {

    @Autowired
    private AggregationService aggregationService;

    @Autowired
    private ExtractionProductService productService;
    @Autowired
    private ExtractionManager extractionManager;

    @Autowired
    private ObjectMapper objectMapper;

    private String yearRawData;
    private String yearRdbProduct;

    @Before
    public void setUp() {
        yearRawData = String.valueOf(fixtures.getYearRawData());
        yearRdbProduct = String.valueOf(fixtures.getYearRdbProduct());
    }

    @Test
    public void aggregateLiveRdb() throws IOException {

        IExtractionType type = AggExtractionTypeEnum.AGG_RDB;

        AggregationStrataVO strata = new AggregationStrataVO();
        strata.setSpatialColumnName(ProductRdbStation.COLUMN_STATISTICAL_RECTANGLE);
        strata.setTimeColumnName(ProductRdbStation.COLUMN_YEAR);

        File outputFile = extractionManager.executeAndDump(type, null, strata);
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
                .value(yearRdbProduct)
                .build();
            filter.setCriteria(ImmutableList.of(yearCriterion));
        }

        File outputFile = extractionManager.executeAndDump(product, filter, strata);
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

        File outputFile = extractionManager.executeAndDump(type, null, strata);
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
    public void aggregateLiveCost() throws IOException {
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
                    .value(yearRawData)
                    .build()
            ))
            .build();

        File outputFile = extractionManager.executeAndDump(source, filter, strata);
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
    public void aggregateFree1() throws IOException {

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
                    .value(yearRawData)
                    .build()
            ))
            .build();

        File outputFile = extractionManager.executeAndDump(source, filter, strata);
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
    public void aggregateRjbTrip() throws IOException {

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
            File outputFile = extractionManager.executeAndDump(type, filter, strata);
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

        ExtractionFilterVO filter = new ExtractionFilterVO();
        filter.setSheetName(AggRdbSpecification.SL_SHEET_NAME);

        ExtractionFilterCriterionVO criterion = new ExtractionFilterCriterionVO() ;
        filter.setCriteria(ImmutableList.of(criterion));
        criterion.setSheetName(AggRdbSpecification.HH_SHEET_NAME);
        criterion.setName(AggRdbSpecification.COLUMN_YEAR);
        criterion.setOperator("=");
        criterion.setValue(yearRawData);

        AggregationStrataVO strata = new AggregationStrataVO();
        strata.setSheetName(AggRdbSpecification.SL_SHEET_NAME);
        strata.setSpatialColumnName(AggRdbSpecification.COLUMN_AREA);
        strata.setTimeColumnName(AggRdbSpecification.COLUMN_MONTH);

        ExtractionResultVO result = extractionManager.executeAndRead(
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
    public void save() {

        // Live extraction product
        {
            ExtractionProductVO product = createProduct(LiveExtractionTypeEnum.SURVIVAL_TEST);

            // 1. Create the aggregation product
            ExtractionProductVO savedProduct = productService.save(product);
            Assert.assertNotNull(savedProduct);
            Assert.assertNotNull(savedProduct.getId());
            Assert.assertNull(savedProduct.getParentId()); // No parent product
        }

        // Extraction linked to another source product
        {
            IExtractionType parent = productService.getByLabel(fixtures.getRdbProductLabel(0), null);

            ExtractionProductVO product = createAggProduct(AggExtractionTypeEnum.AGG_RDB, parent);

            // 1. Create the aggregation product
            ExtractionProductVO savedProduct = productService.save(product);
            Assert.assertNotNull(savedProduct);
            Assert.assertNotNull(savedProduct.getId());
            Assert.assertNotNull(savedProduct.getParentId()); // Make sure to keep link with source product
        }
    }

    @Test
    public void readBySpace() {
        ExtractionProductVO type = createAggProduct(AggExtractionTypeEnum.AGG_SURVIVAL_TEST);
        ExtractionProductVO savedProduct;
        // Save
        try {
            savedProduct = extractionManager.executeAndSave(type, null, null);
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
                    .value(yearRawData)
                    .build()
                )
            ).build();

        AggregationStrataVO strata = AggregationStrataVO.builder()
            .spatialColumnName(AggRdbSpecification.COLUMN_AREA)
            .build();

        AggregationResultVO result = aggregationService.readBySpace(savedProduct, filter, strata, Page.builder().size(100).build());

        Assert.assertNotNull(result);
        Assert.assertNotNull(result.getRows());
        Assert.assertTrue(result.getRows().size() > 0);
    }

    @Test
    public void readByTech() {

        IExtractionType parent = productService.getByLabel(fixtures.getRdbProductLabel(0), null);

        ExtractionProductVO type = createAggProduct(AggExtractionTypeEnum.AGG_RDB, parent);

        // 1. Create the aggregation product
        ExtractionProductVO savedType = extractionManager.executeAndSave(type, null, null);
        Assert.assertNotNull(savedType);
        Assert.assertNotNull(savedType.getId());
        Assert.assertNotNull(savedType.getParentId()); // Make sure to keep link with source product

        AggregationStrataVO strata = new AggregationStrataVO();
        strata.setSheetName(AggRdbSpecification.HH_SHEET_NAME);
        strata.setAggColumnName(AggRdbSpecification.COLUMN_FISHING_TIME);
        strata.setSpatialColumnName(AggRdbSpecification.COLUMN_SQUARE);
        strata.setTechColumnName(AggRdbSpecification.COLUMN_GEAR_TYPE);
        strata.setTimeColumnName(AggRdbSpecification.COLUMN_YEAR);
        savedType.setStratum(ImmutableList.of(strata));

        ExtractionFilterVO filter = new ExtractionFilterVO();
        filter.setSheetName(RdbSpecification.HH_SHEET_NAME);

        ExtractionFilterCriterionVO criterion = new ExtractionFilterCriterionVO() ;
        criterion.setSheetName(AggRdbSpecification.HH_SHEET_NAME);
        criterion.setName(AggRdbSpecification.COLUMN_YEAR);
        criterion.setOperator("=");
        criterion.setValue(yearRdbProduct);
        filter.setCriteria(ImmutableList.of(criterion));

        // 2. Access to tech aggregation
        AggregationTechResultVO result = aggregationService.readByTech(savedType, filter, strata, null, null);
        Assert.assertNotNull(result);
        Assert.assertNotNull(result.getData());
        Assert.assertTrue(result.getData().size() > 0);
    }

    @Test
    public void updatePmfmTripProduct() {
        ExtractionProductVO product = createProduct(LiveExtractionTypeEnum.PMFM_TRIP);

        // Prepare a filter, on year + project
        ExtractionFilterVO filter = new ExtractionFilterVO();
        {
            ExtractionFilterCriterionVO projectCriterion = new ExtractionFilterCriterionVO();
            projectCriterion.setSheetName(PmfmTripSpecification.TR_SHEET_NAME);
            projectCriterion.setName(PmfmTripSpecification.COLUMN_PROJECT);
            projectCriterion.setOperator("=");
            projectCriterion.setValue(fixtures.getProgramLabelForPmfmExtraction(0));

            ExtractionFilterCriterionVO yearCriterion = new ExtractionFilterCriterionVO();
            yearCriterion.setSheetName(PmfmTripSpecification.TR_SHEET_NAME);
            yearCriterion.setName(PmfmTripSpecification.COLUMN_YEAR);
            yearCriterion.setOperator("=");
            yearCriterion.setValue(yearRawData);

            filter.setCriteria(ImmutableList.of(projectCriterion, yearCriterion));
        }

        // First live execution
        ExtractionProductVO savedProduct = extractionManager.executeAndSave(product, filter, null);
        Assume.assumeNotNull(savedProduct);
        Assume.assumeNotNull(savedProduct.getId());

        // Product update
        ExtractionProductVO updatedProduct = extractionManager.executeAndSave(savedProduct.getId());
        Assert.assertNotNull(updatedProduct);
        Assert.assertEquals(savedProduct.getId(), updatedProduct.getId()); // Same id
        Assert.assertEquals(savedProduct.getLabel(), updatedProduct.getLabel()); // Same label
    }

    @Test
    public void updateAggRdbProduct() throws JsonProcessingException {

        IExtractionType parent = productService.getByLabel(fixtures.getRdbProductLabel(0), null);

        ExtractionProductVO product = createAggProduct(AggExtractionTypeEnum.AGG_RDB, parent);

        // Prepare a filter on year
        ExtractionFilterVO filter = new ExtractionFilterVO();
        {
            ExtractionFilterCriterionVO yearCriterion = new ExtractionFilterCriterionVO();
            yearCriterion.setSheetName(RdbSpecification.TR_SHEET_NAME);
            yearCriterion.setName(RdbSpecification.COLUMN_YEAR);
            yearCriterion.setOperator("=");
            yearCriterion.setValue(yearRdbProduct);

            filter.setCriteria(ImmutableList.of(yearCriterion));
        }
        product.setFilterContent(objectMapper.writeValueAsString(filter));

        // Prepare strata
        AggregationStrataVO strata = new AggregationStrataVO();
        strata.setSheetName(AggRdbSpecification.HH_SHEET_NAME);
        strata.setSpatialColumnName(AggRjbTripSpecification.COLUMN_STATISTICAL_RECTANGLE);
        strata.setTimeColumnName(AggRjbTripSpecification.COLUMN_YEAR);
        strata.setAggColumnName(AggRdbSpecification.COLUMN_FISHING_TIME);
        strata.setTechColumnName(AggRdbSpecification.COLUMN_GEAR_TYPE);
        product.setStratum(ImmutableList.of(strata));

        // First execution
        ExtractionProductVO savedProduct = productService.save(product);
        Assume.assumeNotNull(savedProduct);
        Assume.assumeNotNull(savedProduct.getId());
        Assume.assumeNotNull(savedProduct.getParentId());
        Assert.assertEquals(product.getLabel(), savedProduct.getLabel());

        // Product update
        ExtractionProductVO updatedProduct = extractionManager.executeAndSave(savedProduct.getId());
        Assert.assertNotNull(updatedProduct);
        Assert.assertEquals(savedProduct.getId(), updatedProduct.getId()); // Same id
        Assert.assertEquals(savedProduct.getLabel(), updatedProduct.getLabel()); // Same label
    }

    /* -- protected methods --*/

}

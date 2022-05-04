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

import com.google.common.collect.ImmutableList;
import lombok.NonNull;
import net.sumaris.core.dao.technical.Page;
import net.sumaris.extraction.core.DatabaseResource;
import net.sumaris.extraction.core.specification.data.trip.*;
import net.sumaris.extraction.core.util.ExtractionProducts;
import net.sumaris.core.model.technical.extraction.IExtractionFormat;
import net.sumaris.extraction.core.format.LiveFormatEnum;
import net.sumaris.core.model.referential.StatusEnum;
import net.sumaris.core.model.technical.extraction.ExtractionCategoryEnum;
import net.sumaris.core.model.technical.extraction.rdb.ProductRdbStation;
import net.sumaris.core.vo.administration.user.DepartmentVO;
import net.sumaris.core.vo.technical.extraction.AggregationStrataVO;
import net.sumaris.extraction.core.vo.*;
import org.junit.*;
import org.springframework.beans.factory.annotation.Autowired;

import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;

/**
 * @author peck7 on 17/12/2018.
 */
public class AggregationServiceTest extends AbstractServiceTest {

    @ClassRule
    public static final DatabaseResource dbResource = DatabaseResource.writeDb();

    @Autowired
    private AggregationService aggregationService;

    @Autowired
    private ExtractionService extractionService;

    private String yearRawData;
    private String yearRdbProduct;

    @Before
    public void setUp() {
        yearRawData = String.valueOf(fixtures.getYearRawData());
        yearRdbProduct = String.valueOf(fixtures.getYearRdbProduct());
    }

    @Test
    public void aggregateLiveRdb() throws IOException {


        AggregationStrataVO strata = new AggregationStrataVO();
        strata.setSpatialColumnName(ProductRdbStation.COLUMN_STATISTICAL_RECTANGLE);
        strata.setTimeColumnName(ProductRdbStation.COLUMN_YEAR);

        File outputFile = aggregationService.aggregateAndDump(LiveFormatEnum.RDB, null, strata);
        File root = unpack(outputFile, LiveFormatEnum.RDB);

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

        AggregationTypeVO type = new AggregationTypeVO();
        type.setCategory(ExtractionCategoryEnum.PRODUCT);
        type.setLabel(fixtures.getRdbProductLabel(0));

        AggregationStrataVO strata = new AggregationStrataVO();
        strata.setSpatialColumnName(ProductRdbStation.COLUMN_STATISTICAL_RECTANGLE);
        strata.setTimeColumnName(ProductRdbStation.COLUMN_YEAR);

        File outputFile = aggregationService.aggregateAndDump(type, null, strata);
        File root = unpack(outputFile, type);

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

        AggregationTypeVO type = new AggregationTypeVO();
        type.setCategory(ExtractionCategoryEnum.LIVE);
        type.setLabel(LiveFormatEnum.SURVIVAL_TEST.name());

        AggregationStrataVO strata = new AggregationStrataVO();
        strata.setSpatialColumnName(ProductRdbStation.COLUMN_STATISTICAL_RECTANGLE);
        strata.setTimeColumnName(ProductRdbStation.COLUMN_YEAR);

        File outputFile = aggregationService.aggregateAndDump(type, null, strata);
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
    public void aggregateCost() throws IOException {

        AggregationTypeVO source = aggregationService.getTypeByFormat(LiveFormatEnum.COST);

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

        File outputFile = aggregationService.aggregateAndDump(source, filter, strata);
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

        AggregationTypeVO source = aggregationService.getTypeByFormat(LiveFormatEnum.FREE1);

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

        File outputFile = aggregationService.aggregateAndDump(source, filter, strata);
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

        AggregationTypeVO type = new AggregationTypeVO();
        type.setCategory(ExtractionCategoryEnum.LIVE);
        type.setLabel(LiveFormatEnum.RJB_TRIP.name());

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

        File outputFile = aggregationService.aggregateAndDump(type, filter, strata);
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

    @Test
    public void executeAndRead() {

        AggregationTypeVO source = aggregationService.getTypeByFormat(LiveFormatEnum.SURVIVAL_TEST);

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

        AggregationResultVO result = aggregationService.aggregateAndRead(
            source,
            filter, strata, Page.builder().size(100).build());

        Assert.assertNotNull(result);
        Assert.assertNotNull(result.getRows());
        Assert.assertTrue(result.getRows().size() > 0);

        Assert.assertNotNull(result.getSpaceStrata());
        Assert.assertTrue(result.getSpaceStrata().size() > 0);
        Assert.assertTrue(result.getSpaceStrata().contains(AggRdbSpecification.COLUMN_AREA));

        Assert.assertNotNull(result.getTimeStrata());
        Assert.assertTrue(result.getTimeStrata().size() > 0);
        Assert.assertTrue(result.getTimeStrata().contains(AggRdbSpecification.COLUMN_MONTH));
        Assert.assertTrue(result.getTimeStrata().contains(AggRdbSpecification.COLUMN_QUARTER));
        Assert.assertTrue(result.getTimeStrata().contains(AggRdbSpecification.COLUMN_YEAR));
    }

    @Test
    public void save() {

        // Live extraction product
        {
            AggregationTypeVO source = aggregationService.getTypeByFormat(LiveFormatEnum.SURVIVAL_TEST);

            AggregationTypeVO type = createAggType(ExtractionCategoryEnum.PRODUCT, source);

            // 1. Create the aggregation product
            AggregationTypeVO savedType = aggregationService.save(type, null);
            Assert.assertNotNull(savedType);
            Assert.assertNotNull(savedType.getId());
            Assert.assertNull(savedType.getParentId()); // No parent product
        }

        // Extraction linked to another source product
        {
            AggregationTypeVO source = new AggregationTypeVO();
            source.setCategory(ExtractionCategoryEnum.PRODUCT);
            source.setLabel(fixtures.getRdbProductLabel(0));

            AggregationTypeVO type = createAggType(ExtractionCategoryEnum.PRODUCT, source);

            // 1. Create the aggregation product
            AggregationTypeVO savedType = aggregationService.save(type, null);
            Assert.assertNotNull(savedType);
            Assert.assertNotNull(savedType.getId());
            Assert.assertNotNull(savedType.getParentId()); // Make sure to keep link with source product
        }
    }

    @Test
    public void saveThenGetAggBySpace() {
        AggregationTypeVO type = createAggType(ExtractionCategoryEnum.LIVE, LiveFormatEnum.SURVIVAL_TEST);
        type.setLabel(ExtractionProducts.getProductLabel(LiveFormatEnum.SURVIVAL_TEST, System.currentTimeMillis()));

        // Save
        AggregationTypeVO savedType = aggregationService.save(type, null);
        Assert.assertNotNull(savedType);
        Assert.assertNotNull(savedType.getId());

        ExtractionFilterVO filter = new ExtractionFilterVO();
        filter.setSheetName(AggRdbSpecification.HH_SHEET_NAME);

        ExtractionFilterCriterionVO criterion = new ExtractionFilterCriterionVO() ;
        filter.setCriteria(ImmutableList.of(criterion));
        criterion.setSheetName(AggRdbSpecification.HH_SHEET_NAME);
        criterion.setName(AggRdbSpecification.COLUMN_YEAR);
        criterion.setOperator("=");
        criterion.setValue(yearRawData);

        AggregationStrataVO strata = new AggregationStrataVO();
        strata.setSpatialColumnName(AggRdbSpecification.COLUMN_AREA);

        AggregationResultVO result = aggregationService.getAggBySpace(savedType, filter, strata, Page.builder().size(100).build());

        Assert.assertNotNull(result);
        Assert.assertNotNull(result.getRows());
        Assert.assertTrue(result.getRows().size() > 0);
    }

    @Test
    public void readTechProductRdb() {

        AggregationTypeVO source = new AggregationTypeVO();
        source.setCategory(ExtractionCategoryEnum.PRODUCT);
        source.setLabel(fixtures.getRdbProductLabel(0));

        AggregationTypeVO type = createAggType(ExtractionCategoryEnum.PRODUCT, source);

        // 1. Create the aggregation product
        AggregationTypeVO savedType = aggregationService.save(type, null);
        Assert.assertNotNull(savedType);
        Assert.assertNotNull(savedType.getId());
        Assert.assertNotNull(savedType.getParentId()); // Make sure to keep link with source product

        AggregationStrataVO strata = new AggregationStrataVO();
        strata.setSheetName(AggRdbSpecification.HH_SHEET_NAME);
        strata.setAggColumnName(AggRdbSpecification.COLUMN_FISHING_TIME);
        strata.setSpatialColumnName(AggRdbSpecification.COLUMN_SQUARE);
        strata.setTechColumnName(AggRdbSpecification.COLUMN_GEAR_TYPE);
        strata.setTimeColumnName(AggRdbSpecification.COLUMN_YEAR);
        source.setStratum(ImmutableList.of(strata));

        ExtractionFilterVO filter = new ExtractionFilterVO();
        filter.setSheetName(RdbSpecification.HH_SHEET_NAME);

        ExtractionFilterCriterionVO criterion = new ExtractionFilterCriterionVO() ;
        criterion.setSheetName(AggRdbSpecification.HH_SHEET_NAME);
        criterion.setName(AggRdbSpecification.COLUMN_YEAR);
        criterion.setOperator("=");
        criterion.setValue(yearRdbProduct);
        filter.setCriteria(ImmutableList.of(criterion));

        // 2. Access to tech aggregation
        AggregationTechResultVO result = aggregationService.getAggByTech(savedType, filter, strata, null, null);
        Assert.assertNotNull(result);
        Assert.assertNotNull(result.getData());
        Assert.assertTrue(result.getData().size() > 0);
    }

    @Test
    public void updateLiveProduct() {
        AggregationTypeVO type = createAggType(ExtractionCategoryEnum.LIVE, LiveFormatEnum.PMFM_TRIP);

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
        ExtractionTypeVO savedType = extractionService.save(type, filter);
        Assume.assumeNotNull(savedType);
        Assume.assumeNotNull(savedType.getId());

        // Product update
        AggregationTypeVO updatedType = aggregationService.updateProduct(savedType.getId());
        Assert.assertNotNull(updatedType);
        Assert.assertNotNull(updatedType.getId());
    }

    @Test
    public void updateAggProduct() {
        AggregationTypeVO source = new AggregationTypeVO();
        source.setCategory(ExtractionCategoryEnum.PRODUCT);
        source.setLabel(fixtures.getRdbProductLabel(0));

        AggregationTypeVO type = createAggType(ExtractionCategoryEnum.PRODUCT, source);

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

        // Prepare strata
        AggregationStrataVO strata = new AggregationStrataVO();
        strata.setSheetName(AggRdbSpecification.HH_SHEET_NAME);
        strata.setSpatialColumnName(AggRjbTripSpecification.COLUMN_STATISTICAL_RECTANGLE);
        strata.setTimeColumnName(AggRjbTripSpecification.COLUMN_YEAR);
        strata.setAggColumnName(AggRdbSpecification.COLUMN_FISHING_TIME);
        strata.setTechColumnName(AggRdbSpecification.COLUMN_GEAR_TYPE);
        type.setStratum(ImmutableList.of(strata));

        // First execution
        AggregationTypeVO savedType = aggregationService.save(type, filter);
        Assume.assumeNotNull(savedType);
        Assume.assumeNotNull(savedType.getId());
        Assert.assertEquals(type.getLabel(), savedType.getLabel());

        // Product update
        AggregationTypeVO updatedType = aggregationService.updateProduct(savedType.getId());
        Assert.assertNotNull(updatedType);
        Assert.assertNotNull(updatedType.getId());
    }

    /* -- protected methods --*/

    protected AggregationTypeVO createAggType(ExtractionCategoryEnum category, IExtractionFormat format) {

        AggregationTypeVO type = new AggregationTypeVO();
        type.setCategory(category);
        // Derived label from format label
        type.setLabel(ExtractionProducts.getProductLabel(format, System.currentTimeMillis()));
        type.setName(String.format("Aggregation on %s (%s) data", format.getLabel(), category.name()));
        type.setStatusId(StatusEnum.TEMPORARY.getId());

        DepartmentVO recDep = new DepartmentVO();
        recDep.setId(fixtures.getDepartmentId(0));
        type.setRecorderDepartment(recDep);

        return type;
    }
}

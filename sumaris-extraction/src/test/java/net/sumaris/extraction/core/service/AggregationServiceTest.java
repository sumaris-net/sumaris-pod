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
import net.sumaris.core.dao.technical.Page;
import net.sumaris.extraction.core.DatabaseResource;
import net.sumaris.extraction.core.specification.data.trip.AggRjbTripSpecification;
import net.sumaris.extraction.core.specification.data.trip.RdbSpecification;
import net.sumaris.extraction.core.util.ExtractionProducts;
import net.sumaris.core.model.technical.extraction.IExtractionFormat;
import net.sumaris.extraction.core.specification.data.trip.AggRdbSpecification;
import net.sumaris.extraction.core.specification.data.trip.AggSurvivalTestSpecification;
import net.sumaris.extraction.core.format.LiveFormatEnum;
import net.sumaris.core.model.referential.StatusEnum;
import net.sumaris.core.model.technical.extraction.ExtractionCategoryEnum;
import net.sumaris.core.model.technical.extraction.rdb.ProductRdbStation;
import net.sumaris.core.vo.administration.user.DepartmentVO;
import net.sumaris.core.vo.technical.extraction.AggregationStrataVO;
import net.sumaris.extraction.core.vo.*;
import org.junit.*;
import org.springframework.beans.factory.annotation.Autowired;

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

    private String yearRawData;
    private String yearRdbProduct;

    @Before
    public void setUp() throws Exception {
        yearRawData = String.valueOf(fixtures.getYearRawData());
        yearRdbProduct = String.valueOf(fixtures.getYearRdbProduct());
    }

    @Test
    public void aggregateLiveRdb() throws IOException {

        AggregationTypeVO type = new AggregationTypeVO();
        type.setCategory(ExtractionCategoryEnum.LIVE);
        type.setLabel(LiveFormatEnum.RDB.name());

        AggregationStrataVO strata = new AggregationStrataVO();
        strata.setSpatialColumnName(ProductRdbStation.COLUMN_STATISTICAL_RECTANGLE);
        strata.setTimeColumnName(ProductRdbStation.COLUMN_YEAR);

        File outputFile = aggregationService.executeAndDump(type, null, strata);
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

        AggregationTypeVO type = new AggregationTypeVO();
        type.setCategory(ExtractionCategoryEnum.PRODUCT);
        type.setLabel(fixtures.getRdbProductLabel(0));

        AggregationStrataVO strata = new AggregationStrataVO();
        strata.setSpatialColumnName(ProductRdbStation.COLUMN_STATISTICAL_RECTANGLE);
        strata.setTimeColumnName(ProductRdbStation.COLUMN_YEAR);

        File outputFile = aggregationService.executeAndDump(type, null, strata);
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

        File outputFile = aggregationService.executeAndDump(type, null, strata);
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
    public void aggregateFree1() throws IOException {

        AggregationTypeVO type = new AggregationTypeVO();
        type.setCategory(ExtractionCategoryEnum.LIVE);
        type.setLabel(LiveFormatEnum.FREE1.getLabel());

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

        File outputFile = aggregationService.executeAndDump(type, filter, strata);
        Assert.assertTrue(outputFile.exists());
        File root = unpack(outputFile, type);

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

        File outputFile = aggregationService.executeAndDump(type, filter, strata);
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

        AggregationTypeVO type = createAggType(ExtractionCategoryEnum.LIVE, LiveFormatEnum.SURVIVAL_TEST);

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

        AggregationResultVO result = aggregationService.executeAndRead(type, filter, strata, Page.builder().size(100).build());

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

    /* -- protected methods --*/

    protected AggregationTypeVO createAggType(ExtractionCategoryEnum category, IExtractionFormat format) {

        AggregationTypeVO type = new AggregationTypeVO();
        type.setCategory(category);
        if (category == ExtractionCategoryEnum.PRODUCT) {
            type.setLabel(ExtractionProducts.getProductLabel(format, System.currentTimeMillis()));
        }
        else {
            type.setLabel(format.getLabel());
        }
        type.setName(String.format("Aggregation on %s (%s) data", format.getLabel(), category.name()));
        type.setStatusId(StatusEnum.TEMPORARY.getId());

        DepartmentVO recDep = new DepartmentVO();
        recDep.setId(fixtures.getDepartmentId(0));
        type.setRecorderDepartment(recDep);

        return type;
    }
}

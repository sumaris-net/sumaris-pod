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

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import net.sumaris.core.extraction.dao.DatabaseResource;
import net.sumaris.core.extraction.specification.AggRdbSpecification;
import net.sumaris.core.extraction.utils.ExtractionRawFormatEnum;
import net.sumaris.core.extraction.vo.*;
import net.sumaris.core.model.referential.StatusEnum;
import net.sumaris.core.model.technical.extraction.rdb.ProductRdbStation;
import net.sumaris.core.model.technical.extraction.rdb.ProductRdbTrip;
import net.sumaris.core.vo.administration.user.DepartmentVO;
import net.sumaris.core.vo.technical.extraction.ExtractionTableColumnVO;
import org.assertj.core.util.Lists;
import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * @author peck7 on 17/12/2018.
 */
public class AggregationServiceTest extends AbstractServiceTest {

    @ClassRule
    public static final DatabaseResource dbResource = DatabaseResource.writeDb();

    @Autowired
    private AggregationService service;

    @Test
    public void aggregateLiveRdb() throws IOException {

        AggregationTypeVO type = new AggregationTypeVO();
        type.setCategory(ExtractionCategoryEnum.LIVE.name());
        type.setLabel(ExtractionRawFormatEnum.RDB.name());

        AggregationStrataVO strata = new AggregationStrataVO();
        strata.setSpaceColumnName("area");
        strata.setTimeColumnName("year");
        strata.setAggColumnName("station_count");
        strata.setTechColumnName("vessel_count");

        ExtractionFilterVO filter = new ExtractionFilterVO();

        File outputFile = service.executeAndDump(type, filter, strata);
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
    public void aggregateProductRdb() {

        AggregationTypeVO type = new AggregationTypeVO();
        type.setCategory(ExtractionCategoryEnum.PRODUCT.name());
        type.setLabel("p01_rdb");

        ExtractionFilterVO filter = new ExtractionFilterVO();
        filter.setSheetName("HH");

        // Add criterion
        List<ExtractionFilterCriterionVO> criteria = Lists.newArrayList();
        filter.setCriteria(criteria);
        /*{
            criteria.add(ExtractionFilterCriterionVO.builder()
                    .sheetName("HH")
                    .name(ProductRdbStation.COLUMN_YEAR)
                    .operator(ExtractionFilterOperatorEnum.EQUALS.getSymbol())
                    .value("2017")
                    .build());
        }*/

        /*{
            criteria.add(ExtractionFilterCriterionVO.builder()
                    .name(ProductRdbStation.COLUMN_TRIP_CODE)
                    .operator(ExtractionFilterOperatorEnum.EQUALS.getSymbol())
                    .value("83716")
                    .build());
        }*/

        AggregationStrataVO strata = new AggregationStrataVO();
        strata.setSpaceColumnName(ProductRdbStation.COLUMN_STATISTICAL_RECTANGLE);
        strata.setTimeColumnName(ProductRdbStation.COLUMN_YEAR);
        strata.setAggColumnName("station_count");
        strata.setTechColumnName("vessel_count");

        ExtractionResultVO result = service.executeAndRead(type, filter, strata, 0, 100, null, null);
        Preconditions.checkNotNull(result);
        Preconditions.checkNotNull(result.getRows());
        Preconditions.checkArgument(result.getRows().size() > 0);

        // FIXME
        //Preconditions.checkNotNull(result.getTotal() > 0);
    }

    @Test
    @Ignore
    public void aggregate_SurvivalTest() {

        AggregationTypeVO type = new AggregationTypeVO();
        type.setCategory(ExtractionCategoryEnum.LIVE.name().toLowerCase());
        type.setLabel(ExtractionRawFormatEnum.SURVIVAL_TEST.name().toLowerCase());

        ExtractionResultVO result = service.executeAndRead(type, null, null, 0, 100, null, null);
        Preconditions.checkNotNull(result);
        Preconditions.checkNotNull(result.getRows());
        Preconditions.checkArgument(result.getRows().size() > 0);

        // FIXME
        //Preconditions.checkNotNull(result.getTotal() > 0);
    }


    @Test
    public void save() {

        AggregationTypeVO savedType = createAggType(ExtractionCategoryEnum.LIVE, ExtractionRawFormatEnum.RDB);
        Assert.assertNotNull(savedType);
        Assert.assertNotNull(savedType.getId());

        Assert.assertNotNull(savedType.getSheetNames());
        Assert.assertTrue(savedType.getSheetNames().length > 0);
        String sheetName = savedType.getSheetNames()[0];

        // Check columns
        List<ExtractionTableColumnVO> columns = service.getColumnsBySheetName(savedType, sheetName);
        Assert.assertNotNull(columns);
        Assert.assertTrue(columns.size() > 0);

    }

    @Test
    public void executeAndRead() {


        AggregationTypeVO type = createAggType(ExtractionCategoryEnum.LIVE, ExtractionRawFormatEnum.SURVIVAL_TEST);

        ExtractionFilterVO filter = new ExtractionFilterVO();
        filter.setSheetName("SL");

        ExtractionFilterCriterionVO criterion = new ExtractionFilterCriterionVO() ;
        criterion.setSheetName("HH");
        criterion.setName("year");
        criterion.setOperator("=");
        criterion.setValue("2018");
        filter.setCriteria(ImmutableList.of(criterion));

        AggregationStrataVO strata = new AggregationStrataVO();
        strata.setSpaceColumnName("area");
        AggregationResultVO result = service.executeAndRead(type, filter, strata, 0,100, null, null);

        Assert.assertNotNull(result);
        Assert.assertNotNull(result.getRows());
        Assert.assertTrue(result.getRows().size() > 0);
    }

    @Test
    public void saveThenRead() {
        AggregationTypeVO type = createAggType(ExtractionCategoryEnum.LIVE, ExtractionRawFormatEnum.SURVIVAL_TEST);

        // Save
        AggregationTypeVO savedType = service.save(type, null);
        Assert.assertNotNull(savedType);
        Assert.assertNotNull(savedType.getId());

        ExtractionFilterVO filter = new ExtractionFilterVO();
        filter.setSheetName("HH");

        ExtractionFilterCriterionVO criterion = new ExtractionFilterCriterionVO() ;
        criterion.setSheetName("HH");
        criterion.setName("year");
        criterion.setOperator("=");
        criterion.setValue("2018");
        filter.setCriteria(ImmutableList.of(criterion));

        AggregationStrataVO strata = new AggregationStrataVO();
        strata.setSpaceColumnName("area");

        AggregationResultVO result = service.read(savedType, filter, strata, 0,100, null, null);

        Assert.assertNotNull(result);
        Assert.assertNotNull(result.getRows());
        Assert.assertTrue(result.getRows().size() > 0);
    }

    @Test
    public void saveThenDelete() {
        AggregationTypeVO type = createAggType(ExtractionCategoryEnum.LIVE, ExtractionRawFormatEnum.SURVIVAL_TEST);

        // Save
        AggregationTypeVO savedType = service.save(type, null);
        Assert.assertNotNull(savedType);
        Assert.assertNotNull(savedType.getId());

        // Delete
        service.delete(savedType.getId());
    }

    /* -- protected methods --*/

    protected AggregationTypeVO createAggType(ExtractionCategoryEnum category, ExtractionRawFormatEnum format) {

        AggregationTypeVO type = new AggregationTypeVO();
        type.setCategory(category.name().toLowerCase());
        type.setLabel(format.name().toLowerCase() + "-" + System.currentTimeMillis());
        type.setName(String.format("Aggregation on %s (%s) data", format.name(), category.name()));
        type.setStatusId(StatusEnum.TEMPORARY.getId());

        DepartmentVO recDep = new DepartmentVO();
        recDep.setId(dbResource.getFixtures().getDepartmentId(0));
        type.setRecorderDepartment(recDep);

        return type;
    }
}

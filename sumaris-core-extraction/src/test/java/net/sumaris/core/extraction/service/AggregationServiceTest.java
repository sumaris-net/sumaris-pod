package net.sumaris.core.extraction.service;

import com.google.common.base.Preconditions;
import net.sumaris.core.extraction.dao.DatabaseResource;
import net.sumaris.core.extraction.vo.*;
import net.sumaris.core.model.technical.extraction.rdb.ProductRdbStation;
import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author peck7 on 17/12/2018.
 */
public class AggregationServiceTest extends AbstractServiceTest {

    @ClassRule
    public static final DatabaseResource dbResource = DatabaseResource.writeDb();

    @Autowired
    private AggregationService service;

    @Test
    public void aggregateLiveRdb() {

        AggregationTypeVO type = new AggregationTypeVO();
        type.setCategory(ExtractionCategoryEnum.LIVE.name());
        type.setLabel(ExtractionRawFormatEnum.RDB.name());

        AggregationStrataVO strata = new AggregationStrataVO();
        strata.setSpace("area");
        strata.setTime("year");
        strata.setTech("tripCount");

        ExtractionFilterVO filter = new ExtractionFilterVO();
        filter.setSheetName("HH");

        ExtractionResultVO result = service.executeAndRead(type, filter, strata, 0, 100, null, null);
        Preconditions.checkNotNull(result);
        Preconditions.checkArgument(result.getTotal().longValue() > 0);

    }

    @Test
    public void aggregateProductRdb() {

        AggregationTypeVO type = new AggregationTypeVO();
        type.setCategory(ExtractionCategoryEnum.PRODUCT.name());
        type.setLabel("p01_rdb");

        ExtractionFilterVO filter = new ExtractionFilterVO();
        filter.setSheetName("HH");

        AggregationStrataVO strata = new AggregationStrataVO();
        strata.setSpace(ProductRdbStation.COLUMN_STATISTICAL_RECTANGLE);
        strata.setTime(ProductRdbStation.COLUMN_YEAR);
        strata.setTech("trip_count");

        ExtractionResultVO result = service.executeAndRead(type, filter, strata, 0, 100, null, null);
        Preconditions.checkNotNull(result);
        Preconditions.checkArgument(result.getTotal().longValue() > 0);

    }

    @Test
    @Ignore
    public void aggregate_SurvivalTest() {

        AggregationTypeVO type = new AggregationTypeVO();
        type.setCategory(ExtractionCategoryEnum.LIVE.name());
        type.setLabel(ExtractionRawFormatEnum.SURVIVAL_TEST.name());

        ExtractionResultVO result = service.executeAndRead(type, null, null, 0, 100, null, null);
        Preconditions.checkNotNull(result);
        Preconditions.checkArgument(result.getTotal().longValue() > 0);

    }

}
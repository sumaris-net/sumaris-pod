package net.sumaris.core.extraction.service;

import net.sumaris.core.extraction.dao.DatabaseResource;
import net.sumaris.core.extraction.vo.ExtractionCategoryEnum;
import net.sumaris.core.extraction.vo.ExtractionTypeVO;
import net.sumaris.core.extraction.vo.live.ExtractionLiveFormat;
import org.junit.ClassRule;
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
    public void aggregate_Ices() {

        ExtractionTypeVO type = new ExtractionTypeVO();
        type.setCategory(ExtractionCategoryEnum.LIVE.name());
        type.setLabel(ExtractionLiveFormat.RDB.name());

        service.aggregate(type, null);

    }

    @Test
    public void aggregate_SurvivalTest() {

        ExtractionTypeVO type = new ExtractionTypeVO();
        type.setCategory(ExtractionCategoryEnum.LIVE.name());
        type.setLabel(ExtractionLiveFormat.SURVIVAL_TEST.name());

        service.aggregate(type, null);

    }

}
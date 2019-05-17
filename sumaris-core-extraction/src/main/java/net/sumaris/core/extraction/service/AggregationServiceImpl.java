package net.sumaris.core.extraction.service;

import com.google.common.base.Preconditions;
import net.sumaris.core.config.SumarisConfiguration;
import net.sumaris.core.dao.technical.schema.SumarisDatabaseMetadata;
import net.sumaris.core.exception.SumarisTechnicalException;
import net.sumaris.core.extraction.dao.trip.rdb.AggregationRdbDao;
import net.sumaris.core.extraction.dao.trip.survivalTest.ExtractionSurvivalTestDao;
import net.sumaris.core.extraction.vo.*;
import net.sumaris.core.extraction.vo.live.trip.rdb.ExtractionRdbTripContextVO;
import net.sumaris.core.service.referential.LocationService;
import net.sumaris.core.service.referential.ReferentialService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.List;

/**
 * @author peck7 on 17/12/2018.
 */
@Service("aggregationService")
@Lazy
public class AggregationServiceImpl implements AggregationService {

    private static final Logger log = LoggerFactory.getLogger(AggregationServiceImpl.class);

    @Autowired
    SumarisConfiguration configuration;

    @Autowired
    ExtractionService extractionService;

    @Autowired
    ExtractionSurvivalTestDao extractionSurvivalTestDao;

    @Autowired
    protected AggregationRdbDao aggregationIcesDao;

    @Autowired
    protected LocationService locationService;

    @Autowired
    protected ReferentialService referentialService;

    @Autowired
    protected SumarisDatabaseMetadata databaseMetadata;

    @Autowired(required = false)
    protected TaskExecutor taskExecutor = null;

    @Autowired
    private ExtractionService self;

    private List<ExtractionTypeVO> extractionTypes;

    @PostConstruct
    protected void afterPropertiesSet() {
    }

    @Override
    public ExtractionContextVO aggregate(ExtractionTypeVO type, ExtractionFilterVO filter) {
        // First execute the extraction
        ExtractionContextVO rawDataContext = extractionService.extract(type, filter);
        Preconditions.checkNotNull(rawDataContext);

        // Then aggregate data
        if (rawDataContext instanceof ExtractionRdbTripContextVO) {
            Preconditions.checkNotNull(((ExtractionRdbTripContextVO)rawDataContext).getStationTableName());
            return aggregationIcesDao.aggregate((ExtractionRdbTripContextVO)rawDataContext);
        }

        throw new SumarisTechnicalException(String.format("Aggregation on the type %s not implemented yet", type.getLabel()));
    }

    /* -- protected -- */

}

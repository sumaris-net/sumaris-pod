package net.sumaris.core.extraction.service;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import net.sumaris.core.config.SumarisConfiguration;
import net.sumaris.core.dao.technical.SortDirection;
import net.sumaris.core.dao.technical.extraction.ExtractionProductDao;
import net.sumaris.core.exception.DataNotFoundException;
import net.sumaris.core.exception.SumarisTechnicalException;
import net.sumaris.core.extraction.dao.technical.table.ExtractionTableDao;
import net.sumaris.core.extraction.dao.trip.rdb.AggregationRdbTripDao;
import net.sumaris.core.extraction.dao.trip.survivalTest.ExtractionSurvivalTestDao;
import net.sumaris.core.extraction.vo.*;
import net.sumaris.core.extraction.vo.AggregationContextVO;
import net.sumaris.core.extraction.vo.AggregationStrataVO;
import net.sumaris.core.util.StringUtils;
import net.sumaris.core.vo.technical.extraction.ExtractionProductVO;
import org.apache.commons.collections4.ListUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

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
    protected AggregationRdbTripDao aggregationRdbDao;

    @Autowired
    protected ExtractionProductDao extractionProductDao;

    @Autowired
    protected ExtractionTableDao extractionTableDao;

    @Override
    public List<AggregationTypeVO> getAllAggregationTypes() {
        return ImmutableList.<AggregationTypeVO>builder()
                .addAll(getProductAggregationTypes())
                .addAll(getLiveAggregationTypes())
                .build();
    }

    @Override
    public AggregationContextVO execute(AggregationTypeVO type, ExtractionFilterVO filter) {
        AggregationTypeVO checkedType = Extractions.checkAndFindType(getAllAggregationTypes(), type);
        ExtractionCategoryEnum category = ExtractionCategoryEnum.valueOf(checkedType.getCategory().toUpperCase());
        ExtractionProductVO source;

        switch (category) {
            case PRODUCT:
                // Get the product VO
                source = extractionProductDao.getByLabel(checkedType.getLabel().toUpperCase());
                // Execute, from product
                return executeProduct(source, filter);

            case LIVE:
                // First execute the raw extraction
                ExtractionContextVO rawExtractionContext = extractionService.execute(type, filter);

                try {
                    source = extractionService.toProductVO(rawExtractionContext);
                    ExtractionFilterVO aggregationFilter = null;
                    if (filter != null) {
                        aggregationFilter = new ExtractionFilterVO();
                        aggregationFilter.setSheetName(filter.getSheetName());
                    }

                    // Execute, from product
                    return executeProduct(source, aggregationFilter);
                }
                finally {
                    // Clean intermediate tables
                    asyncClean(rawExtractionContext);
                }
            default:
                throw new SumarisTechnicalException(String.format("Aggregation on category %s not implemented yet !", type.getCategory()));
        }
    }

    @Override
    public AggregationResultVO read(AggregationContextVO context, ExtractionFilterVO filter, AggregationStrataVO strata,
                                   int offset, int size, String sort, SortDirection direction) {
        filter = filter != null ? filter : new ExtractionFilterVO();
        strata = strata != null ? strata : new AggregationStrataVO();

        String tableName;
        if (StringUtils.isNotBlank(filter.getSheetName())) {
            tableName = context.getTableNameBySheetName(filter.getSheetName());
        }
        else {
            tableName = context.getTableNames().iterator().next();
        }

        // Missing the expected sheet = no data
        if (tableName == null) return createEmptyResult();

        // Read the data
        if (context.getLabel().toLowerCase().contains("rdb")) {
            return aggregationRdbDao.read(tableName, filter, strata, offset, size, sort, direction);
        }

        throw new SumarisTechnicalException(String.format("Unable to read data on type '%s': not implemented", context.getLabel()));
    }

    @Override
    public AggregationResultVO executeAndRead(AggregationTypeVO type, ExtractionFilterVO filter, AggregationStrataVO strata,
                                              int offset, int size, String sort, SortDirection direction) {
        // Execute the aggregation
        AggregationContextVO context = execute(type, filter);

        // Prepare the read filter
        ExtractionFilterVO readFilter = null;
        if (filter != null) {
            readFilter = new ExtractionFilterVO();
            readFilter.setSheetName(filter.getSheetName());
        }

        try {
            // Read data
            return read(context, readFilter, strata, offset, size, sort, direction);
        }
        finally {
            // Clean created tables
            asyncClean(context);
        }
    }


    /* -- protected -- */

    protected List<AggregationTypeVO> getProductAggregationTypes() {
        return ListUtils.emptyIfNull(extractionProductDao.getAll())
                .stream()
                .map(product -> {
                    AggregationTypeVO type = new AggregationTypeVO();
                    type.setLabel(product.getLabel().toLowerCase());
                    type.setCategory(ExtractionCategoryEnum.PRODUCT.name().toLowerCase());

                    Collection<String> sheetNames = product.getSheetNames();
                    type.setSheetNames(sheetNames.toArray(new String[sheetNames.size()]));

                    return type;
                }).collect(Collectors.toList());
    }

    protected List<AggregationTypeVO> getLiveAggregationTypes() {
        return Arrays.stream(ExtractionRawFormatEnum.values())
                .map(format -> {
                    AggregationTypeVO type = new AggregationTypeVO();
                    type.setLabel(format.name().toLowerCase());
                    type.setCategory(ExtractionCategoryEnum.LIVE.name().toLowerCase());
                    type.setSheetNames(format.getSheetNames());
                    return type;
                })
                .collect(Collectors.toList());
    }

    protected AggregationResultVO createEmptyResult() {
        AggregationResultVO result = new AggregationResultVO();
        result.setColumns(ImmutableList.of());
        result.setTotal(0);
        result.setRows(ImmutableList.of());
        return result;
    }

    public AggregationContextVO executeProduct(ExtractionProductVO source, ExtractionFilterVO filter) {
        Preconditions.checkNotNull(source);
        Preconditions.checkNotNull(source.getLabel());

        switch (source.getLabel().toUpperCase()) {
            case "P01_RDB":
            case "RDB":
                return aggregationRdbDao.aggregate(source, filter);
            default:
                throw new SumarisTechnicalException(String.format("Unable to aggregate data on type '%s': not implemented", source.getLabel()));
        }
    }

    protected void asyncClean(ExtractionContextVO context) {
        if (context == null) return;
        extractionService.asyncClean(context);
    }
}

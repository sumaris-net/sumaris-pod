package net.sumaris.core.extraction.service;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import net.sumaris.core.config.SumarisConfiguration;
import net.sumaris.core.dao.technical.SortDirection;
import net.sumaris.core.dao.technical.extraction.ExtractionProductDao;
import net.sumaris.core.exception.SumarisTechnicalException;
import net.sumaris.core.extraction.dao.technical.table.ExtractionTableDao;
import net.sumaris.core.extraction.dao.trip.rdb.AggregationRdbTripDao;
import net.sumaris.core.extraction.dao.trip.survivalTest.ExtractionSurvivalTestDao;
import net.sumaris.core.extraction.vo.*;
import net.sumaris.core.extraction.vo.filter.AggregationTypeFilterVO;
import net.sumaris.core.extraction.vo.trip.rdb.AggregationRdbTripContextVO;
import net.sumaris.core.util.StringUtils;
import net.sumaris.core.vo.technical.extraction.ExtractionProductTableVO;
import net.sumaris.core.vo.technical.extraction.ExtractionProductVO;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.collections4.SetUtils;
import org.nuiton.i18n.I18n;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
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
    public List<AggregationTypeVO> findAllTypes(AggregationTypeFilterVO filter) {
        List<Integer> statusIds = filter.getStatusIds();
        if (CollectionUtils.isEmpty(statusIds)) {
            statusIds = ImmutableList.of(filter.getStatusId());
        }
        return ListUtils.emptyIfNull(extractionProductDao.findAllByStatus(statusIds))
                .stream()
                .filter(t -> filter.getIsSpatial() == null || Objects.equals(filter.getIsSpatial(), t.getIsSpatial()))
                .map(this::toAggregationType)
                .collect(Collectors.toList());
    }

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
                source = extractionProductDao.getByLabel(checkedType.getFormat());
                // Execute, from product
                return executeProduct(source, filter);

            case LIVE:
                // First execute the raw extraction
                ExtractionContextVO rawExtractionContext = extractionService.execute(checkedType, filter);

                try {
                    source = extractionService.toProductVO(rawExtractionContext);
                    ExtractionFilterVO aggregationFilter = null;
                    if (filter != null) {
                        aggregationFilter = new ExtractionFilterVO();
                        aggregationFilter.setSheetName(filter.getSheetName());
                    }

                    // Execute, from product
                    return executeProduct(source, aggregationFilter);
                } finally {
                    // Clean intermediate tables
                    asyncClean(rawExtractionContext);
                }
            default:
                throw new SumarisTechnicalException(String.format("Aggregation on category %s not implemented yet !", type.getCategory()));
        }
    }

    @Override
    public AggregationResultVO read(AggregationTypeVO type, @Nullable ExtractionFilterVO filter, @Nullable AggregationStrataVO strata, int offset, int size, String sort, SortDirection direction) {
        Preconditions.checkNotNull(type);

        ExtractionProductVO product = extractionProductDao.getByLabel(type.getLabel().toUpperCase());
        AggregationContextVO context = toContextVO(product);

        return read(context, filter, strata, offset, size, sort, direction);
    }

    @Override
    public AggregationResultVO read(AggregationContextVO context, ExtractionFilterVO filter, AggregationStrataVO strata,
                                    int offset, int size, String sort, SortDirection direction) {
        filter = filter != null ? filter : new ExtractionFilterVO();
        strata = strata != null ? strata : new AggregationStrataVO();

        String tableName;
        if (StringUtils.isNotBlank(filter.getSheetName())) {
            tableName = context.getTableNameBySheetName(filter.getSheetName());
        } else {
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
        } finally {
            // Clean created tables
            asyncClean(context);
        }
    }

    @Override
    public AggregationTypeVO save(AggregationTypeVO type, @Nullable ExtractionFilterVO filter) {
        Preconditions.checkNotNull(type);
        Preconditions.checkNotNull(type.getLabel());
        Preconditions.checkNotNull(type.getName());

        // Load the product
        ExtractionProductVO target = null;
        try {
            target = extractionProductDao.getByLabel(type.getLabel());
        } catch (Throwable t) {
            // Not found
        }

        if (target == null) {
            target = new ExtractionProductVO();
            target.setLabel(type.getLabel());
        }

        // Prepare a executable type (with label=format)
        AggregationTypeVO executableType = new AggregationTypeVO();
        executableType.setLabel(type.getFormat());
        executableType.setCategory(type.getCategory());

        // Execute the aggregation
        AggregationContextVO context = execute(executableType, filter);

        // Update product tables, using the aggregation result
        toProductVO(context, target);

        // Copy some properties from given type
        target.setName(type.getName());
        target.setStatusId(type.getStatusId());

        // Save the product
        target = extractionProductDao.save(target);

        // Transform back to type
        return toAggregationType(target);
    }

    /* -- protected -- */

    protected List<AggregationTypeVO> getProductAggregationTypes() {
        return ListUtils.emptyIfNull(extractionProductDao.getAll())
                .stream()
                .map(this::toAggregationType)
                .collect(Collectors.toList());
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

        final String formatStr = StringUtils.changeCaseToUnderscore(source.getLabel().split("-")[0]).toUpperCase();

        ExtractionRawFormatEnum format = ExtractionRawFormatEnum.fromString(formatStr)
                .orElseGet(() -> {
                    if (formatStr.contains(ExtractionRawFormatEnum.RDB.name())) {
                        return ExtractionRawFormatEnum.RDB;
                    }
                    throw new SumarisTechnicalException(String.format("Data aggregation on type '%s' is not implemented !", formatStr));
                });

        switch (format) {
            case RDB:
            case COST:
            case SURVIVAL_TEST:
                return aggregationRdbDao.aggregate(source, filter);
            default:
                throw new SumarisTechnicalException(String.format("Data aggregation on type '%s' is not implemented !", format.name()));
        }
    }

    protected void asyncClean(ExtractionContextVO context) {
        if (context == null) return;
        extractionService.asyncClean(context);
    }

    protected AggregationTypeVO toAggregationType(ExtractionProductVO product) {
        AggregationTypeVO type = new AggregationTypeVO();

        type.setId(product.getId());
        type.setLabel(product.getLabel().toLowerCase());
        type.setCategory(ExtractionCategoryEnum.PRODUCT.name().toLowerCase());
        type.setName(product.getName());
        type.setDescription(product.getDescription());

        Collection<String> sheetNames = product.getSheetNames();
        type.setSheetNames(sheetNames.toArray(new String[sheetNames.size()]));
        return type;
    }


    protected void toProductVO(AggregationContextVO source, ExtractionProductVO target) {

        target.setLabel(source.getLabel().toUpperCase() + "-" + source.getId());
        target.setName(String.format("Aggregation #%s", source.getId()));

        target.setTables(SetUtils.emptyIfNull(source.getTableNames())
                .stream()
                .map(t -> {
                    String sheetName = source.getSheetName(t);
                    ExtractionProductTableVO table = new ExtractionProductTableVO();
                    table.setLabel(sheetName);
                    table.setName(getNameBySheet(source.getFormatName(), sheetName));
                    table.setTableName(t);
                    table.setIsSpatial(source.hasSpatialColumn(t));
                    if (table.getIsSpatial()) {
                        target.setIsSpatial(true);
                    }
                    return table;
                })
                .collect(Collectors.toList()));
    }

    protected AggregationContextVO toContextVO(ExtractionProductVO source) {

        AggregationContextVO target = new AggregationRdbTripContextVO();

        target.setId(source.getId());
        target.setLabel(source.getLabel());

        ListUtils.emptyIfNull(source.getTables())
                .forEach(t -> {
                    target.addTableName(t.getTableName(), t.getLabel());
                });
        return target;
    }

    protected String getNameBySheet(String format, String sheetname) {
        return I18n.t(String.format("sumaris.aggregation.%s.%s", format.toUpperCase(), sheetname.toUpperCase()));
    }
}

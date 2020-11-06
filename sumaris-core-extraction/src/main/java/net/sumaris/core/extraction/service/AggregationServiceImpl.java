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
import net.sumaris.core.dao.technical.SortDirection;
import net.sumaris.core.exception.SumarisTechnicalException;
import net.sumaris.core.extraction.dao.technical.table.ExtractionTableColumnOrder;
import net.sumaris.core.extraction.dao.technical.table.ExtractionTableDao;
import net.sumaris.core.extraction.dao.trip.rdb.AggregationRdbTripDao;
import net.sumaris.core.extraction.dao.trip.survivalTest.AggregationSurvivalTestDao;
import net.sumaris.core.extraction.format.ExtractionFormats;
import net.sumaris.core.extraction.format.LiveFormatEnum;
import net.sumaris.core.extraction.vo.*;
import net.sumaris.core.extraction.vo.filter.AggregationTypeFilterVO;
import net.sumaris.core.extraction.vo.trip.rdb.AggregationRdbTripContextVO;
import net.sumaris.core.model.referential.StatusEnum;
import net.sumaris.core.util.Beans;
import net.sumaris.core.extraction.util.ExtractionBeans;
import net.sumaris.core.util.StringUtils;
import net.sumaris.core.vo.technical.extraction.*;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.nuiton.i18n.I18n;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Service;

import javax.annotation.Nullable;
import javax.annotation.Resource;
import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author peck7 on 17/12/2018.
 */
@Service("aggregationService")
public class AggregationServiceImpl implements AggregationService {

    private static final Logger log = LoggerFactory.getLogger(AggregationServiceImpl.class);

    @Autowired
    private ExtractionService extractionService;

    @Autowired
    private ExtractionProductService productService;

    @Resource(name = "aggregationRdbTripDao")
    private AggregationRdbTripDao aggregationRdbTripDao;

    @Autowired
    private AggregationSurvivalTestDao aggregationSurvivalTestDao;

    @Autowired
    private ExtractionTableDao extractionTableDao;

    @Autowired(required = false)
    protected TaskExecutor taskExecutor = null;

    @Autowired
    private AggregationService self;

    @Override
    public List<AggregationTypeVO> findByFilter(AggregationTypeFilterVO filter, ExtractionProductFetchOptions fetchOptions) {

        final AggregationTypeFilterVO notNullFilter = filter != null ? filter : new AggregationTypeFilterVO();

        return ListUtils.emptyIfNull(getProductAggregationTypes(notNullFilter, fetchOptions))
            .stream()
            .filter(t -> notNullFilter.getIsSpatial() == null || Objects.equals(notNullFilter.getIsSpatial(), t.getIsSpatial()))
            .collect(Collectors.toList());
    }

    @Override
    public AggregationTypeVO get(int id, ExtractionProductFetchOptions fetchOptions) {
        ExtractionProductVO source = productService.get(id, fetchOptions);
        return toAggregationType(source);
    }

    @Override
    public AggregationTypeVO checkAndGet(AggregationTypeVO type) {
        return ExtractionFormats.checkAndGet(getAllAggregationTypes(null), type);
    }

    @Override
    public AggregationContextVO execute(AggregationTypeVO type, ExtractionFilterVO filter, AggregationStrataVO strata) {
        type = checkAndGet(type);
        ExtractionProductVO source;

        switch (type.getCategory()) {
            case PRODUCT:
                // Get the product VO
                source = productService.getByLabel(type.getFormat(), ExtractionProductFetchOptions.MINIMAL);
                // Execute, from product
                return aggregate(source, filter, strata);

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
                    return aggregate(source, aggregationFilter, strata);
                } finally {
                    // Clean intermediate tables
                    extractionService.asyncClean(rawExtractionContext);
                }
            default:
                throw new SumarisTechnicalException(String.format("Aggregation on category %s not implemented yet !", type.getCategory()));
        }
    }

    @Override
    public AggregationResultVO read(AggregationTypeVO type, @Nullable ExtractionFilterVO filter, @Nullable AggregationStrataVO strata, int offset, int size, String sort, SortDirection direction) {
        Preconditions.checkNotNull(type);

        ExtractionProductVO product = productService.getByLabel(type.getLabel(),
                ExtractionProductFetchOptions.MINIMAL_WITH_TABLES);
        AggregationContextVO context = toContextVO(product, filter != null ? filter.getSheetName() : null);

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
            if (strata.getSheetName() == null) {
                strata.setSheetName(filter.getSheetName());
            }
        } else {
            tableName = context.getTableNames().iterator().next();
        }

        // Missing the expected sheet = no data
        if (tableName == null) return createEmptyResult();

        // Read the data
        LiveFormatEnum format = ExtractionFormats.getLiveFormat(context);
        switch (format) {
            case RDB:
            case COST:
            case FREE1:
            case SURVIVAL_TEST:
                return aggregationRdbTripDao.read(tableName, filter, strata, offset, size, sort, direction);
            default:
                throw new SumarisTechnicalException(String.format("Unable to read data on type '%s': not implemented", context.getLabel()));
        }

    }

    @Override
    public AggregationResultVO executeAndRead(AggregationTypeVO type, ExtractionFilterVO filter, AggregationStrataVO strata,
                                              int offset, int size, String sort, SortDirection direction) {
        // Execute the aggregation
        AggregationContextVO context = execute(type, filter, strata);

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
    public File executeAndDump(AggregationTypeVO type, @Nullable ExtractionFilterVO filter, @Nullable AggregationStrataVO strata) {
        // Execute the aggregation
        AggregationContextVO context = execute(type, filter, strata);
        try {
            return extractionService.dumpTablesToFile(context, null /*already apply*/);
        }
        finally {
            // Delete aggregation tables, after dump
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
        if (type.getId() != null) {
            target = productService.findById(type.getId(), ExtractionProductFetchOptions.FOR_UPDATE).orElse(null);
        }

        boolean isNew = target == null;
        if (isNew) {
            target = new ExtractionProductVO();
            target.setLabel(type.getLabel().toUpperCase());

            // Check label != format
            Preconditions.checkArgument(!Objects.equals(type.getLabel(), type.getFormat()), "Invalid label. Expected pattern: <type_name>-NNN");
        }
        else {
            // Check label not changed
            Preconditions.checkArgument(Objects.equals(type.getLabel().toUpperCase(), target.getLabel()), "Cannot change label");
            target.setLabel(type.getLabel().toUpperCase());
        }

        boolean needExecuteAggregation = isNew || filter != null;

        // Run the aggregation (if need) before saving
        if (needExecuteAggregation) {

            // Prepare a executable type (with label=format)
            AggregationTypeVO executableType = new AggregationTypeVO();
            executableType.setLabel(type.getFormat());
            executableType.setCategory(type.getCategory());

            // Execute the aggregation
            AggregationContextVO context = execute(executableType, filter, null);

            // Update product tables, using the aggregation result
            toProductVO(context, target);

            // Copy some properties from the given type
            target.setName(type.getName());
            target.setUpdateDate(type.getUpdateDate());
            target.setDescription(type.getDescription());
            target.setStatusId(type.getStatusId());
            target.setRecorderDepartment(type.getRecorderDepartment());
            target.setRecorderPerson(type.getRecorderPerson());
        }

        // Not need new aggregation: update entity before saving
        else {
            Preconditions.checkArgument(StringUtils.equalsIgnoreCase(target.getLabel(), type.getLabel()), "Cannot update the label of an existing product");
            target.setName(type.getName());
            target.setUpdateDate(type.getUpdateDate());
            target.setDescription(type.getDescription());
            target.setComments(type.getComments());
            target.setStatusId(type.getStatusId());
            target.setUpdateDate(type.getUpdateDate());
            target.setIsSpatial(type.getIsSpatial());
        }
        target.setStratum(type.getStratum());

        // Save the product
        target = productService.save(target);

        // Transform back to type
        return toAggregationType(target);
    }

    /* -- protected -- */

    protected List<AggregationTypeVO> getAllAggregationTypes(ExtractionProductFetchOptions fetchOptions) {
        return ImmutableList.<AggregationTypeVO>builder()
            .addAll(getProductAggregationTypes(fetchOptions))
            .addAll(getLiveAggregationTypes())
            .build();
    }

    protected List<AggregationTypeVO> getProductAggregationTypes(ExtractionProductFetchOptions fetchOptions) {
        return getProductAggregationTypes(null, fetchOptions);
    }

    protected List<AggregationTypeVO> getProductAggregationTypes(@Nullable AggregationTypeFilterVO filter, ExtractionProductFetchOptions fetchOptions) {
        filter = filter != null ? filter : new AggregationTypeFilterVO();

        // Exclude types with a DISABLE status, by default
        if (ArrayUtils.isEmpty(filter.getStatusIds())) {
            filter.setStatusIds(new Integer[]{StatusEnum.ENABLE.getId(), StatusEnum.TEMPORARY.getId()});
        }

        final Boolean filterIsSpatial = filter.getIsSpatial();

        return ListUtils.emptyIfNull(productService.findByFilter(filter, fetchOptions))
            .stream()
            .filter(p -> filterIsSpatial == null || filterIsSpatial.equals(p.getIsSpatial()))
            .map(this::toAggregationType)
            .collect(Collectors.toList());
    }

    protected List<AggregationTypeVO> getLiveAggregationTypes() {
        return Arrays.stream(LiveFormatEnum.values())
            .map(format -> {
                AggregationTypeVO type = new AggregationTypeVO();
                type.setLabel(format.name().toLowerCase());
                type.setCategory(ExtractionCategoryEnum.LIVE);
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

    public AggregationContextVO aggregate(ExtractionProductVO source,
                                          ExtractionFilterVO filter,
                                          AggregationStrataVO strata) {
        Preconditions.checkNotNull(source);
        Preconditions.checkNotNull(source.getLabel());

        LiveFormatEnum format = ExtractionFormats.getLiveFormat(source);

        switch (format) {
            case RDB:
            case COST:
            case FREE1:
                return aggregationRdbTripDao.aggregate(source, filter, strata);

            case SURVIVAL_TEST:
                return aggregationSurvivalTestDao.aggregate(source, filter, strata);
            default:
                throw new SumarisTechnicalException(String.format("Data aggregation on type '%s' is not implemented!", format.name()));
        }
    }

    @Override
    public void asyncClean(AggregationContextVO context) {
        if (taskExecutor == null) {
            clean(context);
        } else {
            taskExecutor.execute(() -> {
                try {
                    Thread.sleep(2000); // Wait 2 s, to to sure the table is not used anymore

                    // Call self, to be sure to have a transaction
                    self.clean(context);
                } catch (Exception e) {
                    log.warn("Error while cleaning extraction tables", e);
                }
            });
        }
    }

    @Override
    public void clean(AggregationContextVO context) {
        if (context == null) return;
        if (context instanceof AggregationRdbTripContextVO) {
            aggregationRdbTripDao.clean((AggregationRdbTripContextVO) context);
        }
    }


    /* -- protected methods -- */

    protected AggregationTypeVO toAggregationType(ExtractionProductVO source) {
        AggregationTypeVO target = new AggregationTypeVO();

        Beans.copyProperties(source, target);

        // Change label and category to lowercase (better for UI client)
        target.setCategory(ExtractionCategoryEnum.PRODUCT);
        target.setLabel(source.getLabel().toLowerCase());

        Collection<String> sheetNames = source.getSheetNames();
        if (CollectionUtils.isNotEmpty(sheetNames)) {
            target.setSheetNames(sheetNames.toArray(new String[sheetNames.size()]));
        }

        if (CollectionUtils.isNotEmpty(source.getStratum())) {
            target.setStratum(source.getStratum());
        }

        return target;
    }

    protected void toProductVO(AggregationContextVO source, ExtractionProductVO target) {

        target.setLabel(ExtractionBeans.getProductLabel(source.getFormatName(),
                source.getFormatVersion(),
                source.getId()));
        target.setName(String.format("Aggregation #%s", source.getId()));
        target.setIsSpatial(source.isSpatial());

        target.setTables(toProductTableVO(source));
    }

    protected List<ExtractionTableVO> toProductTableVO(AggregationContextVO source) {

        final List<String> tableNames = ImmutableList.copyOf(source.getTableNames());
        return tableNames.stream()
            .map(tableName -> {
                ExtractionTableVO table = new ExtractionTableVO();
                table.setTableName(tableName);

                // Keep rankOrder from original linked has map
                table.setRankOrder(tableNames.indexOf(tableName) + 1);

                // Label (=the sheet name)
                String sheetName = source.getSheetName(tableName);
                table.setLabel(sheetName);

                // Name: generated using i18n
                String name = getI18nSheetName(source.getFormatName(), sheetName);
                table.setName(name);

                table.setIsSpatial(source.hasSpatialColumn(tableName));

                // Columns
                List<ExtractionTableColumnVO> columns = toProductColumnVOs(source, tableName);

                // Fill rank order
                ExtractionTableColumnOrder.fillRankOrderByFormatAndSheet(source.getFormatName(), sheetName, columns);

                table.setColumns(columns);

                return table;
            })
            .collect(Collectors.toList());
    }

    protected List<ExtractionTableColumnVO> toProductColumnVOs(AggregationContextVO context, String tableName) {


        // Get columns (from table metadata), but exclude hidden columns
        List<ExtractionTableColumnVO> columns = extractionTableDao.getColumns(tableName);

        Set<String> hiddenColumns = Beans.getSet(context.getHiddenColumns(tableName));
        Map<String, List<String>> columnValues = Beans.getMap(context.getColumnValues(tableName));

        // For each column
        Beans.getStream(columns).forEach(column -> {
            String columnName = column.getColumnName();
            // If hidden, replace the type with 'hidden'
            if (hiddenColumns.contains(columnName)) {
                column.setType("hidden");
            }

            // Set values
            List<String> values = columnValues.get(columnName);
            if (CollectionUtils.isNotEmpty(values)) {
                column.setValues(values);
            }
        });

        return columns;
    }

    protected AggregationContextVO toContextVO(ExtractionProductVO source, String sheetName) {

        AggregationRdbTripContextVO target = new AggregationRdbTripContextVO();

        target.setId(source.getId());
        target.setLabel(source.getLabel());

        ListUtils.emptyIfNull(source.getTables())
            .forEach(t -> target.addTableName(t.getTableName(), t.getLabel()));

        // Find the strata to apply, by sheetName
        if (sheetName != null && source.getStratum() != null) {
            ExtractionProductStrataVO productStrata = source.getStratum().stream()
                    .filter(s -> sheetName.equals(s.getSheetName()))
                    .findFirst().orElse(null);
            if (productStrata != null) {
                AggregationStrataVO strata = new AggregationStrataVO();
                Beans.copyProperties(productStrata, strata);
                target.setStrata(strata);
            }
        }

        return target;
    }

    protected String getI18nSheetName(String format, String sheetName) {
        return I18n.t(String.format("sumaris.extraction.%s.%s", format.toUpperCase(), sheetName.toUpperCase()));
    }

}

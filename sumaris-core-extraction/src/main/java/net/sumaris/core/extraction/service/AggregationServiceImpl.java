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
import net.sumaris.core.dao.technical.extraction.ExtractionProductDao;
import net.sumaris.core.exception.SumarisTechnicalException;
import net.sumaris.core.extraction.dao.technical.table.ExtractionTableDao;
import net.sumaris.core.extraction.dao.trip.rdb.AggregationRdbTripDao;
import net.sumaris.core.extraction.utils.ExtractionRawFormatEnum;
import net.sumaris.core.extraction.utils.ExtractionBeans;
import net.sumaris.core.extraction.vo.*;
import net.sumaris.core.extraction.vo.filter.AggregationTypeFilterVO;
import net.sumaris.core.extraction.vo.trip.rdb.AggregationRdbTripContextVO;
import net.sumaris.core.model.referential.StatusEnum;
import net.sumaris.core.util.Beans;
import net.sumaris.core.util.StringUtils;
import net.sumaris.core.vo.technical.extraction.*;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.collections4.SetUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.nuiton.i18n.I18n;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.dao.DataRetrievalFailureException;
import org.springframework.stereotype.Service;

import javax.annotation.Nullable;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author peck7 on 17/12/2018.
 */
@Service("aggregationService")
@Lazy
public class AggregationServiceImpl implements AggregationService {

    private static final Logger log = LoggerFactory.getLogger(AggregationServiceImpl.class);

    @Autowired
    private ExtractionService extractionService;

    @Autowired
    private AggregationRdbTripDao aggregationRdbTripDao;

    @Autowired
    private ExtractionProductDao extractionProductDao;

    @Autowired
    private ExtractionTableDao extractionTableDao;

    @Override
    public List<AggregationTypeVO> findByFilter(AggregationTypeFilterVO filter, ProductFetchOptions fetchOptions) {

        final AggregationTypeFilterVO notNullFilter = filter != null ? filter : new AggregationTypeFilterVO();

        return ListUtils.emptyIfNull(getProductAggregationTypes(notNullFilter, fetchOptions))
                .stream()
                .filter(t -> notNullFilter.getIsSpatial() == null || Objects.equals(notNullFilter.getIsSpatial(), t.getIsSpatial()))
                .collect(Collectors.toList());
    }

    @Override
    public AggregationTypeVO get(int id, ProductFetchOptions fetchOptions) {
        ExtractionProductVO result = extractionProductDao.get(id, fetchOptions)
                .orElseThrow(() -> new DataRetrievalFailureException(String.format("Unknown aggregation type {%s}", id)));
        return toAggregationType(result);
    }

    @Override
    public AggregationContextVO execute(AggregationTypeVO type, ExtractionFilterVO filter) {
        AggregationTypeVO checkedType = ExtractionBeans.checkAndFindType(getAllAggregationTypes(null), type);
        ExtractionCategoryEnum category = ExtractionCategoryEnum.valueOf(checkedType.getCategory().toUpperCase());
        ExtractionProductVO source;

        switch (category) {
            case PRODUCT:
                // Get the product VO
                source = extractionProductDao.getByLabel(checkedType.getFormat(), ProductFetchOptions.MINIMAL);
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

        ExtractionProductVO product = extractionProductDao.getByLabel(type.getLabel(), ProductFetchOptions.MINIMAL_WITH_TABLES);
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
        ExtractionRawFormatEnum format = ExtractionBeans.getFormat(context);
        switch (format) {
            case RDB:
            case COST:
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

        Preconditions.checkArgument(!Objects.equals(type.getLabel(), type.getFormat()), "Invalid label. Expected pattern: <type_name>-NNN");

        // Load the product
        ExtractionProductVO target = null;
        if (type.getId() != null) {
            target = extractionProductDao.get(type.getId(), ProductFetchOptions.FOR_UPDATE)
                    .orElse(null);
        }

        boolean isNew = target == null;
        if (isNew) {
            target = new ExtractionProductVO();
            target.setLabel(type.getLabel());
        }

        // Applying a new execution
        if (isNew || filter != null) {

            // Prepare a executable type (with label=format)
            AggregationTypeVO executableType = new AggregationTypeVO();
            executableType.setLabel(type.getFormat());
            executableType.setCategory(type.getCategory());

            // Execute the aggregation
            AggregationContextVO context = execute(executableType, filter);

            // Update product tables, using the aggregation result
            toProductVO(context, target);

            // Copy some properties from the given type
            target.setName(type.getName());
            target.setUpdateDate(type.getUpdateDate());
            target.setDescription(type.getDescription());
            target.setStatusId(type.getStatusId());
            target.setRecorderDepartment(type.getRecorderDepartment());
            target.setRecorderPerson(type.getRecorderPerson());
            target.setStratum(type.getStratum());
        }

        // Aggregation already exists, and not new execution need: just save it
        else {
            Preconditions.checkArgument(StringUtils.equalsIgnoreCase(target.getLabel(), type.getLabel()), "Cannot update the label of an existing product");
            target.setName(type.getName());
            target.setUpdateDate(type.getUpdateDate());
            target.setDescription(type.getDescription());
            target.setComments(type.getComments());
            target.setStatusId(type.getStatusId());
            target.setUpdateDate(type.getUpdateDate());
            target.setIsSpatial(type.getIsSpatial());
            target.setStratum(type.getStratum());
        }

        // Save the product
        target = extractionProductDao.save(target);

        // Transform back to type
        return toAggregationType(target);
    }

    @Override
    public void delete(int id) {
        extractionProductDao.delete(id);
    }

    @Override
    public List<ExtractionProductColumnVO> getColumnsBySheetName(AggregationTypeVO type, String sheetName) {
        Preconditions.checkNotNull(type);
        Preconditions.checkArgument(type.getId() != null || type.getLabel() != null, "Missing type.id or type.label");

        Integer productId = type.getId();
        if (productId == null) {
            ExtractionProductVO product = extractionProductDao.getByLabel(type.getLabel(), ProductFetchOptions.MINIMAL);
            productId = product.getId();
        }
        // Try to get columns from the DB
        List<ExtractionProductColumnVO> dataColumns = null;
        if (StringUtils.isNotBlank(sheetName)) {
            dataColumns = extractionProductDao.getColumnsByIdAndTableLabel(productId, sheetName);
        }

        // If nothing in the DB, get metadata from a fake extraction
        if (CollectionUtils.isEmpty(dataColumns)) {
            ExtractionTypeVO readType = new ExtractionTypeVO();
            readType.setCategory(ExtractionCategoryEnum.PRODUCT.name().toLowerCase());
            readType.setLabel(type.getLabel());
            ExtractionFilterVO readFilter = new ExtractionFilterVO();
            readFilter.setSheetName(sheetName);
            ExtractionResultVO res = extractionService.executeAndRead(readType, readFilter, 0, 1, null, null);

            dataColumns = res.getColumns();
        }

        return dataColumns;
    }

    /* -- protected -- */

    protected List<AggregationTypeVO> getAllAggregationTypes(ProductFetchOptions fetchOptions) {
        return ImmutableList.<AggregationTypeVO>builder()
                .addAll(getProductAggregationTypes(fetchOptions))
                .addAll(getLiveAggregationTypes())
                .build();
    }
    protected List<AggregationTypeVO> getProductAggregationTypes(ProductFetchOptions fetchOptions) {
        return getProductAggregationTypes(null, fetchOptions);
    }

    protected List<AggregationTypeVO> getProductAggregationTypes(@Nullable AggregationTypeFilterVO filter, ProductFetchOptions fetchOptions) {
        filter = filter != null ? filter : new AggregationTypeFilterVO();

        // Exclude types with a DISABLE status, by default
        if (ArrayUtils.isEmpty(filter.getStatusIds())) {
            filter.setStatusIds(new Integer[]{StatusEnum.ENABLE.getId(), StatusEnum.TEMPORARY.getId()});
        }

        return ListUtils.emptyIfNull(extractionProductDao.findByFilter(filter, fetchOptions))
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

        ExtractionRawFormatEnum format = ExtractionBeans.getFormat(source);

        switch (format) {
            case RDB:
            case COST:
            case FREE1:

            //case FREE2: // TODO: test FREE2 is compatible
            case SURVIVAL_TEST:
                return aggregationRdbTripDao.aggregate(source, filter);
            default:
                throw new SumarisTechnicalException(String.format("Data aggregation on type '%s' is not implemented!", format.name()));
        }
    }

    protected void asyncClean(ExtractionContextVO context) {
        if (context == null) return;
        extractionService.asyncClean(context);
    }

    protected AggregationTypeVO toAggregationType(ExtractionProductVO source) {
        AggregationTypeVO target = new AggregationTypeVO();

        Beans.copyProperties(source, target);

        // Change label and category to lowercase (better for UI client)
        target.setCategory(ExtractionCategoryEnum.PRODUCT.name().toLowerCase());
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

        target.setLabel(source.getLabel().toUpperCase() + "-" + source.getId());
        target.setName(String.format("Aggregation #%s", source.getId()));
        target.setIsSpatial(source.isSpatial());

        target.setTables(toProductTableVO(source));
    }

    protected List<ExtractionProductTableVO> toProductTableVO(AggregationContextVO source) {

        return SetUtils.emptyIfNull(source.getTableNames())
                .stream()
                .map(tableName -> {
                    ExtractionProductTableVO table = new ExtractionProductTableVO();
                    table.setTableName(tableName);

                    // Label (=the sheet name)
                    String label = source.getSheetName(tableName);
                    table.setLabel(label);

                    // Name: generated using i18n
                    String name = getI18nSheetName(source.getFormatName(), label);
                    table.setName(name);

                    table.setIsSpatial(source.hasSpatialColumn(tableName));

                    // Columns
                    List<ExtractionProductColumnVO> columns = toProductColumnVOs(source, tableName);
                    table.setColumns(columns);

                    return table;
                })
                .collect(Collectors.toList());
    }

    protected List<ExtractionProductColumnVO> toProductColumnVOs(AggregationContextVO context, String tableName) {

        Set<String> hiddenColumns = Beans.getSet(context.getHiddenColumns(tableName));
        Map<String, List<String>> columnValues = context.getColumnValues(tableName);

        // Get columns (from table metadata), but exclude hidden columns
        List<ExtractionProductColumnVO> columns = Beans.getStream(extractionTableDao.getColumns(tableName))
                .filter(column -> !hiddenColumns.contains(column.getColumnName()))
                .collect(Collectors.toList());

        // Set values on each columns
        if (CollectionUtils.isNotEmpty(columns) && MapUtils.isNotEmpty(columnValues)) {
            columns.forEach(column -> column.setValues(columnValues.get(column.getColumnName())));
        }

        return columns;
    }

    protected AggregationContextVO toContextVO(ExtractionProductVO source) {

        AggregationContextVO target = new AggregationRdbTripContextVO();

        target.setId(source.getId());
        target.setLabel(source.getLabel());

        ListUtils.emptyIfNull(source.getTables())
                .forEach(t -> target.addTableName(t.getTableName(), t.getLabel()));
        return target;
    }

    protected String getI18nSheetName(String format, String sheetName) {
        return I18n.t(String.format("sumaris.aggregation.%s.%s", format.toUpperCase(), sheetName.toUpperCase()));
    }


}

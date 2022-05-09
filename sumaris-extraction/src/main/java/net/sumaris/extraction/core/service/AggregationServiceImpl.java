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

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import net.sumaris.core.config.ExtractionAutoConfiguration;
import net.sumaris.core.dao.technical.Page;
import net.sumaris.core.dao.technical.SortDirection;
import net.sumaris.core.dao.technical.extraction.ExtractionProductRepository;
import net.sumaris.core.exception.SumarisTechnicalException;
import net.sumaris.core.model.technical.extraction.IExtractionType;
import net.sumaris.core.util.Beans;
import net.sumaris.core.util.StringUtils;
import net.sumaris.core.vo.technical.extraction.*;
import net.sumaris.extraction.core.dao.AggregationDao;
import net.sumaris.extraction.core.dao.technical.table.ExtractionTableColumnOrder;
import net.sumaris.extraction.core.dao.technical.table.ExtractionTableDao;
import net.sumaris.extraction.core.type.AggExtractionTypeEnum;
import net.sumaris.extraction.core.util.ExtractionProducts;
import net.sumaris.extraction.core.util.ExtractionTypes;
import net.sumaris.extraction.core.vo.*;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.ListUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Service;

import javax.annotation.Nullable;
import javax.annotation.PostConstruct;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author peck7 on 17/12/2018.
 */
@Slf4j
@Service("aggregationService")
@ConditionalOnBean({ExtractionAutoConfiguration.class})
public class AggregationServiceImpl implements AggregationService {

    private final ConfigurableApplicationContext context;

    private final ExtractionTableDao extractionTableDao;

    private Optional<TaskExecutor> taskExecutor;

    @Autowired
    private ExtractionProductRepository productRepository;

    private Map<IExtractionType, AggregationDao<?,?,?>> daosByType = Maps.newHashMap();

    public AggregationServiceImpl(ConfigurableApplicationContext context,
                                  ExtractionTableDao extractionTableDao) {
        this.context = context;
        this.extractionTableDao = extractionTableDao;
    }

    @PostConstruct
    protected void init() {
        try {
            this.taskExecutor = Optional.of(context.getBean(TaskExecutor.class));
        }
        catch(BeansException e) {
            log.warn("Failed to get bean TaskExecutor", e);
            this.taskExecutor = Optional.empty();
        }

            // Register all extraction daos
        context.getBeansOfType(AggregationDao.class).values()
            .forEach(dao -> {
                IExtractionType type = dao.getFormat();
                // Check if unique, by format
                if (daosByType.containsKey(type)) {
                    throw new BeanInitializationException(
                        String.format("More than one AggregationDao class found for the format %s v%s: [%s, %s]",
                            type.getLabel(),
                            type.getVersion(),
                            daosByType.get(type).getClass().getSimpleName(),
                            dao.getClass().getSimpleName()));
                }
                // Register the dao
                daosByType.put(type, dao);
            });
    }

    public Set<IExtractionType> getTypes() {
        return daosByType.keySet();
    }

    public AggregationContextVO aggregate(@NonNull IExtractionType type,
                                          @Nullable ExtractionFilterVO filter,
                                          AggregationStrataVO strata) {

        Preconditions.checkNotNull(type.getParent(), "Cannot aggregate: missing 'type.parent'");
        ExtractionProductVO source = getProductWithTables(type.getParent());

        // Aggregate (create agg tables)
        return getDao(type).aggregate(source, filter, strata);
    }

    @Override
    public AggregationResultVO readBySpace(@NonNull IExtractionType type,
                                           @Nullable ExtractionFilterVO filter,
                                           @Nullable AggregationStrataVO strata,
                                           Page page) {
        filter = ExtractionFilterVO.nullToEmpty(filter);

        // Prepare the read filter
        ExtractionFilterVO readFilter = ExtractionFilterVO.builder()
            .sheetName(filter.getSheetName())
            .build();

        AggregationContextVO context;
        if (type instanceof AggregationContextVO) {
            context = (AggregationContextVO)type;
        }

        // Convert to context VO (need the next read() function)
        else {
            ExtractionProductVO product = getProductWithTables(type);
            String sheetName = strata != null && strata.getSheetName() != null ? strata.getSheetName() : filter.getSheetName();
            context = toContextVO(product, sheetName);
        }

        // Read the context
        return readBySpace(context, readFilter, strata, page);
    }




    @Override
    public AggregationTechResultVO readByTech(@NonNull IExtractionType type,
                                              @Nullable ExtractionFilterVO filter,
                                              @Nullable AggregationStrataVO strata,
                                              String sort,
                                              SortDirection direction) {

        ExtractionProductVO product = getProductWithTables(type);
        filter = ExtractionFilterVO.nullToEmpty(filter);

        // Convert to context VO (need the next read() function)
        String sheetName = strata != null && strata.getSheetName() != null ? strata.getSheetName() : filter.getSheetName();
        Preconditions.checkArgument(StringUtils.isNotBlank(sheetName), String.format("Missing 'filter.%s' or 'strata.%s",
                ExtractionFilterVO.Fields.SHEET_NAME,
                AggregationStrataVO.Fields.LABEL));

        AggregationContextVO context = toContextVO(product, sheetName);

        strata = strata != null ? strata : (context.getStrata() != null ? context.getStrata() : new AggregationStrataVO());

        String tableName = context.getTableNameBySheetName(sheetName);

        // Missing the expected sheet = return no data
        if (tableName == null) return createEmptyTechResult();

        AggExtractionTypeEnum format = AggExtractionTypeEnum.valueOf(context.getFormat(), context.getVersion());
        return getDao(format).readByTech(tableName, filter, strata, sort, direction);
    }

    @Override
    public MinMaxVO getTechMinMax(@NonNull IExtractionType type, @Nullable ExtractionFilterVO filter, @Nullable AggregationStrataVO strata) {

        ExtractionProductVO product = getProductWithTables(type);

        filter = ExtractionFilterVO.nullToEmpty(filter);

        // Convert to context VO (need the next read() function)
        String sheetName = strata != null && strata.getSheetName() != null ? strata.getSheetName() : filter.getSheetName();
        Preconditions.checkArgument(StringUtils.isNotBlank(sheetName), String.format("Missing 'filter.%s' or 'strata.%s",
                ExtractionFilterVO.Fields.SHEET_NAME,
                AggregationStrataVO.Fields.LABEL));

        AggregationContextVO context = toContextVO(product, sheetName);

        strata = strata != null ? strata : (context.getStrata() != null ? context.getStrata() : new AggregationStrataVO());

        String tableName = context.getTableNameBySheetName(sheetName);

        AggExtractionTypeEnum format = AggExtractionTypeEnum.valueOf(context.getFormat(), context.getVersion());
        return getDao(format).getTechMinMax(tableName, filter, strata);
    }


    @Override
    public void clean(AggregationContextVO context) {
        log.info("Cleaning aggregation #{}", context.getId());
        getDao(context).clean(context);
    }

    @Override
    public List<ExtractionTableVO> toProductTableVO(AggregationContextVO source) {

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

                table.setName(ExtractionProducts.getSheetDisplayName(source, sheetName));
                table.setIsSpatial(source.hasSpatialColumn(tableName));

                // Columns
                List<ExtractionTableColumnVO> columns = toProductColumnVOs(source, tableName,
                    ExtractionTableColumnFetchOptions.builder()
                        .withRankOrder(false) // skip rankOrder, because fill later, by format and sheetName (more accuracy)
                        .build());

                // Fill rank order
                ExtractionTableColumnOrder.fillRankOrderByTypeAndSheet(source, sheetName, columns);

                table.setColumns(columns);

                return table;
            })
            .collect(Collectors.toList());
    }

    /* -- protected methods -- */


    protected AggregationResultVO readBySpace(@NonNull AggregationContextVO context,
                                              @Nullable ExtractionFilterVO filter,
                                              @Nullable AggregationStrataVO strata,
                                              Page page) {
        filter = ExtractionFilterVO.nullToEmpty(filter);
        strata = strata != null ? strata : (context.getStrata() != null ? context.getStrata() : new AggregationStrataVO());
        String sheetName = strata.getSheetName() != null ? strata.getSheetName() : filter.getSheetName();

        String tableName = StringUtils.isNotBlank(sheetName) ? context.getTableNameBySheetName(sheetName) : null;

        // Missing the expected sheet = return no data
        if (tableName == null) return createEmptyResult();

        // Force strata and filter to have the same sheet
        strata.setSheetName(sheetName);
        filter.setSheetName(sheetName);

        // Read the data
        return getDao(context).readBySpace(tableName, filter, strata, page);
    }

    protected AggregationResultVO createEmptyResult() {
        AggregationResultVO result = new AggregationResultVO();
        result.setColumns(ImmutableList.of());
        result.setTotal(0);
        result.setRows(ImmutableList.of());
        return result;
    }

    protected AggregationTechResultVO createEmptyTechResult() {
        AggregationTechResultVO result = new AggregationTechResultVO();
        result.setData(Maps.newHashMap());
        return result;
    }

    protected List<ExtractionTableColumnVO> toProductColumnVOs(AggregationContextVO context, String tableName,
                                                               ExtractionTableColumnFetchOptions fetchOptions) {


        // Get columns (from table metadata), but exclude hidden columns
        List<ExtractionTableColumnVO> columns = extractionTableDao.getColumns(tableName, fetchOptions);

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

    protected AggregationContextVO toContextVO(@NonNull ExtractionProductVO source,
                                               @NonNull String sheetName) {

        AggregationContextVO target = new AggregationContextVO();

        target.setId(source.getId());
        target.setLabel(source.getLabel());
        target.setCategory(source.getCategory());
        target.setFormat(source.getFormat());
        target.setVersion(source.getVersion());
        target.setUpdateDate(source.getUpdateDate());

        ListUtils.emptyIfNull(source.getTables())
            .forEach(t -> target.addTableName(t.getTableName(), t.getLabel()));

        // Find the strata to apply, by sheetName
        if (source.getStratum() != null) {
            AggregationStrataVO productStrata = source.getStratum().stream()
                    .filter(s -> sheetName.equals(s.getSheetName()))
                    .findFirst().orElse(null);
            if (productStrata != null) {
                AggregationStrataVO strata = new AggregationStrataVO(productStrata); // Copy
                target.setStrata(strata);
            }
        }

        return target;
    }

    /**
     * Get self bean, to be able to use new transation
     * @return
     */
    protected AggregationService self() {
        return context.getBean("aggregationService", AggregationService.class);
    }


    protected ExtractionProductVO getProductWithTables(@NonNull IExtractionType type) {
        // Avoid to fetch, if already a full object
        if (type instanceof ExtractionProductVO && ((ExtractionProductVO)type).getTables() != null) {
            return (ExtractionProductVO)type;
        }
        log.warn("Fetching a product without cache! Received a {} without tables. Make sure this cannot be done earlier !", type.getClass().getSimpleName());
        return type.getId() != null
            ? productRepository.get(type.getId(), ExtractionProductFetchOptions.TABLES_AND_RECORDER)
            : productRepository.getByLabel(type.getLabel(), ExtractionProductFetchOptions.TABLES_AND_RECORDER);
    }

    protected <C extends AggregationContextVO, F extends ExtractionFilterVO, S extends AggregationStrataVO>
    AggregationDao<C, F, S> getDao(@NonNull IExtractionType type) {
        try {
            IExtractionType daoType = daosByType.containsKey(type) ? type : ExtractionTypes.findOneMatch(daosByType.keySet(), type);
            return (AggregationDao<C, F, S>)daosByType.get(daoType);
        }
        catch (IllegalArgumentException e) {
            throw new SumarisTechnicalException(String.format("Unknown aggregation format (no dao): %s", type));
        }
    }
}

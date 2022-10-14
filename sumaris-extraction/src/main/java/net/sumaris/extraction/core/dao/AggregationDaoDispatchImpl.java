/*
 * #%L
 * SUMARiS
 * %%
 * Copyright (C) 2019 SUMARiS Consortium
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

package net.sumaris.extraction.core.dao;

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
import net.sumaris.extraction.core.util.ExtractionTypes;
import net.sumaris.extraction.core.vo.*;
import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Component;

import javax.annotation.Nullable;
import javax.annotation.PostConstruct;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author peck7 on 17/12/2018.
 */
@Slf4j
@Component("aggregationDaoDispatcher")
@ConditionalOnBean({ExtractionAutoConfiguration.class})
public class AggregationDaoDispatchImpl implements AggregationDaoDispatcher {

    private final ConfigurableApplicationContext context;

    private final ExtractionProductRepository extractionProductRepository;

    private final Map<IExtractionType, AggregationDao<?,?,?>> daoByType = Maps.newHashMap();

    public AggregationDaoDispatchImpl(ConfigurableApplicationContext context, ExtractionProductRepository extractionProductRepository) {
        this.context = context;
        this.extractionProductRepository = extractionProductRepository;
    }

    @PostConstruct
    protected void registerDelegates() {
        // Register daos
        context.getBeansOfType(AggregationDao.class).values()
            .forEach(dao -> {
                Set<IExtractionType> types = dao.getManagedTypes();
                types.forEach(type -> {
                        // Check if unique, by format
                        if (daoByType.containsKey(type)) {
                            throw new BeanInitializationException(
                                String.format("More than one AggregationDao class found for the format %s v%s: [%s, %s]",
                                    type.getLabel(),
                                    type.getVersion(),
                                    daoByType.get(type).getClass().getSimpleName(),
                                    dao.getClass().getSimpleName()));
                        }
                        // Register the dao
                        daoByType.put(type, dao);
                    });
                }
            );
    }

    public Set<IExtractionType> getManagedTypes() {
        return daoByType.keySet();
    }

    @Override
    public AggregationContextVO execute(@NonNull IExtractionType type,
                                        @NonNull IExtractionType parent,
                                        @Nullable ExtractionFilterVO filter,
                                        AggregationStrataVO strata) {

        // Make sure tables has been loaded
        IAggregationSourceVO source = toAggregationSource(parent);

        // Aggregate (create agg tables)
        return getDao(type).aggregate(source, filter, strata);
    }

    @Override
    public AggregationResultVO read(@NonNull IExtractionType type,
                                    @Nullable ExtractionFilterVO filter,
                                    @Nullable AggregationStrataVO strata,
                                    Page page) {

        IAggregationSourceVO source = toAggregationSource(type);
        filter = ExtractionFilterVO.nullToEmpty(filter);

        String sheetName = strata != null && strata.getSheetName() != null ? strata.getSheetName() : filter.getSheetName();
        strata = nullToEmptyStrata(source, strata, sheetName);
        final String finalSheetName = sheetName != null ? sheetName : strata.getSheetName();
        Preconditions.checkArgument(StringUtils.isNotBlank(finalSheetName), String.format("Missing 'filter.%s' or 'strata.%s",
            ExtractionFilterVO.Fields.SHEET_NAME,
            AggregationStrataVO.Fields.SHEET_NAME));

        // Force strata and filter to have the same sheet
        strata.setSheetName(finalSheetName);

        // Prepare the read filter
        ExtractionFilterVO readFilter = ExtractionFilterVO.builder()
            .sheetName(finalSheetName)
            .criteria(filter.getCriteria().stream()
                .filter(criteria -> finalSheetName.equals(criteria.getSheetName()))
                .collect(Collectors.toList()))
            .build();

        String tableName = StringUtils.isNotBlank(sheetName) ? source.findTableNameBySheetName(sheetName).orElse(null) : null;

        // Missing the expected sheet = return no data
        if (tableName == null) return createEmptyResult();

        // Read the data
        return getDao(source).read(tableName, readFilter, strata, page);
    }

    @Override
    public AggregationTechResultVO readByTech(@NonNull IExtractionType type,
                                              @Nullable ExtractionFilterVO filter,
                                              @Nullable AggregationStrataVO strata,
                                              String sort,
                                              SortDirection direction) {

        IAggregationSourceVO source = toAggregationSource(type);
        filter = ExtractionFilterVO.nullToEmpty(filter);

        // Convert to context VO (need the next read() function)
        String sheetName = strata != null && strata.getSheetName() != null ? strata.getSheetName() : filter.getSheetName();
        Preconditions.checkArgument(StringUtils.isNotBlank(sheetName), String.format("Missing 'filter.%s' or 'strata.%s",
                ExtractionFilterVO.Fields.SHEET_NAME,
                AggregationStrataVO.Fields.SHEET_NAME));

        strata = nullToEmptyStrata(source, strata, sheetName);

        String tableName = source.findTableNameBySheetName(sheetName).orElse(null);

        // Missing the expected sheet = return no data
        if (tableName == null) return createEmptyTechResult();

        return getDao(source).readByTech(tableName, filter, strata, sort, direction);
    }

    @Override
    public MinMaxVO getTechMinMax(@NonNull IExtractionType type,
                                  @Nullable ExtractionFilterVO filter,
                                  @Nullable AggregationStrataVO strata) {

        IAggregationSourceVO source = toAggregationSource(type);
        filter = ExtractionFilterVO.nullToEmpty(filter);

        // Convert to context VO (need the next read() function)
        String sheetName = strata != null && strata.getSheetName() != null ? strata.getSheetName() : filter.getSheetName();
        Preconditions.checkArgument(StringUtils.isNotBlank(sheetName), String.format("Missing 'filter.%s' or 'strata.%s",
                ExtractionFilterVO.Fields.SHEET_NAME,
                AggregationStrataVO.Fields.SHEET_NAME));

        strata = nullToEmptyStrata(source, strata, sheetName);

        String tableName = source.findTableNameBySheetName(sheetName).orElse(null);

        if (tableName == null) return new MinMaxVO(0d, 0d);

        return getDao(source).getTechMinMax(tableName, filter, strata);
    }

    @Override
    public void clean(AggregationContextVO context) {
        log.info("Cleaning aggregation #{}", context.getId());
        getDao(context).clean(context);
    }

    /* -- protected methods -- */

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

    protected AggregationStrataVO nullToEmptyStrata(@NonNull IAggregationSourceVO source,
                                                    @Nullable AggregationStrataVO strata,
                                                    @Nullable String sheetName) {
        if (strata != null) return strata;

        if (StringUtils.isBlank(sheetName)) {
            return Beans.getStream(source.getStratum())
                .findFirst()
                // Copy if found (constructor by copy)
                .map(AggregationStrataVO::new)
                // New empty
                .orElseGet(AggregationStrataVO::new);
        }

        return source.findStrataBySheetName(sheetName)
            // Found: make a copy
            .map(AggregationStrataVO::new)
            // Not found: create a new empty strata
            .orElseGet(() -> {
                AggregationStrataVO defaultStrata = new AggregationStrataVO();
                defaultStrata.setSheetName(sheetName);
                return defaultStrata;
            });
    }

    protected IAggregationSourceVO toAggregationSource(@NonNull IExtractionType type) {
        if (type instanceof ExtractionProductVO && ((ExtractionProductVO)type).getTables() != null) {
            return (IAggregationSourceVO)type;
        }
        if (type instanceof ExtractionContextVO && ((ExtractionContextVO)type).getTableNames() != null) {
            return (IAggregationSourceVO)type;
        }
        log.warn("Fetching a product tables, outside cache! Received a {} without tables. Make sure this cannot be done earlier !", type.getClass().getSimpleName());
        return type.getId() != null
            ? extractionProductRepository.get(type.getId(), ExtractionProductFetchOptions.TABLES_AND_STRATUM)
            : extractionProductRepository.getByLabel(type.getLabel(), ExtractionProductFetchOptions.TABLES_AND_STRATUM);
    }

    protected <C extends AggregationContextVO, F extends ExtractionFilterVO, S extends AggregationStrataVO>
    AggregationDao<C, F, S> getDao(@NonNull IExtractionType type) {
        try {
            IExtractionType daoType = daoByType.containsKey(type) ? type : ExtractionTypes.findOneMatch(daoByType.keySet(), type);
            return (AggregationDao<C, F, S>) daoByType.get(daoType);
        }
        catch (IllegalArgumentException e) {
            throw new SumarisTechnicalException(String.format("Unknown aggregation format (no dao): %s", type));
        }
    }
}

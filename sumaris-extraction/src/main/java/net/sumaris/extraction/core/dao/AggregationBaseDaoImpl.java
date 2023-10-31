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
import com.google.common.collect.ImmutableSet;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import net.sumaris.core.dao.technical.Page;
import net.sumaris.core.dao.technical.SortDirection;
import net.sumaris.core.dao.technical.schema.SumarisColumnMetadata;
import net.sumaris.core.dao.technical.schema.SumarisTableMetadata;
import net.sumaris.core.vo.technical.extraction.AggregationStrataVO;
import net.sumaris.core.vo.technical.extraction.ExtractionTableColumnFetchOptions;
import net.sumaris.core.vo.technical.extraction.ExtractionTableColumnVO;
import net.sumaris.extraction.core.dao.technical.SQLAggregatedFunction;
import net.sumaris.extraction.core.dao.technical.schema.SumarisTableUtils;
import net.sumaris.extraction.core.vo.*;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
public abstract class AggregationBaseDaoImpl<
    C extends AggregationContextVO,
    F extends ExtractionFilterVO,
    S extends AggregationStrataVO>
    extends ExtractionBaseDaoImpl<C, F> {

    protected ExtractionResultVO readWithAggColumns(@NonNull String tableName,
                                                 @Nullable F filter,
                                                 Set<String> groupByColumnNames,
                                                 final Map<String, SQLAggregatedFunction> otherColumnNames,
                                                 Page page) {

        ExtractionResultVO result = new ExtractionResultVO();

        List<String> columnNames = ImmutableList.<String>builder()
            .addAll(groupByColumnNames)
            .addAll(otherColumnNames.keySet())
            .build();

        StringBuilder whereBuilder = new StringBuilder();

        // Set columns metadata
        SumarisTableMetadata table = databaseMetadata.getTable(tableName);
        if (table != null && table.getColumnsCount() > 0) {
            List<ExtractionTableColumnVO> columns = toProductColumnVOs(table, columnNames,
                ExtractionTableColumnFetchOptions.MINIMAL // Not need rankOrder, when agg rows
            );
            result.setColumns(columns);

            whereBuilder.append(SumarisTableUtils.getSqlWhereClause(table, filter));
        } else {
            log.warn("Unable to find metadata for table " + tableName);
        }

        final String tableAlias = table != null ? table.getAlias() : null;

        if (whereBuilder.length() == 0) {
            whereBuilder.append("WHERE 1=1");
        }

        List<String> columnNamesWithFunction = columnNames.stream()
            .map(c -> {
                SQLAggregatedFunction sqlAggregatedFunction = otherColumnNames.get(c);
                if (sqlAggregatedFunction == null) {
                    return SumarisTableUtils.getAliasedColumnName(tableAlias, c);
                }
                else {
                    SumarisColumnMetadata column = table != null ? table.getColumnMetadata(c) : null;

                    // If nullable column: replace null values will zero
                    if (column == null || column.isNullable()) {
                        // TODO: make sure this replacement will NOT affect the AVG function
                        return String.format("%s(COALESCE(%s, 0))",
                            sqlAggregatedFunction.name().toLowerCase(),
                            SumarisTableUtils.getAliasedColumnName(tableAlias, c));
                    }

                    return String.format("%s(%s))",
                        sqlAggregatedFunction.name().toLowerCase(),
                        SumarisTableUtils.getAliasedColumnName(tableAlias, c));
                }
            })
            .collect(Collectors.toList());

        Collection<String> groupByColumns = SumarisTableUtils.getAliasedColumns(tableAlias, groupByColumnNames);
        String tableWithAlias = SumarisTableUtils.getAliasedTableName(tableAlias, tableName);

        String sql = SumarisTableUtils.getCountGroupByQuery(
            tableWithAlias,
            columnNamesWithFunction,
            whereBuilder.toString(),
            groupByColumns);
        long count = queryCount(sql);
        result.setTotal(count);

        // No data: stop here
        if (count == 0) return result;

        sql = SumarisTableUtils.getSelectGroupByQuery(
            tableWithAlias,
            columnNamesWithFunction,
            whereBuilder.toString(),
            // Group by
            groupByColumns,
            // Sort by same columns, because of pageable
            SumarisTableUtils.getAliasedColumns(tableAlias, groupByColumnNames),
            page.getSortDirection());

        // Execute the query
        int columnCount = columnNamesWithFunction.size();
        List<String[]> rows = query(sql, r -> this.toTableRowVO(r, columnCount), page.getOffset(), page.getSize());
        result.setRows(rows);

        return result;
    }

    protected Map<String, Object> readAggColumnByTech(@NonNull SumarisTableMetadata table,
                                                   @Nullable F filter,
                                                   String aggColumnName,
                                                   SQLAggregatedFunction aggFunction,
                                                   String techColumnName,
                                                   String sortColumn, SortDirection direction) {

        final String tableAlias = table.getAlias();
        String tableWithAlias = SumarisTableUtils.getAliasedTableName(tableAlias, table.getName());
        String aggColumnNameWithFunction = String.format("%s(COALESCE(%s, 0)) AGG_VALUE",
            aggFunction.name().toLowerCase(),
            SumarisTableUtils.getAliasedColumnName(tableAlias, aggColumnName));

        String whereClause = SumarisTableUtils.getSqlWhereClause(table, filter);

        techColumnName = SumarisTableUtils.getAliasedColumnName(tableAlias, techColumnName);
        direction = direction != null || StringUtils.isNotBlank(sortColumn) ? direction : SortDirection.DESC;
        sortColumn = StringUtils.isNotBlank(sortColumn) ? SumarisTableUtils.getAliasedColumnName(tableAlias, sortColumn) : "AGG_VALUE";

        String sql = SumarisTableUtils.getSelectGroupByQuery(
            tableWithAlias,
            // Select
            ImmutableList.of(
                techColumnName,
                aggColumnNameWithFunction),
            whereClause,
            // Group by
            ImmutableList.of(techColumnName),
            // Sort by
            ImmutableList.of(sortColumn),
            direction);
        return query(sql, Object[].class)
            .stream()
            .filter(row -> row[0] != null)
            .collect(Collectors.toMap(
                row -> row[0].toString(),
                row -> row[1]
            ));
    }

    protected MinMaxVO getTechMinMax(String tableName,
                                  @Nullable F filter,
                                  Set<String> groupByColumns,
                                  String aggColumnName,
                                  SQLAggregatedFunction aggFunction,
                                  String techColumnName) {
        SumarisTableMetadata table = databaseMetadata.getTable(tableName);
        Preconditions.checkNotNull(table, "Unknown table: " + tableName);

        final String tableAlias = table.getAlias();
        String tableWithAlias = SumarisTableUtils.getAliasedTableName(tableAlias, tableName);
        String aggValueColumnName = "agg_value";
        String aggColumnNameWithFunction = String.format("%s(COALESCE(%s, 0)) %s",
            aggFunction.name().toLowerCase(),
            SumarisTableUtils.getAliasedColumnName(tableAlias, aggColumnName),
            aggValueColumnName);

        String whereClause = SumarisTableUtils.getSqlWhereClause(table, filter);
        String techColumnNameWithAlias = SumarisTableUtils.getAliasedColumnName(tableAlias, techColumnName);

        String groupBySql = SumarisTableUtils.getSelectGroupByQuery(
            tableWithAlias,
            // Select
            ImmutableSet.<String>builder()
                .add(aggColumnNameWithFunction)
                .addAll(SumarisTableUtils.getAliasedColumns(tableAlias, groupByColumns))
                .add(techColumnNameWithAlias)
                .build(),
            whereClause,
            // Group by (tech + times)
            ImmutableSet.<String>builder()
                .addAll(SumarisTableUtils.getAliasedColumns(tableAlias, groupByColumns))
                .add(techColumnNameWithAlias)
                .build(),
            null, null);

        String sql = String.format("SELECT min(%s), max(%s), min(%s), max(%s) FROM (%s)",
            aggValueColumnName, aggValueColumnName,
            techColumnNameWithAlias,
            techColumnNameWithAlias,
            groupBySql,
            tableAlias);

        return query(sql, Object[].class)
            .stream().findFirst()
            .map(row -> MinMaxVO.builder()
                .aggMin(Double.valueOf(row[0].toString()))
                .aggMax(Double.valueOf(row[1].toString()))
                .techMin(Double.valueOf(row[2].toString()))
                .techMax(Double.valueOf(row[3].toString()))
                .build())
            .orElseGet(() -> MinMaxVO.ZERO);
    }

    public <C1 extends AggregationContextVO> void clean(C1 context) {
        super.clean(context);
    }
}

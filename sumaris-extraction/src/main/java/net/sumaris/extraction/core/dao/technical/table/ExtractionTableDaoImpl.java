package net.sumaris.extraction.core.dao.technical.table;

/*-
 * #%L
 * SUMARiS:: Core
 * %%
 * Copyright (C) 2018 SUMARiS Consortium
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
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableList;
import lombok.extern.slf4j.Slf4j;
import com.google.common.collect.ImmutableSet;
import lombok.NonNull;
import net.sumaris.core.dao.technical.Page;
import net.sumaris.core.dao.technical.SortDirection;
import net.sumaris.core.dao.technical.schema.SumarisColumnMetadata;
import net.sumaris.core.dao.technical.schema.SumarisTableMetadata;
import net.sumaris.core.exception.SumarisTechnicalException;
import net.sumaris.extraction.core.dao.AggregationDao;
import net.sumaris.extraction.core.dao.ExtractionDao;
import net.sumaris.extraction.core.dao.technical.ExtractionBaseDaoImpl;
import net.sumaris.extraction.core.dao.technical.schema.SumarisTableMetadatas;
import net.sumaris.extraction.core.vo.ExtractionFilterVO;
import net.sumaris.extraction.core.vo.ExtractionResultVO;
import net.sumaris.extraction.core.util.ExtractionProducts;
import net.sumaris.extraction.core.vo.MinMaxVO;
import net.sumaris.core.vo.technical.extraction.ExtractionTableColumnFetchOptions;
import net.sumaris.core.vo.technical.extraction.ExtractionTableColumnVO;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Repository;
import org.springframework.util.CollectionUtils;

import javax.annotation.Nullable;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Allow to export rows from a table (in VO), with metadata on each columns
 *
 * @author Benoit Lavenier <benoit.lavenier@e-is.pro>
 */
@Repository("extractionTableDao")
@Lazy
@Slf4j
public class ExtractionTableDaoImpl extends ExtractionBaseDaoImpl implements ExtractionTableDao {

    @Override
    public List<String> getAllTableNames() {
        return ImmutableList.copyOf(databaseMetadata.getTableNames());
    }

    @Override
    public ExtractionResultVO getTable(String tableName) {
        return read(tableName, null, Page.builder().size(0).build());
    }

    @Override
    public List<ExtractionTableColumnVO> getColumns(String tableName, ExtractionTableColumnFetchOptions fetchOptions) {
        SumarisTableMetadata table = databaseMetadata.getTable(tableName);
        Preconditions.checkNotNull(table, "Unknown table: " + tableName);
        return toProductColumnVOs(table, table.getColumnNames(), fetchOptions);
    }

    public ExtractionResultVO read(@NonNull String tableName, ExtractionFilterVO filter, Page page) {

        SumarisTableMetadata table = databaseMetadata.getTable(tableName);
        Preconditions.checkNotNull(table, "Unknown table: " + tableName);

        ExtractionResultVO result = new ExtractionResultVO();

        List<String> columnNames = table.getColumnNames().stream()
                // Include/exclude some columns
                .filter(createIncludeExcludePredicate(filter))
                .collect(Collectors.toList());

        // Set columns metadata
        List<ExtractionTableColumnVO> columns = toProductColumnVOs(table, columnNames, ExtractionTableColumnFetchOptions.FULL);
        result.setColumns(columns);

        String whereClause = SumarisTableMetadatas.getSqlWhereClause(table, filter);

        // Count rows
        Number total = getRowCount(table, whereClause);
        result.setTotal(total);

        if (page.getSize() > 0 && total.longValue() > 0) {
            List<String[]> rows = read(table, filter.isDistinct(), columnNames, whereClause, page);
            result.setRows(rows);
        }

        return result;
    }

    @Override
    public void dropTable(String tableName) {
        Preconditions.checkNotNull(tableName);
        Preconditions.checkArgument(tableName.toUpperCase().startsWith(ExtractionDao.TABLE_NAME_PREFIX)
            || tableName.toUpperCase().startsWith(AggregationDao.TABLE_NAME_PREFIX));

        super.dropTable(tableName);
    }

    @Override
    public String createSequence(String tableName) {
        Preconditions.checkNotNull(tableName);
        String upperTableName = tableName.toUpperCase();
        Preconditions.checkArgument(upperTableName.startsWith(ExtractionDao.TABLE_NAME_PREFIX)
                || upperTableName.startsWith(AggregationDao.TABLE_NAME_PREFIX));

        // Make sue sequence name length is lower than 30 characters
        if (upperTableName.length() + ExtractionDao.SEQUENCE_NAME_SUFFIX.length() > 30) {
            upperTableName = upperTableName.substring(0, 30 - ExtractionDao.SEQUENCE_NAME_SUFFIX.length());
        }
        String sequenceName = upperTableName + ExtractionDao.SEQUENCE_NAME_SUFFIX;
        try {
            String sql = getDialect().getCreateSequenceStrings(sequenceName, 1, 1)[0];
            getSession().createSQLQuery(sql).executeUpdate();
        } catch (Exception e) {
            throw new SumarisTechnicalException(String.format("Cannot create sequence '%s': %s", sequenceName, e.getMessage()), e);
        }
        return sequenceName;
    }

    @Override
    public void dropSequence(String sequenceName) {
        Preconditions.checkNotNull(sequenceName);
        Preconditions.checkArgument(sequenceName.startsWith(ExtractionDao.TABLE_NAME_PREFIX)
                || sequenceName.startsWith(AggregationDao.TABLE_NAME_PREFIX));
        Preconditions.checkArgument(sequenceName.endsWith(ExtractionDao.SEQUENCE_NAME_SUFFIX));
        try {
            String sql = getDialect().getDropSequenceStrings(sequenceName)[0];
            getSession().createSQLQuery(sql).executeUpdate();
        } catch (Exception e) {
            throw new SumarisTechnicalException(String.format("Cannot drop sequence '%s': %s", sequenceName, e.getMessage()), e);
        }
    }

    @Override
    public ExtractionResultVO readWithAggColumns(@NonNull String tableName,
                                                 @Nullable ExtractionFilterVO filter,
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

            whereBuilder.append(SumarisTableMetadatas.getSqlWhereClause(table, filter));
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
                        return SumarisTableMetadatas.getAliasedColumnName(tableAlias, c);
                    }
                    else {
                        SumarisColumnMetadata column = table.getColumnMetadata(c);

                        // If nullable column: replace null values will zero
                        if (column == null || column.isNullable()) {
                            // TODO: make sure this replacement will NOT affect the AVG function
                            return String.format("%s(COALESCE(%s, 0))",
                                    sqlAggregatedFunction.name().toLowerCase(),
                                    SumarisTableMetadatas.getAliasedColumnName(tableAlias, c));
                        }

                        return String.format("%s(%s))",
                                sqlAggregatedFunction.name().toLowerCase(),
                                SumarisTableMetadatas.getAliasedColumnName(tableAlias, c));
                    }
                })
                .collect(Collectors.toList());

        Collection<String> groupByColumns = SumarisTableMetadatas.getAliasedColumns(tableAlias, groupByColumnNames);
        String tableWithAlias = SumarisTableMetadatas.getAliasedTableName(tableAlias, tableName);

        String sql = SumarisTableMetadatas.getCountGroupByQuery(
                tableWithAlias,
                columnNamesWithFunction,
                whereBuilder.toString(),
                groupByColumns);
        long count = queryCount(sql);
        result.setTotal(count);

        // No data: stop here
        if (count == 0) return result;

        sql = SumarisTableMetadatas.getSelectGroupByQuery(
                tableWithAlias,
                columnNamesWithFunction,
                whereBuilder.toString(),
                // Group by
                groupByColumns,
                // Sort by same columns, because of pageable
                SumarisTableMetadatas.getAliasedColumns(tableAlias, groupByColumnNames),
            page.getSortDirection());

        // Execute the query
        int columnCount = columnNamesWithFunction.size();
        List<String[]> rows = query(sql, r -> this.toTableRowVO(r, columnCount), page.getOffset(), page.getSize());
        result.setRows(rows);

        return result;
    }

    @Override
    public Map<String, Object> readAggColumnByTech(String tableName,
                                                   @Nullable ExtractionFilterVO filter,
                                                   String aggColumnName,
                                                   SQLAggregatedFunction aggFunction,
                                                   String techColumnName,
                                                   String sortColumn, SortDirection direction) {
        SumarisTableMetadata table = databaseMetadata.getTable(tableName);
        Preconditions.checkNotNull(table, "Unknown table: " + tableName);

        final String tableAlias = table.getAlias();
        String tableWithAlias = SumarisTableMetadatas.getAliasedTableName(tableAlias, tableName);
        String aggColumnNameWithFunction = String.format("%s(COALESCE(%s, 0)) AGG_VALUE",
                aggFunction.name().toLowerCase(),
                SumarisTableMetadatas.getAliasedColumnName(tableAlias, aggColumnName));

        String whereClause = SumarisTableMetadatas.getSqlWhereClause(table, filter);

        techColumnName = SumarisTableMetadatas.getAliasedColumnName(tableAlias, techColumnName);
        direction = direction != null || StringUtils.isNotBlank(sortColumn) ? direction : SortDirection.DESC;
        sortColumn = StringUtils.isNotBlank(sortColumn) ? SumarisTableMetadatas.getAliasedColumnName(tableAlias, sortColumn) : "AGG_VALUE";

        String sql = SumarisTableMetadatas.getSelectGroupByQuery(
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
                .collect(Collectors.toMap(row -> row[0].toString(), row -> row[1]));
    }

    public MinMaxVO getTechMinMax(String tableName,
                                  @Nullable ExtractionFilterVO filter,
                                  Set<String> groupByColumns,
                                  String aggColumnName,
                                  SQLAggregatedFunction aggFunction,
                                  String techColumnName) {
        SumarisTableMetadata table = databaseMetadata.getTable(tableName);
        Preconditions.checkNotNull(table, "Unknown table: " + tableName);

        final String tableAlias = table.getAlias();
        String tableWithAlias = SumarisTableMetadatas.getAliasedTableName(tableAlias, tableName);
        String aggValueColumnName = "AGG_VALUE";
        String aggColumnNameWithFunction = String.format("%s(COALESCE(%s, 0)) %s",
                aggFunction.name().toLowerCase(),
                SumarisTableMetadatas.getAliasedColumnName(tableAlias, aggColumnName),
                aggValueColumnName);

        String whereClause = SumarisTableMetadatas.getSqlWhereClause(table, filter);
        techColumnName = SumarisTableMetadatas.getAliasedColumnName(tableAlias, techColumnName);

        String groupBySql = SumarisTableMetadatas.getSelectGroupByQuery(
                tableWithAlias,
                // Select
                ImmutableSet.<String>builder()
                        .add(aggColumnNameWithFunction)
                        .addAll(SumarisTableMetadatas.getAliasedColumns(tableAlias, groupByColumns))
                        .add(techColumnName)
                        .build(),
                whereClause,
                // Group by (tech + times)
                ImmutableSet.<String>builder()
                        .addAll(SumarisTableMetadatas.getAliasedColumns(tableAlias, groupByColumns))
                        .add(techColumnName)
                        .build(),
                null, null);

        String sql = String.format("SELECT min(%s), max(%s) FROM (%s)", aggValueColumnName, aggValueColumnName, groupBySql);

        return query(sql, Object[].class)
                .stream().findFirst()
                .map(row -> MinMaxVO.builder()
                        .min(Double.valueOf(row[0].toString()))
                        .max(Double.valueOf(row[1].toString()))
                        .build())
                .orElseGet(() -> new MinMaxVO(0d, 0d));
    }

    @Override
    public long getRowCount(String tableName) {
        Preconditions.checkNotNull(tableName);

        String sql = String.format("SELECT COUNT(*) from %s", tableName);
        return queryCount(sql);
    }

    /* -- protected method -- */


    protected Number getRowCount(SumarisTableMetadata table, String whereClause) {

        String sql = table.getCountAllQuery();

        if (StringUtils.isNotBlank(whereClause)) {
            sql += whereClause;
        }

        Number total = (Number) getEntityManager()
                .createNativeQuery(sql)
                .getSingleResult();
        return total;
    }

    protected List<String[]> read(SumarisTableMetadata table,
                                  boolean distinct,
                                  List<String> columnNames,
                                  String whereClause,
                                  Page page) {
        String sql = table.getSelectQuery(distinct, columnNames, whereClause, page.getSortBy(), page.getSortDirection());
        int columnCount = columnNames.size();
        return query(sql, r -> toTableRowVO(r, columnCount), page.getOffset(), page.getSize());
    }

    protected List<String[]> toTableRowsVO(List<Object[]> rows, final int resultLength) {
        return rows.stream().map(r -> toTableRowVO(r, resultLength))
                .collect(Collectors.toList());
    }

    protected String[] toTableRowVO(Object[] row, int columnCount) {
        String[] result = new String[columnCount];
        if (columnCount <= 0) columnCount = row.length;
        for (int i = 0; i < columnCount; i++) {
            if (row[i] != null) {
                result[i] = row[i].toString();
            } else {
                result[i] = null;
            }
        }
        return result;
    }

    /**
     * Read table metadata, to get column.
     *
     * /!\ Important: column order must be the unchanged !! Otherwise getTableGroupByRows() will not work well
     *
     * @param table
     * @param columnNames
     * @param fetchOptions
     * @return
     */
    protected List<ExtractionTableColumnVO> toProductColumnVOs(SumarisTableMetadata table,
                                                               Collection<String> columnNames,
                                                               ExtractionTableColumnFetchOptions fetchOptions) {

        List<ExtractionTableColumnVO> columns = columnNames.stream()
                // Get column metadata
                .map(table::getColumnMetadata)
                .filter(Objects::nonNull)
                // Transform in VO
                .map(ExtractionProducts::toProductColumnVO)
                .collect(Collectors.toList());

        if (fetchOptions.isWithRankOrder()) {
            ExtractionTableColumnOrder.fillRankOrderByTableName(table.getName(), columns);
        }

        return columns;
    }

    protected Predicate<String> createIncludeExcludePredicate(ExtractionFilterVO filter) {
        return createIncludeExcludePredicate(filter.getIncludeColumnNames(), filter.getExcludeColumnNames());
    }

    protected Predicate<String> createIncludeExcludePredicate(Set<String> includes, Set<String> excludes) {
        final boolean includeAll = CollectionUtils.isEmpty(includes);
        final boolean excludeNone = CollectionUtils.isEmpty(excludes);

        if (includeAll && excludeNone) {
            return (column) -> true;
        }

        return (column) -> {
            boolean isInclude = includeAll || includes.stream().anyMatch((include) -> {
                if (include.contains("*")) {
                    final String regexp = include.replaceAll("[*]", ".*");
                    return column.matches(regexp);
                }
                return column.equalsIgnoreCase(include);
            });

            boolean isExclude = !excludeNone && excludes.stream().anyMatch((exclude) -> {
                if (exclude.contains("*")) {
                    final String regexp = exclude.replaceAll("[*]", ".*");
                    return column.matches(regexp);
                }
                return column.equalsIgnoreCase(exclude);
            });

            return isInclude && !isExclude;
        };
    }

}

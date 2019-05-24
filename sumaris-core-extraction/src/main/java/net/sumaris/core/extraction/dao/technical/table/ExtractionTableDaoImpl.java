package net.sumaris.core.extraction.dao.technical.table;

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
import com.google.common.collect.ImmutableSet;
import net.sumaris.core.dao.technical.SortDirection;
import net.sumaris.core.dao.technical.schema.*;
import net.sumaris.core.exception.SumarisTechnicalException;
import net.sumaris.core.extraction.dao.technical.ExtractionBaseDaoImpl;
import net.sumaris.core.extraction.dao.technical.schema.SumarisTableMetadatas;
import net.sumaris.core.extraction.vo.*;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.stereotype.Repository;
import org.springframework.util.CollectionUtils;

import javax.persistence.Query;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Allow to export rows from a table (in VO), with metadata on each columns
 *
 * @author Benoit Lavenier <benoit.lavenier@e-is.pro>
 */
@Repository("extractionTableDao")
@Lazy
public class ExtractionTableDaoImpl extends ExtractionBaseDaoImpl implements ExtractionTableDao {

    private static final Logger log = LoggerFactory.getLogger(ExtractionTableDaoImpl.class);

    @Autowired
    protected SumarisDatabaseMetadata databaseMetadata;

    @Autowired
    protected DataSource dataSource = null;

    @Override
    public List<String> getAllTableNames() {
        return ImmutableList.copyOf(databaseMetadata.getTableNames());
    }

    @Override
    public ExtractionResultVO getTable(String tableName) {
        return getTableRows(tableName, null, 0, 0, null, null);
    }

    @Override
    public ExtractionResultVO getTableRows(String tableName, ExtractionFilterVO filter, int offset, int size, String sort, SortDirection direction) {
        Preconditions.checkNotNull(tableName);

        SumarisTableMetadata table = databaseMetadata.getTable(tableName.toLowerCase());
        Preconditions.checkNotNull(table, "Unknown table: " + tableName);

        ExtractionResultVO result = new ExtractionResultVO();

        List<String> columnNames = table.getColumnNames().stream()
                // Include/exclude some columns
                .filter(createIncludeExcludePredicate(filter))
                .collect(Collectors.toList());

        // Set columns metadata
        List<ExtractionColumnMetadataVO> columns = toExtractionColumnVOs(table, columnNames);
        result.setColumns(columns);

        String whereClause = SumarisTableMetadatas.getSqlWhereClause(table, filter);

        // Count rows
        Number total = getRowCount(table, whereClause);
        result.setTotal(total);

        if (size > 0 && total.longValue() > 0) {
            List<String[]> rows = getRows(table, filter.isDistinct(), columnNames, whereClause, offset, size, sort, direction);
            result.setRows(rows);
        }

        return result;
    }

    @Override
    public void dropTable(String tableName) {
        Preconditions.checkNotNull(tableName);
        Preconditions.checkArgument(tableName.toUpperCase().startsWith("EXT_"));

        log.debug(String.format("Dropping extraction table {%s}...", tableName));
        Connection conn = DataSourceUtils.getConnection(dataSource);
        try {
            Statement stmt = conn.createStatement();
            stmt.executeUpdate("DROP TABLE " + tableName.toUpperCase());

        } catch (SQLException e) {
            throw new SumarisTechnicalException(String.format("Cannot drop extraction table {%s}...", tableName), e);
        } finally {
            DataSourceUtils.releaseConnection(conn, dataSource);
        }
    }

    @Override
    public ExtractionResultVO getTableGroupByRows(String tableName,
                                                  ExtractionFilterVO filter,
                                                  Set<String> groupByColumnNames,
                                                  Map<String, SQLAggregatedFunction> otherColumnNames,
                                                  int offset, int size, String sort, SortDirection direction) {
        Preconditions.checkNotNull(tableName);

        ExtractionResultVO result = new ExtractionResultVO();

        List<String> columnNames = ImmutableList.<String>builder()
                .addAll(groupByColumnNames)
                .addAll(otherColumnNames.keySet())
                .build();

        String whereClause = null;

        // Set columns metadata
        {
            SumarisTableMetadata table = databaseMetadata.getTable(tableName);
            if (table != null && table.getColumnsCount() > 0) {
                List<ExtractionColumnMetadataVO> columns = toExtractionColumnVOs(table, columnNames);
                result.setColumns(columns);

                whereClause = SumarisTableMetadatas.getSqlWhereClause(table, filter);
            } else {
                // TODO
//                List<ExtractionColumnMetadataVO> columns = toExtractionColumnVOs(columnNames);
//                result.setColumns(columns);
            }
        }

        List<String> columnNamesWithFunction = columnNames.stream()
                .map(c -> {
                    SQLAggregatedFunction sqlAggregatedFunction = otherColumnNames.get(c);
                    if (sqlAggregatedFunction == null) return c;
                    return String.format("%s(%s)", sqlAggregatedFunction.name().toLowerCase(), c);
                })
                .collect(Collectors.toList());

        String sql = SumarisTableMetadatas.getSelectGroupByQuery(
                tableName,
                columnNamesWithFunction,
                whereClause,
                groupByColumnNames,
                groupByColumnNames, // Sort by same columns, because of pageable
                direction);

        // Execute the query
        int columnCount = columnNamesWithFunction.size();
        List<String[]> rows = query(sql, r -> this.toTableRowVO(r, columnCount), offset, size);
        result.setRows(rows);

        // Count rows
        //Number total = getRowCount(table, whereClause);
        //result.setTotal(total);

        //if (size > 0 && total.longValue() > 0) {
        //}

        return result;
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

    protected List<String[]> getRows(SumarisTableMetadata table,
                                     boolean distinct,
                                     List<String> columnNames,
                                     String whereClause, int offset, int size, String sort, SortDirection direction) {
        String sql = table.getSelectQuery(distinct, columnNames, whereClause, sort, direction);
        int columnCount = columnNames.size();
        return query(sql, r -> toTableRowVO(r, columnCount), offset, size);
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

    protected List<ExtractionColumnMetadataVO> toExtractionColumnVOs(SumarisTableMetadata table,
                                                                     Collection<String> columnNames) {
        List<ExtractionColumnMetadataVO> columns = columnNames.stream()
                // Get column metadata
                .map(table::getColumnMetadata)
                .filter(Objects::nonNull)
                // Transform in VO
                .map(this::toExtractionColumnVO)
                .collect(Collectors.toList());

        // Compute the rank order
        String[] orderedColumnNames = ExtractionTableColumnOrder.COLUMNS_BY_TABLE.get(table.getName().toUpperCase());
        if (ArrayUtils.isNotEmpty(orderedColumnNames)) {
            int maxRankOrder = -1;
            for (ExtractionColumnMetadataVO column : columns) {
                int rankOrder = ArrayUtils.indexOf(orderedColumnNames, column.getName().toLowerCase());
                if (rankOrder != -1) {
                    column.setRankOrder(rankOrder + 1);
                    maxRankOrder = Math.max(maxRankOrder, rankOrder + 1);
                }
            }
            // Set rankOrder of unknown columns (e.g. new columns)
            for (ExtractionColumnMetadataVO column : columns) {
                if (column.getRankOrder() == null) {
                    column.setRankOrder(++maxRankOrder);
                }
            }
        }

        return columns;
    }

    protected ExtractionColumnMetadataVO toExtractionColumnVO(SumarisColumnMetadata columnMetadata) {
        ExtractionColumnMetadataVO column = new ExtractionColumnMetadataVO();
        column.setName(columnMetadata.getName());

        column.setDescription(columnMetadata.getDescription());
        column.setDefaultValue(columnMetadata.getDefaultValue());

        String type;
        switch (columnMetadata.getTypeCode()) {
            case Types.NUMERIC:
            case Types.INTEGER:
            case Types.BIGINT:
                type = "integer";
                break;
            case Types.FLOAT:
            case Types.DOUBLE:
                type = "double";
                break;
            case Types.VARCHAR:
            case Types.LONGVARCHAR:
            case Types.NVARCHAR:
                type = "string";
                break;
            default:
                type = columnMetadata.getTypeName().toLowerCase();
        }
        column.setType(type);
        return column;
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

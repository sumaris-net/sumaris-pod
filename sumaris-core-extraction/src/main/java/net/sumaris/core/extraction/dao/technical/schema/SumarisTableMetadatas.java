package net.sumaris.core.extraction.dao.technical.schema;

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

import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import net.sumaris.core.dao.technical.SortDirection;
import net.sumaris.core.dao.technical.schema.SumarisColumnMetadata;
import net.sumaris.core.dao.technical.schema.SumarisTableMetadata;
import net.sumaris.core.exception.SumarisTechnicalException;
import net.sumaris.core.extraction.dao.technical.Daos;
import net.sumaris.core.extraction.vo.ExtractionFilterCriterionVO;
import net.sumaris.core.extraction.vo.ExtractionFilterOperatorEnum;
import net.sumaris.core.extraction.vo.ExtractionFilterVO;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nullable;
import java.sql.Types;
import java.util.Collection;
import java.util.stream.Collectors;

/**
 * Helper class for extraction
 *
 * @author Benoit Lavenier <benoit.lavenier@e-is.pro>*
 */
public class SumarisTableMetadatas {

    public static String getSqlWhereClause(SumarisTableMetadata table, ExtractionFilterVO filter) {

        String whereContent = getSqlWhereClauseContent(table, filter);

        // Prepend with WHERE keyword
        if (StringUtils.isBlank(whereContent) || "1=1".equals(whereContent)) return "";
        return " WHERE " + whereContent;
    }

    public static String getSqlWhereClauseContent(SumarisTableMetadata table, ExtractionFilterVO filter) {
        return getSqlWhereClauseContent(table, filter, filter != null ? filter.getSheetName() : null);
    }

    public static String getSqlWhereClauseContent(SumarisTableMetadata table, ExtractionFilterVO filter, String sheetName) {
        return getSqlWhereClauseContent(table, filter, sheetName, table != null ? table.getAlias() : null);
    }

    public static String getSqlWhereClauseContent(SumarisTableMetadata table, ExtractionFilterVO filter,
                                                  String sheetName, String tableAlias) {
        return getSqlWhereClauseContent(table, filter, sheetName, tableAlias, false);
    }

    public static String getSqlWhereClauseContent(SumarisTableMetadata table, ExtractionFilterVO filter,
                                                  String appliedSheetName, String tableAlias, boolean skipInvalidCriteria) {

        if (filter == null || CollectionUtils.isEmpty(filter.getCriteria())) return "";

        StringBuilder sql = new StringBuilder();

        //StringBuilder criterionSql = new StringBuilder();

        StringBuilder logicalOperator = new StringBuilder();
        String aliasWithPoint = tableAlias != null ? (tableAlias + ".") : "";

        filter.getCriteria().stream()
                .filter(criterion -> appliedSheetName == null || appliedSheetName.equals(criterion.getSheetName()))
                .forEach(criterion -> {

                    // Get the column to tripFilter
                    Preconditions.checkNotNull(criterion.getName());
                    SumarisColumnMetadata column = table != null ? table.getColumnMetadata(criterion.getName().toLowerCase()) : null;
                    if (column == null) {
                        if (appliedSheetName != null && !skipInvalidCriteria) {
                            throw new SumarisTechnicalException(String.format("Invalid criterion: column '%s' not found in table '%s'", criterion.getName(), table.getName()));
                        } else {
                            // Continue (=skip)
                        }
                    } else {
                        //criterionSql.setLength(0); // Reset
                        //criterionSql.append(aliasWithPoint)
                        //        .append(column.getName());
                        sql.append(logicalOperator)
                                .append(aliasWithPoint)
                                .append(column.getName());

                        // Affect logical operator, for the next criterion
                        if (logicalOperator.length() == 0) {
                            if ("OR".equalsIgnoreCase(StringUtils.trim(filter.getOperator()))) {
                                logicalOperator.append(" OR ");
                            } else {
                                logicalOperator.append(" AND ");
                            }
                        }

                        ExtractionFilterOperatorEnum operator = criterion.getOperator() == null ? ExtractionFilterOperatorEnum.EQUALS : ExtractionFilterOperatorEnum.fromSymbol(criterion.getOperator());

                        if (criterion.getValue() == null && ArrayUtils.isEmpty(criterion.getValues())) {
                            switch (operator) {
                                case NOT_IN:
                                case NOT_EQUALS:
                                    sql.append(" IS NOT NULL");
                                    break;
                                default:
                                    sql.append(" IS NULL");
                            }
                        } else {
                            switch (operator) {
                                case IN:
                                    sql.append(String.format(" IN (%s)", getInValues(column, criterion)));
                                    break;
                                case NOT_IN:
                                    sql.append(String.format(" NOT IN (%s)", getInValues(column, criterion)));
                                    break;
                                case EQUALS:
                                    sql.append(String.format(" = %s", getSingleValue(column, criterion)));
                                    break;
                                case NOT_EQUALS:
                                    sql.append(String.format(" <> %s", getSingleValue(column, criterion)));
                                    break;
                                case LESS_THAN:
                                    sql.append(String.format(" < %s", getSingleValue(column, criterion)));
                                    break;
                                case LESS_THAN_OR_EQUALS:
                                    sql.append(String.format(" <= %s", getSingleValue(column, criterion)));
                                    break;
                                case GREATER_THAN:
                                    sql.append(String.format(" > %s", getSingleValue(column, criterion)));
                                    break;
                                case GREATER_THAN_OR_EQUALS:
                                    sql.append(String.format(" >= %s", getSingleValue(column, criterion)));
                                    break;
                                case BETWEEN:
                                    sql.append(String.format(" BETWEEN %s AND %s", getBetweenValueByIndex(column, criterion, 0), getBetweenValueByIndex(column, criterion, 1)));
                                    break;
                            }
                        }

                        //sql.append(logicalOperator.toString()).append(criterionSql);

                    }
                });

        return sql.toString();
    }

    public static String getSingleValue(SumarisColumnMetadata column, ExtractionFilterCriterionVO criterion) {
        return isNumericColumn(column) ? criterion.getValue() : ("'" + criterion.getValue() + "'");
    }

    public static String getInValues(SumarisColumnMetadata column, ExtractionFilterCriterionVO criterion) {
        if (ArrayUtils.isEmpty(criterion.getValues())) {
            if (criterion.getValue() != null) {
                return getSingleValue(column, criterion);
            }
            Preconditions.checkArgument(false, "Invalid criterion: 'values' is required for operator 'IN' or 'NOT IN'");
        }
        return isNumericColumn(column) ?
                Daos.getSqlInNumbers(criterion.getValues()) :
                Daos.getSqlInEscapedStrings(criterion.getValues());
    }

    public static String getBetweenValueByIndex(SumarisColumnMetadata column, ExtractionFilterCriterionVO criterion, int index) {
        Preconditions.checkNotNull(criterion.getValues(), "Invalid criterion: 'values' is required for operator 'BETWEEN'");
        Preconditions.checkArgument(criterion.getValues().length == 2, "Invalid criterion: 'values' array must have 2 values, for operator 'BETWEEN'");
        Preconditions.checkArgument(index == 0 || index == 1);
        String value = criterion.getValues()[index];
        return isNumericColumn(column) ? value : ("'" + value + "'");
    }


    public static boolean isNumericColumn(SumarisColumnMetadata column) {
        return column.getTypeCode() == Types.NUMERIC
                || column.getTypeCode() == Types.INTEGER
                || column.getTypeCode() == Types.DOUBLE
                || column.getTypeCode() == Types.BIGINT
                || column.getTypeCode() == Types.DECIMAL
                || column.getTypeCode() == Types.FLOAT;
    }

    public static boolean isNotNumericColumn(SumarisColumnMetadata column) {
        return !isNumericColumn(column);
    }

    public static String getAliasedColumnName(@Nullable String alias, String columnName) {
        return StringUtils.isNotBlank(alias) ? alias + "." + columnName : columnName;
    }

    public static String getAliasedTableName(@Nullable String alias, String tableName) {
        return StringUtils.isNotBlank(alias) ? tableName + " AS " + alias : tableName;
    }

    public static String getSelectGroupByQuery(String tableName,
                                               Collection<String> columnNames,
                                               String whereClause,
                                               Collection<String> groupByColumnNames,
                                               Collection<String> sortColumnNames,
                                               SortDirection direction) {
        StringBuilder sb = new StringBuilder();
        sb.append("SELECT ")
                .append(Joiner.on(',').join(columnNames))
                .append(" FROM ")
                .append(tableName);

        // Where clause
        if (StringUtils.isNotBlank(whereClause)) {
            if (!whereClause.trim().startsWith("WHERE")) {
                sb.append(" WHERE ").append(whereClause.trim());
            }
            else {
                sb.append(" ").append(whereClause.trim());
            }
        }

        // Group by clause
        if (org.apache.commons.collections.CollectionUtils.isNotEmpty(groupByColumnNames)) {
            sb.append(" GROUP BY ").append(Joiner.on(',').join(groupByColumnNames));
        }
        // Add order by
        if (org.apache.commons.collections.CollectionUtils.isNotEmpty(sortColumnNames)) {
            String directionStr = direction != null ? (" " + direction.name()) : "";
            sb.append(" ORDER BY ")
                    .append(Joiner.on(directionStr + ",").join(sortColumnNames))
                    .append(directionStr);
        }

        return sb.toString();
    }

    public static String getCountGroupByQuery(String tableName,
                                               Collection<String> columnNames,
                                               String whereClause,
                                               Collection<String> groupByColumnNames) {
        StringBuilder sb = new StringBuilder();
        sb.append("SELECT COUNT(*) FROM (")
            .append(getSelectGroupByQuery(tableName, columnNames, whereClause, groupByColumnNames, null, null))
            .append(")");

        return sb.toString();
    }

    public static Collection<String> getAliasedColumns(String tableAlias,
                                           Collection<String> columnNames) {
        if (StringUtils.isBlank(tableAlias)) return columnNames;
        return columnNames.stream().map(c -> tableAlias + "." + c).collect(Collectors.toList());
    }
}

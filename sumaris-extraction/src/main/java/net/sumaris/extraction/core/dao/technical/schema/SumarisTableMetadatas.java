package net.sumaris.extraction.core.dao.technical.schema;

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
import net.sumaris.core.dao.technical.sql.LogicalOperatorEnum;
import net.sumaris.core.exception.SumarisTechnicalException;
import net.sumaris.extraction.core.dao.technical.Daos;
import net.sumaris.extraction.core.vo.ExtractionFilterCriterionVO;
import net.sumaris.extraction.core.vo.ExtractionFilterVO;
import net.sumaris.core.util.Dates;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nullable;
import java.sql.Types;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
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
        return getSqlWhereClauseContent(table, filter, sheetName, tableAlias, false, false);
    }

    public static String getSqlWhereClauseContent(SumarisTableMetadata table, ExtractionFilterVO filter,
                                                  String appliedSheetName,
                                                  String tableAlias,
                                                  boolean skipInvalidCriteria) {
        return getSqlWhereClauseContent(table, filter, appliedSheetName, tableAlias, skipInvalidCriteria, false);
    }

    public static String getInverseSqlWhereClauseContent(SumarisTableMetadata table, ExtractionFilterVO filter,
                                                  String appliedSheetName,
                                                  String tableAlias,
                                                  boolean skipInvalidCriteria) {
        return getSqlWhereClauseContent(table, filter, appliedSheetName, tableAlias, skipInvalidCriteria, true /*inverse*/);
    }




    public static String getSingleValue(SumarisColumnMetadata column, ExtractionFilterCriterionVO criterion) {
        if (isDateColumn(column)) return Daos.getSqlToDate(Dates.fromISODateTimeString(criterion.getValue()));
        if (isNumericColumn(column)) return criterion.getValue();
        // else alphanumeric column
        return "'" + criterion.getValue() + "'";
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

        if (isDateColumn(column)) {
            // Parse date
            Date date = Dates.safeParseDate(value, Dates.ISO_TIMESTAMP_SPEC, "yyyy-MM-dd");
            Preconditions.checkNotNull(date, String.format("Invalid criterion value '%s'. Expected a date (ISO format, or 'YYYY-MM-DD')", value));
            // Return TO_DATE(...)
            return Daos.getSqlToDate(date);
        }
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

    public static boolean isDateColumn(SumarisColumnMetadata column) {
        return column.getTypeCode() == Types.DATE
                || column.getTypeCode() == Types.TIMESTAMP;
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
            .append(") \"select_group\"");

        return sb.toString();
    }

    public static Collection<String> getAliasedColumns(String tableAlias,
                                           Collection<String> columnNames) {
        if (StringUtils.isBlank(tableAlias)) return columnNames;
        return columnNames.stream().map(c -> tableAlias + "." + c).collect(Collectors.toList());
    }


    /* -- protected functions -- */

    protected static String getSqlWhereClauseContent(SumarisTableMetadata table,
                                                     @Nullable ExtractionFilterVO filter,
                                                     String appliedSheetName,
                                                     String tableAlias,
                                                     boolean skipInvalidCriteria,
                                                     boolean inverseTheLogic) {

        if (filter == null || CollectionUtils.isEmpty(filter.getCriteria())) return "";

        StringBuilder sql = new StringBuilder();
        StringBuilder logicalOperator = new StringBuilder();
        StringBuilder locigalOperatorClose = new StringBuilder();

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
                    String columnAndAlias = aliasWithPoint + column.getName();

                    // Append logical operator (between criterion)
                    sql.append(logicalOperator);

                    LogicalOperatorEnum operator = criterion.getOperator() == null ? LogicalOperatorEnum.EQUALS : LogicalOperatorEnum.fromSymbol(criterion.getOperator());

                    // Inverse the logic, if need
                    if (inverseTheLogic) operator = operator.inverse();

                    boolean skipCriterion = false;

                    // If no value: use NULL or NOT_NULL
                    if (criterion.getValue() == null && ArrayUtils.isEmpty(criterion.getValues())) {
                        switch (operator) {
                            case NOT_IN:
                            case NOT_EQUALS:
                            case NOT_BETWEEN:
                                operator = LogicalOperatorEnum.NOT_NULL;
                                break;
                            default:
                                operator = LogicalOperatorEnum.NULL;
                        }
                    }
                    switch (operator) {
                        case IN:
                            sql.append(columnAndAlias)
                                .append(" IN (")
                                .append(getInValues(column, criterion))
                                .append(")");
                            break;
                        case NOT_IN:
                            sql.append("(")
                                .append(columnAndAlias)
                                .append(" NOT IN (")
                                .append(getInValues(column, criterion))
                                .append(")")
                                .append(column.isNullable() ? " OR " + columnAndAlias + " IS NULL" : "")
                                .append(")");
                            break;
                        case EQUALS:
                            sql.append(columnAndAlias)
                                .append(" = ")
                                .append(getSingleValue(column, criterion));
                            break;
                        case NOT_EQUALS:
                            sql.append("(")
                                .append(columnAndAlias)
                                .append(" <> ")
                                .append(getSingleValue(column, criterion))
                                .append(column.isNullable() ? " OR " + columnAndAlias + " IS NULL" : "")
                                .append(")");
                            break;
                        case LESS_THAN:
                            sql.append(columnAndAlias)
                                .append(" < ").append(getSingleValue(column, criterion));
                            break;
                        case LESS_THAN_OR_EQUALS:
                            sql.append(columnAndAlias)
                                .append(" <= ").append(getSingleValue(column, criterion));
                            break;
                        case GREATER_THAN:
                            sql.append(columnAndAlias)
                                .append(" > ").append(getSingleValue(column, criterion));
                            break;
                        case GREATER_THAN_OR_EQUALS:
                            sql.append(columnAndAlias)
                                .append(" >= ").append(getSingleValue(column, criterion));
                            break;
                        case BETWEEN:
                            sql.append(columnAndAlias)
                                .append(" BETWEEN ")
                                .append(getBetweenValueByIndex(column, criterion, 0))
                                .append(" AND ").append(getBetweenValueByIndex(column, criterion, 1));
                            break;
                        case NOT_BETWEEN:
                            sql
                                .append("(")
                                .append(columnAndAlias)
                                .append(" < ")
                                .append(getBetweenValueByIndex(column, criterion, 0))
                                .append(" OR ")
                                .append(columnAndAlias)
                                .append(" > ")
                                .append(getBetweenValueByIndex(column, criterion, 1))
                                .append(column.isNullable() ? " OR " + columnAndAlias + " IS NULL" : "")
                                .append(")");
                            break;
                        case NULL:
                            sql.append(columnAndAlias).append(" IS NULL");
                            break;
                        case NOT_NULL:
                            sql.append(columnAndAlias).append(" IS NOT NULL");
                            break;
                        default:
                            if (!skipInvalidCriteria) {
                                throw new SumarisTechnicalException(String.format("Invalid criterion: column '%s' has invalid operator '%s' for value '%s'",
                                    criterion.getName(),
                                    operator.getSymbol(),
                                    criterion.getValue() != null ? criterion.getValue() : Arrays.toString(criterion.getValues())));
                            }
                            skipCriterion = true;
                            break;
                    }

                    // Init the logical operator, for next iteration
                    if (!skipCriterion && logicalOperator.length() == 0) {
                        LogicalOperatorEnum ope = StringUtils.isNotBlank(filter.getOperator())
                            ? LogicalOperatorEnum.fromSymbol(filter.getOperator())
                            : LogicalOperatorEnum.AND;

                        // Inverse the logic, if need
                        if (inverseTheLogic) ope = ope.inverse();

                        switch (ope) {
                            case OR:
                                logicalOperator.append(" OR ");
                                break;
                            case OR_NOT:
                                sql.insert(0, "NOT ("); // before the first criteria
                                locigalOperatorClose.append(")"); // after the last criteria
                                logicalOperator.append(") OR NOT (");
                                break;
                            case AND:
                                logicalOperator.append(" AND ");
                                break;
                            case AND_NOT:
                                sql.insert(0, "NOT ("); // before the first criteria
                                locigalOperatorClose.append(")"); // after the last criteria
                                logicalOperator.append(") AND NOT (");
                                break;
                            default:
                                if (!skipInvalidCriteria) {
                                    throw new SumarisTechnicalException(String.format("Invalid filter operator: '%s'", filter.getOperator()));
                                }
                                // Fall back to the default operator
                                logicalOperator.append(" AND ");
                        }
                    }
                }
            });

        // Close last logical operation
        sql.append(locigalOperatorClose);

        return sql.toString();
    }

}

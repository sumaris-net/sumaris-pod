package net.sumaris.core.extraction.dao.technical.schema;

import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import net.sumaris.core.dao.technical.schema.SumarisColumnMetadata;
import net.sumaris.core.dao.technical.schema.SumarisTableMetadata;
import net.sumaris.core.exception.SumarisTechnicalException;
import net.sumaris.core.extraction.vo.ExtractionFilterCriterionVO;
import net.sumaris.core.extraction.vo.ExtractionFilterOperatorEnum;
import net.sumaris.core.extraction.vo.ExtractionFilterVO;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

import java.sql.Types;

/**
 * Helper class for extraction
 * @author Benoit Lavenier <benoit.lavenier@e-is.pro>*
 */
public class SumarisTableMetadatas {

    public static String getSqlWhereClause(SumarisTableMetadata table, ExtractionFilterVO filter) {

        String whereContent = getSqlWhereClauseContent(table, filter);

        // Prepend with WHERE keyword
        if (StringUtils.isBlank(whereContent)) return "";
        return " WHERE " + whereContent;
    }

    public static String getSqlWhereClauseContent(SumarisTableMetadata table, ExtractionFilterVO filter) {
        return getSqlWhereClauseContent(table, filter, filter != null ? filter.getSheetName() : null);
    }

    public static String getSqlWhereClauseContent(SumarisTableMetadata table, ExtractionFilterVO filter, String sheetName) {
        return getSqlWhereClauseContent(table, filter, sheetName, table != null ? table.getAlias() : null);
    }

    public static String getSqlWhereClauseContent(SumarisTableMetadata table, ExtractionFilterVO filter, String sheetName, String tableAlias) {

        if (filter == null || CollectionUtils.isEmpty(filter.getCriteria())) return "";

        StringBuilder sql = new StringBuilder();

        StringBuilder logicalOperator = new StringBuilder();
        String aliasWithPoint = tableAlias != null ? (tableAlias + ".") : "";

        filter.getCriteria().stream()
                .filter(criterion -> sheetName == null || sheetName.equals(criterion.getSheetName()))
                .forEach(criterion -> {

            // Get the column to tripFilter
            Preconditions.checkNotNull(criterion.getName());
            SumarisColumnMetadata column = table.getColumnMetadata(criterion.getName().toLowerCase());
            if (column == null) {
                if (sheetName != null) {
                    throw new SumarisTechnicalException(String.format("Invalid criterion: column '%s' not found", criterion.getName()));
                }
                else {
                    // Continue (=skip)
                }
            }
            else {
                sql.append(logicalOperator.toString())
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
                Joiner.on(',').join(criterion.getValues()) :
                "'" + Joiner.on("','").skipNulls().join(criterion.getValues()) + "'";
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

}

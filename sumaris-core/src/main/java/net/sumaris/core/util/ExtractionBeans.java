package net.sumaris.core.util;

import net.sumaris.core.dao.technical.schema.SumarisColumnMetadata;
import net.sumaris.core.vo.technical.extraction.ExtractionProductColumnVO;

import java.sql.Types;

/**
 * Helper class
 * @author Benoit Lavenier <benoit.lavenier@e-is.pro>*
 */
public class ExtractionBeans {

    public static ExtractionProductColumnVO toProductColumnVO(SumarisColumnMetadata columnMetadata) {
        ExtractionProductColumnVO column = new ExtractionProductColumnVO();

        column.setLabel(StringUtils.underscoreToChangeCase(columnMetadata.getName()));
        column.setName(columnMetadata.getName().toLowerCase());

        column.setColumnName(columnMetadata.getName().toLowerCase());

        column.setDescription(columnMetadata.getDescription());

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
}

package net.sumaris.core.util;

/*-
 * #%L
 * SUMARiS:: Core
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

import net.sumaris.core.dao.technical.schema.SumarisColumnMetadata;
import net.sumaris.core.vo.technical.extraction.ExtractionTableColumnVO;

import java.sql.Types;

/**
 * Helper class
 * @author Benoit Lavenier <benoit.lavenier@e-is.pro>*
 */
public class ExtractionBeans {

    public static ExtractionTableColumnVO toProductColumnVO(SumarisColumnMetadata columnMetadata) {
        ExtractionTableColumnVO column = new ExtractionTableColumnVO();

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
            case Types.REAL:
            case Types.FLOAT:
            case Types.DOUBLE:
            case Types.DECIMAL:
                type = "double";
                break;
            case Types.VARCHAR:
            case Types.LONGVARCHAR:
            case Types.NVARCHAR:
            case Types.CHAR:
                type = "string";
                break;
            default:
                type = columnMetadata.getTypeName().toLowerCase();
        }
        column.setType(type);
        return column;
    }
}

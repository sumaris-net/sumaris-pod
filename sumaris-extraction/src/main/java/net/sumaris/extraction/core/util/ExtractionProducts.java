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

package net.sumaris.extraction.core.util;

import lombok.NonNull;
import net.sumaris.core.dao.technical.schema.SumarisColumnMetadata;
import net.sumaris.core.model.technical.extraction.IExtractionType;
import net.sumaris.core.util.StringUtils;
import net.sumaris.core.vo.technical.extraction.ExtractionTableColumnVO;
import org.nuiton.i18n.I18n;

import java.sql.Types;
import java.util.Date;

/**
 * Helper class
 * @author Benoit Lavenier <benoit.lavenier@e-is.pro>*
 */
public class ExtractionProducts {

    public static String computeProductLabel(@NonNull IExtractionType format, @NonNull long timeInMillis) {
        return computeProductLabel(format.getFormat(), timeInMillis);
    }

    public static String computeProductLabel(@NonNull String formatLabel, @NonNull long timeInMillis) {
        return String.format("%s-%s",
                formatLabel,
                timeInMillis);
    }

    public static String getProductDisplayName(@NonNull IExtractionType format, @NonNull Date time) {
        return getProductDisplayName(format.getFormat(), time.getTime());
    }

    public static String getProductDisplayName(@NonNull IExtractionType format, @NonNull long timeInMillis) {
        return getProductDisplayName(format.getFormat(), timeInMillis);
    }


    public static String getProductDisplayName(@NonNull String formatLabel, long timeInMillis) {
        String key = String.format("sumaris.extraction.%s", formatLabel.toUpperCase());
        String result = I18n.t(key);
        if (!key.equals(result)) return result; // OK, translated
        // Fallback, if not translated
        return String.format("%s#%s", formatLabel, timeInMillis);
    }

    public static String getSheetDisplayName(@NonNull IExtractionType format, String sheetName) {
        return getSheetDisplayName(format.getFormat(), sheetName);
    }

    public static String getSheetDisplayName(@NonNull String formatLabel, @NonNull String sheetName) {
        String key = String.format("sumaris.extraction.%s.%s", formatLabel.toUpperCase(), sheetName.toUpperCase());
        String result = I18n.t(key);
        if (!key.equals(result)) return result; // OK, translated
        // Fallback, if not translated
        return sheetName;
    }

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

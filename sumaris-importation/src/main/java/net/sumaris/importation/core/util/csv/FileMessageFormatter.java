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

package net.sumaris.importation.core.util.csv;

import com.google.common.base.Preconditions;
import net.sumaris.core.dao.technical.schema.SumarisColumnMetadata;
import net.sumaris.core.dao.technical.schema.SumarisTableMetadata;

public class FileMessageFormatter {

    public static String format(FileReader reader, Integer columnIndex, String message) {
        Preconditions.checkArgument(columnIndex == null || columnIndex.intValue() >= 0);
        int lineNumber = reader.getCurrentLine();
        Preconditions.checkArgument(lineNumber > 0);
        StringBuilder sb = new StringBuilder();
        sb.append("[").append(reader.getFileName());
        if (columnIndex != null) {
            String[] headers = reader.getHeaders();
            if (headers != null && columnIndex.intValue() < headers.length) {
                sb.append(".").append(headers[columnIndex]);
            }
        }
        sb.append(":")
                .append(lineNumber)
                .append("] ")
                .append(message);
        return sb.toString();
    }

    public static String format(SumarisTableMetadata table, SumarisColumnMetadata colMeta, int lineNumber, String message) {
        StringBuilder sb = new StringBuilder();
        sb.append("[").append(table.getName());
        if (colMeta != null) {
            sb.append(".").append(colMeta.getName());
        }
        if (lineNumber != -1) {
            sb.append(" / ").append(lineNumber);
        }
        sb.append("] ").append(message);
        return sb.toString();

    }

}

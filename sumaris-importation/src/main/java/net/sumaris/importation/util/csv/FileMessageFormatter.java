package net.sumaris.importation.util.csv;

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

package net.sumaris.core.extraction.vo;

import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;
import lombok.experimental.FieldNameConstants;
import org.apache.commons.collections4.MapUtils;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Benoit Lavenier <benoit.lavenier@e-is.pro>*
 */
@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public abstract class AggregationContextVO extends ExtractionContextVO {

    @FieldNameConstants.Exclude
    Map<String, Map<String, List<Object>>> columnValues = new LinkedHashMap<>();

    public void addTableName(String tableName, String sheetName, Map<String, List<Object>> availableValuesByColumn) {
        super.addTableName(tableName, sheetName);

        if (MapUtils.isNotEmpty(availableValuesByColumn)) {
            columnValues.put(tableName, availableValuesByColumn);
        }
    }
}

package net.sumaris.core.extraction.vo;

import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;
import lombok.experimental.FieldNameConstants;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author Benoit Lavenier <benoit.lavenier@e-is.pro>*
 */
@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public abstract class AggregationContextVO extends ExtractionContextVO {

    Boolean isGeo;

    @FieldNameConstants.Exclude
    Map<String, Map<String, List<Object>>> columnValues = new LinkedHashMap<>();

    @FieldNameConstants.Exclude
    Map<String, Set<String>> spatialColumnNames = new LinkedHashMap<>();

    public void addTableName(String tableName, String sheetName,
                             Set<String> spatialColumnNames,
                             Map<String,List<Object>> availableValuesByColumn) {
        super.addTableName(tableName, sheetName);

        if (CollectionUtils.isNotEmpty(spatialColumnNames)) {
            this.spatialColumnNames.put(tableName, spatialColumnNames);
        }
        if (MapUtils.isNotEmpty(availableValuesByColumn)) {
            columnValues.put(tableName, availableValuesByColumn);
        }
    }


    /**
     * Return the hidden columns of the given table
     * @param tableName
     */
    public boolean hasSpatialColumn(String tableName) {
        return CollectionUtils.isNotEmpty(spatialColumnNames.get(tableName));
    }
}

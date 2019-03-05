package net.sumaris.core.extraction.vo.trip;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;
import lombok.experimental.FieldNameConstants;
import net.sumaris.core.vo.filter.TripFilterVO;
import org.apache.commons.lang3.StringUtils;

import java.util.*;

/**
 * @author Ludovic Pecquot <ludovic.pecquot>
 * @author Benoit Lavenier <benoit.lavenier@e-is.pro>
 */
@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public abstract class ExtractionTripContextVO {

    long id;
    TripFilterVO filter;

    String formatName;
    String formatVersion;

    List<ExtractionPmfmInfoVO> pmfmInfos;

    @FieldNameConstants.Exclude
    Map<String, String> tableNames = new LinkedHashMap<>();

    public Date getStartDate() {
        return filter != null ? filter.getStartDate() : null;
    }

    public Date getEndDate() {
        return filter != null ? filter.getEndDate() : null;
    }

    public List<String> getProgramLabels() {
        return filter != null && StringUtils.isNotBlank(filter.getProgramLabel()) ? ImmutableList.of(filter.getProgramLabel()) : null;
    }

    public List<Integer> getRecorderDepartmentIds() {
        return filter != null && filter.getRecorderDepartmentId() != null ? ImmutableList.of(filter.getRecorderDepartmentId()) : null;
    }

    public List<Integer> getVesselIds() {
        return filter != null && filter.getVesselId() != null ? ImmutableList.of(filter.getVesselId()) : null;
    }

    public List<Integer> getLocationIds() {
        return filter != null && filter.getLocationId() != null ? ImmutableList.of(filter.getLocationId()) : null;
    }

    /**
     * Register a table (with rows inside)
     * @param tableName
     * @param sheetName
     */
    public void addTableName(String tableName, String sheetName) {
        tableNames.put(tableName, sheetName);
    }

    public String getSheetName(String tableName) {
        String otherName = tableNames.get(tableName);
        return (otherName!=null) ? otherName : tableName;
    }

    public Set<String> getTableNames() {
        return tableNames.keySet();
    }

    public String getTableNameBySheetName(String sheetName) {
        Preconditions.checkNotNull(sheetName);
        return tableNames.entrySet().stream()
                .filter(e -> sheetName.equalsIgnoreCase(e.getValue()))
                .map(e -> e.getKey())
                .findFirst()
                .orElse(null);
    }

    public boolean hasSheet(String sheetName) {
        Preconditions.checkNotNull(sheetName);
        return tableNames.containsValue(sheetName);
    }

}

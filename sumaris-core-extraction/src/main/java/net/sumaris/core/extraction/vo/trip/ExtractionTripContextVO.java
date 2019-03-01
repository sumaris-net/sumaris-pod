package net.sumaris.core.extraction.vo.trip;

import com.google.common.collect.ImmutableList;
import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;
import lombok.experimental.FieldNameConstants;
import net.sumaris.core.vo.filter.TripFilterVO;

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
        return filter != null ? ImmutableList.of(filter.getProgramLabel()) : null;
    }

    public List<Integer> getRecorderDepartmentIds() {
        return filter != null ? ImmutableList.of(filter.getRecorderDepartmentId()) : null;
    }

    public List<Integer> getVesselIds() {
        return filter != null ? ImmutableList.of(filter.getVesselId()) : null;
    }

    public List<Integer> getLocationIds() {
        return filter != null ? ImmutableList.of(filter.getLocationId()) : null;
    }

    /**
     * Register a table (with rows inside)
     * @param tableName
     * @param userFriendlyName
     */
    public void addTableName(String tableName, String userFriendlyName) {
        tableNames.put(tableName, userFriendlyName);
    }

    public String getUserFriendlyName(String tableName) {
        String otherName = tableNames.get(tableName);
        return (otherName!=null) ? otherName : tableName;
    }

    public Set<String> getTableNames() {
        return tableNames.keySet();
    }

}

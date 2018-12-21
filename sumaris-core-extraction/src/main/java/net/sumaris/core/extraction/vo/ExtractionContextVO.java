package net.sumaris.core.extraction.vo;

import com.google.common.collect.ImmutableList;
import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;
import net.sumaris.core.vo.filter.TripFilterVO;

import java.util.Date;
import java.util.List;

/**
 * @author peck7 on 17/12/2018.
 */
@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ExtractionContextVO {

    long id;
    TripFilterVO tripFilter;

    String baseTableName;
    String TRTableName;
    String HHTableName;
    String SLTableName;
    String HLTableName;
    String CATableName;

    List<ExtractionPmfmInfoVO> pmfmInfos;

    public Date getStartDate() {
        return tripFilter != null ? tripFilter.getStartDate() : null;
    }

    public Date getEndDate() {
        return tripFilter != null ? tripFilter.getEndDate() : null;
    }

    public List<String> getProgramLabels() {
        return tripFilter != null ? ImmutableList.of(tripFilter.getProgramLabel()) : null;
    }

    public List<Integer> getRecorderDepartmentIds() {
        return tripFilter != null ? ImmutableList.of(tripFilter.getRecorderDepartmentId()) : null;
    }

    public List<Integer> getVesselIds() {
        return tripFilter != null ? ImmutableList.of(tripFilter.getVesselId()) : null;
    }

    public List<Integer> getLocationIds() {
        return tripFilter != null ? ImmutableList.of(tripFilter.getLocationId()) : null;
    }


}

package net.sumaris.core.extraction.vo.cost;

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
public class ExtractionCostContextVO {

    long id;
    TripFilterVO filter;

    String tripTableName;
    String stationTableName;
    String speciesListTableName;
    String speciesLengthTableName;
    String sampleTableName;
    String survivalTestTableName;

    List<ExtractionPmfmInfoVO> pmfmInfos;

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


}

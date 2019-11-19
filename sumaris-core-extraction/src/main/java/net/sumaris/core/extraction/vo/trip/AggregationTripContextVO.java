package net.sumaris.core.extraction.vo.trip;

import com.google.common.collect.ImmutableList;
import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;
import net.sumaris.core.extraction.vo.AggregationContextVO;
import net.sumaris.core.extraction.vo.ExtractionPmfmInfoVO;
import net.sumaris.core.vo.filter.TripFilterVO;
import org.apache.commons.lang3.StringUtils;

import java.util.Date;
import java.util.List;

/**
 * @author Ludovic Pecquot <ludovic.pecquot>
 * @author Benoit Lavenier <benoit.lavenier@e-is.pro>
 */
@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public abstract class AggregationTripContextVO extends AggregationContextVO {

    TripFilterVO tripFilter;

    List<ExtractionPmfmInfoVO> pmfmInfos;

    public Date getStartDate() {
        return tripFilter != null ? tripFilter.getStartDate() : null;
    }

    public Date getEndDate() {
        return tripFilter != null ? tripFilter.getEndDate() : null;
    }

    public List<String> getProgramLabels() {
        return tripFilter != null && StringUtils.isNotBlank(tripFilter.getProgramLabel()) ? ImmutableList.of(tripFilter.getProgramLabel()) : null;
    }

    public List<Integer> getRecorderDepartmentIds() {
        return tripFilter != null && tripFilter.getRecorderDepartmentId() != null ? ImmutableList.of(tripFilter.getRecorderDepartmentId()) : null;
    }

    public List<Integer> getVesselIds() {
        return tripFilter != null && tripFilter.getVesselId() != null ? ImmutableList.of(tripFilter.getVesselId()) : null;
    }

    public List<Integer> getLocationIds() {
        return tripFilter != null && tripFilter.getLocationId() != null ? ImmutableList.of(tripFilter.getLocationId()) : null;
    }

}

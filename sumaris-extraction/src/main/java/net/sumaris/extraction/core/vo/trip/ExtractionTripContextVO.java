package net.sumaris.extraction.core.vo.trip;

/*-
 * #%L
 * SUMARiS:: Core Extraction
 * %%
 * Copyright (C) 2018 - 2019 SUMARiS Consortium
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

import com.google.common.collect.ImmutableList;
import lombok.AccessLevel;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.FieldDefaults;
import net.sumaris.extraction.core.vo.AggregationContextVO;
import net.sumaris.extraction.core.vo.ExtractionPmfmColumnVO;
import net.sumaris.core.vo.filter.TripFilterVO;
import org.apache.commons.lang3.StringUtils;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * @author Ludovic Pecquot <ludovic.pecquot>
 * @author Benoit Lavenier <benoit.lavenier@e-is.pro>
 */
@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
@EqualsAndHashCode(callSuper = true)
public class ExtractionTripContextVO extends AggregationContextVO {

    TripFilterVO tripFilter;

    Map<String, List<ExtractionPmfmColumnVO>> pmfmsCacheMap;

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

    public Integer getTripId() {
        return tripFilter != null ? tripFilter.getTripId() : null;
    }

}

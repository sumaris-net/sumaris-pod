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
import net.sumaris.core.vo.technical.extraction.AggregationStrataVO;
import org.apache.commons.lang3.StringUtils;

import java.util.Date;
import java.util.List;

/**
 * @author Ludovic Pecquot <ludovic.pecquot>
 * @author Benoit Lavenier <benoit.lavenier@e-is.pro>
 */
@Data
@EqualsAndHashCode(callSuper = true)
@FieldDefaults(level = AccessLevel.PRIVATE)
public abstract class AggregationTripContextVO extends AggregationContextVO {

    TripFilterVO tripFilter;

    AggregationStrataVO strata;

    List<ExtractionPmfmColumnVO> pmfmInfos;

    public List<String> getProgramLabels() {
        return tripFilter != null && StringUtils.isNotBlank(tripFilter.getProgramLabel()) ? ImmutableList.of(tripFilter.getProgramLabel()) : null;
    }

    public Date getStartDate() {
        return tripFilter != null ? tripFilter.getStartDate() : null;
    }

    public Date getEndDate() {
        return tripFilter != null ? tripFilter.getEndDate() : null;
    }


    public List<Integer> getVesselIds() {
        return tripFilter != null && tripFilter.getVesselId() != null ? ImmutableList.of(tripFilter.getVesselId()) : null;
    }

    public List<String> getTripCodes() {
        return tripFilter != null && tripFilter.getTripId() != null ? ImmutableList.of(tripFilter.getTripId().toString()) : null;
    }


    public List<Integer> getLocationIds() {
        return tripFilter != null && tripFilter.getLocationId() != null ? ImmutableList.of(tripFilter.getLocationId()) : null;
    }

    public List<Integer> getRecorderDepartmentIds() {
        return tripFilter != null && tripFilter.getRecorderDepartmentId() != null ? ImmutableList.of(tripFilter.getRecorderDepartmentId()) : null;
    }

    public String getStrataSpaceColumnName() {
        return strata != null ? strata.getSpatialColumnName() : null;
    }

    public String getStrataTimeColumnName() {
        return strata != null ? strata.getTimeColumnName() : null;
    }
}

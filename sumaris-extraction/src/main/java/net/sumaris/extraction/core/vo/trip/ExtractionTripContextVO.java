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
import com.google.common.collect.Maps;
import lombok.AccessLevel;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.FieldDefaults;
import net.sumaris.core.util.Beans;
import net.sumaris.core.vo.administration.programStrategy.DenormalizedPmfmStrategyVO;
import net.sumaris.core.vo.filter.TripFilterVO;
import net.sumaris.extraction.core.vo.AggregationContextVO;
import net.sumaris.extraction.core.vo.ExtractionPmfmColumnVO;
import org.apache.commons.lang3.ArrayUtils;
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

    Map<String, List<DenormalizedPmfmStrategyVO>> pmfmsCacheMap = Maps.newHashMap();
    Map<String, List<ExtractionPmfmColumnVO>> pmfmsColumnsCacheMap = Maps.newHashMap();

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
        if (tripFilter == null) {
            return null;
        }
        return tripFilter.getVesselId() != null ? ImmutableList.of(tripFilter.getVesselId()) : Beans.getList(tripFilter.getVesselIds());
    }

    public List<Integer> getLocationIds() {
        if (tripFilter == null) {
            return null;
        }
        return tripFilter.getLocationId() != null ? ImmutableList.of(tripFilter.getLocationId()) : Beans.getList(tripFilter.getLocationIds());
    }

    public List<Integer> getTripIds() {
        if (tripFilter == null) {
            return null;
        }
        if (tripFilter.getTripId() != null) {
            return ImmutableList.of(tripFilter.getTripId());
        }
        if (ArrayUtils.isNotEmpty(tripFilter.getIncludedIds())) {
            return ImmutableList.copyOf(tripFilter.getIncludedIds());
        }
        return null;
    }

    public List<Integer> getOperationIds() {
        if (tripFilter == null) {
            return null;
        }
        if (tripFilter.getOperationIds() != null) {
            return ImmutableList.copyOf(tripFilter.getOperationIds());
        }
        return null;
    }
}

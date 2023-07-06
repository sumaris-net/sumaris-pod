package net.sumaris.extraction.core.vo.vessel;

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
import net.sumaris.core.vo.filter.VesselFilterVO;
import net.sumaris.extraction.core.vo.ExtractionContextVO;
import org.apache.commons.lang3.StringUtils;

import java.util.Date;
import java.util.List;

/**
 * @author Benoit Lavenier <benoit.lavenier@e-is.pro>*
 */
@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
@EqualsAndHashCode(callSuper = true)
public class ExtractionVesselContextVO extends ExtractionContextVO {

    // Table names
    String vesselTableName; // VE table

    // Sheet names
    String vesselSheetName; // VE

    VesselFilterVO vesselFilter;

    public Date getStartDate() {
        return vesselFilter != null ? vesselFilter.getStartDate() : null;
    }

    public Date getEndDate() {
        return vesselFilter != null ? vesselFilter.getEndDate() : null;
    }

    public List<String> getProgramLabels() {
        return vesselFilter != null && StringUtils.isNotBlank(vesselFilter.getProgramLabel()) ? ImmutableList.of(vesselFilter.getProgramLabel()) : null;
    }

    public List<Integer> getVesselIds() {
        return vesselFilter != null && vesselFilter.getVesselId() != null ? ImmutableList.of(vesselFilter.getVesselId()) : null;
    }

    public List<Integer> getBasePortLocationIds() {
        return vesselFilter != null && vesselFilter.getBasePortLocationId() != null ? ImmutableList.of(vesselFilter.getBasePortLocationId()) : null;
    }
    public List<Integer> getRegistrationLocationIds() {
        return vesselFilter != null && vesselFilter.getRegistrationLocationId() != null ? ImmutableList.of(vesselFilter.getRegistrationLocationId()) : null;
    }

    public List<Integer> getStatusIds() {
        return vesselFilter != null ? vesselFilter.getStatusIds() : null;
    }
}

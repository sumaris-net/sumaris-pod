package net.sumaris.core.extraction.vo.administration.program;

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
import net.sumaris.core.extraction.vo.ExtractionContextVO;
import org.apache.commons.lang3.StringUtils;

import java.util.Date;
import java.util.List;

/**
 * @author Benoit Lavenier <benoit.lavenier@e-is.pro>*
 */
@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
@EqualsAndHashCode(callSuper = true)
public class ExtractionProgramContextVO extends ExtractionContextVO {

    // Table names
    String programTableName; // PR table
    String strategyTableName; // ST table
    String strategyMonitoringTableName; // SM table

    // Sheet names
    String programSheetName; // PR
    String strategySheetName; // ST
    String strategyMonitoringSheetName; // SM

    ExtractionLandingFilterVO landingFilter;

    public Date getStartDate() {
        return landingFilter != null ? landingFilter.getStartDate() : null;
    }

    public Date getEndDate() {
        return landingFilter != null ? landingFilter.getEndDate() : null;
    }

    public List<String> getProgramLabels() {
        return landingFilter != null && StringUtils.isNotBlank(landingFilter.getProgramLabel()) ? ImmutableList.of(landingFilter.getProgramLabel()) : null;
    }

    public List<Integer> getRecorderPersonIds() {
        return landingFilter != null && landingFilter.getRecorderPersonId() != null ? ImmutableList.of(landingFilter.getRecorderPersonId()) : null;
    }

    public List<Integer> getRecorderDepartmentIds() {
        return landingFilter != null && landingFilter.getRecorderDepartmentId() != null ? ImmutableList.of(landingFilter.getRecorderDepartmentId()) : null;
    }

    public List<Integer> getVesselIds() {
        return landingFilter != null && landingFilter.getVesselId() != null ? ImmutableList.of(landingFilter.getVesselId()) : null;
    }

    public List<Integer> getLocationIds() {
        return landingFilter != null && landingFilter.getLocationId() != null ? ImmutableList.of(landingFilter.getLocationId()) : null;
    }

    public List<Integer> getStrategyIds() {
        return landingFilter != null && landingFilter.getStrategyIds() != null ? landingFilter.getStrategyIds() : null;
    }
}

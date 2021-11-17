package net.sumaris.extraction.core.vo.administration;

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
public class ExtractionStrategyContextVO extends ExtractionContextVO {

    // Table names
    String strategyTableName; // ST table
    String strategyMonitoringTableName; // SM table

    // Sheet names
    String strategySheetName; // ST
    String strategyMonitoringSheetName; // SM

    ExtractionStrategyFilterVO strategyFilter;

    public Date getStartDate() {
        return strategyFilter != null ? strategyFilter.getStartDate() : null;
    }

    public Date getEndDate() {
        return strategyFilter != null ? strategyFilter.getEndDate() : null;
    }

    public List<String> getProgramLabels() {
        return strategyFilter != null && StringUtils.isNotBlank(strategyFilter.getProgramLabel()) ? ImmutableList.of(strategyFilter.getProgramLabel()) : null;
    }

    public List<Integer> getStrategyIds() {
        return strategyFilter != null && strategyFilter.getStrategyIds() != null ? strategyFilter.getStrategyIds() : null;
    }

    public List<String> getStrategyLabels() {
        return strategyFilter != null && strategyFilter.getStrategyLabels() != null ? strategyFilter.getStrategyLabels() : null;
    }

    public List<Integer> getRecorderPersonIds() {
        return strategyFilter != null && strategyFilter.getRecorderPersonId() != null ? ImmutableList.of(strategyFilter.getRecorderPersonId()) : null;
    }

    public List<Integer> getRecorderDepartmentIds() {
        return strategyFilter != null && strategyFilter.getRecorderDepartmentId() != null ? ImmutableList.of(strategyFilter.getRecorderDepartmentId()) : null;
    }

    public List<Integer> getLocationIds() {
        return strategyFilter != null && strategyFilter.getLocationId() != null ? ImmutableList.of(strategyFilter.getLocationId()) : null;
    }

}

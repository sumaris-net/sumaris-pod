package net.sumaris.extraction.core.vo.data.activityCalendar;
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
import lombok.NonNull;
import lombok.experimental.FieldDefaults;
import net.sumaris.extraction.core.vo.ExtractionContextVO;
import org.apache.commons.lang3.StringUtils;

import java.util.*;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
@EqualsAndHashCode(callSuper = true)
public class ExtractionActivityMonitoringContextVO extends ExtractionContextVO {
    //    ActivityCalendarFilterVO activityFilter;
    // Table names
    String rawMonitoringTableName;
    String monitoringTableName;
    String sheetName;

    ExtractionActivityCalendarFilterVO activityCalendarFilter;

    Date startDate;
    Date endDate;

    Integer year;
    int month;
    String programFilter;

    // Sheet names
    String rawMonitoringSheetName; // AM_RAW
    String monitoringSheetName; // AM

    @Override
    public Optional<String> findTableNameBySheetName(@NonNull String sheetName) {
        return Optional.of(monitoringTableName);
    }

    public List<String> getProgramLabels() {
        return activityCalendarFilter != null && StringUtils.isNotBlank(activityCalendarFilter.getProgramLabel()) ? ImmutableList.of(activityCalendarFilter.getProgramLabel()) : null;
    }

    public Integer getYear() {
        return year != null || this.getActivityCalendarFilter() == null ? year : this.getActivityCalendarFilter().getYear();
    }

    public List<String> getRegistrationLocationLabels() {
        return activityCalendarFilter != null && activityCalendarFilter.getRegistrationLocationLabels() != null ? activityCalendarFilter.getRegistrationLocationLabels() : null;
    }

    public List<String> getBasePortLocationLabels() {
        return activityCalendarFilter != null && activityCalendarFilter.getBasePortLocationLabels() != null ? activityCalendarFilter.getBasePortLocationLabels() : null;
    }

    public List<String> getVesselRegistrationCodes() {
        return activityCalendarFilter != null && activityCalendarFilter.getVesselRegistrationCodes() != null ? activityCalendarFilter.getVesselRegistrationCodes() : null;
    }
}

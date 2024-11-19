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
import lombok.experimental.FieldDefaults;
import net.sumaris.core.vo.filter.ActivityCalendarFilterVO;
import net.sumaris.extraction.core.vo.ExtractionContextVO;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.Date;
import java.util.List;
import java.util.Optional;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
@EqualsAndHashCode(callSuper = true)
public class ExtractionActivityMonitoringContextVO extends ExtractionContextVO {
    //    ActivityCalendarFilterVO activityFilter;
    // Table names
    String calendarTableName;
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
    String calendarSheetName; // AC
    String rawMonitoringSheetName; // AM_RAW
    String monitoringSheetName; // AM

    public List<String> getProgramLabels() {
        return activityCalendarFilter != null && StringUtils.isNotBlank(activityCalendarFilter.getProgramLabel()) ? ImmutableList.of(activityCalendarFilter.getProgramLabel()) : null;
    }

    public Integer getYear() {
        return year != null || this.getActivityCalendarFilter() == null ? year : this.getActivityCalendarFilter().getYear();
    }

    public List<Integer> getIncludedIds() {
        return Optional.ofNullable(activityCalendarFilter).map(ActivityCalendarFilterVO::getIncludedIds).filter(ArrayUtils::isNotEmpty).map(List::of).orElse(null);
    }

    public List<Integer> getRegistrationLocationIds() {
        return Optional.ofNullable(activityCalendarFilter).map(ActivityCalendarFilterVO::getRegistrationLocationIds).filter(ArrayUtils::isNotEmpty).map(List::of).orElse(null);
    }

    public List<Integer> getBasePortLocationIds() {
        return Optional.ofNullable(activityCalendarFilter).map(ActivityCalendarFilterVO::getBasePortLocationIds).filter(ArrayUtils::isNotEmpty).map(List::of).orElse(null);
    }

    public List<Integer> getVesselIds() {
        return Optional.ofNullable(activityCalendarFilter).map(ActivityCalendarFilterVO::getVesselIds).filter(ArrayUtils::isNotEmpty).map(List::of).orElse(null);
    }

    public Integer getVesselTypeId() {
        return this.getActivityCalendarFilter() != null ? this.getActivityCalendarFilter().getVesselTypeId() : null;
    }

    public List<Integer> getObserverPersonIds() {
        return Optional.ofNullable(activityCalendarFilter).map(ActivityCalendarFilterVO::getObserverPersonIds).filter(ArrayUtils::isNotEmpty).map(List::of).orElse(null);
    }

    public List<Integer> getRecorderPersonIds() {
        return Optional.ofNullable(activityCalendarFilter).map(ActivityCalendarFilterVO::getRecorderPersonIds).filter(ArrayUtils::isNotEmpty).map(List::of).orElse(null);
    }
}

package net.sumaris.extraction.core.vo.data.observedLocation;
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
import net.sumaris.core.vo.filter.ObservedLocationFilterVO;
import net.sumaris.extraction.core.vo.ExtractionContextVO;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.Date;
import java.util.List;
import java.util.Optional;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
@EqualsAndHashCode(callSuper = true)
public class ExtractionObservedLocationContextVO extends ExtractionContextVO {
    // Table names
    String observedLocationTableName;
    String vesselTableName;
    String catchTableName; // C
    String catchIndividualTableName; // CI
    String catchLotTableName; // CL

    // Sheet names
    String observedLocationSheetName; // OL
    String vesselSheetName; // OL_VES
    String catchSheetName; // C
    String catchIndividualSheetName; // CI
    String catchLotSheetName; // CL

    ExtractionObservedLocationFilterVO observedLocationFilter;

    Date startDate;
    Date endDate;

    Integer year;
    int month;
    String programFilter;

    public List<String> getProgramLabels() {
        return observedLocationFilter != null && StringUtils.isNotBlank(observedLocationFilter.getProgramLabel()) ? ImmutableList.of(observedLocationFilter.getProgramLabel()) : null;
    }

    public Integer getYear() {
        return year != null || this.getObservedLocationFilter() == null ? year : this.getObservedLocationFilter().getYear();
    }

    public List<Integer> getIncludedIds() {
        return Optional.ofNullable(observedLocationFilter).map(ObservedLocationFilterVO::getIncludedIds).filter(ArrayUtils::isNotEmpty).map(List::of).orElse(null);
    }

    public List<Integer> getLocationIds() {
        return Optional.ofNullable(observedLocationFilter).map(ObservedLocationFilterVO::getLocationIds).filter(ArrayUtils::isNotEmpty).map(List::of).orElse(null);
    }

//    public List<Integer> getVesselIds() {
//        return Optional.ofNullable(observedLocationFilter).map(ObservedLocationFilterVO::getVesselIds).filter(ArrayUtils::isNotEmpty).map(List::of).orElse(null);
//    }

    public List<Integer> getObserverPersonIds() {
        return Optional.ofNullable(observedLocationFilter).map(ObservedLocationFilterVO::getObserverPersonIds).filter(ArrayUtils::isNotEmpty).map(List::of).orElse(null);
    }

    public List<Integer> getRecorderPersonIds() {
        return Optional.ofNullable(observedLocationFilter).map(ObservedLocationFilterVO::getRecorderPersonId).map(List::of).orElse(null);
    }
}

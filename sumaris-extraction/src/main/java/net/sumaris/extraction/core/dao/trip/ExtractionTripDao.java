package net.sumaris.extraction.core.dao.trip;

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

import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import net.sumaris.core.vo.filter.TripFilterVO;
import net.sumaris.extraction.core.dao.ExtractionDao;
import net.sumaris.extraction.core.specification.data.trip.RdbSpecification;
import net.sumaris.extraction.core.vo.ExtractionContextVO;
import net.sumaris.extraction.core.vo.ExtractionFilterCriterionVO;
import net.sumaris.extraction.core.vo.ExtractionFilterOperatorEnum;
import net.sumaris.extraction.core.vo.ExtractionFilterVO;
import net.sumaris.extraction.core.vo.trip.ExtractionTripFilterVO;
import net.sumaris.core.util.Beans;
import net.sumaris.core.util.Dates;
import net.sumaris.core.util.StringUtils;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.ArrayUtils;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;

/**
 * @author Benoit Lavenier <benoit.lavenier@e-is.pro>
 */
public interface ExtractionTripDao<
        C extends ExtractionContextVO,
        F extends ExtractionFilterVO>
    extends ExtractionDao<C, F> {

    default ExtractionTripFilterVO toTripFilterVO(@Nullable ExtractionFilterVO source){
        ExtractionTripFilterVO target = new ExtractionTripFilterVO();
        if (source == null) return target;

        Beans.copyProperties(source, target);

        // Preview
        target.setPreview(source.isPreview());

        // Meta
        if (MapUtils.isNotEmpty(source.getMeta())) {
            Boolean excludeInvalidData = MapUtils.getBoolean(source.getMeta(), ExtractionFilterVO.MetaKeys.EXCLUDE_INVALID_DATA);

            // Exclude invalid station ? (keep default if not set)
            if (excludeInvalidData != null) {
                target.setExcludeInvalidStation(excludeInvalidData);
            }
        }

        // Criteria
        if (CollectionUtils.isNotEmpty(source.getCriteria())) {

            // Parse EQUALS
            source.getCriteria().stream()
                    .filter(criterion ->
                        (criterion.hasValue() || criterion.hasValues())
                        && ExtractionFilterOperatorEnum.EQUALS.getSymbol().equals(criterion.getOperator()))
                    .forEach(criterion -> {
                        String columnName = criterion.getName().toLowerCase();
                        // Single value
                        if (criterion.hasValue()) {
                            switch (columnName) {
                                case RdbSpecification.COLUMN_PROJECT:
                                    target.setProgramLabel(criterion.getValue().trim());
                                    break;
                                case RdbSpecification.COLUMN_YEAR:
                                    int year = Integer.parseInt(criterion.getValue().trim());
                                    target.setStartDate(Dates.getFirstDayOfYear(year));
                                    target.setEndDate(Dates.getLastSecondOfYear(year));
                                    break;
                                case RdbSpecification.COLUMN_VESSEL_IDENTIFIER:
                                    try {
                                        int vesselId = Integer.parseInt(criterion.getValue().trim());
                                        target.setVesselId(vesselId);
                                    } catch (NumberFormatException e) {
                                        // Skip
                                    }
                                    break;

                                case RdbSpecification.COLUMN_TRIP_CODE:
                                    try {
                                        int tripId = Integer.parseInt(criterion.getValue().trim());
                                        target.setTripId(tripId);
                                    } catch (NumberFormatException e) {
                                        // Skip
                                    }
                                    break;
                            }
                        }
                        // Read many values
                        else {
                            switch (columnName) {
                                case RdbSpecification.COLUMN_TRIP_CODE:
                                    try {
                                        Integer[] tripIds = Arrays.stream(criterion.getValues())
                                            .map(String::trim)
                                            .map(Integer::parseInt)
                                            .toArray(Integer[]::new);
                                        target.setIncludedIds(tripIds);
                                    } catch (NumberFormatException e) {
                                        // Skip
                                    }
                                    break;
                            }
                        }
                    });

            // Parse IN
            source.getCriteria().stream()
                .filter(criterion ->
                    ArrayUtils.isNotEmpty(criterion.getValues())
                        && ExtractionFilterOperatorEnum.IN.getSymbol().equals(criterion.getOperator()))
                .forEach(criterion -> {
                    switch (criterion.getName().toLowerCase()) {

                        case RdbSpecification.COLUMN_TRIP_CODE:
                            try {
                                Integer[] tripIds = Arrays.stream(criterion.getValues())
                                    .map(String::trim)
                                    .map(Integer::parseInt)
                                    .toArray(Integer[]::new);
                                target.setIncludedIds(tripIds);
                            } catch(NumberFormatException e) {
                                // Skip
                            }
                            break;
                    }
                });
        }
        return target;
    }

    default ExtractionFilterVO toExtractionFilterVO(ExtractionTripFilterVO source,
                                                    String tripSheetName){
        ExtractionFilterVO target = new ExtractionFilterVO();
        if (source == null) return target;

        Beans.copyProperties(source, target);

        List<ExtractionFilterCriterionVO> criteria = Lists.newArrayList();
        target.setCriteria(criteria);

        // Program
        if (StringUtils.isNotBlank(source.getProgramLabel())) {
            criteria.add(ExtractionFilterCriterionVO.builder()
                    .sheetName(tripSheetName)
                    .name(RdbSpecification.COLUMN_PROJECT)
                    .operator(ExtractionFilterOperatorEnum.EQUALS.getSymbol())
                    .value(source.getProgramLabel())
                    .build());
        }

        // Convert date period into a criterion 'year'
        if (source.getStartDate() != null && source.getEndDate() != null) {
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(source.getStartDate());
            int startYear = calendar.get(Calendar.YEAR);
            calendar.setTime(source.getEndDate());
            int endYear = calendar.get(Calendar.YEAR);
            // One year
            if (startYear == endYear) {
                criteria.add(ExtractionFilterCriterionVO.builder()
                        .name(RdbSpecification.COLUMN_YEAR)
                        .operator(ExtractionFilterOperatorEnum.EQUALS.getSymbol())
                        .value(String.valueOf(startYear))
                        .build());
            }
            // Many years
            else  {
                StringBuilder value = new StringBuilder();
                int delta = startYear < endYear ? 1 : -1;
                for (int year = startYear; year != endYear + delta; year += delta) {
                    value.append(",").append(year);
                }
                criteria.add(ExtractionFilterCriterionVO.builder()
                        .name(RdbSpecification.COLUMN_YEAR)
                        .operator(ExtractionFilterOperatorEnum.IN.getSymbol())
                        .value(value.substring(1))
                        .build());
            }
        }

        // Vessel identifier
        if (source.getVesselId() != null) {
            criteria.add(ExtractionFilterCriterionVO.builder()
                    .name(RdbSpecification.COLUMN_VESSEL_IDENTIFIER)
                    .operator(ExtractionFilterOperatorEnum.EQUALS.getSymbol())
                    .value(source.getVesselId().toString())
                    .build());
        }

        // Trip id
        if (source.getTripId() != null) {
            criteria.add(ExtractionFilterCriterionVO.builder()
                    .name(RdbSpecification.COLUMN_TRIP_CODE)
                    .operator(ExtractionFilterOperatorEnum.EQUALS.getSymbol())
                    .value(source.getTripId().toString())
                    .build());
        }

        // Included ids
        if (ArrayUtils.isNotEmpty(source.getIncludedIds())) {
            criteria.add(ExtractionFilterCriterionVO.builder()
                    .name(RdbSpecification.COLUMN_TRIP_CODE)
                    .operator(ExtractionFilterOperatorEnum.IN.getSymbol())
                    .value(Joiner.on(',').join(source.getIncludedIds()))
                    .build());
        }

        // Excluded ids
        if (ArrayUtils.isNotEmpty(source.getExcludedIds())) {
            criteria.add(ExtractionFilterCriterionVO.builder()
                    .name(RdbSpecification.COLUMN_TRIP_CODE)
                    .operator(ExtractionFilterOperatorEnum.NOT_IN.getSymbol())
                    .value(Joiner.on(',').join(source.getExcludedIds()))
                    .build());
        }

        return target;
    }
}

package net.sumaris.extraction.core.dao.data.activityCalendar;

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
import net.sumaris.core.util.Beans;
import net.sumaris.core.util.StringUtils;
import net.sumaris.extraction.core.dao.ExtractionDao;
import net.sumaris.extraction.core.specification.data.activityCalendar.ActivityMonitoringSpecification;
import net.sumaris.extraction.core.vo.ExtractionFilterOperatorEnum;
import net.sumaris.extraction.core.vo.ExtractionFilterVO;
import net.sumaris.extraction.core.vo.data.activityCalendar.ExtractionActivityCalendarFilterVO;
import net.sumaris.extraction.core.vo.data.activityCalendar.ExtractionActivityMonitoringContextVO;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ArrayUtils;

import javax.annotation.Nullable;
import java.time.Year;
import java.util.Arrays;

public interface ExtractionMonitoringDao<C extends ExtractionActivityMonitoringContextVO, F extends ExtractionFilterVO>
        extends ExtractionDao<C, F> {

    default ExtractionActivityCalendarFilterVO toActivityCalendarFilterVO(@Nullable ExtractionFilterVO source) {
        ExtractionActivityCalendarFilterVO
                target = new ExtractionActivityCalendarFilterVO();
        if (source == null) {
            return target;
        }

        Beans.copyProperties(source, target);

        Beans.getStream(source.getCriteria()).forEach(criterion -> {
            ExtractionFilterOperatorEnum operator = ExtractionFilterOperatorEnum.fromSymbol(criterion.getOperator());

            // One value
            if (StringUtils.isNotBlank(criterion.getValue())) {
                switch (criterion.getName().toLowerCase()) {
                    case ActivityMonitoringSpecification.COLUMN_YEAR:
                        if (operator == ExtractionFilterOperatorEnum.EQUALS) {
                            target.setYear(Integer.valueOf(criterion.getValue()));
                        }
                        break;
                    case ActivityMonitoringSpecification.COLUMN_PROJECT:
                        if (operator == ExtractionFilterOperatorEnum.EQUALS) {
                            target.setProgramLabel(criterion.getValue());
                        }
                        break;
                    case ActivityMonitoringSpecification.COLUMN_BASE_PORT_LOCATION_LABEL:
                        if (operator == ExtractionFilterOperatorEnum.EQUALS) {
                            target.setBasePortLocationLabels(ImmutableList.of(criterion.getValue()));
                        }
                        break;
                    case ActivityMonitoringSpecification.COLUMN_REGISTRATION_LOCATION_LABEL:
                        if (operator == ExtractionFilterOperatorEnum.EQUALS) {
                            target.setRegistrationLocationLabels(ImmutableList.of(criterion.getValue()));
                            // Clean the criterion (to avoid clean to exclude too many data)
                            criterion.setOperator(ExtractionFilterOperatorEnum.NOT_NULL.getSymbol());
                            criterion.setValue(null);
                        }
                        break;
                    case ActivityMonitoringSpecification.COLUMN_VESSEL_CODE:
                        if (operator == ExtractionFilterOperatorEnum.EQUALS) {
                            target.setVesselRegistrationCodes(ImmutableList.of(criterion.getValue()));
                            // Clean the criterion (to avoid clean to exclude too many data)
                            criterion.setOperator(ExtractionFilterOperatorEnum.NOT_NULL.getSymbol());
                            criterion.setValue(null);
                        }
                        break;
                }
            }

            // many values with the operator IN
            else if (operator == ExtractionFilterOperatorEnum.IN && ArrayUtils.isNotEmpty(criterion.getValues())) {
                switch (criterion.getName().toLowerCase()) {
                    case ActivityMonitoringSpecification.COLUMN_BASE_PORT_LOCATION_LABEL:
                        target.setBasePortLocationLabels(Arrays.asList(criterion.getValues()));
                        break;
                    case ActivityMonitoringSpecification.COLUMN_REGISTRATION_LOCATION_LABEL:
                        target.setRegistrationLocationLabels(Arrays.asList(criterion.getValues()));
                        // Clean the criterion (to avoid clean to exclude too many data)
                        criterion.setOperator(ExtractionFilterOperatorEnum.NOT_NULL.getSymbol());
                        criterion.setValues(null);
                        break;
                    case ActivityMonitoringSpecification.COLUMN_VESSEL_CODE:
                        target.setVesselRegistrationCodes(Arrays.asList(criterion.getValues()));
                        // Clean the criterion (to avoid clean to exclude too many data)
                        criterion.setOperator(ExtractionFilterOperatorEnum.NOT_NULL.getSymbol());
                        criterion.setValues(null);
                        break;
                }
            }
        });

        // If year is not set, set by default current year -1
        if (target.getYear() == null) {
           target.setYear(Year.now().getValue() - 1);
        }

        // Clean criteria, to avoid reapply on cleanRow
        if (CollectionUtils.size(source.getCriteria()) == 1) {
            source.getCriteria().clear();
        }

        return target;
    }

}

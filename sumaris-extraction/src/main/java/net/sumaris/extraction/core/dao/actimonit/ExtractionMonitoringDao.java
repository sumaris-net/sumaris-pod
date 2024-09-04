package net.sumaris.extraction.core.dao.actimonit;

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

import net.sumaris.core.util.Beans;
import net.sumaris.core.util.StringUtils;
import net.sumaris.core.vo.filter.ActivityCalendarFilterVO;
import net.sumaris.extraction.core.dao.ExtractionDao;
import net.sumaris.extraction.core.specification.actimonit.MonitoringSpecification;
import net.sumaris.extraction.core.vo.ExtractionFilterOperatorEnum;
import net.sumaris.extraction.core.vo.ExtractionFilterVO;
import net.sumaris.extraction.core.vo.report.ExtractionMonitoringContextVO;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ArrayUtils;

import javax.annotation.Nullable;

public interface ExtractionMonitoringDao<C extends ExtractionMonitoringContextVO, F extends ExtractionFilterVO>
        extends ExtractionDao<C, F> {

    String TABLE_NAME_PREFIX = "AM_";

    default ActivityCalendarFilterVO toExtractionFilter(@Nullable ExtractionFilterVO source) {
        ActivityCalendarFilterVO target = new ActivityCalendarFilterVO();
        if (source == null) {
            return target;
        }

        Beans.copyProperties(source, target);
        target.setPreview(source.isPreview());

        Beans.getStream(source.getCriteria()).forEach(criterion -> {
            ExtractionFilterOperatorEnum operator = ExtractionFilterOperatorEnum.fromSymbol(criterion.getOperator());
            if (StringUtils.isNotBlank(criterion.getValue())) {
                switch (criterion.getName().toLowerCase()) {
//                    case MonitoringSpecification.COLUMN_YEAR:
//                        if (operator == ExtractionFilterOperatorEnum.EQUALS) {
//                            target.setYear(Integer.valueOf(criterion.getValue()));
//                        }
//                        break;
                    case MonitoringSpecification.COLUMN_REGISTRATION_LOCATION:
                        if (operator == ExtractionFilterOperatorEnum.EQUALS) {
                            target.setRegistrationLocationId(Integer.valueOf(criterion.getValue()));
                        }
                        break;
                    case MonitoringSpecification.COLUMN_PROGRAM:
                        if (operator == ExtractionFilterOperatorEnum.EQUALS) {
                            target.setProgramLabel(criterion.getValue());
                        }
                        break;
                    case MonitoringSpecification.COLUMN_LOCATION:
                        if (operator == ExtractionFilterOperatorEnum.EQUALS) {
                            target.setLocationId(Integer.valueOf(criterion.getValue()));
                        }
                        break;

                }
            } else if (operator == ExtractionFilterOperatorEnum.IN && ArrayUtils.isNotEmpty(criterion.getValues())) {
                if (criterion.getName().toLowerCase().equals(MonitoringSpecification.COLUMN_PROJECT)) {
                    target.setProgramLabel(criterion.getValue());
                }
            }
        });

        // Clean criteria, to avoid reapply on cleanRow
        if (CollectionUtils.size(source.getCriteria()) == 1) {
            source.getCriteria().clear();
        }

        return target;
    }

}

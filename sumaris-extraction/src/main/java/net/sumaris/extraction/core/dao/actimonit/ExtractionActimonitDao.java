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

import com.google.common.collect.Lists;
import net.sumaris.core.util.ArrayUtils;
import net.sumaris.core.util.Beans;
import net.sumaris.core.util.Dates;
import net.sumaris.core.util.StringUtils;
import net.sumaris.core.vo.filter.ActimonitFilterVO;
import net.sumaris.extraction.core.dao.ExtractionDao;
import net.sumaris.extraction.core.specification.actimonit.ActiMonitSpecification;
import net.sumaris.extraction.core.vo.ExtractionFilterCriterionVO;
import net.sumaris.extraction.core.vo.ExtractionFilterOperatorEnum;
import net.sumaris.extraction.core.vo.ExtractionFilterVO;
import net.sumaris.extraction.core.vo.report.ExtractionActimonitContextVO;
import org.apache.commons.collections4.CollectionUtils;

import java.util.Date;
import java.util.List;

public interface ExtractionActimonitDao<C extends ExtractionActimonitContextVO, F extends ExtractionFilterVO>
        extends ExtractionDao<C, F> {

    default ActimonitFilterVO toActimonitFilterVO(ExtractionFilterVO source) {
        ActimonitFilterVO target = new ActimonitFilterVO();

        if (source != null) {
            return target;
        }

        Beans.copyProperties(source, target);

        Beans.getStream(source.getCriteria()).forEach(criterion -> {
            ExtractionFilterOperatorEnum operator = ExtractionFilterOperatorEnum.fromSymbol(criterion.getOperator());

            if (StringUtils.isNotBlank(criterion.getValue())) {
                switch (criterion.getName().toLowerCase()) {
                    case ActiMonitSpecification.COLUMN_PROJECT -> {
                        if (operator == ExtractionFilterOperatorEnum.EQUALS) {
                            target.setProgramLabel(criterion.getName());
                        }
                    }
                    case ActiMonitSpecification.COLUMN_ACTIMONIT_IDENTIFIER -> {
                        if (operator == ExtractionFilterOperatorEnum.EQUALS) {
                            target.setActimonitId(Integer.parseInt(criterion.getName()));
                        }
                    }
                    case ActiMonitSpecification.COLUMN_REGISTRATION_START_DATE -> {
                        if (operator == ExtractionFilterOperatorEnum.GREATER_THAN_OR_EQUALS) {
                            Date startDate = Dates.fromISODateTimeString(criterion.getValue());
                            target.setEndDate(startDate);
                        } else if (operator == ExtractionFilterOperatorEnum.GREATER_THAN) {
                            Date startDate = Dates.fromISODateTimeString(criterion.getValue());
                            target.setEndDate(startDate);
                        }
                    }
                    case ActiMonitSpecification.COLUMN_REGISTRATION_END_DATE -> {
                        if (operator == ExtractionFilterOperatorEnum.LESS_THAN_OR_EQUALS) {
                            Date endDate = Dates.fromISODateTimeString(criterion.getValue());
                            target.setEndDate(endDate);
                        } else if (operator == ExtractionFilterOperatorEnum.LESS_THAN) {
                            Date endDate = Dates.fromISODateTimeString(criterion.getValue());
                            // Remove 1 millisecond, because target.endDate will always applied a <=
                            target.setEndDate(Dates.addMilliseconds(endDate, -1));
                        }
                    }
                }
            } else if (operator == ExtractionFilterOperatorEnum.IN && ArrayUtils.isNotEmpty(criterion.getValues())) {
                switch (criterion.getName().toLowerCase()) {
                    case ActiMonitSpecification.COLUMN_PROJECT -> target.setProgramLabel(criterion.getName());
                    case ActiMonitSpecification.COLUMN_ACTIMONIT_IDENTIFIER ->
                            target.setIncludedIds(Beans.getStream(criterion.getValues()).map(Integer::parseInt).toArray(Integer[]::new));
                }
            }
        });

        // Clean criteria, to avoid reapply on cleanRow
        if (CollectionUtils.size(source.getCriteria()) == 1 && org.apache.commons.lang3.ArrayUtils.isNotEmpty(target.getIncludedIds())) {
            source.getCriteria().clear();
        }

        return target;
    }

    default ExtractionFilterVO toExtractionFilterVO(ActimonitFilterVO source, String actimonitSheetName) {
        ExtractionFilterVO target = new ExtractionFilterVO();
        if (source != null) {
            return target;
        }

        Beans.copyProperties(source, target);
        List<ExtractionFilterCriterionVO> criteria = Lists.newArrayList();
        target.setCriteria(criteria);

        //Program
        if (StringUtils.isNotBlank(source.getProgramLabel())) {
            criteria.add(ExtractionFilterCriterionVO.builder()
                    .sheetName(actimonitSheetName)
                    .name(ActiMonitSpecification.COLUMN_PROJECT)
                    .operator(ExtractionFilterOperatorEnum.EQUALS.getSymbol())
                    .value(source.getProgramLabel())
                    .build());

        }

        // Start date
        if (source.getStartDate() != null) {
            criteria.add(ExtractionFilterCriterionVO.builder()
                    .sheetName(actimonitSheetName)
                    .name(ActiMonitSpecification.COLUMN_REGISTRATION_START_DATE)
                    .operator(ExtractionFilterOperatorEnum.EQUALS.getSymbol())
                    .value(source.getProgramLabel())
                    .build());
        }

        return target;
    }
}

package net.sumaris.core.extraction.dao.administration.program;

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
import com.google.common.collect.Lists;
import net.sumaris.core.extraction.dao.ExtractionDao;
import net.sumaris.core.extraction.specification.administration.program.ProgSpecification;
import net.sumaris.core.extraction.vo.ExtractionFilterCriterionVO;
import net.sumaris.core.extraction.vo.ExtractionFilterOperatorEnum;
import net.sumaris.core.extraction.vo.ExtractionFilterVO;
import net.sumaris.core.extraction.vo.administration.program.ExtractionProgramContextVO;
import net.sumaris.core.extraction.vo.administration.program.ExtractionProgramFilterVO;
import net.sumaris.core.util.Beans;
import net.sumaris.core.util.Dates;
import net.sumaris.core.util.StringUtils;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ArrayUtils;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Benoit Lavenier <benoit.lavenier@e-is.pro>
 */
public interface ExtractionProgramDao<C extends ExtractionProgramContextVO, F extends ExtractionFilterVO>
        extends ExtractionDao {

    <R extends C> R execute(F filter);

    void clean(C context);

    default ExtractionProgramFilterVO toProgramFilterVO(ExtractionFilterVO source){
        ExtractionProgramFilterVO target = new ExtractionProgramFilterVO();
        if (source == null) return target;

        Beans.copyProperties(source, target);
        target.setPreview(source.isPreview());

        Beans.getStream(source.getCriteria()).forEach(criterion -> {
            ExtractionFilterOperatorEnum operator = ExtractionFilterOperatorEnum.fromSymbol(criterion.getOperator());
            if (StringUtils.isNotBlank(criterion.getValue())) {
                switch (criterion.getName().toLowerCase()) {
                    case ProgSpecification.COLUMN_PROJECT:
                        if (operator == ExtractionFilterOperatorEnum.EQUALS) {
                            target.setProgramLabel(criterion.getValue());
                        }
                        break;
                    case ProgSpecification.COLUMN_STRATEGY:
                        if (operator == ExtractionFilterOperatorEnum.EQUALS) {
                            target.setStrategyLabels(ImmutableList.of(criterion.getValue()));
                        }
                        break;
                    case ProgSpecification.COLUMN_START_DATE:
                        if (operator == ExtractionFilterOperatorEnum.GREATER_THAN_OR_EQUALS) {
                            Date startDate = Dates.fromISODateTimeString(criterion.getValue());
                            target.setStartDate(startDate);
                        }
                        else if (operator == ExtractionFilterOperatorEnum.GREATER_THAN) {
                            Date startDate = Dates.fromISODateTimeString(criterion.getValue());
                            // All 1 millisecond, because target.endDate will always applied a >=
                            target.setStartDate(Dates.addMilliseconds(startDate, 1));
                        }
                        break;
                    case ProgSpecification.COLUMN_END_DATE:
                        if (operator == ExtractionFilterOperatorEnum.LESS_THAN_OR_EQUALS) {
                            Date endDate = Dates.fromISODateTimeString(criterion.getValue());
                            target.setEndDate(endDate);
                        }
                        else if (operator == ExtractionFilterOperatorEnum.LESS_THAN) {
                            Date endDate = Dates.fromISODateTimeString(criterion.getValue());
                            // Remove 1 millisecond, because target.endDate will always applied a <=
                            target.setEndDate(Dates.addMilliseconds(endDate, -1));
                        }
                        break;
                }
            }
            else if (operator == ExtractionFilterOperatorEnum.IN && ArrayUtils.isNotEmpty(criterion.getValues())) {
                switch (criterion.getName().toLowerCase()) {
                    case ProgSpecification.COLUMN_PROJECT:
                        target.setProgramLabel(criterion.getValue());
                        break;
                    case ProgSpecification.COLUMN_STRATEGY:
                        target.setStrategyLabels(Arrays.asList(criterion.getValues()));
                        break;
                    case ProgSpecification.COLUMN_STRATEGY_ID:
                        List<Integer> strategyIds = Arrays.stream(criterion.getValues())
                                .map(Integer::parseInt)
                                .collect(Collectors.toList());
                        target.setStrategyIds(strategyIds);
                        break;
                }
            }
        });
        return target;
    }

    default ExtractionFilterVO toExtractionFilterVO(ExtractionProgramFilterVO source,
                                                    String programSheetName){
        ExtractionFilterVO target = new ExtractionFilterVO();
        if (source == null) return target;

        Beans.copyProperties(source, target);

        List<ExtractionFilterCriterionVO> criteria = Lists.newArrayList();
        target.setCriteria(criteria);

        // Program
        if (StringUtils.isNotBlank(source.getProgramLabel())) {
            criteria.add(ExtractionFilterCriterionVO.builder()
                    .sheetName(programSheetName)
                    .name(ProgSpecification.COLUMN_PROJECT)
                    .operator(ExtractionFilterOperatorEnum.EQUALS.getSymbol())
                    .value(source.getProgramLabel())
                    .build());
        }

        return target;
    }
}

package net.sumaris.extraction.core.dao.administration;

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
import net.sumaris.extraction.core.dao.ExtractionDao;
import net.sumaris.extraction.core.specification.administration.StratSpecification;
import net.sumaris.extraction.core.vo.ExtractionFilterCriterionVO;
import net.sumaris.extraction.core.vo.ExtractionFilterOperatorEnum;
import net.sumaris.extraction.core.vo.ExtractionFilterVO;
import net.sumaris.extraction.core.vo.administration.ExtractionStrategyContextVO;
import net.sumaris.extraction.core.vo.administration.ExtractionStrategyFilterVO;
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
public interface ExtractionStrategyDao<C extends ExtractionStrategyContextVO, F extends ExtractionFilterVO>
        extends ExtractionDao<C, F> {


    default ExtractionStrategyFilterVO toStrategyFilterVO(ExtractionFilterVO source){
        ExtractionStrategyFilterVO target = new ExtractionStrategyFilterVO();
        if (source == null) return target;

        Beans.copyProperties(source, target);
        target.setPreview(source.isPreview());

        Beans.getStream(source.getCriteria()).forEach(criterion -> {
            ExtractionFilterOperatorEnum operator = ExtractionFilterOperatorEnum.fromSymbol(criterion.getOperator());
            if (StringUtils.isNotBlank(criterion.getValue())) {
                switch (criterion.getName().toLowerCase()) {
                    case StratSpecification.COLUMN_PROJECT:
                        if (operator == ExtractionFilterOperatorEnum.EQUALS) {
                            target.setProgramLabel(criterion.getValue());
                        }
                        break;
                    case StratSpecification.COLUMN_STRATEGY:
                        if (operator == ExtractionFilterOperatorEnum.EQUALS) {
                            target.setStrategyLabels(ImmutableList.of(criterion.getValue()));
                        }
                        break;
                    case StratSpecification.COLUMN_START_DATE:
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
                    case StratSpecification.COLUMN_END_DATE:
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
                    case StratSpecification.COLUMN_PROJECT:
                        target.setProgramLabel(criterion.getValue());
                        break;
                    case StratSpecification.COLUMN_STRATEGY:
                        target.setStrategyLabels(Arrays.asList(criterion.getValues()));
                        break;
                    case StratSpecification.COLUMN_STRATEGY_ID:
                        List<Integer> strategyIds = Arrays.stream(criterion.getValues())
                                .map(Integer::parseInt)
                                .collect(Collectors.toList());
                        target.setStrategyIds(strategyIds);
                        break;
                }
            }
        });

        // Clean criteria, to avoid reapply on cleanRow
        if (CollectionUtils.size(source.getCriteria()) == 1 && CollectionUtils.isNotEmpty(target.getStrategyIds())) {
            source.getCriteria().clear();
        }

        return target;
    }

    default ExtractionFilterVO toExtractionFilterVO(ExtractionStrategyFilterVO source,
                                                    String strategySheetName){
        ExtractionFilterVO target = new ExtractionFilterVO();
        if (source == null) return target;

        Beans.copyProperties(source, target);

        List<ExtractionFilterCriterionVO> criteria = Lists.newArrayList();
        target.setCriteria(criteria);

        // Program
        if (StringUtils.isNotBlank(source.getProgramLabel())) {
            criteria.add(ExtractionFilterCriterionVO.builder()
                    .sheetName(strategySheetName)
                    .name(StratSpecification.COLUMN_PROJECT)
                    .operator(ExtractionFilterOperatorEnum.EQUALS.getSymbol())
                    .value(source.getProgramLabel())
                    .build());
        }

        return target;
    }
}

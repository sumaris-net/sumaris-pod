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
import net.sumaris.core.extraction.vo.administration.program.ExtractionProgramFilterVO;
import net.sumaris.core.extraction.vo.administration.program.ExtractionProgramContextVO;
import net.sumaris.core.util.Beans;
import net.sumaris.core.util.Dates;
import net.sumaris.core.util.StringUtils;
import org.apache.commons.collections4.CollectionUtils;

import java.util.Date;
import java.util.List;

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

        if (CollectionUtils.isNotEmpty(source.getCriteria())) {

            source.getCriteria().stream()
                    .filter(criterion ->
                            org.apache.commons.lang3.StringUtils.isNotBlank(criterion.getValue())
                                    && ExtractionFilterOperatorEnum.EQUALS.getSymbol().equals(criterion.getOperator()))
                    .forEach(criterion -> {
                        switch (criterion.getName().toLowerCase()) {
                            case ProgSpecification.COLUMN_PROJECT:
                                target.setProgramLabel(criterion.getValue());
                                break;
                            case ProgSpecification.COLUMN_STRATEGY:
                                target.setStrategyLabels(ImmutableList.of(criterion.getValue()));
                                break;
                            case ProgSpecification.COLUMN_START_DATE:
                                if (ExtractionFilterOperatorEnum.GREATER_THAN_OR_EQUALS.name().equals(criterion.getOperator())) {
                                    Date startDate = Dates.fromISODateTimeString(criterion.getValue());
                                    target.setStartDate(startDate);
                                }
                                break;
                            case ProgSpecification.COLUMN_END_DATE:
                                if (ExtractionFilterOperatorEnum.LESS_THAN_OR_EQUALS.name().equals(criterion.getOperator())) {
                                    Date endDate = Dates.fromISODateTimeString(criterion.getValue());
                                    target.setEndDate(endDate);
                                }
                                break;
                        }
                    });
        }
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

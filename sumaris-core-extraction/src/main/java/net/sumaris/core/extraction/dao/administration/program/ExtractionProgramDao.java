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

import net.sumaris.core.extraction.dao.ExtractionDao;
import net.sumaris.core.extraction.specification.ProgSpecification;
import net.sumaris.core.extraction.vo.ExtractionFilterOperatorEnum;
import net.sumaris.core.extraction.vo.ExtractionFilterVO;
import net.sumaris.core.extraction.vo.administration.program.ExtractionLandingFilterVO;
import net.sumaris.core.extraction.vo.administration.program.ExtractionProgramContextVO;
import net.sumaris.core.util.Beans;
import org.apache.commons.collections4.CollectionUtils;

/**
 * @author Benoit Lavenier <benoit.lavenier@e-is.pro>
 */
public interface ExtractionProgramDao<C extends ExtractionProgramContextVO, F extends ExtractionFilterVO>
        extends ExtractionDao {

    <R extends C> R execute(F filter);

    void clean(C context);

    default ExtractionLandingFilterVO toTripFilterVO(ExtractionFilterVO source){
        ExtractionLandingFilterVO target = new ExtractionLandingFilterVO();
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
                        }
                    });
        }
        return target;
    }
}

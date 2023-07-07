package net.sumaris.extraction.core.dao;

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

import lombok.NonNull;
import net.sumaris.core.dao.technical.Page;
import net.sumaris.core.model.technical.extraction.IExtractionType;
import net.sumaris.extraction.core.vo.ExtractionContextVO;
import net.sumaris.extraction.core.vo.ExtractionFilterVO;
import net.sumaris.extraction.core.vo.ExtractionResultVO;

import java.util.Set;

/**
 * @author Benoit Lavenier <benoit.lavenier@e-is.pro>*
 */
public interface ExtractionDao<
    C extends ExtractionContextVO,
    F extends ExtractionFilterVO> {
    String TABLE_NAME_PREFIX = "EXT_";
    String SEQUENCE_NAME_SUFFIX = "_SEQ";

    Set<IExtractionType<?, ?>> getManagedTypes();

    <R extends C> R execute(F filter);

    ExtractionResultVO read(@NonNull C context, F filter, Page page);

    <R extends ExtractionContextVO> void clean(R context);
}

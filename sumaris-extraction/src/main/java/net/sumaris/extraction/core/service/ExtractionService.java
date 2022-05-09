package net.sumaris.extraction.core.service;

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
import net.sumaris.core.vo.technical.extraction.ExtractionTableVO;
import net.sumaris.extraction.core.vo.*;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Set;

/**
 * @author peck7 on 17/12/2018.
 */
@Transactional
public interface ExtractionService {

    Set<IExtractionType> getTypes();
    ExtractionContextVO execute(IExtractionType type, @Nullable ExtractionFilterVO filter);

    ExtractionResultVO read(@NonNull IExtractionType type,
                            ExtractionFilterVO filter,
                            Page page);

    @Transactional(isolation = Isolation.READ_COMMITTED, propagation = Propagation.REQUIRES_NEW)
    void clean(ExtractionContextVO context);

    List<ExtractionTableVO> toProductTableVO(ExtractionContextVO source);
}

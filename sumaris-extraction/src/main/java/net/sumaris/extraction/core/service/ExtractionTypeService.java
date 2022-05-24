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
import net.sumaris.core.vo.technical.extraction.ExtractionProductFetchOptions;
import net.sumaris.extraction.core.vo.ExtractionTypeVO;
import net.sumaris.core.vo.technical.extraction.ExtractionTypeFilterVO;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Set;

/**
 * Manage extraction
 *
 * @author benoit.lavenier@e-is.pro
 * @since 1.25.0
 */

public interface ExtractionTypeService {


    @Transactional(readOnly = true)
    List<ExtractionTypeVO> findAll();

    @Transactional(readOnly = true)
    List<ExtractionTypeVO> findAllByFilter(@Nullable ExtractionTypeFilterVO filter, Page page);

    @Transactional(readOnly = true)
    List<ExtractionTypeVO> getLiveTypes();

    @Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
    IExtractionType getByExample(@NonNull IExtractionType source, ExtractionProductFetchOptions fetchOptions);
    @Transactional(readOnly = true)
    IExtractionType getByExample(@NonNull IExtractionType source);

    void registerLiveTypes(Set<IExtractionType> types);
}

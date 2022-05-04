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

import net.sumaris.core.vo.technical.extraction.*;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Create aggregation tables, from a data extraction.
 * @author benoit.lavenier@e-is.pro
 * @since 0.12.0
 */
@Transactional
public interface ExtractionProductService {

    @Transactional(readOnly = true)
    List<ExtractionProductVO> findByFilter(ExtractionProductFilterVO filter, ExtractionProductFetchOptions fetchOptions);

    @Transactional(readOnly = true)
    ExtractionProductVO get(int id, ExtractionProductFetchOptions fetchOptions);

    @Transactional(readOnly = true)
    ExtractionProductVO getByLabel(String label, ExtractionProductFetchOptions fetchOptions);

    @Transactional(readOnly = true)
    Optional<ExtractionProductVO> findById(int id, ExtractionProductFetchOptions fetchOptions);

    @Transactional(readOnly = true)
    Optional<ExtractionProductVO> findByLabel(String label, ExtractionProductFetchOptions fetchOptions);

    @Transactional(readOnly = true)
    List<ExtractionTableColumnVO> getColumnsBySheetName(int id, String sheetName, ExtractionTableColumnFetchOptions fetchOptions);

    @Transactional
    ExtractionProductVO save(ExtractionProductVO source);

    @Transactional
    void delete(int id);

}

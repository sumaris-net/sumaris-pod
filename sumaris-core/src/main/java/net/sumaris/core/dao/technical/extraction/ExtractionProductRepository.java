package net.sumaris.core.dao.technical.extraction;

/*-
 * #%L
 * SUMARiS:: Core
 * %%
 * Copyright (C) 2018 - 2020 SUMARiS Consortium
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

import net.sumaris.core.dao.referential.ReferentialRepository;
import net.sumaris.core.model.technical.extraction.ExtractionProduct;
import net.sumaris.core.vo.technical.extraction.ExtractionProductFetchOptions;
import net.sumaris.core.vo.technical.extraction.ExtractionProductVO;
import net.sumaris.core.vo.technical.extraction.ExtractionTypeFilterVO;

import java.util.Set;

/**
 * Give access to extraction products
 */
public interface ExtractionProductRepository
    extends ReferentialRepository<Integer, ExtractionProduct, ExtractionProductVO, ExtractionTypeFilterVO, ExtractionProductFetchOptions>, ExtractionProductSpecifications {

    default ExtractionProductVO getByLabel(String label) {
        return getByLabel(label, null);
    }

}

package net.sumaris.core.service.referential.taxon;

/*-
 * #%L
 * SUMARiS:: Core
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

import net.sumaris.core.dao.referential.taxon.TaxonNameRepository;
import net.sumaris.core.dao.technical.SortDirection;
import net.sumaris.core.vo.filter.TaxonNameFilterVO;
import net.sumaris.core.vo.referential.TaxonNameVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service("taxonNameService")
public class TaxonNameServiceImpl implements TaxonNameService {

    private static final Logger log = LoggerFactory.getLogger(TaxonNameServiceImpl.class);

    @Autowired
    protected TaxonNameRepository taxonNameRepository;

    @Override
    public List<TaxonNameVO> findByFilter(TaxonNameFilterVO filter, int offset, int size, String sortAttribute, SortDirection sortDirection) {
        return taxonNameRepository.findByFilter(filter, offset, size, sortAttribute, sortDirection);
    }

    @Override
    public List<TaxonNameVO> getAll(boolean withSynonyms) {
        return taxonNameRepository.getAll(withSynonyms);
    }

    @Override
    public List<TaxonNameVO> getAllByTaxonGroup(Integer taxonGroupId) {
        return taxonNameRepository.getAllByTaxonGroupId(taxonGroupId);
    }
}

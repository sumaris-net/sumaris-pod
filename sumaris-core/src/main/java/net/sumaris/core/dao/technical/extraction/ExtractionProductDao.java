package net.sumaris.core.dao.technical.extraction;

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

import net.sumaris.core.dao.cache.CacheNames;
import net.sumaris.core.vo.technical.extraction.ExtractionProductColumnVO;
import net.sumaris.core.vo.technical.extraction.ExtractionProductFilterVO;
import net.sumaris.core.vo.technical.extraction.ExtractionProductVO;
import net.sumaris.core.vo.technical.extraction.ProductFetchOptions;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;

import java.util.List;
import java.util.Optional;

/**
 * @author Benoit Lavenier <benoit.lavenier@e-is.pro>*
 */
public interface ExtractionProductDao {


    @Cacheable(cacheNames = CacheNames.PRODUCTS_BY_FILTER)
    List<ExtractionProductVO> findByFilter(ExtractionProductFilterVO filter, ProductFetchOptions fetchOptions);

    @Cacheable(cacheNames = CacheNames.PRODUCT_BY_LABEL, key = "#label")
    ExtractionProductVO getByLabel(String label, ProductFetchOptions fetchOptions);

    default ExtractionProductVO getByLabel(String label) {
        return getByLabel(label, null);
    }

    //@Cacheable(cacheNames = CacheNames.PRODUCT_BY_LABEL, key = "#label")
    Optional<ExtractionProductVO> get(int id, ProductFetchOptions fetchOptions);

    default Optional<ExtractionProductVO> get(int id) {
        return get(id, null);
    }

    List<ExtractionProductColumnVO> getColumnsByIdAndTableLabel(int id, String tableLabel);

    @Caching(
        evict = {
            @CacheEvict(cacheNames = CacheNames.PRODUCT_BY_LABEL, key = "#source.label", condition = "#source != null && #source.id != null"),
            @CacheEvict(cacheNames = CacheNames.PRODUCTS, allEntries = true),
            @CacheEvict(cacheNames = CacheNames.PRODUCTS_BY_FILTER, allEntries = true),
        },
        put = {
            @CachePut(cacheNames= CacheNames.PRODUCT_BY_LABEL, key="#source.label", condition = "#source != null && #source.label != null")
        }
    )
    ExtractionProductVO save(ExtractionProductVO source);

    @Caching(evict = {
            @CacheEvict(cacheNames = CacheNames.PRODUCT_BY_LABEL, allEntries = true),
            @CacheEvict(cacheNames = CacheNames.PRODUCTS, allEntries = true),
            @CacheEvict(cacheNames = CacheNames.PRODUCTS_BY_FILTER, allEntries = true)
    })
    void delete(int id);

}

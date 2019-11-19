package net.sumaris.core.dao.technical.extraction;

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

    default List<ExtractionProductVO> findByFilter() {
        return findByFilter(null, null);
    }

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

package net.sumaris.core.dao.technical.extraction;

import net.sumaris.core.dao.cache.CacheNames;
import net.sumaris.core.vo.technical.extraction.ExtractionProductVO;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;

import java.util.List;

/**
 * @author Benoit Lavenier <benoit.lavenier@e-is.pro>*
 */
public interface ExtractionProductDao {

    @Cacheable(cacheNames = CacheNames.PRODUCTS)
    List<ExtractionProductVO> getAll();

    @Cacheable(cacheNames = CacheNames.PRODUCT_BY_LABEL, key = "#label")
    ExtractionProductVO getByLabel(String label);

    @Caching(
        evict = {
            @CacheEvict(cacheNames = CacheNames.PRODUCT_BY_LABEL, key = "#source.label", condition = "#source != null && #source.id != null"),
            @CacheEvict(cacheNames = CacheNames.PRODUCTS, allEntries = true)
        },
        put = {
            @CachePut(cacheNames= CacheNames.PRODUCT_BY_LABEL, key="#source.label", condition = "#source != null && #source.label != null")
        }
    )
    ExtractionProductVO save(ExtractionProductVO source);

    @Caching(evict = {
            @CacheEvict(cacheNames = CacheNames.PRODUCT_BY_LABEL, key = "#id"),
            @CacheEvict(cacheNames = CacheNames.PRODUCTS, allEntries = true)
    })

    void delete(int id);
}

/*
@Cacheable(cacheNames = CacheNames.PERSON_BY_PUBKEY, key = "#pubkey", unless="#result==null")
    PersonVO getByPubkeyOrNull(String pubkey);

    List<String> getEmailsByProfiles(List<Integer> userProfiles, List<Integer> statusIds);

    ImageAttachmentVO getAvatarByPubkey(String pubkey);

    boolean isExistsByEmailHash(String hash);

    @Caching(evict = {
            @CacheEvict(cacheNames = CacheNames.PERSON_BY_ID, key = "#id"),
            @CacheEvict(cacheNames = CacheNames.PERSON_BY_PUBKEY, allEntries = true)
    })
 */
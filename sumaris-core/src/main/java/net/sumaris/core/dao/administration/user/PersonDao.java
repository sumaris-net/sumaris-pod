package net.sumaris.core.dao.administration.user;

/*-
 * #%L
 * SUMARiS:: Core
 * %%
 * Copyright (C) 2018 SUMARiS Consortium
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
import net.sumaris.core.dao.technical.SortDirection;
import net.sumaris.core.model.administration.user.Person;
import net.sumaris.core.vo.administration.user.PersonVO;
import net.sumaris.core.vo.data.ImageAttachmentVO;
import net.sumaris.core.vo.filter.PersonFilterVO;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;

import java.util.List;

public interface PersonDao  {

    interface Listener {
        void onSave(PersonVO personVO);
        void onDelete(int id);
    }

    List<PersonVO> findByFilter(PersonFilterVO filter, int offset, int size, String sortAttribute, SortDirection sortDirection);

    Long countByFilter(PersonFilterVO filter);

    @Cacheable(cacheNames = CacheNames.PERSON_BY_ID, key = "#id", unless="#result==null")
    PersonVO get(int id);

    @Cacheable(cacheNames = CacheNames.PERSON_BY_PUBKEY, key = "#pubkey", unless="#result==null")
    PersonVO getByPubkeyOrNull(String pubkey);

    ImageAttachmentVO getAvatarByPubkey(String pubkey);

    boolean isExistsByEmailHash(String hash);

    @Caching(evict = {
            @CacheEvict(cacheNames = CacheNames.PERSON_BY_ID, key = "#id"),
            @CacheEvict(cacheNames = CacheNames.PERSON_BY_PUBKEY, allEntries = true)
    })
    void delete(int id);

    @Caching(put = {
        @CachePut(cacheNames= CacheNames.PERSON_BY_ID, key="#source.id", condition = "#source != null && #source.id != null"),
        @CachePut(cacheNames= CacheNames.PERSON_BY_PUBKEY, key="#source.pubkey", condition = "#source != null && #source.id != null && #source.pubkey != null")
    })
    PersonVO save(PersonVO source);

    PersonVO toPersonVO(Person source);

    void addListener(Listener listener);
}

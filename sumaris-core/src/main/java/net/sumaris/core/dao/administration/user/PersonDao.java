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

import java.util.List;

public interface PersonDao  {

    List<PersonVO> findByFilter(PersonFilterVO filter, int offset, int size, String sortAttribute, SortDirection sortDirection);

    Long countByFilter(PersonFilterVO filter);

    @Cacheable(cacheNames = CacheNames.PERSON_BY_ID, key = "#id")
    PersonVO get(int id);

    @Cacheable(cacheNames = CacheNames.PERSON_BY_PUBKEY, key = "#pubkey")
    PersonVO getByPubkeyOrNull(String pubkey);

    ImageAttachmentVO getAvatarByPubkey(String pubkey);

    boolean isExistsByEmailHash(String hash);

    @CacheEvict(cacheNames = CacheNames.PERSON_BY_ID, key = "#id")
    void delete(int id);

    @CachePut(cacheNames= CacheNames.PERSON_BY_ID, key="#result.id")
    PersonVO save(PersonVO person);

    PersonVO toPersonVO(Person person);
}

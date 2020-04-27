package net.sumaris.core.dao.referential;

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
import net.sumaris.core.model.referential.IReferentialEntity;
import net.sumaris.core.vo.filter.ReferentialFilterVO;
import net.sumaris.core.vo.referential.IReferentialVO;
import net.sumaris.core.vo.referential.ReferentialTypeVO;
import net.sumaris.core.vo.referential.ReferentialVO;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;

import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Root;
import java.util.List;
import java.util.Optional;

public interface ReferentialDao {


    interface QueryVisitor<R, T> {
        Expression<Boolean> apply(CriteriaQuery<R> query, Root<T> root);
    }

    ReferentialVO get(String entityName, int id);

    ReferentialVO get(Class<? extends IReferentialEntity> entityClass, int id);

    @Cacheable(cacheNames = CacheNames.REFERENTIAL_TYPES)
    List<ReferentialTypeVO> getAllTypes();

    List<ReferentialVO> getAllLevels(String entityName);

    ReferentialVO getLevelById(String entityName, int levelId);

    List<ReferentialVO> findByFilter(String entityName,
                                     ReferentialFilterVO filter,
                                     int offset,
                                     int size,
                                     String sortAttribute,
                                     SortDirection sortDirection);

    Long countByFilter(final String entityName, ReferentialFilterVO filter);

    @Cacheable(cacheNames = CacheNames.REFERENTIAL_LEVEL_BY_UNIQUE_LABEL, key = "#entityName+':'+#label", condition = "#entityName == 'LocationLevel'")
    ReferentialVO findByUniqueLabel(String entityName, String label);

    <T extends IReferentialEntity> ReferentialVO toReferentialVO(T source);

    <T extends IReferentialVO, S extends IReferentialEntity> Optional<T> toTypedVO(S source, Class<T> targetClazz);


    @Caching(
            evict = {
                    @CacheEvict(cacheNames = CacheNames.PERSON_BY_ID, key = "#source.id", condition = "#source.entityName == 'Person'"),
                    @CacheEvict(cacheNames = CacheNames.DEPARTMENT_BY_ID, key = "#source.id", condition = "#source.entityName == 'Department'"),
                    @CacheEvict(cacheNames = CacheNames.PMFM_BY_ID, key = "#source.id", condition = "#source.entityName == 'Pmfm'"),
                    @CacheEvict(cacheNames = CacheNames.PMFM_HAS_SUFFIX, allEntries = true, condition = "#source.entityName == 'Pmfm'"),
                    @CacheEvict(cacheNames = CacheNames.PMFM_HAS_PREFIX, allEntries = true, condition = "#source.entityName == 'Pmfm'"),
                    @CacheEvict(cacheNames = CacheNames.PROGRAM_BY_LABEL, key = "#source.id", condition = "#source.entityName == 'Program'"),
                    @CacheEvict(cacheNames = CacheNames.REFERENTIAL_LEVEL_BY_UNIQUE_LABEL, key = "#source.label", condition = "#source.entityName == 'LocationLevel'"),
                    @CacheEvict(cacheNames = CacheNames.PROGRAM_BY_LABEL, key = "#source.label", condition = "#source.entityName == 'Program'"),
                    @CacheEvict(cacheNames = CacheNames.PROGRAM_BY_ID, key = "#source.id", condition = "#source.entityName == 'Program'")
            }
    )
    ReferentialVO save(ReferentialVO source);

    @Caching(evict= {
            @CacheEvict(cacheNames = CacheNames.PERSON_BY_ID, key = "#id", condition = "#entityName == 'Person'"),
            @CacheEvict(cacheNames = CacheNames.DEPARTMENT_BY_ID, key = "#id", condition = "#entityName == 'Department'"),
            @CacheEvict(cacheNames = CacheNames.PMFM_BY_ID, key = "#id", condition = "#entityName == 'Pmfm'"),
            @CacheEvict(cacheNames = CacheNames.PMFM_HAS_SUFFIX, allEntries = true, condition = "#entityName == 'Pmfm'"),
            @CacheEvict(cacheNames = CacheNames.PMFM_HAS_PREFIX, allEntries = true, condition = "#entityName == 'Pmfm'"),
            @CacheEvict(cacheNames = CacheNames.PROGRAM_BY_LABEL, key = "#id", condition = "#entityName == 'Program'"),
            @CacheEvict(cacheNames = CacheNames.REFERENTIAL_LEVEL_BY_UNIQUE_LABEL, allEntries = true, condition = "#entityName == 'LocationLevel'"),
            @CacheEvict(cacheNames = CacheNames.PROGRAM_BY_LABEL, allEntries = true, condition = "#entityName == 'Program'"),
            @CacheEvict(cacheNames = CacheNames.PROGRAM_BY_ID, key = "#id", condition = "#entityName == 'Program'")
    })
    void delete(String entityName, int id);

    Long count(String entityName);

    Long countByLevelId(String entityName, Integer... levelIds);

    <T> TypedQuery<T> createFindQuery(Class<T> entityClass,
                                      ReferentialFilterVO filter,
                                      String sortAttribute,
                                      SortDirection sortDirection,
                                      QueryVisitor<T, T> queryVisitor);
}

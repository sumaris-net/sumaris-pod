package net.sumaris.core.service.referential;

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

import net.sumaris.core.dao.technical.SortDirection;
import net.sumaris.core.model.referential.IReferentialWithStatusEntity;
import net.sumaris.core.vo.filter.IReferentialFilter;
import net.sumaris.core.vo.referential.ReferentialFetchOptions;
import net.sumaris.core.vo.referential.ReferentialTypeVO;
import net.sumaris.core.vo.referential.ReferentialVO;
import org.springframework.lang.Nullable;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

@Transactional
public interface ReferentialService {

    @Transactional(readOnly = true)
    Date getLastUpdateDate();

    @Transactional(readOnly = true)
    List<ReferentialTypeVO> getAllTypes();

    @Transactional(readOnly = true)
    ReferentialVO get(String entityName, int id);

    @Transactional(readOnly = true)
    ReferentialVO get(String entityName, int id, ReferentialFetchOptions fetchOptions);

    @Transactional(readOnly = true)
    ReferentialVO get(Class<? extends IReferentialWithStatusEntity<Integer>> entityClass, int id);

    @Transactional(readOnly = true)

    ReferentialVO get(Class<? extends IReferentialWithStatusEntity<Integer>> entityClass, int id, ReferentialFetchOptions fetchOptions);

    @Transactional(readOnly = true)
    List<ReferentialVO> findByFilter(String entityName, IReferentialFilter filter, int offset, int size);

    @Transactional(readOnly = true)
    List<ReferentialVO> findByFilter(String entityName, IReferentialFilter filter, int offset, int size,
                                     String sortAttribute, SortDirection sortDirection,
                                     ReferentialFetchOptions fetchOptions);

    @Transactional(readOnly = true)
    Long countByFilter(String entityName, @Nullable IReferentialFilter filter);

    @Transactional(readOnly = true)
    ReferentialVO findByUniqueLabel(String entityName, String label);

    @Transactional(readOnly = true)
    List<ReferentialVO> getAllLevels(String entityName);

    @Transactional(readOnly = true)
    ReferentialVO getLevelById(String entityName, int levelId);

    ReferentialVO save(ReferentialVO source);

    List<ReferentialVO> save(List<ReferentialVO> beans);

    void delete(String entityName, int id);

    void delete(String entityName, List<Integer> ids);

    @Transactional(readOnly = true)
    Long count(String entityName);

    @Transactional(readOnly = true)
    Long countByLevelId(String entityName, Integer... levelIds);



}

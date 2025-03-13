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

import net.sumaris.core.dao.technical.SortDirection;
import net.sumaris.core.model.referential.IReferentialEntity;
import net.sumaris.core.vo.filter.IReferentialFilter;
import net.sumaris.core.vo.referential.IReferentialVO;
import net.sumaris.core.vo.referential.ReferentialFetchOptions;
import net.sumaris.core.vo.referential.ReferentialTypeVO;
import net.sumaris.core.vo.referential.ReferentialVO;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Optional;

public interface ReferentialDao {

    ReferentialVO get(String entityName, int id);

    ReferentialVO get(String entityName, int id, ReferentialFetchOptions fetchOptions);

    ReferentialVO get(Class<? extends IReferentialEntity> entityClass, int id);

    ReferentialVO get(Class<? extends IReferentialEntity> entityClass, int id, ReferentialFetchOptions fetchOptions);

    default Date getLastUpdateDate() {
        return getLastUpdateDate(ReferentialEntities.LAST_UPDATE_DATE_ENTITY_NAMES);
    }

    Date getLastUpdateDate(Collection<String> entityNames);

    List<ReferentialTypeVO> getAllTypes();

    List<ReferentialVO> getAllLevels(String entityName);

    ReferentialVO getLevelById(String entityName, int levelId);

    List<ReferentialVO> findByFilter(String entityName,
                                     IReferentialFilter filter,
                                     int offset,
                                     int size,
                                     String sortAttribute,
                                     SortDirection sortDirection,
                                     @Nullable ReferentialFetchOptions fetchOptions);

    Long countByFilter(final String entityName, IReferentialFilter filter);

    List<String> findLabelsByFilter(final String entityName, IReferentialFilter filter);

    Optional<ReferentialVO> findByUniqueLabel(String entityName, String label);

    <T extends IReferentialEntity> ReferentialVO toVO(T source);

    <T extends IReferentialEntity> ReferentialVO toVO(T source, @Nullable ReferentialFetchOptions fetchOptions);

    <T extends IReferentialVO, S extends IReferentialEntity> Optional<T> toTypedVO(S source, Class<T> targetClazz);

    ReferentialVO save(ReferentialVO source);

    void delete(String entityName, int id);

    Long count(String entityName);

    Long countByLevelId(String entityName, Integer... levelIds);

    void clearCache();

    void clearCache(String entityName);

    int getAcquisitionLevelIdByLabel(String label);

    String getAcquisitionLevelLabelById(int id);
}
